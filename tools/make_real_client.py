"""Set up a modern Minecraft version as a "real" client the time machine can open.

    python3 tools/make_real_client.py 1.21.8

Writes ~/minecraft-<version>/play.sh, which starts that version straight from the files the
official launcher already downloaded (versions/, libraries/, assets/), offline, as the old
hand-written scripts do. Nothing is downloaded: play the version once from the launcher first so
its files exist. Then add the directory to RealVersions.KNOWN.

macOS on Apple silicon only, which is what this machine is.
"""
import json
import os
import stat
import sys

MC = os.path.expanduser("~/Library/Application Support/minecraft")
JAVA = {
    21: os.path.join(MC, "runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java"),
}


def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for rule in rules:
        os_rule = rule.get("os", {})
        if os_rule.get("name") not in (None, "osx") or os_rule.get("arch") not in (None, "arm64"):
            continue
        ok = rule["action"] == "allow"
    return ok


def main(version):
    meta = json.load(open(os.path.join(MC, "versions", version, version + ".json")))
    java = JAVA[meta["javaVersion"]["majorVersion"]]
    classpath = []
    for lib in meta["libraries"]:
        name = lib["name"]
        # Intel Mac natives are useless here; the arm64 and "patch" ones are what LWJGL loads.
        if not allowed(lib) or name.endswith(":natives-macos") and "lwjgl" in name:
            continue
        artifact = lib.get("downloads", {}).get("artifact")
        if artifact:
            classpath.append(os.path.join(MC, "libraries", artifact["path"]))
    classpath.append(os.path.join(MC, "versions", version, version + ".jar"))
    missing = [p for p in classpath + [java] if not os.path.exists(p)]
    if missing:
        sys.exit("Missing (play this version once from the launcher first):\n  " + "\n  ".join(missing))

    game_dir = os.path.expanduser("~/minecraft-" + version)
    natives = os.path.join(game_dir, "natives")
    os.makedirs(natives, exist_ok=True)
    script = os.path.join(game_dir, "play.sh")
    q = lambda s: '"' + s.replace('"', '\\"') + '"'
    with open(script, "w") as f:
        f.write("#!/bin/bash\n")
        f.write("# Minecraft %s -- Java %d, written by timemachine-mod/tools/make_real_client.py\n"
                % (version, meta["javaVersion"]["majorVersion"]))
        f.write("cd %s\n" % q(game_dir))
        f.write("exec %s -XstartOnFirstThread -Xmx2G -Xms1G \\\n" % q(java))
        f.write("  -Djava.library.path=%s -Dorg.lwjgl.system.SharedLibraryExtractPath=%s \\\n" % (q(natives), q(natives)))
        f.write("  -Djna.tmpdir=%s -Dio.netty.native.workdir=%s \\\n" % (q(natives), q(natives)))
        f.write("  -Xdock:name=%s \\\n" % q("Minecraft " + version))
        f.write("  -cp %s \\\n" % q(":".join(classpath)))
        f.write("  %s --username Elduin --version %s --gameDir %s --assetsDir %s --assetIndex %s \\\n"
                % (meta["mainClass"], version, q(game_dir), q(os.path.join(MC, "assets")), meta["assetIndex"]["id"]))
        f.write('  --uuid 00000000000000000000000000000000 --accessToken 0 --userType legacy --versionType release "$@"\n')
    os.chmod(script, os.stat(script).st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    print("wrote", script, "with", len(classpath), "jars")


if __name__ == "__main__":
    main(sys.argv[1])

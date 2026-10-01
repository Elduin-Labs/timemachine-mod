"""Set up a downloaded Minecraft version as a "real" client the time machine can open.

    python3 tools/make_real_client.py 1.21.8 [1.16.5 ...]

Writes ~/minecraft-<version>/play.sh, which starts that version straight from the files the
official launcher already downloaded (versions/, libraries/, assets/), offline, the same way the
older hand-written scripts do. Nothing is downloaded: play the version once from the launcher
first so its files exist. Then add the directory to RealVersions.KNOWN.

Handles both version-file formats: 1.13+ ("arguments") and 1.6-1.12 ("minecraftArguments").
Versions older than 1.6 keep their hand-written scripts — they need the legacy resources folder
and the launchwrapper, which this does not set up.

macOS only. Java 8 versions run on the launcher's Intel Java under Rosetta (their LWJGL has no
Apple silicon build); Java 17+ versions run natively.
"""
import json
import os
import stat
import sys
import zipfile

MC = os.path.expanduser("~/Library/Application Support/minecraft")


def java_for(component):
    runtime = os.path.join(MC, "runtime", component)
    for platform in ("mac-os-arm64", "mac-os"):
        java = os.path.join(runtime, platform, component, "jre.bundle", "Contents", "Home", "bin", "java")
        if os.path.exists(java):
            return java, platform == "mac-os-arm64"
    sys.exit("No Java runtime '%s' — play this version once from the launcher first." % component)


def allowed(rules):
    if not rules:
        return True
    ok = False
    for rule in rules:
        if "features" in rule:
            continue
        os_rule = rule.get("os", {})
        if os_rule.get("name") not in (None, "osx") or "arch" in os_rule:
            continue
        ok = rule["action"] == "allow"
    return ok


def main(version):
    meta = json.load(open(os.path.join(MC, "versions", version, version + ".json")))
    if "arguments" not in meta and "minecraftArguments" not in meta:
        sys.exit("%s is too old for this script" % version)
    if meta["assetIndex"]["id"] in ("legacy", "pre-1.6"):
        sys.exit("%s uses the pre-1.6 resources folder; write its script by hand" % version)

    component = meta.get("javaVersion", {}).get("component", "jre-legacy")
    java, arm64 = java_for(component)
    game_dir = os.path.expanduser("~/minecraft-" + version)
    natives = os.path.join(game_dir, "natives")
    os.makedirs(natives, exist_ok=True)

    classpath = []
    lwjgl3 = False
    for lib in meta["libraries"]:
        name = lib["name"]
        if not allowed(lib.get("rules")):
            continue
        if name.startswith("org.lwjgl:lwjgl:3"):
            lwjgl3 = True
        downloads = lib.get("downloads", {})
        # New-style natives are ordinary libraries: keep only the ones for this Java's CPU.
        if ":natives-macos" in name and name.startswith("org.lwjgl"):
            if arm64 != name.endswith("-arm64") and not name.endswith("-patch"):
                continue
        if "artifact" in downloads:
            classpath.append(os.path.join(MC, "libraries", downloads["artifact"]["path"]))
        # Old-style natives are a classifier jar that has to be unpacked next to the game.
        classifier = lib.get("natives", {}).get("osx")
        if classifier:
            jar = os.path.join(MC, "libraries", downloads["classifiers"][classifier]["path"])
            with zipfile.ZipFile(jar) as z:
                for entry in z.namelist():
                    if not entry.startswith("META-INF/") and not entry.endswith("/"):
                        with open(os.path.join(natives, os.path.basename(entry)), "wb") as out:
                            out.write(z.read(entry))
    classpath.append(os.path.join(MC, "versions", version, version + ".jar"))
    missing = [p for p in classpath if not os.path.exists(p)]
    if missing:
        sys.exit("Missing (play this version once from the launcher first):\n  " + "\n  ".join(missing))

    values = {
        "auth_player_name": "Elduin", "version_name": version, "game_directory": game_dir,
        "assets_root": os.path.join(MC, "assets"), "game_assets": os.path.join(MC, "assets"),
        "assets_index_name": meta["assetIndex"]["id"], "auth_uuid": "0" * 32, "auth_access_token": "0",
        "auth_session": "0", "user_type": "legacy", "version_type": "release", "user_properties": "{}",
        "clientid": "0", "auth_xuid": "0",
    }
    if "arguments" in meta:
        game_args = [a for a in meta["arguments"]["game"] if isinstance(a, str)]
    else:
        game_args = meta["minecraftArguments"].split()
    for i, arg in enumerate(game_args):
        for key, value in values.items():
            arg = arg.replace("${%s}" % key, value)
        game_args[i] = arg

    q = lambda s: '"' + s.replace('"', '\\"') + '"'
    script = os.path.join(game_dir, "play.sh")
    with open(script, "w") as f:
        f.write("#!/bin/bash\n")
        f.write("# Minecraft %s -- %s, written by timemachine-mod/tools/make_real_client.py\n"
                % (version, "Java %s" % meta.get("javaVersion", {}).get("majorVersion", 8)
                   + ("" if arm64 else " (x86_64 via Rosetta)")))
        f.write("cd %s\n" % q(game_dir))
        # LWJGL 3 needs the first thread on macOS; LWJGL 2 hangs if it gets it.
        f.write("exec %s %s-Xmx2G -Xms1G \\\n" % (q(java), "-XstartOnFirstThread " if lwjgl3 else ""))
        f.write("  -Djava.library.path=%s -Dorg.lwjgl.system.SharedLibraryExtractPath=%s \\\n" % (q(natives), q(natives)))
        f.write("  -Djna.tmpdir=%s -Dio.netty.native.workdir=%s \\\n" % (q(natives), q(natives)))
        f.write("  -Xdock:name=%s \\\n" % q("Minecraft " + version))
        f.write("  -cp %s \\\n" % q(":".join(classpath)))
        f.write("  %s %s \"$@\"\n" % (meta["mainClass"], " ".join(q(a) for a in game_args)))
    os.chmod(script, os.stat(script).st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    print("wrote", script, "with", len(classpath), "jars", "(arm64)" if arm64 else "(x86_64)")


if __name__ == "__main__":
    for v in sys.argv[1:]:
        main(v)

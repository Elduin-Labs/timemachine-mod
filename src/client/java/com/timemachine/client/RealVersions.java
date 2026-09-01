package com.timemachine.client;

import com.timemachine.Timeline;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The old Minecraft clients that are actually installed on this computer, and how to start one.
 *
 * <p>The dial can send the world back to any of the fifty-odd versions in {@link Timeline}, but
 * that is a costume: the game you are playing is still 1.21.11 pretending. This class is the
 * other half of the trick — for the handful of versions that exist on disk as a real, playable
 * client, it can close 1.21.11 and open the genuine article.
 *
 * <p>Those clients live one per directory in the user's home folder, each with a hand-written
 * launch script beside its saves (see the {@code minecraft-<version>} dirs). Nothing here
 * downloads or installs anything; a version that isn't already set up simply isn't offered, and
 * on a machine with none of them the "really go there" half of the dial stays dark.
 */
public final class RealVersions {

    /**
     * One installed client: which directory it lives in, which script starts it, and where it
     * sits on the dial.
     *
     * <p>The timeline is coarser than reality — it carries one Beta 1.9 entry, not six
     * pre-releases — so several installs can legitimately claim the same stop. The later install
     * wins, which is why beta19pre3 is listed after beta19pre2.
     */
    private record Candidate(String directory, String script, String timelineId) { }

    private static final List<Candidate> KNOWN = List.of(
            new Candidate("minecraft-alpha",      "play-alpha.sh", "a1.2.6"),
            new Candidate("minecraft-beta10",     "play.sh",       "b1.0"),
            new Candidate("minecraft-beta13",     "play.sh",       "b1.3"),
            new Candidate("minecraft-beta15",     "play.sh",       "b1.5"),
            new Candidate("minecraft-beta173",    "play.sh",       "b1.7.3"),
            new Candidate("minecraft-beta181",    "play.sh",       "b1.8.1"),
            new Candidate("minecraft-beta19pre2", "play.sh",       "b1.9-pre6"),
            new Candidate("minecraft-beta19pre3", "play.sh",       "b1.9-pre6"),
            new Candidate("minecraft-1.9",        "play.sh",       "1.9.4"),
            new Candidate("minecraft-1.12.2",     "play.sh",       "1.12.2"));

    /** An installed client, resolved to a real file on disk. */
    public record Installed(int index, String name, Path script, Path directory) { }

    private static List<Installed> cache;

    private RealVersions() { }

    /**
     * Every installed client, oldest first. Scanned once — a version cannot appear or vanish
     * while the game is running, and this is read every frame the dial is open.
     */
    public static List<Installed> all() {
        if (cache != null) {
            return cache;
        }
        List<Installed> found = new ArrayList<>();
        try {
            Path home = Path.of(System.getProperty("user.home"));
            for (Candidate candidate : KNOWN) {
                Path directory = home.resolve(candidate.directory());
                Path script = directory.resolve(candidate.script());
                if (!Files.isRegularFile(script) || !Files.isExecutable(script)) {
                    continue;
                }
                int index = Timeline.indexOf(candidate.timelineId());
                if (index < 0) {
                    continue;
                }
                // Later entries win the stop they share, so drop the earlier claim.
                found.removeIf(existing -> existing.index() == index);
                found.add(new Installed(index, Timeline.get(index).name(), script, directory));
            }
        } catch (Exception ignored) {
            // A home directory we can't read is the same as no versions installed.
        }
        found.sort((a, b) -> Integer.compare(a.index(), b.index()));
        cache = Collections.unmodifiableList(found);
        return cache;
    }

    /** @return true if any real client is installed at all. */
    public static boolean any() {
        return !all().isEmpty();
    }

    /**
     * The installed client closest to a stop on the dial, or null if none are installed.
     * Ties go to the older one — travelling too far back is more in the spirit than not far enough.
     */
    public static Installed nearestTo(int index) {
        Installed best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Installed installed : all()) {
            int distance = Math.abs(installed.index() - index);
            if (distance < bestDistance) {
                best = installed;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Start an old client. Its output goes to {@code game.log} beside its saves, the same place
     * the desktop shortcut for that version puts it, so a failed launch is diagnosable afterwards.
     *
     * <p>The new process is deliberately not tied to this one's streams: the caller is about to
     * quit, and a child sharing a dying JVM's stdout dies with it.
     *
     * @return true if the process started
     */
    public static boolean launch(Installed installed) {
        try {
            File log = installed.directory().resolve("game.log").toFile();
            new ProcessBuilder("/bin/bash", installed.script().toString())
                    .directory(installed.directory().toFile())
                    .redirectOutput(log)
                    .redirectErrorStream(true)
                    .start();
            return true;
        } catch (Exception failed) {
            return false;
        }
    }
}

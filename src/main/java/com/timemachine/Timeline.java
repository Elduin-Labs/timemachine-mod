package com.timemachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every version the dial can land on, oldest first, from the four-day prototype Notch put on
 * TIGSource in May 2009 through to the version this mod is built against.
 *
 * <p>The list is the spine of the whole mod: an era is nothing but an index into it, and every
 * question the mod asks — "does the Nether exist yet?", "had shields been invented?" — is a
 * comparison of two indices. Items, blocks and mobs are dated the same way (see
 * {@link ContentCalendar}), so "not invented yet" is just {@code addedIn(item) > current}.
 *
 * <p>Point releases that added no content are left out. There is no interesting difference
 * between 1.16.4 and 1.16.5 to travel between, and a dial with nine hundred identical entries is
 * a worse dial.
 */
public final class Timeline {

    /**
     * One stop on the dial.
     *
     * @param id      stable key, used by {@link Feature}, the command and the save file
     * @param name    what the dial shows
     * @param date    release date, YYYY-MM-DD, shown under the name
     * @param era     Pre-Classic / Classic / Indev / Infdev / Alpha / Beta / Release
     * @param note    one line on what this version is remembered for
     */
    public record Version(String id, String name, String date, String era, String note) {
        /** Index of this version in the timeline. */
        public int index() {
            return INDEX_BY_ID.get(id);
        }
    }

    private static final List<Version> VERSIONS = new ArrayList<>();
    private static final Map<String, Integer> INDEX_BY_ID = new HashMap<>();

    private static void add(String id, String name, String date, String era, String note) {
        INDEX_BY_ID.put(id, VERSIONS.size());
        VERSIONS.add(new Version(id, name, date, era, note));
    }

    static {
        add("rd-132211", "rd-132211", "2009-05-13", "Pre-Classic",
                "Cave Game. Grass, stone, and a world 256 blocks wide.");

        add("c0.0.11a", "Classic 0.0.11a", "2009-05-18", "Classic",
                "Building only. Water and lava flow forever.");
        add("c0.0.13a", "Classic 0.0.13a", "2009-05-31", "Classic",
                "Multiplayer. Everyone is Steve and nobody can die.");
        add("c0.24_st", "Survival Test 0.24", "2009-09-01", "Classic",
                "Health, mobs and mining arrive. Blocks finally cost something.");
        add("c0.30", "Classic 0.30", "2009-11-10", "Classic",
                "The last Classic. Creative mode as it was first meant.");

        add("in-20100128", "Indev 0.31", "2010-01-28", "Indev",
                "Crafting, furnaces, tools, chests. Minecraft becomes a game.");
        add("in-20100223", "Indev 20100223", "2010-02-23", "Indev",
                "Biome-flavoured finite worlds, before they went infinite.");

        add("inf-20100227", "Infdev 20100227", "2010-02-27", "Infdev",
                "The world stops having edges.");
        add("inf-20100625", "Infdev 20100625", "2010-06-25", "Infdev",
                "Caves, ravines and dungeons under the endless map.");

        add("a1.0.0", "Alpha 1.0.0", "2010-06-30", "Alpha",
                "Alpha begins. Ores, mobs, day and night.");
        add("a1.0.4", "Alpha 1.0.4", "2010-08-04", "Alpha",
                "Boats. Fishing. Sneaking.");
        add("a1.0.11", "Alpha 1.0.11", "2010-08-20", "Alpha",
                "Survival multiplayer, at last.");
        add("a1.0.14", "Alpha 1.0.14", "2010-08-30", "Alpha",
                "Minecarts, powered rails, note blocks in the works.");
        add("a1.1.0", "Alpha 1.1.0", "2010-09-13", "Alpha",
                "Ladders, sugar cane, tall grass.");
        add("a1.1.2", "Alpha 1.1.2", "2010-09-19", "Alpha",
                "Cake, and the first of the food that isn't pork.");
        add("a1.2.0", "Alpha 1.2.0", "2010-10-30", "Alpha",
                "Halloween Update. The Nether opens.");
        add("a1.2.2", "Alpha 1.2.2", "2010-11-04", "Alpha",
                "Dyes, wool colours, and a sky that finally has fog.");
        add("a1.2.6", "Alpha 1.2.6", "2010-12-03", "Alpha",
                "The last Alpha. Snow, ice and the first winter.");

        add("b1.0", "Beta 1.0", "2010-12-20", "Beta",
                "Beta begins. The game goes on sale properly.");
        add("b1.2", "Beta 1.2", "2011-01-13", "Beta",
                "Lapis, dispensers, note blocks, cocoa.");
        add("b1.3", "Beta 1.3", "2011-02-22", "Beta",
                "Beds. Sleeping through the night becomes possible.");
        add("b1.4", "Beta 1.4", "2011-03-31", "Beta",
                "Wolves. Cookies. A dog that follows you home.");
        add("b1.5", "Beta 1.5", "2011-04-19", "Beta",
                "Weather, achievements, and the first rain.");
        add("b1.6", "Beta 1.6", "2011-05-25", "Beta",
                "Maps, tall grass, and trapdoors.");
        add("b1.7.3", "Beta 1.7.3", "2011-07-08", "Beta",
                "Pistons. The version half the internet still calls the best one.");
        add("b1.8.1", "Beta 1.8.1", "2011-09-19", "Beta",
                "Adventure Update. Hunger, sprinting, XP, endermen, ravines.");
        add("b1.9-pre6", "Beta 1.9 pre-6", "2011-11-08", "Beta",
                "The long pre-release run into release. Villages get villagers.");

        add("1.0.0", "1.0.0", "2011-11-18", "Release",
                "Minecraft is released. The End, the dragon, brewing, enchanting.");
        add("1.1", "1.1", "2012-01-12", "Release",
                "Spawn eggs, golden apples that matter, bow enchantments.");
        add("1.2.5", "1.2.5", "2012-04-04", "Release",
                "The world grows to 256 blocks tall. Jungles and ocelots.");
        add("1.3.2", "1.3.2", "2012-08-15", "Release",
                "Trading, tripwire, ender chests, and singleplayer becomes multiplayer.");
        add("1.4.7", "1.4.7", "2013-01-09", "Release",
                "Pretty Scary Update. The Wither, anvils, beacons, item frames.");
        add("1.5.2", "1.5.2", "2013-04-25", "Release",
                "Redstone Update. Hoppers, droppers, comparators, quartz.");
        add("1.6.4", "1.6.4", "2013-09-19", "Release",
                "Horse Update. Horses, leads, name tags, carpets.");
        add("1.7.10", "1.7.10", "2014-06-26", "Release",
                "World of Color. Every biome redrawn, stained glass, acacia.");
        add("1.8.9", "1.8.9", "2015-12-09", "Release",
                "Bountiful Update. Slime blocks, armour stands, granite, rabbits.");
        add("1.9.4", "1.9.4", "2016-05-10", "Release",
                "Combat Update. Attack cooldown, off-hand, shields, elytra.");
        add("1.10.2", "1.10.2", "2016-06-23", "Release",
                "Frostburn. Polar bears, magma blocks, structure blocks.");
        add("1.11.2", "1.11.2", "2016-12-21", "Release",
                "Exploration Update. Llamas, shulker boxes, woodland mansions.");
        add("1.12.2", "1.12.2", "2017-09-18", "Release",
                "World of Color. Concrete, glazed terracotta, parrots.");
        add("1.13.2", "1.13.2", "2018-10-22", "Release",
                "Update Aquatic. Swimming, tridents, and the Flattening.");
        add("1.14.4", "1.14.4", "2019-07-19", "Release",
                "Village & Pillage. Every village rebuilt, lanterns, crossbows.");
        add("1.15.2", "1.15.2", "2020-01-21", "Release",
                "Buzzy Bees. Bees, hives and honey.");
        add("1.16.5", "1.16.5", "2021-01-15", "Release",
                "Nether Update. Netherite, five nether biomes, piglins.");
        add("1.17.1", "1.17.1", "2021-07-06", "Release",
                "Caves & Cliffs I. Copper, amethyst, deepslate, axolotls, goats.");
        add("1.18.2", "1.18.2", "2022-02-28", "Release",
                "Caves & Cliffs II. World height doubles, terrain redrawn.");
        add("1.19.4", "1.19.4", "2023-03-14", "Release",
                "The Wild Update. Deep dark, the Warden, mangroves, allays.");
        add("1.20.6", "1.20.6", "2024-04-29", "Release",
                "Trails & Tales. Archaeology, cherry groves, camels, armadillos.");
        add("1.21", "1.21", "2024-06-13", "Release",
                "Tricky Trials. Trial chambers, the mace, breezes, the crafter.");
        add("1.21.4", "1.21.4", "2024-12-03", "Release",
                "The Garden Awakens. Pale gardens and the creaking.");
        add("1.21.5", "1.21.5", "2025-03-25", "Release",
                "Spring to Life. Fireflies, wildflowers, leaf litter.");
        add("1.21.8", "1.21.8", "2025-07-17", "Release",
                "Chase the Skies. Happy ghasts you can ride.");
        add("1.21.11", "1.21.11", "2025-12-09", "Release",
                "The present. Where you started.");
    }

    /** Index of the newest version — where an untouched world sits. */
    public static final int PRESENT = VERSIONS.size() - 1;

    private Timeline() {
    }

    public static List<Version> all() {
        return VERSIONS;
    }

    public static int size() {
        return VERSIONS.size();
    }

    public static Version get(int index) {
        return VERSIONS.get(clamp(index));
    }

    public static int clamp(int index) {
        return Math.max(0, Math.min(PRESENT, index));
    }

    /** @return the index of {@code id}, or -1 if no such version is on the dial. */
    public static int indexOf(String id) {
        return INDEX_BY_ID.getOrDefault(id, -1);
    }

    /**
     * Index of {@code id}, blowing up if it is missing. For constants written into this mod's own
     * source — a typo in a {@link Feature} or {@link ContentCalendar} entry should fail at class
     * load, not silently date something to the beginning of time.
     */
    static int require(String id) {
        Integer index = INDEX_BY_ID.get(id);
        if (index == null) {
            throw new IllegalArgumentException("no such version on the timeline: " + id);
        }
        return index;
    }
}

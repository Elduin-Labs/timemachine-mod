package com.timemachine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When each item and each mob was invented.
 *
 * <p>A thousand items cannot be dated one line at a time, and most of them do not need to be:
 * Minecraft names things by the update that added them. Everything with {@code deepslate} in its
 * id arrived in Caves &amp; Cliffs; everything with {@code crimson} arrived with the Nether
 * Update. So the calendar is a list of rules, newest first, and the first one that matches wins.
 * That ordering is load-bearing — it means an explicit newer entry always beats an older
 * substring rule, and only the reverse case (an ancient item swept up by a modern rule, like
 * {@code soul_sand} under {@code soul_}) needs a hand-written exception, which is what the block
 * at the top of {@link #RULES} is for.
 *
 * <p>Anything the rules do not recognise is dated to Indev, when crafting and most of the basic
 * palette appeared. That is the conservative answer: unknown things survive every trip except one
 * into the first eight months of the game's life, so a gap in this table can never wrongly
 * confiscate something in the eras people actually visit.
 *
 * <p>Items outside the {@code minecraft} namespace — this mod's own Time Machine, anything from
 * another mod — are exempt entirely. They have no place in Minecraft's history, and confiscating
 * the Time Machine would strand you in the Stone Age with no way home.
 */
public final class ContentCalendar {

    private ContentCalendar() {
    }

    // ------------------------------------------------------------------ rule plumbing

    private enum Match {EXACT, CONTAINS, PREFIX, SUFFIX}

    private record Rule(Match match, String needle, int index) {
        boolean test(String path) {
            return switch (match) {
                case EXACT -> path.equals(needle);
                case CONTAINS -> path.contains(needle);
                case PREFIX -> path.startsWith(needle);
                case SUFFIX -> path.endsWith(needle);
            };
        }
    }

    private static final List<Rule> RULES = new ArrayList<>();
    private static final Map<String, Integer> ENTITY_DATES = new HashMap<>();

    /** Everything unrecognised is assumed to be as old as crafting itself. */
    private static final int DEFAULT_ITEM = Timeline.require("in-20100128");
    /** Unrecognised mobs are assumed to date from the start of Alpha. */
    private static final int DEFAULT_ENTITY = Timeline.require("a1.0.0");

    private static String current;

    /** Opens a group; every {@code exact}/{@code has}/... after this dates to {@code versionId}. */
    private static void in(String versionId) {
        current = versionId;
    }

    private static void exact(String... paths) {
        int index = Timeline.require(current);
        for (String path : paths) {
            RULES.add(new Rule(Match.EXACT, path, index));
        }
    }

    private static void has(String... needles) {
        int index = Timeline.require(current);
        for (String needle : needles) {
            RULES.add(new Rule(Match.CONTAINS, needle, index));
        }
    }

    private static void prefix(String... needles) {
        int index = Timeline.require(current);
        for (String needle : needles) {
            RULES.add(new Rule(Match.PREFIX, needle, index));
        }
    }

    private static void suffix(String... needles) {
        int index = Timeline.require(current);
        for (String needle : needles) {
            RULES.add(new Rule(Match.SUFFIX, needle, index));
        }
    }

    private static void mobs(String versionId, String... ids) {
        int index = Timeline.require(versionId);
        for (String id : ids) {
            ENTITY_DATES.put(id, index);
        }
    }

    // ------------------------------------------------------------------ the calendar

    static {
        // --- Exceptions: old things that a newer substring rule below would otherwise catch.
        in("a1.2.0");
        exact("soul_sand", "glowstone", "glowstone_dust");
        in("in-20100128");
        exact("chainmail_helmet", "chainmail_chestplate", "chainmail_leggings",
                "chainmail_boots");
        in("c0.30");
        exact("white_wool");             // the only wool colour that predates the dyes
        in("1.17.1");
        exact("sculk_sensor");           // the only sculk block that predates the deep dark
        in("b1.6");
        exact("oak_trapdoor");           // every other trapdoor is 1.13 or later
        in("1.8.9");
        exact("iron_trapdoor");

        // --- 1.21.8 Chase the Skies
        in("1.21.8");
        has("happy_ghast", "dried_ghast", "harness");

        // --- 1.21.5 Spring to Life
        in("1.21.5");
        has("firefly_bush", "wildflowers", "leaf_litter", "cactus_flower", "dry_grass", "bush");
        exact("music_disc_tears", "music_disc_lava_chicken", "blue_egg", "brown_egg");

        // --- 1.21.4 The Garden Awakens
        in("1.21.4");
        has("pale_oak", "pale_moss", "pale_hanging_moss", "resin", "creaking");

        // --- 1.21 Tricky Trials
        in("1.21");
        has("trial_", "breeze", "ominous", "wind_charge", "copper_bulb", "copper_grate",
                "copper_door", "copper_trapdoor", "chiseled_copper", "copper_chest", "tuff_",
                "polished_tuff", "bogged", "creaking");
        exact("mace", "heavy_core", "vault", "crafter", "breeze_rod", "wind_charge",
                "music_disc_creator", "music_disc_creator_music_box", "music_disc_precipice",
                "music_disc_relic", "flow_banner_pattern", "guster_banner_pattern",
                "field_masoned_banner_pattern", "bordure_indented_banner_pattern");

        // --- 1.20 Trails & Tales, and the Armored Paws drop
        in("1.20.6");
        has("cherry", "sniffer", "torchflower", "pitcher", "suspicious_", "decorated_pot",
                "calibrated_sculk", "smithing_template", "armor_trim", "netherite_upgrade",
                "armadillo", "wolf_armor", "bamboo_", "camel", "chiseled_bookshelf",
                "hanging_sign", "pottery_sherd");
        exact("brush", "sniffer_egg", "piglin_head");

        // --- 1.19 The Wild Update
        in("1.19.4");
        has("mangrove", "sculk", "allay", "frog", "tadpole", "warden", "echo_shard",
                "recovery_compass", "reinforced_deepslate", "froglight", "packed_mud", "mud_",
                "goat_horn");
        exact("mud", "disc_fragment_5", "music_disc_5", "music_disc_otherside");

        // --- 1.17 Caves & Cliffs, part one
        in("1.17.1");
        has("copper", "amethyst", "deepslate", "dripstone", "dripleaf", "azalea", "moss_",
                "glow_", "candle", "axolotl", "goat", "powder_snow", "cave_vines",
                "hanging_roots", "rooted_dirt", "raw_iron", "raw_gold", "raw_copper", "bundle",
                "spore_blossom", "lightning_rod", "tinted_glass", "spyglass");
        exact("calcite", "tuff", "smooth_basalt", "lightning_rod", "candle", "small_dripleaf",
                "big_dripleaf", "pointed_dripstone", "sculk_sensor");

        // --- 1.16 Nether Update
        in("1.16.5");
        has("netherite", "ancient_debris", "crimson", "warped", "blackstone", "basalt",
                "shroomlight", "twisting_vines", "weeping_vines", "nether_sprouts",
                "nether_gold_ore", "piglin", "hoglin", "zoglin", "strider", "respawn_anchor",
                "lodestone", "soul_", "quartz_bricks", "polished_blackstone");
        exact("chain", "target", "music_disc_pigstep", "netherite_scrap", "crying_obsidian",
                "gilded_blackstone", "nether_sprouts", "warped_fungus_on_a_stick");

        // --- 1.15 Buzzy Bees
        in("1.15.2");
        has("honey", "honeycomb", "bee_");
        exact("bee_nest", "beehive", "bee_spawn_egg");

        // --- 1.14 Village & Pillage
        in("1.14.4");
        has("lantern", "campfire", "grindstone", "stonecutter", "cartography_table",
                "fletching_table", "smithing_table", "loom", "composter", "scaffolding",
                "lectern", "jigsaw", "sweet_berr", "crossbow", "pillager", "ravager", "panda",
                "fox_", "wandering_trader", "cat_spawn_egg", "leather_horse_armor", "barrel",
                "smoker", "blast_furnace", "bell");
        exact("bamboo", "bamboo_sapling", "suspicious_stew", "cornflower",
                "lily_of_the_valley", "wither_rose", "jigsaw", "creeper_banner_pattern",
                "skull_banner_pattern", "flower_banner_pattern", "mojang_banner_pattern",
                "globe_banner_pattern", "piglin_banner_pattern", "bell");

        // --- 1.13 Update Aquatic
        in("1.13.2");
        has("trident", "nautilus", "heart_of_the_sea", "conduit", "phantom", "turtle", "kelp",
                "seagrass", "sea_pickle", "coral", "blue_ice", "dolphin", "pufferfish",
                "tropical_fish", "drowned", "stripped_", "prismarine_stairs", "prismarine_slab",
                "scute");
        exact("cod", "salmon", "cooked_cod", "cooked_salmon", "debug_stick",
                "phantom_membrane", "turtle_egg", "dried_kelp", "dried_kelp_block");
        // Every trapdoor but oak's and iron's, both of which are excepted above.
        suffix("_trapdoor");

        // --- 1.12 World of Color
        in("1.12.2");
        has("concrete", "glazed_terracotta", "parrot");
        exact("knowledge_book", "totem_of_undying");

        // --- 1.11 Exploration Update
        in("1.11.2");
        has("shulker_box", "llama", "observer", "evoker", "vex", "vindicator");
        exact("totem_of_undying", "iron_nugget", "observer", "shulker_box");

        // --- 1.10 Frostburn
        in("1.10.2");
        exact("magma_block", "nether_wart_block", "red_nether_bricks", "bone_block",
                "structure_void", "polar_bear_spawn_egg", "husk_spawn_egg", "stray_spawn_egg");

        // --- 1.9 Combat Update
        in("1.9.4");
        has("chorus", "purpur", "shulker", "end_rod", "end_crystal", "beetroot", "frosted_ice",
                "spectral_arrow", "tipped_arrow", "lingering_potion", "dragon_breath",
                "structure_block");
        exact("shield", "elytra", "end_stone_bricks", "dirt_path");

        // --- 1.8 Bountiful Update
        in("1.8.9");
        has("prismarine", "sea_lantern", "armor_stand", "slime_block", "banner", "red_sandstone",
                "granite", "diorite", "andesite", "coarse_dirt", "rabbit", "mutton", "guardian",
                "endermite", "barrier");
        exact("wet_sponge", "spruce_door", "birch_door", "jungle_door", "acacia_door",
                "dark_oak_door", "iron_trapdoor", "mossy_stone_bricks");

        // --- 1.7 The Update that Changed the World
        in("1.7.10");
        has("stained_glass", "acacia", "dark_oak", "packed_ice", "podzol", "red_sand");
        exact("sunflower", "lilac", "rose_bush", "peony", "large_fern", "tall_grass");

        // --- 1.6 Horse Update
        in("1.6.4");
        has("horse", "hay_block", "coal_block", "terracotta", "carpet", "name_tag", "donkey",
                "mule");
        exact("lead");

        // --- 1.5 Redstone Update
        in("1.5.2");
        has("hopper", "dropper", "comparator", "daylight_detector", "quartz", "activator_rail",
                "trapped_chest", "redstone_block", "weighted_pressure_plate", "nether_brick");
        exact("redstone_block", "tripwire_hook");

        // --- 1.4 Pretty Scary Update
        in("1.4.7");
        has("anvil", "beacon", "wither", "item_frame", "carrot", "potato", "pumpkin_pie",
                "enchanted_book", "nether_star", "command_block");
        exact("witch_spawn_egg", "bat_spawn_egg", "flower_pot", "beacon");

        // --- 1.3 The one where singleplayer became multiplayer
        in("1.3.2");
        has("emerald", "tripwire", "ender_chest", "cocoa", "writable_book", "written_book");

        // --- 1.2 Jungles
        in("1.2.5");
        has("jungle", "redstone_lamp", "ocelot");
        exact("iron_golem_spawn_egg", "villager_spawn_egg");

        // --- 1.1 Spawn eggs, in general
        in("1.1");
        suffix("_spawn_egg");
        in("1.1");
        exact("golden_apple", "enchanted_golden_apple");

        // --- 1.0.0 Release: the End, brewing, enchanting
        in("1.0.0");
        has("potion", "brewing", "cauldron", "blaze", "ghast_tear", "magma_cream",
                "glistering_melon", "fermented_spider_eye", "spider_eye", "nether_wart",
                "end_stone", "end_portal", "dragon_egg", "eye_of_ender", "ender_eye",
                "enchanting_table", "glass_bottle", "brewing_stand", "mooshroom",
                "mushroom_block");
        exact("experience_bottle", "nether_brick_fence");

        // --- Beta 1.8, the Adventure Update
        in("b1.8.1");
        has("melon", "ender_pearl", "glass_pane", "iron_bars", "stone_brick", "mycelium",
                "brick_stairs", "fence_gate", "enderman", "silverfish", "cave_spider");
        exact("vine", "lily_pad", "brick_slab");

        // --- Beta 1.7.3, the one people still play
        in("b1.7.3");
        exact("piston", "sticky_piston", "shears");

        // --- Beta 1.6
        in("b1.6");
        exact("map", "filled_map", "dead_bush");

        // --- Beta 1.5
        in("b1.5");
        has("lapis");
        exact("detector_rail", "powered_rail");

        // --- Beta 1.4
        in("b1.4");
        exact("cookie", "bone");

        // --- Beta 1.3
        in("b1.3");
        suffix("_bed");
        exact("repeater");

        // --- Beta 1.2
        in("b1.2");
        exact("dispenser", "note_block", "sandstone", "sugar", "ink_sac", "bone_meal");
        suffix("_dye", "_wool");

        // --- Alpha 1.2.0, the Halloween Update: the Nether
        in("a1.2.0");
        has("nether", "glowstone", "jack_o_lantern", "ghast", "pumpkin");
        exact("clock", "fishing_rod", "soul_sand");

        // --- Alpha 1.0.x: the shape of the game as most people first met it
        in("a1.0.0");
        has("minecart", "rail", "diamond", "redstone", "gold_ore", "iron_ore", "snow",
                "cactus", "sugar_cane", "clay", "jukebox", "music_disc",
                "bookshelf", "ladder", "sign", "boat");
        exact("ice", "bucket", "water_bucket", "lava_bucket", "milk_bucket", "flint",
                "flint_and_steel", "bow", "arrow", "paper", "book", "compass", "slime_ball",
                "leather", "saddle", "feather", "gunpowder", "string", "wheat", "wheat_seeds",
                "bread", "farmland", "cake");

        // --- Classic: the handful of blocks that were there before crafting existed
        in("c0.30");
        exact("stone", "dirt", "grass_block", "cobblestone", "oak_planks", "oak_sapling",
                "bedrock", "sand", "gravel", "coal_ore", "oak_log", "oak_leaves", "sponge",
                "glass", "white_wool", "dandelion", "poppy", "brown_mushroom", "red_mushroom",
                "gold_block", "iron_block", "diamond_block", "bricks", "tnt", "mossy_cobblestone",
                "obsidian", "torch", "stone_slab", "smooth_stone_slab", "chest", "furnace",
                "crafting_table", "water", "lava", "oak_stairs", "cobblestone_stairs");

        in("rd-132211");
        exact("stone", "grass_block", "dirt");

        // ------------------------------------------------------------ mobs

        mobs("c0.24_st", "zombie", "skeleton", "creeper", "spider", "pig", "sheep");
        mobs("a1.0.0", "cow", "chicken");
        mobs("a1.0.11", "slime");
        mobs("a1.2.0", "ghast", "zombified_piglin");
        mobs("a1.2.2", "squid");
        mobs("b1.4", "wolf");
        mobs("b1.8.1", "enderman", "silverfish", "cave_spider");
        mobs("b1.9-pre6", "villager", "blaze", "magma_cube", "snow_golem", "ender_dragon",
                "mooshroom");
        mobs("1.2.5", "iron_golem", "ocelot");
        mobs("1.4.7", "bat", "witch", "wither");
        mobs("1.6.4", "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "leash_knot");
        mobs("1.8.9", "rabbit", "endermite", "guardian", "elder_guardian", "armor_stand");
        mobs("1.9.4", "shulker", "shulker_bullet");
        mobs("1.10.2", "polar_bear", "husk", "stray");
        mobs("1.11.2", "llama", "llama_spit", "evoker", "evoker_fangs", "vex", "vindicator",
                "wither_skeleton", "zombie_villager");
        mobs("1.12.2", "parrot", "illusioner");
        mobs("1.13.2", "turtle", "phantom", "drowned", "cod", "salmon", "pufferfish",
                "tropical_fish", "dolphin", "trident");
        mobs("1.14.4", "panda", "fox", "cat", "pillager", "ravager", "trader_llama",
                "wandering_trader");
        mobs("1.15.2", "bee");
        mobs("1.16.5", "piglin", "piglin_brute", "hoglin", "zoglin", "strider");
        mobs("1.17.1", "axolotl", "goat", "glow_squid", "glow_item_frame", "marker");
        mobs("1.19.4", "warden", "allay", "frog", "tadpole", "text_display", "item_display",
                "block_display", "interaction");
        mobs("1.20.6", "camel", "sniffer", "armadillo");
        mobs("1.21", "breeze", "bogged", "breeze_wind_charge", "wind_charge", "ominous_item_spawner");
        mobs("1.21.4", "creaking");
        mobs("1.21.8", "happy_ghast");
    }

    // ------------------------------------------------------------------ lookup

    /** Resolved dates, filled in on first sight of an item. Read from several threads. */
    private static final Map<Item, Integer> ITEM_CACHE = new ConcurrentHashMap<>();

    /** Sentinel for "this item is not part of Minecraft's history and is never confiscated". */
    public static final int EXEMPT = Integer.MIN_VALUE;

    /**
     * @return the timeline index of the version that introduced {@code item}, or {@link #EXEMPT}
     * for anything outside vanilla.
     */
    public static int addedIn(Item item) {
        Integer cached = ITEM_CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        int resolved = resolve(item);
        ITEM_CACHE.put(item, resolved);
        return resolved;
    }

    private static int resolve(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        if (!"minecraft".equals(id.getNamespace())) {
            return EXEMPT;
        }
        String path = id.getPath();

        // A spawn egg is exactly as old as the mob inside it, once eggs themselves exist.
        if (path.endsWith("_spawn_egg")) {
            Integer mob = ENTITY_DATES.get(path.substring(0, path.length() - "_spawn_egg".length()));
            if (mob != null) {
                return Math.max(mob, Timeline.require("1.1"));
            }
        }

        for (Rule rule : RULES) {
            if (rule.test(path)) {
                return rule.index();
            }
        }
        return DEFAULT_ITEM;
    }

    /** True if {@code stack} has not been invented in the era the world is currently sitting in. */
    public static boolean isAnachronistic(ItemStack stack) {
        return notYetAt(stack, Era.index());
    }

    /**
     * True if {@code stack} would not exist in the version at {@code index}. Asking about an era
     * other than the current one is what lets the dial tell you what a trip will cost before you
     * take it, without anything having to move {@link Era} out from under the rest of the game.
     */
    public static boolean notYetAt(ItemStack stack, int index) {
        if (stack.isEmpty()) {
            return false;
        }
        int added = addedIn(stack.getItem());
        return added != EXEMPT && added > index;
    }

    /** @return the timeline index of the version that introduced {@code type}. */
    public static int addedIn(EntityType<?> type) {
        Identifier id = EntityType.getId(type);
        if (!"minecraft".equals(id.getNamespace())) {
            return EXEMPT;
        }
        return ENTITY_DATES.getOrDefault(id.getPath(), DEFAULT_ENTITY);
    }

    /** True if {@code entity} is a mob from a version the world has not reached yet. */
    public static boolean isAnachronistic(Entity entity) {
        int added = addedIn(entity.getType());
        return added != EXEMPT && added > Era.index();
    }
}

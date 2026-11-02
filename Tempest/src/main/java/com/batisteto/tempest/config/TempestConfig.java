package com.batisteto.tempest.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Quality/scale values default high because the mod targets
 * high-end hardware, but every knob here lets a server or player scale down.
 */
public class TempestConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AUTO_SPAWN;
    public static final ModConfigSpec.IntValue AUTO_SPAWN_CHECK_TICKS;
    public static final ModConfigSpec.DoubleValue AUTO_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_CELLS;
    public static final ModConfigSpec.BooleanValue HAIL_DAMAGE;

    public static final ModConfigSpec.BooleanValue TORNADOES_ENABLED;
    public static final ModConfigSpec.BooleanValue TORNADO_BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue TORNADO_ENTITY_DAMAGE;
    public static final ModConfigSpec.DoubleValue TORNADO_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_EF_SCALE;

    public static final ModConfigSpec.IntValue STORM_RENDER_DISTANCE;
    public static final ModConfigSpec.DoubleValue CLOUD_DENSITY;
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ModConfigSpec.IntValue RAYMARCH_QUALITY;
    public static final ModConfigSpec.BooleanValue HIDE_VANILLA_CLOUDS;
    public static final ModConfigSpec.IntValue CLOUD_RANGE_CELLS;
    public static final ModConfigSpec.IntValue CLOUD_SEGMENTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("simulation");
        AUTO_SPAWN = b.comment("Let storms spawn on their own over time.")
                .define("autoSpawn", true);
        AUTO_SPAWN_CHECK_TICKS = b.comment("How often (in ticks) to roll for a new storm.")
                .defineInRange("autoSpawnCheckTicks", 200, 20, 12000);
        AUTO_SPAWN_CHANCE = b.comment("Chance per check for a new storm cell to form.")
                .defineInRange("autoSpawnChance", 0.08, 0.0, 1.0);
        MAX_ACTIVE_CELLS = b.comment("Maximum simultaneous storm cells per dimension.")
                .defineInRange("maxActiveCells", 6, 1, 32);
        HAIL_DAMAGE = b.comment("Mature supercells/cyclones hurt players caught in the open (a helmet greatly reduces it).")
                .define("hailDamage", true);
        b.pop();

        b.push("tornadoes");
        TORNADOES_ENABLED = b.define("tornadoesEnabled", true);
        TORNADO_BLOCK_DAMAGE = b.comment("Whether tornadoes can destroy blocks (also requires the mobGriefing gamerule).")
                .define("tornadoBlockDamage", true);
        TORNADO_ENTITY_DAMAGE = b.define("tornadoEntityDamage", true);
        TORNADO_SPAWN_CHANCE = b.comment("Chance per tick, per mature supercell/cyclone with a strong mesocyclone, to drop a tornado.")
                .defineInRange("tornadoSpawnChance", 0.0015, 0.0, 1.0);
        MAX_EF_SCALE = b.comment("Highest EF scale tier that can be rolled (0-5).")
                .defineInRange("maxEfScale", 5, 0, 5);
        b.pop();

        b.push("rendering");
        STORM_RENDER_DISTANCE = b.comment("Blocks: how far away storm clouds are still drawn. Large so supercells are visible on the horizon.")
                .defineInRange("stormRenderDistance", 768, 64, 4096);
        CLOUD_DENSITY = b.comment("Multiplier for cloud layer count/opacity.")
                .defineInRange("cloudDensity", 1.0, 0.1, 2.0);
        PARTICLE_DENSITY = b.comment("Multiplier for rain/hail/debris particle counts.")
                .defineInRange("particleDensity", 1.0, 0.1, 3.0);
        RAYMARCH_QUALITY = b.comment("Reserved for the future volumetric shader pass (ULTRA/EXTREME tiers). Higher = sharper clouds, more GPU cost.")
                .defineInRange("raymarchSteps", 48, 8, 128);
        b.pop();

        b.push("clouds");
        HIDE_VANILLA_CLOUDS = b.comment("Turn off vanilla's blocky clouds while playing (restored on logout); Tempest draws its own soft cloud layer.")
                .define("hideVanillaClouds", true);
        CLOUD_RANGE_CELLS = b.comment("Radius, in 150-block cells, of the ambient cloud field around the camera. Higher = more clouds to the horizon.")
                .defineInRange("cloudRangeCells", 5, 2, 9);
        CLOUD_SEGMENTS = b.comment("Mesh detail of each cloud blob (longitude segments). Higher = smoother, more GPU/CPU.")
                .defineInRange("cloudSegments", 14, 8, 28);
        b.pop();

        SPEC = b.build();
    }
}

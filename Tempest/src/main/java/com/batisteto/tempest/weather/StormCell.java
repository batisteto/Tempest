package com.batisteto.tempest.weather;

import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.registry.ModEntities;
import com.batisteto.tempest.entity.TornadoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * One simulated storm cell: a real (if simplified) developing -> mature -> dissipating
 * lifecycle, with mesocyclone buildup driving tornado formation for supercells/cyclones.
 */
public class StormCell {
    private static int NEXT_ID = 1;
    private final SimplexNoise wobbleNoise;

    public final int id;
    public final StormType type;
    public double x, z;
    public float radius;
    public float intensity;
    public StormStage stage = StormStage.DEVELOPING;
    public int age;
    public int stageTicks;
    public float motionX, motionZ;
    public float mesocyclone;
    private int lightningCooldown;
    public boolean dead;

    public StormCell(StormType type, double x, double z, float motionX, float motionZ, RandomSource random) {
        this.id = NEXT_ID++;
        this.type = type;
        this.x = x;
        this.z = z;
        this.motionX = motionX;
        this.motionZ = motionZ;
        this.radius = type.baseRadius * 0.25f;
        this.intensity = 0.1f;
        this.wobbleNoise = new SimplexNoise(random);
        this.lightningCooldown = 100;
    }

    private int developDuration() {
        return switch (type) {
            case RAIN, SQUALL -> 300;
            case THUNDERSTORM -> 500;
            case SUPERCELL -> 900;
            case CYCLONE -> 1400;
        };
    }

    private int matureDuration() {
        return switch (type) {
            case RAIN, SQUALL -> 800;
            case THUNDERSTORM -> 1600;
            case SUPERCELL -> 3600;
            case CYCLONE -> 6000;
        };
    }

    public void tick(ServerLevel level, RandomSource random) {
        age++;
        stageTicks++;

        float targetRadius = type.baseRadius * (0.4f + 0.6f * intensity);
        radius += (targetRadius - radius) * 0.01f;

        switch (stage) {
            case DEVELOPING -> {
                intensity = Math.min(1.0f, intensity + 0.0025f);
                if (stageTicks > developDuration()) {
                    stage = StormStage.MATURE;
                    stageTicks = 0;
                }
            }
            case MATURE -> {
                intensity = Math.min(1.0f, Math.max(0.6f, intensity + (random.nextFloat() - 0.5f) * 0.01f));
                if (type.canTornado) {
                    mesocyclone = Math.min(1.0f, mesocyclone + type.baseMesocycloneGrowth * intensity * 0.01f);
                }
                if (stageTicks > matureDuration()) {
                    stage = StormStage.DISSIPATING;
                    stageTicks = 0;
                }
            }
            case DISSIPATING -> {
                intensity = Math.max(0f, intensity - 0.003f);
                mesocyclone = Math.max(0f, mesocyclone - 0.01f);
                if (intensity <= 0.02f) {
                    dead = true;
                }
            }
        }

        // Steering wobble on top of the base motion vector (keeps storms from moving in dead-straight lines).
        double t = age * 0.01;
        float wobbleX = (float) wobbleNoise.getValue(t, 0) * 0.04f;
        float wobbleZ = (float) wobbleNoise.getValue(0, t) * 0.04f;
        x += motionX + wobbleX;
        z += motionZ + wobbleZ;

        if (type.canThunder) {
            lightningCooldown--;
            if (lightningCooldown <= 0 && intensity > 0.3f) {
                strikeLightning(level, random);
                float freq = switch (type) {
                    case SUPERCELL -> 40f;
                    case CYCLONE -> 60f;
                    default -> 90f;
                };
                lightningCooldown = (int) (freq / Math.max(0.2f, intensity)) + random.nextInt(40);
            }
        }

        if (TempestConfig.HAIL_DAMAGE.get() && type.canTornado && stage == StormStage.MATURE
                && intensity > 0.6f && age % 25 == 0) {
            hailStrike(level);
        }

        if (TempestConfig.TORNADOES_ENABLED.get() && type.canTornado && stage == StormStage.MATURE
                && mesocyclone > 0.55f) {
            double chance = TempestConfig.TORNADO_SPAWN_CHANCE.get() * mesocyclone;
            if (random.nextDouble() < chance) {
                spawnTornado(level, random);
            }
        }
    }

    /** Hail pelts players standing in the open under the core of a severe storm. */
    private void hailStrike(ServerLevel level) {
        double reach = radius * 0.8;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz > reach * reach) continue;
            if (!level.canSeeSky(player.blockPosition())) continue;
            float damage = player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? 2.0f : 0.5f;
            player.hurt(level.damageSources().generic(), damage);
        }
    }

    private void strikeLightning(ServerLevel level, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * radius * 0.6;
        int bx = (int) Math.round(x + Math.cos(angle) * dist);
        int bz = (int) Math.round(z + Math.sin(angle) * dist);
        int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(bx + 0.5, by, bz + 0.5);
            level.addFreshEntity(bolt);
        }
    }

    private void spawnTornado(ServerLevel level, RandomSource random) {
        int maxEf = TempestConfig.MAX_EF_SCALE.get();
        float roll = mesocyclone * intensity + random.nextFloat() * 0.3f;
        int efOrdinal = Math.min(maxEf, (int) (roll * 4));
        EFScale ef = EFScale.byOrdinalClamped(efOrdinal);

        TornadoEntity tornado = ModEntities.TORNADO.get().create(level);
        if (tornado == null) return;
        int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        tornado.moveTo(x, by, z, 0, 0);
        tornado.setEfScale(ef);
        tornado.setMotion(motionX * 1.6f, motionZ * 1.6f);
        level.addFreshEntity(tornado);
        mesocyclone *= 0.4f; // spending the mesocyclone makes back-to-back spawns less likely
    }

    /** Skip the developing stage - used by commands/presets so a storm is immediately at full strength. */
    public void forceMature() {
        stage = StormStage.MATURE;
        stageTicks = 0;
        intensity = 0.9f;
        radius = type.baseRadius * 0.95f;
        if (type.canTornado) mesocyclone = 0.8f;
    }

    public StormCellData toNetworkData() {
        return new StormCellData(id, x, z, radius, type.ordinal(), stage.ordinal(), intensity, motionX, motionZ, 0);
    }

    public BlockPos centerPos(ServerLevel level) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        return new BlockPos((int) x, y, (int) z);
    }
}

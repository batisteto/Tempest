package com.batisteto.tempest.entity;

import com.batisteto.tempest.block.TempestBlockTags;
import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.registry.ModParticles;
import com.batisteto.tempest.weather.EFScale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_EF =
            SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);

    private final SimplexNoise wobbleNoise;
    private final int maxAgeTicks;
    private float motionX;
    private float motionZ;
    private int damageCooldown;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.wobbleNoise = new SimplexNoise(RandomSource.create(this.getId() * 7919L + 13L));
        this.maxAgeTicks = 500 + level.random.nextInt(2200);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EF, 0);
    }

    public void setEfScale(EFScale ef) {
        this.entityData.set(DATA_EF, ef.ordinal());
    }

    public EFScale getEfScale() {
        return EFScale.byOrdinalClamped(this.entityData.get(DATA_EF));
    }

    public void setMotion(float mx, float mz) {
        this.motionX = mx;
        this.motionZ = mz;
    }

    public float suctionRadius() {
        return 3.0f + getEfScale().ordinal() * 2.2f;
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount > maxAgeTicks) {
            discard();
            return;
        }

        EFScale ef = getEfScale();
        float suctionRadius = suctionRadius();
        float coreRadius = suctionRadius * 0.35f;

        double t = tickCount * 0.02;
        // Wobble adds a wandering lean to the path without overpowering the storm's actual
        // heading - it was previously as large as the base speed, which read as erratic/random
        // movement instead of a tornado that's clearly going somewhere.
        float wobbleX = (float) wobbleNoise.getValue(t, 0) * 0.1f;
        float wobbleZ = (float) wobbleNoise.getValue(0, t) * 0.1f;
        double nx = getX() + motionX + wobbleX;
        double nz = getZ() + motionZ + wobbleZ;
        double ny = getY();
        if (!level().isClientSide) {
            ny = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(nx, 0, nz)).getY();
        }
        setPos(nx, ny, nz);

        if (!level().isClientSide) {
            applySuctionAndDamage(suctionRadius, coreRadius, ef);
            if (TempestConfig.TORNADO_BLOCK_DAMAGE.get()) {
                tornadoBlockDamage(ef, suctionRadius);
            }
            if (tickCount % 30 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.WEATHER, 4.0f, 0.5f);
            }
        } else {
            spawnClientDustParticles(suctionRadius);
        }
    }

    private void applySuctionAndDamage(float suctionRadius, float coreRadius, EFScale ef) {
        if (damageCooldown > 0) damageCooldown--;
        AABB area = new AABB(blockPosition()).inflate(suctionRadius, 30, suctionRadius);
        List<Entity> targets = level().getEntities(this, area,
                e -> e instanceof LivingEntity || e instanceof ItemEntity);
        boolean canDamage = TempestConfig.TORNADO_ENTITY_DAMAGE.get();

        for (Entity e : targets) {
            double dx = e.getX() - getX();
            double dz = e.getZ() - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > suctionRadius || dist < 0.01) continue;

            double pull = (1.0 - dist / suctionRadius) * (0.15 + ef.ordinal() * 0.08);
            double vx = -dx / dist * pull;
            double vz = -dz / dist * pull;
            double vy = dist < coreRadius ? 0.12 + ef.ordinal() * 0.02 : 0.02;
            e.setDeltaMovement(e.getDeltaMovement().add(vx, vy, vz));

            if (canDamage && damageCooldown <= 0 && e instanceof LivingEntity living && dist < coreRadius * 1.4) {
                living.hurt(this.level().damageSources().flyIntoWall(), ef.damagePerHit);
            }
        }
        if (damageCooldown <= 0) damageCooldown = 10;
    }

    /**
     * Strips blocks column by column from the top down (the wind hits the highest block first), only
     * ever at or above the ground level the tornado stands on - so roofs, walls, leaves and crops go,
     * the floor/terrain never does. Which blocks can go is decided by the tempest:tornado_destructible
     * tier tags for this EF level.
     */
    private void tornadoBlockDamage(EFScale ef, float suctionRadius) {
        if (!level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        RandomSource random = serverLevel.random;
        BlockPos center = blockPosition();
        int radius = (int) (suctionRadius * 1.1f);
        int attempts = 6 + ef.ordinal() * 6;
        float chance = 0.45f + 0.08f * ef.ordinal();
        TagKey<Block> tag = TempestBlockTags.destructibleTagFor(ef.destructionTier);

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!serverLevel.hasChunkAt(new BlockPos(x, center.getY(), z))) continue;

            int top = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            for (int y = top; y >= center.getY() && y > top - 4; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = serverLevel.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.is(tag) && random.nextFloat() < chance) {
                    breakBlock(serverLevel, pos, state, random);
                }
                break; // only the first solid block from the top is ever exposed to the wind
            }
        }
    }

    private void breakBlock(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        level.destroyBlock(pos, false);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.4, 0.4, 0.4, 0.25);
        if (random.nextFloat() < 0.2f) {
            ItemEntity debris = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(state.getBlock()));
            debris.setDeltaMovement((random.nextDouble() - 0.5) * 0.8, 0.5 + random.nextDouble() * 0.4,
                    (random.nextDouble() - 0.5) * 0.8);
            level.addFreshEntity(debris);
        }
    }

    private void spawnClientDustParticles(float suctionRadius) {
        RandomSource random = level().random;
        int ef = getEfScale().ordinal();
        int count = 5 + ef * 3;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = suctionRadius * (0.3 + random.nextDouble() * 1.0);
            double px = getX() + Math.cos(angle) * dist;
            double pz = getZ() + Math.sin(angle) * dist;
            double py = getY() + random.nextDouble() * (2.0 + ef);
            // tangential swirl + lift, slightly inward
            double swirl = 0.22 + ef * 0.03;
            level().addParticle(ModParticles.DEBRIS.get(), px, py, pz,
                    -Math.sin(angle) * swirl - Math.cos(angle) * 0.05, 0.12 + random.nextDouble() * 0.1,
                    Math.cos(angle) * swirl - Math.sin(angle) * 0.05);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_EF, tag.getInt("EfScale"));
        this.motionX = tag.getFloat("MotionX");
        this.motionZ = tag.getFloat("MotionZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("EfScale", this.entityData.get(DATA_EF));
        tag.putFloat("MotionX", motionX);
        tag.putFloat("MotionZ", motionZ);
    }
}

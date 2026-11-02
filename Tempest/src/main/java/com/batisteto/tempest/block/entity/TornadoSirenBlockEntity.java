package com.batisteto.tempest.block.entity;

import com.batisteto.tempest.entity.TornadoEntity;
import com.batisteto.tempest.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TornadoSirenBlockEntity extends BlockEntity {
    private static final double RANGE = 96.0;
    private int alarmCooldown;
    private boolean alarmActive;

    public TornadoSirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TORNADO_SIREN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TornadoSirenBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<TornadoEntity> nearby = serverLevel.getEntitiesOfClass(TornadoEntity.class, new AABB(pos).inflate(RANGE));
        boolean detected = !nearby.isEmpty();

        if (detected && !be.alarmActive) {
            be.alarmActive = true;
            be.alarmCooldown = 0;
        } else if (!detected) {
            be.alarmActive = false;
        }

        if (be.alarmActive && be.alarmCooldown-- <= 0) {
            be.alarmCooldown = 60;
            serverLevel.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 3.0f, 0.7f);
        }
    }

    public boolean isAlarmActive() {
        return alarmActive;
    }
}

package com.batisteto.tempest.block;

import com.batisteto.tempest.block.entity.TornadoSirenBlockEntity;
import com.batisteto.tempest.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Standalone detector/alarm: sounds a horn and can be read via comparator-style proximity when a tornado nears. */
public class TornadoSirenBlock extends BaseEntityBlock {
    private static final MapCodec<TornadoSirenBlock> CODEC = simpleCodec(TornadoSirenBlock::new);

    public TornadoSirenBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TornadoSirenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.TORNADO_SIREN.get(), TornadoSirenBlockEntity::serverTick);
    }
}

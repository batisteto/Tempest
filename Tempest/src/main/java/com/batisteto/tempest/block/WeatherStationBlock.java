package com.batisteto.tempest.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to open the radar screen (reads the client's already-synced storm cell cache). */
public class WeatherStationBlock extends Block {
    public WeatherStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            com.batisteto.tempest.client.RadarScreenOpener.open();
        }
        return InteractionResult.SUCCESS;
    }
}

package com.batisteto.tempest.block;

import com.batisteto.tempest.entity.TornadoEntity;
import com.batisteto.tempest.weather.WeatherManager;
import com.batisteto.tempest.weather.WindUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Right-click to read the local wind speed/direction (blends ambient wind with nearby storm inflow). */
public class AnemometerBlock extends Block {
    public AnemometerBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        List<TornadoEntity> nearby = serverLevel.getEntitiesOfClass(TornadoEntity.class, new AABB(pos).inflate(48));
        if (!nearby.isEmpty()) {
            player.displayClientMessage(Component.literal("§4§lEXTREME WINDS - TORNADO NEARBY - TAKE COVER"), true);
            return InteractionResult.CONSUME;
        }

        WeatherManager mgr = WeatherManager.get(serverLevel);
        float[] wind = WindUtil.computeWindAt(pos.getX(), pos.getZ(), mgr.windX, mgr.windZ, mgr.networkSnapshot());
        float speed = WindUtil.speedMph(wind[0], wind[1]);
        String dir = WindUtil.compassDirection(wind[0], wind[1]);
        player.displayClientMessage(Component.literal(String.format("§bWind: %.1f mph from the %s", speed, dir)), true);
        return InteractionResult.CONSUME;
    }
}

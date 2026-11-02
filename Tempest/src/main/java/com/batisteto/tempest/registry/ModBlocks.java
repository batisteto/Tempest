package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.block.AnemometerBlock;
import com.batisteto.tempest.block.TornadoSirenBlock;
import com.batisteto.tempest.block.WeatherStationBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Tempest.MODID);

    public static final DeferredHolder<Block, Block> ANEMOMETER = BLOCKS.register("anemometer",
            () -> new AnemometerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredHolder<Block, Block> WEATHER_STATION = BLOCKS.register("weather_station",
            () -> new WeatherStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredHolder<Block, Block> TORNADO_SIREN = BLOCKS.register("tornado_siren",
            () -> new TornadoSirenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(2.5f)
                    .sound(SoundType.METAL)));
}

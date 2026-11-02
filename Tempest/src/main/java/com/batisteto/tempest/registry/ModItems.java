package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Tempest.MODID);

    public static final DeferredHolder<Item, Item> ANEMOMETER_ITEM = ITEMS.register("anemometer",
            () -> new BlockItem(ModBlocks.ANEMOMETER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> WEATHER_STATION_ITEM = ITEMS.register("weather_station",
            () -> new BlockItem(ModBlocks.WEATHER_STATION.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> TORNADO_SIREN_ITEM = ITEMS.register("tornado_siren",
            () -> new BlockItem(ModBlocks.TORNADO_SIREN.get(), new Item.Properties()));
}

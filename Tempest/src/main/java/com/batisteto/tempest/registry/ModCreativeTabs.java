package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Tempest.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEMPEST_TAB = TABS.register("tempest",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tempest"))
                    .icon(() -> new ItemStack(ModItems.WEATHER_STATION_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ANEMOMETER_ITEM.get());
                        output.accept(ModItems.WEATHER_STATION_ITEM.get());
                        output.accept(ModItems.TORNADO_SIREN_ITEM.get());
                    })
                    .build());
}

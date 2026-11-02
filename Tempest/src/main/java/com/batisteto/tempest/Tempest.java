package com.batisteto.tempest;

import com.batisteto.tempest.command.TempestCommand;
import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.event.ServerEvents;
import com.batisteto.tempest.network.TempestNetwork;
import com.batisteto.tempest.registry.ModBlockEntities;
import com.batisteto.tempest.registry.ModBlocks;
import com.batisteto.tempest.registry.ModCreativeTabs;
import com.batisteto.tempest.registry.ModEntities;
import com.batisteto.tempest.registry.ModItems;
import com.batisteto.tempest.registry.ModParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Tempest - realistic procedural weather for NeoForge 1.21.1.
 * Storm cells are simulated server-side (SavedData) and follow a real supercell-style
 * lifecycle (developing -> mature -> dissipating). Vanilla's own rain/thunder + lightning
 * systems are reused as the base layer; Tempest adds storm cells, wind, tornadoes,
 * volumetric-look cloud rendering and radar/anemometer/siren blocks on top.
 */
@Mod(Tempest.MODID)
public class Tempest {
    public static final String MODID = "tempest";

    public Tempest(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(TempestNetwork::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, TempestConfig.SPEC);

        NeoForge.EVENT_BUS.register(new ServerEvents());
        NeoForge.EVENT_BUS.register(new TempestCommand());
    }
}

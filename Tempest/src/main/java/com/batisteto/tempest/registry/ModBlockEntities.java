package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.block.entity.TornadoSirenBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tempest.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TornadoSirenBlockEntity>> TORNADO_SIREN =
            BLOCK_ENTITIES.register("tornado_siren", () -> BlockEntityType.Builder.of(
                    TornadoSirenBlockEntity::new, ModBlocks.TORNADO_SIREN.get()).build(null));
}

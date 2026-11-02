package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.entity.TornadoEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Tempest.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TornadoEntity>> TORNADO =
            ENTITY_TYPES.register("tornado", () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .sized(4.0f, 24.0f)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .build("tornado"));
}

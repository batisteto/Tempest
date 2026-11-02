package com.batisteto.tempest.registry;

import com.batisteto.tempest.Tempest;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Tempest.MODID);

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> HAIL =
            PARTICLE_TYPES.register("hail", () -> new SimpleParticleType(false));

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> DEBRIS =
            PARTICLE_TYPES.register("debris", () -> new SimpleParticleType(false));

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> RAIN_STREAK =
            PARTICLE_TYPES.register("rain_streak", () -> new SimpleParticleType(false));
}

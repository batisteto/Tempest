package com.batisteto.tempest.client;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.client.particle.DebrisParticle;
import com.batisteto.tempest.client.particle.HailParticle;
import com.batisteto.tempest.client.particle.RainStreakParticle;
import com.batisteto.tempest.entity.client.TornadoRenderer;
import com.batisteto.tempest.registry.ModEntities;
import com.batisteto.tempest.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Tempest.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TORNADO.get(), TornadoRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HAIL.get(), HailParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DEBRIS.get(), DebrisParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RAIN_STREAK.get(), RainStreakParticle.Provider::new);
    }
}

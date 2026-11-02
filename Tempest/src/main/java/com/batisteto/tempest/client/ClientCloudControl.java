package com.batisteto.tempest.client;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.config.TempestConfig;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Tempest draws its own soft cloud layer, so vanilla's blocky clouds are switched off while you're
 * in a world and put back (to whatever you had) when you leave it. Disable with hideVanillaClouds.
 */
@EventBusSubscriber(modid = Tempest.MODID, value = Dist.CLIENT)
public class ClientCloudControl {
    private static CloudStatus saved;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (!TempestConfig.HIDE_VANILLA_CLOUDS.get()) {
            restore(mc);
            return;
        }
        CloudStatus current = mc.options.cloudStatus().get();
        if (current != CloudStatus.OFF) {
            saved = current;
            mc.options.cloudStatus().set(CloudStatus.OFF);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
    }

    private static void restore(Minecraft mc) {
        if (saved != null) {
            mc.options.cloudStatus().set(saved);
            saved = null;
        }
    }
}

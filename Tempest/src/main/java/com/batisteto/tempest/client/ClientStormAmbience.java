package com.batisteto.tempest.client;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.weather.StormCellData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Vanilla's own dimension-wide sky darkening is permanently disabled (see WeatherManager) since
 * it isn't localized. This replaces it with: darker/hazier fog, a full-screen tint (fog alone
 * barely touches nearby terrain), and an "overcast" factor the sky renderer uses to fill the
 * sky with cloud. Visual only - block light levels and mob spawning are unaffected.
 */
@EventBusSubscriber(modid = Tempest.MODID, value = Dist.CLIENT)
public class ClientStormAmbience {
    // Storms count out to this multiple of their data radius so rain / darkening roughly match
    // how far the cloud structure visually spreads.
    private static final float TRIGGER_RADIUS_MULT = 1.3f;

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float darken = stormDarkenFactor();
        if (darken <= 0f) return;
        // pull the fog towards a cool grey-brown haze, then darken it
        float haze = darken * 0.55f;
        float r = Mth.lerp(haze, event.getRed(), 0.34f) * (1f - darken * 0.5f);
        float g = Mth.lerp(haze, event.getGreen(), 0.34f) * (1f - darken * 0.5f);
        float b = Mth.lerp(haze, event.getBlue(), 0.38f) * (1f - darken * 0.45f);
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float darken = stormDarkenFactor();
        if (darken <= 0f) return;
        // Real haze, but not so tight that a tornado 100 blocks away disappears.
        event.setFarPlaneDistance(Math.max(90f, event.getFarPlaneDistance() * (1f - darken * 0.55f)));
        event.setNearPlaneDistance(event.getNearPlaneDistance() * (1f - darken * 0.6f));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        float darken = stormDarkenFactor();
        if (darken <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int alpha = (int) (darken * 150f);
        int color = (alpha << 24) | 0x0E1118; // neutral dark blue-grey
        event.getGuiGraphics().fill(0, 0, width, height, color);
    }

    /** 0 = clear, up to ~1 = deep inside a severe, mature storm. */
    public static float stormDarkenFactor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0f;
        double x = mc.player.getX();
        double z = mc.player.getZ();

        float best = 0f;
        for (StormCellData storm : ClientWeatherCache.getStorms()) {
            double effectiveRadius = storm.radius() * TRIGGER_RADIUS_MULT;
            double dx = storm.x() - x;
            double dz = storm.z() - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist >= effectiveRadius) continue;

            float proximity = Mth.clamp(1f - (float) (dist / effectiveRadius), 0f, 1f);
            float factor = Mth.clamp(proximity * 1.6f, 0f, 1f) * (0.35f + 0.65f * storm.intensity());
            if (factor > best) best = factor;
        }
        return best;
    }

    /** Wider, smoother version of the above: how overcast the sky is around the camera (0..1). */
    public static float overcast() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0f;
        double x = mc.player.getX();
        double z = mc.player.getZ();

        float best = 0f;
        for (StormCellData storm : ClientWeatherCache.getStorms()) {
            double reach = storm.radius() * 3.2;
            double dx = storm.x() - x;
            double dz = storm.z() - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist >= reach) continue;

            float p = 1f - (float) (dist / reach);
            p = p * p * (3f - 2f * p);
            float factor = p * (0.3f + 0.7f * storm.intensity());
            if (factor > best) best = factor;
        }
        return best;
    }
}

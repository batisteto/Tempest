package com.batisteto.tempest.client;

import com.batisteto.tempest.weather.StormCellData;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

/** Static client-side holder for the most recent {@code WeatherSyncPayload}. */
public final class ClientWeatherCache {
    private static volatile List<StormCellData> storms = Collections.emptyList();
    private static volatile float windX;
    private static volatile float windZ;
    private static volatile long lastUpdateNanos = System.nanoTime();

    private ClientWeatherCache() {
    }

    public static void update(List<StormCellData> newStorms, float wx, float wz) {
        storms = newStorms;
        windX = wx;
        windZ = wz;
        lastUpdateNanos = System.nanoTime();
    }

    public static List<StormCellData> getStorms() {
        return storms;
    }

    public static float getWindX() {
        return windX;
    }

    public static float getWindZ() {
        return windZ;
    }

    /** Ticks elapsed since the last sync packet - renderers extrapolate storm motion by this much. */
    public static float ticksSinceUpdate() {
        return Mth.clamp((System.nanoTime() - lastUpdateNanos) / 50_000_000f, 0f, 25f);
    }
}

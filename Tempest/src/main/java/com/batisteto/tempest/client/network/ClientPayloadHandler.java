package com.batisteto.tempest.client.network;

import com.batisteto.tempest.client.ClientWeatherCache;
import com.batisteto.tempest.network.WeatherSyncPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleWeatherSync(WeatherSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientWeatherCache.update(payload.storms(), payload.windX(), payload.windZ()));
    }
}

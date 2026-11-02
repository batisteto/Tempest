package com.batisteto.tempest.network;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.client.network.ClientPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TempestNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Tempest.MODID).versioned("1.0.0");
        registrar.playToClient(WeatherSyncPayload.TYPE, WeatherSyncPayload.STREAM_CODEC, ClientPayloadHandler::handleWeatherSync);
    }
}

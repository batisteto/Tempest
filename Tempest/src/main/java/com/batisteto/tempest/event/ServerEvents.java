package com.batisteto.tempest.event;

import com.batisteto.tempest.network.WeatherSyncPayload;
import com.batisteto.tempest.weather.WeatherManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ticks each level's {@link WeatherManager} and periodically broadcasts a snapshot to clients. */
public class ServerEvents {
    private int broadcastTicks;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        WeatherManager mgr = WeatherManager.get(level);
        mgr.tick(level);

        broadcastTicks++;
        if (broadcastTicks >= 10) {
            broadcastTicks = 0;
            WeatherSyncPayload payload = new WeatherSyncPayload(mgr.networkSnapshot(), mgr.windX, mgr.windZ);
            for (ServerPlayer player : level.players()) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            WeatherManager mgr = WeatherManager.get(level);
            PacketDistributor.sendToPlayer(player, new WeatherSyncPayload(mgr.networkSnapshot(), mgr.windX, mgr.windZ));
        }
    }
}

package com.batisteto.tempest.network;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.weather.StormCellData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record WeatherSyncPayload(List<StormCellData> storms, float windX, float windZ) implements CustomPacketPayload {
    public static final Type<WeatherSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Tempest.MODID, "weather_sync"));

    public static final StreamCodec<FriendlyByteBuf, WeatherSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeFloat(payload.windX());
                buf.writeFloat(payload.windZ());
                buf.writeVarInt(payload.storms().size());
                for (StormCellData s : payload.storms()) {
                    StormCellData.STREAM_CODEC.encode(buf, s);
                }
            },
            buf -> {
                float wx = buf.readFloat();
                float wz = buf.readFloat();
                int n = buf.readVarInt();
                List<StormCellData> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(StormCellData.STREAM_CODEC.decode(buf));
                return new WeatherSyncPayload(list, wx, wz);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

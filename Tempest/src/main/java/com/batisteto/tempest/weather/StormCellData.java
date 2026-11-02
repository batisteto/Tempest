package com.batisteto.tempest.weather;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Lightweight, immutable snapshot of a {@link StormCell} sent to clients for rendering
 * (clouds, radar screen, wind computation). Never sent more than a few times a second.
 */
public record StormCellData(int id, double x, double z, float radius, int type, int stage,
                             float intensity, float motionX, float motionZ, int efScale) {

    public static final StreamCodec<FriendlyByteBuf, StormCellData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                buf.writeVarInt(d.id());
                buf.writeDouble(d.x());
                buf.writeDouble(d.z());
                buf.writeFloat(d.radius());
                buf.writeByte(d.type());
                buf.writeByte(d.stage());
                buf.writeFloat(d.intensity());
                buf.writeFloat(d.motionX());
                buf.writeFloat(d.motionZ());
                buf.writeByte(d.efScale());
            },
            buf -> new StormCellData(
                    buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
                    buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readByte()
            )
    );

    public StormType stormType() {
        return StormType.byOrdinalClamped(type);
    }

    public StormStage stormStage() {
        return StormStage.byOrdinalClamped(stage);
    }
}

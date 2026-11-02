package com.batisteto.tempest.entity.client;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.client.render.CloudBlobs;
import com.batisteto.tempest.client.render.SkyCloudRenderer;
import com.batisteto.tempest.entity.TornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Smooth, soft-edged funnel. Shape depends on EF scale: rope (EF0-1: thin, curling), cone
 * (EF2-3) or wedge (EF4-5: huge, wide). Per-vertex alpha fades at the silhouette, the streak
 * texture rotates around the funnel for the swirl, and a dust skirt tinted from the block it
 * stands on (orange over terracotta, brown over dirt...) wraps the base.
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tempest.MODID, "textures/entity/tornado_funnel.png");
    private static final int RINGS = 34;
    private static final int SEGMENTS = 24;
    private static final float CLOUD_BASE = 158f;
    private static final float[] TOP_COLOR = {62f, 62f, 72f};

    // per-vertex data indices
    private static final int X = 0, Y = 1, Z = 2, R = 3, G = 4, B = 5, A = 6, U = 7, V = 8, F = 9;

    public TornadoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float camX = (float) (cam.x - ex), camY = (float) (cam.y - ey), camZ = (float) (cam.z - ez);

        int ef = entity.getEfScale().ordinal();
        int kind = ef >= 4 ? 2 : (ef >= 2 ? 1 : 0); // 0 rope, 1 cone, 2 wedge
        float r0 = entity.suctionRadius() * 1.15f;
        float height = Math.max(55f, CLOUD_BASE - (float) ey);
        double time = (entity.tickCount + partialTick) / 20.0;
        float[] dust = dustColor(entity);
        float[] base = {Mth.lerp(0.7f, 130f, dust[0]) * 0.9f, Mth.lerp(0.7f, 130f, dust[1]) * 0.9f,
                Mth.lerp(0.7f, 130f, dust[2]) * 0.9f};

        int stride = SEGMENTS + 1;
        int count = (RINGS + 1) * stride;
        float[][] d = new float[10][count];

        for (int ring = 0; ring <= RINGS; ring++) {
            float t = ring / (float) RINGS;
            float rad = radius(kind, r0, t);
            float swayAmp = (kind == 0 ? 7f : (kind == 1 ? 3.5f : 1.6f)) * (float) Math.pow(t, 1.3);
            float phase = (float) time * 0.9f + t * (kind == 0 ? 7f : 4f);
            float ox = (float) Math.sin(phase) * swayAmp;
            float oz = (float) Math.cos(phase * 0.83f + 1f) * swayAmp;
            float yy = t * height;

            float m = Mth.clamp(t / 0.8f, 0f, 1f);
            m = m * m * (3f - 2f * m);
            float cr = Mth.lerp(m, base[0], TOP_COLOR[0]);
            float cg = Mth.lerp(m, base[1], TOP_COLOR[1]);
            float cb = Mth.lerp(m, base[2], TOP_COLOR[2]);
            float topFade = 1f - smooth01((t - 0.86f) / 0.14f);
            float baseAlpha = Mth.lerp(t, 0.9f, 0.8f) * topFade;
            float spin = (float) time * 0.45f * (1.5f - t) - t * 0.9f;

            for (int s = 0; s <= SEGMENTS; s++) {
                int i = ring * stride + s;
                float phi = (float) (Math.PI * 2 * s / SEGMENTS);
                float nx = (float) Math.cos(phi), nz = (float) Math.sin(phi);
                float px = ox + nx * rad, pz = oz + nz * rad;

                float nl = Mth.sqrt(1.04f);
                float nnx = nx / nl, nny = 0.2f / nl, nnz = nz / nl;
                float vx = camX - px, vy = camY - yy, vz = camZ - pz;
                float vl = Math.max(0.001f, Mth.sqrt(vx * vx + vy * vy + vz * vz));
                float facing = (nnx * vx + nny * vy + nnz * vz) / vl;

                float edge = Mth.clamp(Math.abs(facing) * 1.9f, 0f, 1f);
                edge = edge * edge * (3f - 2f * edge);

                d[X][i] = px;
                d[Y][i] = yy;
                d[Z][i] = pz;
                d[R][i] = cr;
                d[G][i] = cg;
                d[B][i] = cb;
                d[A][i] = baseAlpha * edge * (facing >= 0f ? 1f : 0.65f);
                d[U][i] = s / (float) SEGMENTS * 2f + spin;
                d[V][i] = t * 4f;
                d[F][i] = facing;
            }
        }

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));

        // back faces first, then front faces
        for (int pass = 0; pass < 2; pass++) {
            for (int ring = 0; ring < RINGS; ring++) {
                for (int s = 0; s < SEGMENTS; s++) {
                    int i0 = ring * stride + s, i1 = i0 + 1, i2 = i1 + stride, i3 = i0 + stride;
                    float avg = (d[F][i0] + d[F][i1] + d[F][i2] + d[F][i3]) * 0.25f;
                    if ((pass == 0) != (avg <= 0f)) continue;
                    put(buf, mat, d, i0);
                    put(buf, mat, d, i1);
                    put(buf, mat, d, i2);
                    put(buf, mat, d, i3);
                }
            }
        }

        // dust skirt wrapped around the base
        float[] skirtLit = {Mth.clamp(dust[0] * 1.05f, 0f, 255f), Mth.clamp(dust[1] * 1.05f, 0f, 255f),
                Mth.clamp(dust[2] * 1.05f, 0f, 255f)};
        float[] skirtShadow = {dust[0] * 0.6f, dust[1] * 0.6f, dust[2] * 0.6f};
        float[] sun = SkyCloudRenderer.sunVector();
        CloudBlobs.emit(buf, mat, new CloudBlobs.Blob(0, r0 * 0.4, 0, r0 * 2.3f + 2f, r0 * 0.8f + 1f, r0 * 2.3f + 2f,
                0.25f, skirtLit, skirtShadow, 0.7f), 0, 0, 0, camX, camY, camZ, sun, 14);
        CloudBlobs.emit(buf, mat, new CloudBlobs.Blob(Math.sin(time * 0.9) * 0.8, r0 * 1.3, Math.cos(time * 0.8) * 0.8,
                r0 * 1.5f, r0 * 1.5f, r0 * 1.5f, 0.45f, skirtLit, skirtShadow, 0.55f), 0, 0, 0, camX, camY, camZ, sun, 14);

        // The tornado brings its own parent cloud: a dark, slowly rotating shelf where the funnel
        // attaches, so it reads as hanging from a storm even when no storm cell is overhead.
        float k = Math.max(SkyCloudRenderer.daylightFactor(), 0.45f);
        float[] capLit = {84f * k, 86f * k, 100f * k};
        float[] capShadow = {32f * k, 34f * k, 46f * k};
        float capR = Math.max(r0 * 1.6f, 11f);
        float capY = height + 1f;
        CloudBlobs.emit(buf, mat, new CloudBlobs.Blob(0, capY, 0, capR * 2.8f, capR * 0.5f, capR * 2.8f, 0.45f,
                capLit, capShadow, 0.97f), 0, 0, 0, camX, camY, camZ, sun, 16);
        for (int i = 0; i < 6; i++) {
            double ang = time * 0.12 + i * (Math.PI * 2 / 6);
            double rr = capR * (1.3 + 0.35 * ((i * 7) % 3));
            float rx = capR * (1.3f + 0.25f * ((i * 5) % 4));
            CloudBlobs.emit(buf, mat, new CloudBlobs.Blob(Math.cos(ang) * rr, capY + (i % 2) * 3, Math.sin(ang) * rr,
                    rx, rx * 0.2f, rx, 0.45f, capLit, capShadow, 0.95f), 0, 0, 0, camX, camY, camZ, sun, 14);
        }
    }

    private static float smooth01(float x) {
        float c = Mth.clamp(x, 0f, 1f);
        return c * c * (3f - 2f * c);
    }

    private static float radius(int kind, float r0, float t) {
        float flare = 1f + 0.4f * (float) Math.pow(1f - t, 6);
        float r;
        if (kind == 2) {
            r = r0 * (0.75f + 0.3f * t) + r0 * 1.7f * (float) Math.pow(t, 4.5);
        } else if (kind == 1) {
            r = r0 * (0.28f + 0.3f * t) + r0 * 1.9f * (float) Math.pow(t, 3.2);
        } else {
            r = r0 * (0.1f + 0.1f * t) + r0 * 1.5f * (float) Math.pow(t, 6.0);
        }
        return r * flare;
    }

    /** Dust takes its colour from the block under the tornado (terracotta -> orange, sand -> tan...). */
    private static float[] dustColor(TornadoEntity e) {
        Level level = e.level();
        BlockPos below = e.blockPosition().below();
        BlockState state = level.getBlockState(below);
        int col = state.getMapColor(level, below).col;
        float r = (col >> 16) & 255, g = (col >> 8) & 255, b = col & 255;
        // grass / leaves / plants kick up brown soil, not green mist
        if (g > r + 25f && g > b + 40f) {
            r = 124f;
            g = 98f;
            b = 72f;
        }
        return new float[]{
                Mth.clamp(r * 0.9f + 22f, 0f, 255f),
                Mth.clamp(g * 0.85f + 18f, 0f, 255f),
                Mth.clamp(b * 0.8f + 14f, 0f, 255f)};
    }

    private static void put(VertexConsumer buf, Matrix4f mat, float[][] d, int i) {
        buf.addVertex(mat, d[X][i], d[Y][i], d[Z][i])
                .setColor(Mth.clamp((int) d[R][i], 0, 255), Mth.clamp((int) d[G][i], 0, 255),
                        Mth.clamp((int) d[B][i], 0, 255), Mth.clamp((int) (d[A][i] * 255f), 0, 255))
                .setUv(d[U][i], d[V][i])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}

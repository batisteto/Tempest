package com.batisteto.tempest.client.render;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.client.ClientStormAmbience;
import com.batisteto.tempest.client.ClientWeatherCache;
import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.weather.StormCellData;
import com.batisteto.tempest.weather.StormType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces vanilla's blocky clouds with soft, shaded cloud meshes ({@link CloudBlobs}):
 * an ambient drifting cumulus field that thickens into full overcast near storms, plus a
 * dedicated structure per storm cell - flat dark deck, rising towers, a lowered wall cloud, a
 * downwind-sheared anvil. Vertices are emitted into one translucent
 * batch that the engine depth-sorts, so overlapping blobs blend correctly.
 */
@EventBusSubscriber(modid = Tempest.MODID, value = Dist.CLIENT)
public class SkyCloudRenderer {
    private static final ResourceLocation CLOUD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Tempest.MODID, "textures/misc/cloud_soft.png");
    private static final float CELL = 150f;
    public static final float DECK_HEIGHT = 160f;
    private static final float TWO_PI = (float) (Math.PI * 2);

    private static final float[] FAIR_LIT = {252f, 252f, 255f};
    private static final float[] FAIR_SHADOW = {150f, 163f, 190f};
    private static final float[] OVERCAST_LIT = {138f, 141f, 154f};
    private static final float[] OVERCAST_SHADOW = {54f, 58f, 72f};

    private static final List<CloudBlobs.Blob> BLOBS = new ArrayList<>();
    private static long lastFrameNanos;
    private static double driftX, driftZ;

    private static final float[] SUN = {0.3f, 0.9f, 0.2f};
    private static float daylight = 1f;
    private static float warm = 0f;

    /** Current sun direction (unit vector), updated every frame by the sky pass. */
    public static float[] sunVector() {
        return SUN;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        long now = System.nanoTime();
        double dt = lastFrameNanos == 0 ? 0 : Math.min(0.25, (now - lastFrameNanos) / 1.0e9);
        lastFrameNanos = now;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        // clouds drift with the ambient wind
        driftX += ClientWeatherCache.getWindX() * dt * 0.4;
        driftZ += ClientWeatherCache.getWindZ() * dt * 0.4;

        updateLighting(level);
        float overcast = ClientStormAmbience.overcast();
        double time = level.getGameTime() / 20.0;

        BLOBS.clear();
        buildAmbient(cam, overcast);
        buildStorms(mc, cam, time);
        if (BLOBS.isEmpty()) return;

        RenderType renderType = RenderType.entityTranslucent(CLOUD_TEXTURE);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(renderType);
        int segments = TempestConfig.CLOUD_SEGMENTS.get();

        // At this stage the camera rotation is NOT reliably part of the event's pose stack / the GL
        // model-view matrix (that made clouds follow the camera instead of staying in the world).
        // So: GL model-view = identity, and the view rotation is applied here on the CPU instead.
        // Vertices are camera-relative (world - camera), exactly like vanilla's own sky/cloud code.
        float pitch = camera.getXRot() * ((float) Math.PI / 180f);
        float yaw = camera.getYRot() * ((float) Math.PI / 180f);
         Matrix4f view = new Matrix4f().rotateX(pitch).rotateY(yaw + (float) Math.PI);

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            modelViewStack.identity();
            RenderSystem.applyModelViewMatrix();

            for (CloudBlobs.Blob blob : BLOBS) {
                CloudBlobs.emit(buffer, view, blob, cam.x, cam.y, cam.z, 0, 0, 0, SUN, segments);
            }
            buffers.endBatch(renderType);
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Brightness factor for the time of day (dim at night), shared with the tornado's cloud cap. */
    public static float daylightFactor() {
        return daylight;
    }

    // ---------------------------------------------------------------- lighting

    private static void updateLighting(ClientLevel level) {
        double tt = (level.getDayTime() % 24000L) / 24000.0;
        double theta = (tt - 0.25) * Math.PI * 2;
        float h = (float) Math.cos(theta);
        float sx = (float) -Math.sin(theta);
        float sy = Math.max(h, 0.25f);
        float len = Mth.sqrt(sx * sx + sy * sy + 0.04f);
        SUN[0] = sx / len;
        SUN[1] = sy / len;
        SUN[2] = 0.2f / len;
        daylight = Mth.clamp(h * 1.6f + 0.4f, 0.14f, 1f);
        warm = Mth.clamp(1f - Math.abs(h) / 0.28f, 0f, 1f);
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
    }

    private static float[] scale(float[] c, float k) {
        return new float[]{Mth.clamp(c[0] * k, 0f, 255f), Mth.clamp(c[1] * k, 0f, 255f), Mth.clamp(c[2] * k, 0f, 255f)};
    }

    /** Apply time of day: dim at night, warm orange near sunrise/sunset. */
    private static float[] daylit(float[] c, float k, float warmth) {
        return new float[]{
                Mth.clamp(c[0] * k + warmth * 22f, 0f, 255f),
                Mth.clamp(c[1] * k - warmth * 10f, 0f, 255f),
                Mth.clamp(c[2] * k - warmth * 45f, 0f, 255f)};
    }

    // ------------------------------------------------------------ ambient field

    private static float rand(long seed, int salt) {
        long z = seed + salt * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        return (float) ((z >>> 40) / 16777216.0);
    }

    private static void buildAmbient(Vec3 cam, float overcast) {
        int range = TempestConfig.CLOUD_RANGE_CELLS.get();
        float density = TempestConfig.CLOUD_DENSITY.get().floatValue();
        float coverage = Mth.clamp(Mth.lerp(overcast, 0.30f, 0.92f) * density, 0f, 0.97f);

        float[] lit = daylit(mix(FAIR_LIT, OVERCAST_LIT, overcast), daylight, warm);
        float[] shadow = daylit(mix(FAIR_SHADOW, OVERCAST_SHADOW, overcast), daylight * 0.9f, warm * 0.5f);

        int cx0 = Mth.floor((cam.x - driftX) / CELL);
        int cz0 = Mth.floor((cam.z - driftZ) / CELL);
        double maxDist = range * CELL;

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                long ix = cx0 + dx, iz = cz0 + dz;
                long seed = ix * 341873128712L + iz * 132897987541L;
                if (rand(seed, 0) > coverage) continue;

                double wx = ix * CELL + rand(seed, 1) * CELL + driftX;
                double wz = iz * CELL + rand(seed, 2) * CELL + driftZ;
                double dist = Math.hypot(wx - cam.x, wz - cam.z);
                if (dist > maxDist) continue;

                float fade = Mth.clamp((float) ((maxDist - dist) / (CELL * 1.5)), 0f, 1f);
                float size = (0.75f + rand(seed, 3) * 0.9f) * (1f + overcast * 0.5f);
                double baseY = 178 + rand(seed, 4) * 40 - overcast * 22;

                if (rand(seed, 5) > 0.94f && overcast < 0.5f) {
                    // lone lenticular cap
                    BLOBS.add(new CloudBlobs.Blob(wx, baseY + 22, wz, 85f * size, 13f * size, 60f * size, 0.5f,
                            lit, shadow, 0.9f * fade));
                    continue;
                }

                int n = 5 + (int) (rand(seed, 6) * 4);
                 for (int k = 0; k < n; k++) {
                    double bx = wx + (rand(seed, 10 + k) - 0.5) * 80 * size;
                    double bz = wz + (rand(seed, 20 + k) - 0.5) * 80 * size;
                    double by = baseY + rand(seed, 30 + k) * 12 * size;
                    float rx = (20 + rand(seed, 40 + k) * 24) * size;
                    float ry = rx * (0.40f + rand(seed, 50 + k) * 0.22f);
                    float rz = rx * (0.8f + rand(seed, 60 + k) * 0.4f);
                    BLOBS.add(new CloudBlobs.Blob(bx, by, bz, rx, ry, rz, 0.2f, lit, shadow, 0.93f * fade));
                }
            }
        }
    }

    // ------------------------------------------------------------------ storms

    private static float[][] palette(StormType type, float intensity) {
        float[][] base = switch (type) {
            case RAIN -> new float[][]{{165f, 168f, 178f}, {95f, 100f, 114f}};
            case SQUALL -> new float[][]{{150f, 153f, 166f}, {80f, 85f, 100f}};
            case THUNDERSTORM -> new float[][]{{128f, 130f, 144f}, {60f, 63f, 78f}};
            case SUPERCELL -> new float[][]{{108f, 108f, 126f}, {42f, 42f, 58f}};
            case CYCLONE -> new float[][]{{96f, 100f, 118f}, {36f, 40f, 54f}};
        };
        float k = Math.max(daylight, 0.45f) * (1f - 0.18f * intensity);
        return new float[][]{daylit(base[0], k, warm * 0.5f), daylit(base[1], k, warm * 0.3f)};
    }

    private static void buildStorms(Minecraft mc, Vec3 cam, double time) {
        List<StormCellData> storms = ClientWeatherCache.getStorms();
        if (storms.isEmpty()) return;

        float ahead = ClientWeatherCache.ticksSinceUpdate();
        double maxDist = TempestConfig.STORM_RENDER_DISTANCE.get();
        float windX = ClientWeatherCache.getWindX(), windZ = ClientWeatherCache.getWindZ();
        float windLen = Math.max(0.001f, Mth.sqrt(windX * windX + windZ * windZ));

        for (StormCellData s : storms) {
            double sx = s.x() + s.motionX() * ahead;
            double sz = s.z() + s.motionZ() * ahead;
            double dx = sx - cam.x, dz = sz - cam.z;
            if (dx * dx + dz * dz > maxDist * maxDist) continue;
            buildStorm(s, sx, sz, time, windX / windLen, windZ / windLen);
        }
    }

    private static void buildStorm(StormCellData s, double sx, double sz, double time,
                                   float windDirX, float windDirZ) {
        StormType type = s.stormType();
        float radius = s.radius() * 1.15f;
        float intensity = s.intensity();
        float[][] pal = palette(type, intensity);
        float[] lit = pal[0], shadow = pal[1];
        long seed = s.id() * 7919L;
        boolean severe = type == StormType.SUPERCELL || type == StormType.CYCLONE;
        float spin = (float) (time * 0.03);
        float presence = Mth.clamp(intensity * 2.5f, 0.25f, 1f);

        // flat dark deck - the cloud base
        int deckCount = Mth.clamp((int) (radius / 14f), 7, 18);
        for (int i = 0; i < deckCount; i++) {
            float ang = rand(seed, i) * TWO_PI;
            float rr = radius * 0.85f * Mth.sqrt(rand(seed, 100 + i));
            double bx = sx + Math.cos(ang) * rr;
            double bz = sz + Math.sin(ang) * rr;
            double by = DECK_HEIGHT - intensity * 10 + rand(seed, 200 + i) * 6;
            float rx = radius * (0.26f + 0.2f * rand(seed, 300 + i));
            BLOBS.add(new CloudBlobs.Blob(bx, by, bz, rx, rx * 0.2f, rx * (0.8f + 0.4f * rand(seed, 400 + i)), 0.4f,
                    lit, shadow, 0.96f * presence));
        }

        // rising towers
        if (type.canThunder) {
            int towers = severe ? 7 : 4;
            float[] towerLit = scale(lit, 1.12f);
            for (int i = 0; i < towers; i++) {
                float ang = spin * 0.8f + i * TWO_PI / towers + rand(seed, 500 + i);
                float rr = radius * (0.1f + 0.4f * rand(seed, 600 + i));
                double bx = sx + Math.cos(ang) * rr;
                double bz = sz + Math.sin(ang) * rr;
                double by = DECK_HEIGHT + 12 + rand(seed, 700 + i) * 34 * intensity;
                float rx = radius * (0.2f + 0.12f * rand(seed, 800 + i));
                BLOBS.add(new CloudBlobs.Blob(bx, by, bz, rx, rx * 0.8f, rx, 0.5f, towerLit, shadow, 0.95f * presence));
            }
        }

        if (severe) {
            // lowered, rotating wall cloud where tornadoes form
            float[] wallLit = scale(lit, 0.7f), wallShadow = scale(shadow, 0.7f);
            for (int i = 0; i < 3; i++) {
                float ang = spin * 1.5f + i * TWO_PI / 3f;
                double bx = sx + Math.cos(ang) * radius * 0.12;
                double bz = sz + Math.sin(ang) * radius * 0.12;
                float rx = radius * 0.17f;
                BLOBS.add(new CloudBlobs.Blob(bx, DECK_HEIGHT - 11, bz, rx, rx * 0.6f, rx, 1f,
                        wallLit, wallShadow, 0.97f * presence));
            }

            // anvil, sheared downwind
            float[] anvilLit = scale(lit, 1.25f), anvilShadow = scale(shadow, 1.15f);
            for (int i = 0; i < 5; i++) { double bx = sx + windDirX * radius * 0.5 + (rand(seed, 900 + i) - 0.5) * radius * 0.8;
                double bz = sz + windDirZ * radius * 0.5 + (rand(seed, 1000 + i) - 0.5) * radius * 0.8;
                float rx = radius * (0.45f + 0.25f * rand(seed, 1100 + i));
                BLOBS.add(new CloudBlobs.Blob(bx, DECK_HEIGHT + 56 + rand(seed, 1200 + i) * 6, bz,
                        rx, rx * 0.09f, rx, 0.6f, anvilLit, anvilShadow,
                        0.85f * Mth.clamp(intensity * 1.6f, 0f, 1f)));
            }
        }
    }
}
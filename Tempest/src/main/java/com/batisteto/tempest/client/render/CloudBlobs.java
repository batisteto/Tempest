package com.batisteto.tempest.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Soft-edged, shaded ellipsoid "blobs" - the building block for clouds, rain shafts and dust skirts.
 *
 * Instead of flat textured cards, each blob is a real 3D mesh: per-vertex colour is lit from the sun
 * (bright tops, dark undersides) and per-vertex alpha fades towards the silhouette (facing ratio),
 * which is what gives the smooth, volumetric look without a shader. Only camera-facing quads are
 * emitted, so a blob never overlaps itself.
 */
public final class CloudBlobs {
    private CloudBlobs() {
    }

    public static final class Blob {
        public final double x, y, z;
        public final float rx, ry, rz;
        /** Scale applied to the lower half: 1 = round, ~0.2 = flat-bottomed cumulus. */
        public final float flatBottom;
        public final float[] lit;
        public final float[] shadow;
        public final float alpha;

        public Blob(double x, double y, double z, float rx, float ry, float rz, float flatBottom,
                    float[] lit, float[] shadow, float alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.flatBottom = flatBottom;
            this.lit = lit;
            this.shadow = shadow;
            this.alpha = alpha;
        }
    }

    private static int cols = -1;
    private static int rows;
    private static float[] ux, uy, uz, px, py, pz, cr, cg, cb, ca, face;

    private static void ensureGrid(int n) {
        if (n == cols) return;
        cols = n;
        rows = Math.max(5, (n * 2) / 3);
        int count = (cols + 1) * (rows + 1);
        ux = new float[count];
        uy = new float[count];
        uz = new float[count];
        px = new float[count];
        py = new float[count];
        pz = new float[count];
        cr = new float[count];
        cg = new float[count];
        cb = new float[count];
        ca = new float[count];
        face = new float[count];
        for (int j = 0; j <= rows; j++) {
            double lat = -Math.PI / 2 + Math.PI * j / rows;
            for (int i = 0; i <= cols; i++) {
                double lon = Math.PI * 2 * i / cols;
                int idx = j * (cols + 1) + i;
                ux[idx] = (float) (Math.cos(lat) * Math.cos(lon));
                uy[idx] = (float) Math.sin(lat);
                uz[idx] = (float) (Math.cos(lat) * Math.sin(lon));
            }
        }
    }

    /**
     * Vertices are placed at (blob - off). Camera and matrix must be in that same frame: e.g. off = camera
     * position with the camera at (0,0,0) for the sky pass, or off = 0 with the camera given relative to
     * the entity for entity renderers.
     */
    public static void emit(VertexConsumer buf, Matrix4f mat, Blob b, double offX, double offY, double offZ,
                            double camX, double camY, double camZ, float[] sun, int segments) {
        ensureGrid(segments);
        int count = (cols + 1) * (rows + 1);

        for (int idx = 0; idx < count; idx++) {
            float sx = ux[idx], sy = uy[idx], sz = uz[idx];
            float ys = sy < 0 ? b.flatBottom : 1f;

            float wx = (float) (b.x - offX) + sx * b.rx;
            float wy = (float) (b.y - offY) + sy * b.ry * ys;
            float wz = (float) (b.z - offZ) + sz * b.rz;

            float nx = sx / b.rx, ny = sy / (b.ry * ys), nz = sz / b.rz;
            float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl < 1e-6f) nl = 1e-6f;
            nx /= nl;
            ny /= nl;
            nz /= nl;

            float vx = (float) (camX - wx), vy = (float) (camY - wy), vz = (float) (camZ - wz);
            float vl = Mth.sqrt(vx * vx + vy * vy + vz * vz);
            if (vl < 1e-3f) vl = 1e-3f;
            float facing = (nx * vx + ny * vy + nz * vz) / vl;

            float edge = Mth.clamp(facing * 1.7f, 0f, 1f);
            edge = edge * edge * (3f - 2f * edge);

            float light = Mth.clamp(0.5f * (ny * 0.8f + 0.6f * (nx * sun[0] + ny * sun[1] + nz * sun[2])) + 0.5f, 0f, 1f);

            px[idx] = wx;
            py[idx] = wy;
            pz[idx] = wz;
            face[idx] = facing;
            cr[idx] = b.shadow[0] + (b.lit[0] - b.shadow[0]) * light;
            cg[idx] = b.shadow[1] + (b.lit[1] - b.shadow[1]) * light;
            cb[idx] = b.shadow[2] + (b.lit[2] - b.shadow[2]) * light;
            ca[idx] = b.alpha * edge;
        }

        int stride = cols + 1;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                int v0 = j * stride + i;
                int v1 = v0 + 1;
                int v2 = v1 + stride;
                int v3 = v0 + stride;
                if (face[v0] + face[v1] + face[v2] + face[v3] <= 0.08f) continue;
                float u0 = i / (float) cols, u1 = (i + 1) / (float) cols;
                float t0 = j / (float) rows, t1 = (j + 1) / (float) rows;
                put(buf, mat, v0, u0, t0);
                put(buf, mat, v1, u1, t0);
                put(buf, mat, v2, u1, t1);
                put(buf, mat, v3, u0, t1);
            }
        }
    }

    private static void put(VertexConsumer buf, Matrix4f mat, int v, float u, float t) {
        buf.addVertex(mat, px[v], py[v], pz[v])
                .setColor(Mth.clamp((int) cr[v], 0, 255), Mth.clamp((int) cg[v], 0, 255),
                        Mth.clamp((int) cb[v], 0, 255), Mth.clamp((int) (ca[v] * 255f), 0, 255))
                .setUv(u, t)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}

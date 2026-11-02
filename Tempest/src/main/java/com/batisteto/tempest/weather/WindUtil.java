package com.batisteto.tempest.weather;

import java.util.List;

/** Combines ambient wind with nearby storm inflow into a single local wind vector. */
public final class WindUtil {
    private WindUtil() {
    }

    public static float[] computeWindAt(double x, double z, float ambientX, float ambientZ, List<StormCellData> storms) {
        float wx = ambientX;
        float wz = ambientZ;
        for (StormCellData storm : storms) {
            double dx = storm.x() - x;
            double dz = storm.z() - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double influenceRadius = storm.radius() * 2.2;
            if (dist > influenceRadius || dist < 0.001) continue;
            double falloff = 1.0 - (dist / influenceRadius);
            double strength = falloff * falloff * storm.intensity() * 22.0;
            wx += (float) (dx / dist * strength);
            wz += (float) (dz / dist * strength);
        }
        return new float[]{wx, wz};
    }

    public static float speedMph(float wx, float wz) {
        return (float) Math.sqrt(wx * wx + wz * wz) * 2.237f;
    }

    public static String compassDirection(float wx, float wz) {
        double deg = Math.toDegrees(Math.atan2(wx, -wz));
        if (deg < 0) deg += 360;
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int idx = (int) Math.round(deg / 45.0) % 8;
        return dirs[idx];
    }
}

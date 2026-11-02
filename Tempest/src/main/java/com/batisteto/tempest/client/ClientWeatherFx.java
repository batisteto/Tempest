package com.batisteto.tempest.client;

import com.batisteto.tempest.Tempest;
import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.registry.ModParticles;
import com.batisteto.tempest.weather.StormCellData;
import com.batisteto.tempest.weather.StormType;
import com.batisteto.tempest.weather.WindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Spawns wind-slanted rain streaks and hail around the player while they're inside a storm's
 * footprint. Slant comes straight from the local wind vector: stronger wind = flatter, longer
 * diagonal streaks.
 */
@EventBusSubscriber(modid = Tempest.MODID, value = Dist.CLIENT)
public class ClientWeatherFx {
    // Kept in sync with ClientStormAmbience.
    private static final float TRIGGER_RADIUS_MULT = 1.3f;
    private static final double FALL_SPEED = 1.6;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        List<StormCellData> storms = ClientWeatherCache.getStorms();
        StormCellData active = findStormAt(storms, player.getX(), player.getZ());
        if (active == null || active.intensity() < 0.08f) return;

        float[] wind = WindUtil.computeWindAt(player.getX(), player.getZ(),
                ClientWeatherCache.getWindX(), ClientWeatherCache.getWindZ(), storms);
        float driftX = Mth.clamp(wind[0] * 0.045f, -1.6f, 1.6f);
        float driftZ = Mth.clamp(wind[1] * 0.045f, -1.6f, 1.6f);

        RandomSource random = mc.level.random;
        float density = TempestConfig.PARTICLE_DENSITY.get().floatValue();

        // Slanted rain arrives from upwind, so shift the spawn volume upwind by roughly the
        // horizontal distance a streak covers while falling.
        double offX = -driftX / FALL_SPEED * 12.0;
        double offZ = -driftZ / FALL_SPEED * 12.0;

        int rainCount = (int) (14 + active.intensity() * 80 * density);
        for (int i = 0; i < rainCount; i++) {
            double px = player.getX() + offX + (random.nextDouble() - 0.5) * 46;
            double pz = player.getZ() + offZ + (random.nextDouble() - 0.5) * 46;
            double py = player.getY() + random.nextDouble() * 26 - 4;
            if (py < mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz))) continue;
            mc.level.addParticle(ModParticles.RAIN_STREAK.get(), px, py, pz, driftX, -FALL_SPEED, driftZ);
        }

        boolean severe = active.stormType() == StormType.SUPERCELL || active.stormType() == StormType.CYCLONE
                || (active.stormType() == StormType.THUNDERSTORM && active.intensity() > 0.6f);
        if (severe) {
            int hailCount = (int) (4 + active.intensity() * 9 * density);
            for (int i = 0; i < hailCount; i++) {
                double px = player.getX() + offX + (random.nextDouble() - 0.5) * 28;
                double pz = player.getZ() + offZ + (random.nextDouble() - 0.5) * 28;
                double py = player.getY() + 10 + random.nextDouble() * 6;
                mc.level.addParticle(ModParticles.HAIL.get(), px, py, pz, driftX * 1.4, -1.0, driftZ * 1.4);
            }
        }
    }

    private static StormCellData findStormAt(List<StormCellData> storms, double x, double z) {
        StormCellData best = null;
        double bestDist = Double.MAX_VALUE;
        for (StormCellData storm : storms) {
            double dx = storm.x() - x;
            double dz = storm.z() - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < storm.radius() * TRIGGER_RADIUS_MULT && dist < bestDist) {
                best = storm;
                bestDist = dist;
            }
        }
        return best;
    }
}

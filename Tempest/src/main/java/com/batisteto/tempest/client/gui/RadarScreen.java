package com.batisteto.tempest.client.gui;

import com.batisteto.tempest.client.ClientWeatherCache;
import com.batisteto.tempest.entity.TornadoEntity;
import com.batisteto.tempest.weather.StormCellData;
import com.batisteto.tempest.weather.StormType;
import com.batisteto.tempest.weather.WindUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** NEXRAD-reflectivity-styled top-down radar reading from the already-synced weather cache. */
public class RadarScreen extends Screen {
    private static final double RANGE = 900.0;

    public RadarScreen() {
        super(Component.literal("Tempest Doppler Radar"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int radiusPx = Math.min(this.width, this.height) / 3;

        g.fill(cx - radiusPx - 4, cy - radiusPx - 4, cx + radiusPx + 4, cy + radiusPx + 4, 0xC0001A0D);
        drawRing(g, cx, cy, radiusPx, 0xFF1F6B3A);
        drawRing(g, cx, cy, radiusPx * 2 / 3, 0x601F6B3A);
        drawRing(g, cx, cy, radiusPx / 3, 0x601F6B3A);

        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) return;
        double px = this.minecraft.player.getX();
        double pz = this.minecraft.player.getZ();

        List<StormCellData> storms = ClientWeatherCache.getStorms();
        boolean tornadoWatch = false;
        for (StormCellData storm : storms) {
            double dx = storm.x() - px;
            double dz = storm.z() - pz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > RANGE) continue;
            int sx = cx + (int) (dx / RANGE * radiusPx);
            int sy = cy + (int) (dz / RANGE * radiusPx);
            int blipColor = reflectivityColor(storm.intensity(), storm.stormType());
            int blipRadius = Math.max(2, (int) (storm.radius() / RANGE * radiusPx));
            g.fill(sx - blipRadius, sy - blipRadius, sx + blipRadius, sy + blipRadius, blipColor);
            if (storm.stormType() == StormType.SUPERCELL || storm.stormType() == StormType.CYCLONE) {
                tornadoWatch = true;
            }
        }

        boolean tornadoWarning = !this.minecraft.level.getEntitiesOfClass(TornadoEntity.class,
                new AABB(px - RANGE, this.minecraft.player.getY() - 60, pz - RANGE,
                        px + RANGE, this.minecraft.player.getY() + 60, pz + RANGE)).isEmpty();

        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);

        g.drawCenteredString(this.font, "TEMPEST DOPPLER RADAR", cx, cy - radiusPx - 26, 0xFFFFFFFF);

        float[] wind = WindUtil.computeWindAt(px, pz, ClientWeatherCache.getWindX(), ClientWeatherCache.getWindZ(), storms);
        String windLine = String.format("Wind: %.1f mph %s", WindUtil.speedMph(wind[0], wind[1]), WindUtil.compassDirection(wind[0], wind[1]));
        g.drawCenteredString(this.font, windLine, cx, cy + radiusPx + 12, 0xFF99CCFF);

        if (tornadoWarning) {
            g.drawCenteredString(this.font, "\u00A74\u00A7lTORNADO WARNING - TAKE SHELTER", cx, cy + radiusPx + 26, 0xFFFF5555);
        } else if (tornadoWatch) {
            g.drawCenteredString(this.font, "\u00A76TORNADO WATCH - SUPERCELL DETECTED", cx, cy + radiusPx + 26, 0xFFFFAA00);
        }
    }

    private void drawRing(GuiGraphics g, int cx, int cy, int r, int color) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments;
            int x = cx + (int) (Math.cos(a) * r);
            int y = cy + (int) (Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private int reflectivityColor(float intensity, StormType type) {
        if (intensity < 0.25f) return 0xFF2E8B57;
        if (intensity < 0.5f) return 0xFFDAA520;
        if (intensity < 0.75f) return 0xFFCD5C5C;
        return (type == StormType.SUPERCELL || type == StormType.CYCLONE) ? 0xFFFF00FF : 0xFFFF2222;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

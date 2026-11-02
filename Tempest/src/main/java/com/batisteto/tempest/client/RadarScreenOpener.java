package com.batisteto.tempest.client;

import com.batisteto.tempest.client.gui.RadarScreen;
import net.minecraft.client.Minecraft;

/** Kept in its own class so it is only ever class-loaded when actually invoked client-side. */
public final class RadarScreenOpener {
    private RadarScreenOpener() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new RadarScreen());
    }
}

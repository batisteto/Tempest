package com.batisteto.tempest.weather;

/** Broad categories of storm cell Tempest can simulate. */
public enum StormType {
    RAIN(false, false, 40f, 0f),
    SQUALL(false, false, 55f, 0.05f),
    THUNDERSTORM(true, false, 70f, 0.1f),
    SUPERCELL(true, true, 140f, 0.35f),
    CYCLONE(true, true, 220f, 0.25f);

    public final boolean canThunder;
    public final boolean canTornado;
    public final float baseRadius;
    public final float baseMesocycloneGrowth;

    StormType(boolean canThunder, boolean canTornado, float baseRadius, float baseMesocycloneGrowth) {
        this.canThunder = canThunder;
        this.canTornado = canTornado;
        this.baseRadius = baseRadius;
        this.baseMesocycloneGrowth = baseMesocycloneGrowth;
    }

    public static StormType byOrdinalClamped(int ordinal) {
        StormType[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}

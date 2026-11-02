package com.batisteto.tempest.weather;

/** Enhanced Fujita scale tiers. Public meteorological classification, not tied to any mod's assets. */
public enum EFScale {
    EF0(65, 85, 0, 0.4f, 6f),
    EF1(86, 110, 0, 0.8f, 9f),
    EF2(111, 135, 1, 1.4f, 13f),
    EF3(136, 165, 1, 2.2f, 17f),
    EF4(166, 200, 2, 3.2f, 22f),
    EF5(201, 320, 3, 4.5f, 28f);

    public final int minMph;
    public final int maxMph;
    /** Which tornado_destructible_tierN block tag this EF level is allowed to break. */
    public final int destructionTier;
    public final float damagePerHit;
    public final float baseRadius;

    EFScale(int minMph, int maxMph, int destructionTier, float damagePerHit, float baseRadius) {
        this.minMph = minMph;
        this.maxMph = maxMph;
        this.destructionTier = destructionTier;
        this.damagePerHit = damagePerHit;
        this.baseRadius = baseRadius;
    }

    public static EFScale byOrdinalClamped(int ordinal) {
        EFScale[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}

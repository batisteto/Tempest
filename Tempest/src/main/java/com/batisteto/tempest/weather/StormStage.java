package com.batisteto.tempest.weather;

public enum StormStage {
    DEVELOPING,
    MATURE,
    DISSIPATING;

    public static StormStage byOrdinalClamped(int ordinal) {
        StormStage[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}

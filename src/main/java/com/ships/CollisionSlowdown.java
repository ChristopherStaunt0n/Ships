package com.ships;

final class CollisionSlowdown {
    private final double durationSeconds;
    private final double reduction;
    private double remainingSeconds;

    CollisionSlowdown(double durationSeconds, double reductionPercent) {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Collision slowdown duration must be positive");
        }
        if (!Double.isFinite(reductionPercent) || reductionPercent < 0 || reductionPercent > 100) {
            throw new IllegalArgumentException("Collision slowdown reduction must be from 0 to 100 percent");
        }
        this.durationSeconds = durationSeconds;
        this.reduction = reductionPercent / 100.0;
    }

    void trigger() {
        remainingSeconds = durationSeconds;
    }

    void advance(double elapsedSeconds) {
        remainingSeconds = Math.max(0, remainingSeconds - elapsedSeconds);
    }

    double speedMultiplier() {
        if (remainingSeconds == 0) return 1;
        return 1 - reduction * remainingSeconds / durationSeconds;
    }

    double remainingSeconds() {
        return remainingSeconds;
    }
}

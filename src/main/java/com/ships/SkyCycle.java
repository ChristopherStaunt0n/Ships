package com.ships;

import java.awt.Color;

final class SkyCycle {
    static final double CYCLE_SECONDS = 5 * 60;

    private static final double[] STOPS = {0, 0.17, 0.31, 0.69, 0.84, 1};
    private static final Color[] SKY_TINTS = {
            new Color(13, 20, 67, 92),
            new Color(246, 125, 105, 52),
            new Color(67, 166, 210, 12),
            new Color(67, 166, 210, 12),
            new Color(235, 105, 123, 48),
            new Color(13, 20, 67, 92)
    };
    private static final Color[] HORIZON_TINTS = {
            new Color(20, 35, 79, 66),
            new Color(252, 174, 109, 42),
            new Color(139, 215, 223, 8),
            new Color(139, 215, 223, 8),
            new Color(251, 151, 98, 54),
            new Color(20, 35, 79, 66)
    };

    private SkyCycle() {}

    static Color topTint(double elapsedSeconds) {
        return sample(elapsedSeconds, SKY_TINTS);
    }

    static Color horizonTint(double elapsedSeconds) {
        return sample(elapsedSeconds, HORIZON_TINTS);
    }

    private static Color sample(double elapsedSeconds, Color[] colors) {
        double cycle = ((elapsedSeconds % CYCLE_SECONDS) + CYCLE_SECONDS) % CYCLE_SECONDS;
        double position = cycle / CYCLE_SECONDS;
        for (int i = 1; i < STOPS.length; i++) {
            if (position <= STOPS[i]) {
                double progress = (position - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
                double eased = (1 - Math.cos(progress * Math.PI)) / 2;
                return interpolate(colors[i - 1], colors[i], eased);
            }
        }
        return colors[colors.length - 1];
    }

    private static Color interpolate(Color from, Color to, double amount) {
        return new Color(channel(from.getRed(), to.getRed(), amount),
                channel(from.getGreen(), to.getGreen(), amount),
                channel(from.getBlue(), to.getBlue(), amount),
                channel(from.getAlpha(), to.getAlpha(), amount));
    }

    private static int channel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }
}

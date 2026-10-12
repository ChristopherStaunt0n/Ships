package com.ships;

import java.awt.geom.Point2D;

final class PlayerControls {
    private PlayerControls() {}

    static Point2D.Double velocityTowardMouse(double velocityX, double velocityY,
                                              double deltaX, double deltaY, double speed,
                                              boolean steering) {
        if (!steering) return new Point2D.Double(velocityX, velocityY);
        return new Point2D.Double(clamp(deltaX, speed), clamp(deltaY, speed));
    }

    private static double clamp(double value, double maximum) {
        return Math.max(-maximum, Math.min(maximum, value));
    }
}

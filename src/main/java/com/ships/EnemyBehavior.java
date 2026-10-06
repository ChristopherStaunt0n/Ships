package com.ships;

import java.awt.geom.Point2D;

/** A strategy can be replaced on a live enemy whenever its tactical situation changes. */
public interface EnemyBehavior {
    Point2D.Double direction(EnemyContext context);

    record EnemyContext(double x, double y, double playerX, double playerY, double healthRatio) {}
}

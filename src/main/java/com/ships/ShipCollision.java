package com.ships;

final class ShipCollision {
    private ShipCollision() {}

    static final class Body {
        double x;
        double y;
        double vx;
        double vy;
        final double radius;

        Body(double x, double y, double vx, double vy, double radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }
    }

    static boolean bounce(Body first, Body second) {
        double dx = second.x - first.x;
        double dy = second.y - first.y;
        double minimumDistance = first.radius + second.radius;
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance) return false;

        double distance = Math.sqrt(distanceSquared);
        double nx = distance == 0 ? 1 : dx / distance;
        double ny = distance == 0 ? 0 : dy / distance;
        double overlap = minimumDistance - distance;
        first.x -= nx * overlap / 2;
        first.y -= ny * overlap / 2;
        second.x += nx * overlap / 2;
        second.y += ny * overlap / 2;

        double relativeNormalSpeed = (second.vx - first.vx) * nx + (second.vy - first.vy) * ny;
        if (relativeNormalSpeed < 0) {
            first.vx += relativeNormalSpeed * nx;
            first.vy += relativeNormalSpeed * ny;
            second.vx -= relativeNormalSpeed * nx;
            second.vy -= relativeNormalSpeed * ny;
        }
        return true;
    }

    static boolean bounceAtBounds(Body body, double width, double height, double topInset) {
        boolean collided = false;
        double left = body.radius;
        double right = Math.max(left, width - body.radius);
        double top = topInset + body.radius;
        double bottom = Math.max(top, height - body.radius);
        if (body.x < left) {
            body.x = left;
            if (body.vx < 0) body.vx = -body.vx;
            collided = true;
        } else if (body.x > right) {
            body.x = right;
            if (body.vx > 0) body.vx = -body.vx;
            collided = true;
        }
        if (body.y < top) {
            body.y = top;
            if (body.vy < 0) body.vy = -body.vy;
            collided = true;
        } else if (body.y > bottom) {
            body.y = bottom;
            if (body.vy > 0) body.vy = -body.vy;
            collided = true;
        }
        return collided;
    }
}

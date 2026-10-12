package com.ships;

final class TurnMath {
    private TurnMath() {}

    static double rotateToward(double current, double target, double maxStep) {
        double difference = Math.atan2(Math.sin(target - current), Math.cos(target - current));
        if (Math.abs(difference) <= maxStep) return target;
        return current + Math.copySign(maxStep, difference);
    }
}

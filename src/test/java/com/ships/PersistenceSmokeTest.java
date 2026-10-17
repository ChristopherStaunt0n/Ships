package com.ships;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PersistenceSmokeTest {
    public static void main(String[] args) {
        List<ShipPreset> presets = ShipPreset.defaults();
        require(presets.size() == 3, "expected three starter presets");
        require(presets.stream().map(ShipPreset::id).distinct().count() == 3, "preset IDs must be unique");

        Map<String, Object> presetDocument = Map.of("presets",
                presets.stream().map(ShipPreset::toJson).toList());
        Object decoded = Json.parse(Json.stringify(presetDocument));
        List<?> entries = (List<?>) ShipPreset.object(decoded).get("presets");
        List<ShipPreset> restored = entries.stream()
                .map(entry -> ShipPreset.fromJson(ShipPreset.object(entry))).toList();
        require(restored.equals(presets), "preset JSON round-trip must preserve all visual states");

        GameStore.SavedGame save = new GameStore.SavedGame(UUID.randomUUID().toString(), "Smoke test",
                presets.get(0).id(), 125, 240, 75, 100, 5, 6, 3, 50, 15, 480, true, 120,
                List.of(new GameStore.EnemySave(presets.get(1).id(), 400, 350, 40, 80)));
        GameStore.SavedGame restoredSave = GameStore.SavedGame.fromJson(
                ShipPreset.object(Json.parse(Json.stringify(save.toJson()))));
        require(restoredSave.equals(save), "save JSON round-trip must preserve game state");
        require(restoredSave.turnSpeed() == 6, "saved turn speed must round-trip");
        require(restoredSave.collisionDuration() == 3 && restoredSave.collisionReduction() == 50,
                "collision modifiers must round-trip through save data");
        require(restoredSave.showFrameRate(), "frame-rate preference must round-trip through save data");
        Map<String, Object> legacySaveData = new java.util.LinkedHashMap<>(save.toJson());
        legacySaveData.remove("turnSpeed");
        legacySaveData.remove("collisionDuration");
        legacySaveData.remove("collisionReduction");
        legacySaveData.remove("showFrameRate");
        GameStore.SavedGame legacySave = GameStore.SavedGame.fromJson(legacySaveData);
        require(legacySave.turnSpeed() == 6, "older saves must receive the default turn speed");
        require(legacySave.collisionDuration() == 3 && legacySave.collisionReduction() == 50,
                "older saves must receive the default collision settings");
        require(legacySave.showFrameRate(), "older saves must default to showing frame rate");
        FrameRateCounter frameRate = new FrameRateCounter(0);
        for (int frame = 0; frame < 60; frame++) {
            frameRate.frame((long) frame * 16_000_000);
        }
        require(frameRate.displayedFrames() == 0, "frame counter must wait for its first one-second window");
        frameRate.frame(1_000_000_000L);
        require(frameRate.displayedFrames() == 61,
                "frame counter must report the actual frames in the elapsed one-second window");
        CollisionSlowdown slowdown = new CollisionSlowdown(3, 50);
        require(slowdown.speedMultiplier() == 1, "collision modifier is inactive before impact");
        slowdown.trigger();
        require(slowdown.speedMultiplier() == 0.5, "collision applies configured initial reduction");
        slowdown.advance(1.5);
        require(slowdown.speedMultiplier() == 0.75, "collision reduction should fade linearly");
        slowdown.advance(1.5);
        require(slowdown.speedMultiplier() == 1 && slowdown.remainingSeconds() == 0,
                "collision reduction should expire after its configured duration");
        require(TurnMath.rotateToward(0, Math.PI / 2, Math.PI / 4) == Math.PI / 4,
                "turning must be limited to the configured step");
        require(TurnMath.rotateToward(Math.toRadians(179), Math.toRadians(-179), Math.toRadians(3))
                        == Math.toRadians(-179),
                "turning must use the shortest direction across angle wraparound");
        require(SkyCycle.topTint(0).equals(SkyCycle.topTint(SkyCycle.CYCLE_SECONDS)),
                "sky tint must repeat at the five-minute cycle boundary");
        Color nightTint = SkyCycle.topTint(0);
        Color daylightTint = SkyCycle.topTint(SkyCycle.CYCLE_SECONDS * 0.5);
        require(!nightTint.equals(daylightTint) && daylightTint.getAlpha() < nightTint.getAlpha(),
                "sky tint must transition between night and a less-tinted daylight");
        require(SkyCycle.horizonTint(SkyCycle.CYCLE_SECONDS * 0.2).getAlpha() < 255,
                "sky tint must remain translucent");
        Point2D.Double coastingVelocity = PlayerControls.velocityTowardMouse(3, -2, -8, 7, 5, false);
        require(coastingVelocity.x == 3 && coastingVelocity.y == -2,
                "mouse movement must not redirect player velocity when steering is inactive");
        Point2D.Double steeredVelocity = PlayerControls.velocityTowardMouse(3, -2, -8, 7, 5, true);
        require(steeredVelocity.x == -5 && steeredVelocity.y == 5,
                "holding steering must update player velocity toward the mouse");
        require(presets.get(0).shapeAt(100, 100, 0, 1).contains(100, 100),
                "ship hit-zone shape must contain its center");
        ShipCollision.Body leftShip = new ShipCollision.Body(10, 20, 2, 0, 10);
        ShipCollision.Body rightShip = new ShipCollision.Body(25, 20, -2, 0, 10);
        require(ShipCollision.bounce(leftShip, rightShip), "overlapping ships must collide");
        require(leftShip.x < 10 && rightShip.x > 25, "colliding ships must be separated");
        require(leftShip.vx < 0 && rightShip.vx > 0, "colliding ships must bounce apart");
        ShipCollision.Body wallShip = new ShipCollision.Body(2, 20, -3, 0, 10);
        require(ShipCollision.bounceAtBounds(wallShip, 100, 100, 0), "screen edge must be a collision");
        require(wallShip.x == 10 && wallShip.vx == 3, "screen collision must reflect without damage state");
        System.out.println("Persistence and hit-zone smoke tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

package com.ships;

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
                presets.get(0).id(), 125, 240, 75, 100, 5, 15, 480, 120,
                List.of(new GameStore.EnemySave(presets.get(1).id(), 400, 350, 40, 80)));
        GameStore.SavedGame restoredSave = GameStore.SavedGame.fromJson(
                ShipPreset.object(Json.parse(Json.stringify(save.toJson()))));
        require(restoredSave.equals(save), "save JSON round-trip must preserve game state");
        require(presets.get(0).shapeAt(100, 100, 0, 1).contains(100, 100),
                "ship hit-zone shape must contain its center");
        System.out.println("Persistence and hit-zone smoke tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

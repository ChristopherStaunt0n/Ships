package com.ships;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class GameStore {
    private final Path root = Path.of("data");
    private final Path presetsFile = root.resolve("presets.json");
    private final Path savesDir = root.resolve("saves");

    GameStore() throws IOException {
        Files.createDirectories(savesDir);
        if (!Files.exists(presetsFile)) savePresets(ShipPreset.defaults());
    }

    List<ShipPreset> loadPresets() throws IOException {
        Object parsed = Json.parse(Files.readString(presetsFile, StandardCharsets.UTF_8));
        List<ShipPreset> result = new ArrayList<>();
        for (Object item : list(ShipPreset.object(parsed).get("presets"))) {
            result.add(ShipPreset.fromJson(ShipPreset.object(item)));
        }
        return result;
    }

    void savePresets(List<ShipPreset> presets) throws IOException {
        List<Object> values = presets.stream().map(ShipPreset::toJson).map(v -> (Object) v).toList();
        write(presetsFile, Json.stringify(Map.of("presets", values)));
    }

    List<SavedGame> loadGames() throws IOException {
        List<SavedGame> games = new ArrayList<>();
        try (var files = Files.list(savesDir)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                Object parsed = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
                games.add(SavedGame.fromJson(ShipPreset.object(parsed)));
            }
        }
        games.sort(Comparator.comparing(SavedGame::name));
        return games;
    }

    void saveGame(SavedGame game) throws IOException {
        write(savesDir.resolve(game.id() + ".json"), Json.stringify(game.toJson()));
    }

    private void write(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static List<?> list(Object value) {
        if (!(value instanceof List<?> result)) throw new IllegalArgumentException("Expected JSON array");
        return result;
    }

    record SavedGame(String id, String name, String playerPresetId, double playerX, double playerY,
                     double playerHealth, double playerMaxHealth, double speed, double turnSpeed,
                     double collisionDuration, double collisionReduction, double damage, double range,
                     boolean showFrameRate, int score, List<EnemySave> enemies) {
        SavedGame { enemies = List.copyOf(enemies); }
        Map<String, Object> toJson() {
            List<Object> enemyData = enemies.stream().map(e -> (Object) Map.of(
                    "presetId", e.presetId(), "x", e.x(), "y", e.y(),
                    "health", e.health(), "maxHealth", e.maxHealth())).toList();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("name", name);
            data.put("playerPresetId", playerPresetId);
            data.put("playerX", playerX);
            data.put("playerY", playerY);
            data.put("playerHealth", playerHealth);
            data.put("playerMaxHealth", playerMaxHealth);
            data.put("speed", speed);
            data.put("turnSpeed", turnSpeed);
            data.put("collisionDuration", collisionDuration);
            data.put("collisionReduction", collisionReduction);
            data.put("damage", damage);
            data.put("range", range);
            data.put("showFrameRate", showFrameRate);
            data.put("score", score);
            data.put("enemies", enemyData);
            return data;
        }
        static SavedGame fromJson(Map<String, Object> data) {
            List<EnemySave> enemies = new ArrayList<>();
            for (Object item : list(data.get("enemies"))) {
                Map<String, Object> e = ShipPreset.object(item);
                enemies.add(new EnemySave(ShipPreset.string(e, "presetId"),
                        ShipPreset.number(e, "x"), ShipPreset.number(e, "y"),
                        ShipPreset.number(e, "health"), ShipPreset.number(e, "maxHealth")));
            }
            return new SavedGame(ShipPreset.string(data, "id"), ShipPreset.string(data, "name"),
                    ShipPreset.string(data, "playerPresetId"), ShipPreset.number(data, "playerX"),
                    ShipPreset.number(data, "playerY"), ShipPreset.number(data, "playerHealth"),
                    ShipPreset.number(data, "playerMaxHealth"), ShipPreset.number(data, "speed"),
                    data.containsKey("turnSpeed") ? ShipPreset.number(data, "turnSpeed") : 6,
                    data.containsKey("collisionDuration") ? ShipPreset.number(data, "collisionDuration") : 3,
                    data.containsKey("collisionReduction") ? ShipPreset.number(data, "collisionReduction") : 50,
                    ShipPreset.number(data, "damage"), ShipPreset.number(data, "range"),
                    !data.containsKey("showFrameRate") || Boolean.TRUE.equals(data.get("showFrameRate")),
                    (int) ShipPreset.number(data, "score"), enemies);
        }
    }

    record EnemySave(String presetId, double x, double y, double health, double maxHealth) {}
}

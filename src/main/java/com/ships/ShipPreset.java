package com.ships;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

record ShipPreset(String id, String name, double size, Map<String, Visual> visuals) {
    enum ShapeKind { TRIANGLE, DIAMOND, HEXAGON }
    enum State { FULL, HALF, SUNKEN }
    record Visual(ShapeKind shape, int color) {}

    ShipPreset {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        size = Math.max(20, Math.min(90, size));
        visuals = Map.copyOf(visuals);
        for (State state : State.values()) {
            if (!visuals.containsKey(state.name())) throw new IllegalArgumentException("Missing " + state + " visual");
        }
    }

    Visual visualFor(double durabilityRatio) {
        return visuals.get(durabilityRatio <= 0 ? State.SUNKEN.name()
                : durabilityRatio <= 0.5 ? State.HALF.name() : State.FULL.name());
    }

    Shape shapeAt(double x, double y, double angle, double durabilityRatio) {
        Visual visual = visualFor(durabilityRatio);
        double radius = size / 2.0;
        Path2D path = new Path2D.Double();
        int points = switch (visual.shape()) {
            case TRIANGLE -> 3;
            case DIAMOND -> 4;
            case HEXAGON -> 6;
        };
        for (int i = 0; i < points; i++) {
            double a = angle + (Math.PI * 2 * i / points);
            double factor = visual.shape() == ShapeKind.DIAMOND && i % 2 == 1 ? 0.62 : 1.0;
            double px = x + Math.cos(a) * radius * factor;
            double py = y + Math.sin(a) * radius * factor;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.closePath();
        return path;
    }

    Map<String, Object> toJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("size", size);
        Map<String, Object> states = new LinkedHashMap<>();
        visuals.forEach((state, visual) -> states.put(state, Map.of(
                "shape", visual.shape().name(), "color", visual.color())));
        data.put("visuals", states);
        return data;
    }

    static ShipPreset fromJson(Map<String, Object> data) {
        Map<String, Object> stateData = object(data.get("visuals"));
        Map<String, Visual> visuals = new LinkedHashMap<>();
        for (State state : State.values()) {
            Map<String, Object> item = object(stateData.get(state.name()));
            visuals.put(state.name(), new Visual(ShapeKind.valueOf(string(item, "shape")),
                    (int) number(item, "color")));
        }
        return new ShipPreset(string(data, "id"), string(data, "name"), number(data, "size"), visuals);
    }

    static List<ShipPreset> defaults() {
        return List.of(
                preset("Harbor Kite", 48, ShapeKind.TRIANGLE, 0x35B8A6, 0xC18B42, 0x62666C),
                preset("Ironclad", 58, ShapeKind.HEXAGON, 0x4C86D9, 0xA96554, 0x555A66),
                preset("Corsair", 42, ShapeKind.DIAMOND, 0xD9684D, 0xB78346, 0x5F6266));
    }

    private static ShipPreset preset(String name, double size, ShapeKind base, int full, int half, int sunken) {
        return new ShipPreset(UUID.randomUUID().toString(), name, size, Map.of(
                State.FULL.name(), new Visual(base, full),
                State.HALF.name(), new Visual(base, half),
                State.SUNKEN.name(), new Visual(ShapeKind.DIAMOND, sunken)));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String, Object>) map;
    }
    static String string(Map<String, Object> data, String key) { return String.valueOf(data.get(key)); }
    static double number(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof Number n)) throw new IllegalArgumentException("Expected numeric " + key);
        return n.doubleValue();
    }
}

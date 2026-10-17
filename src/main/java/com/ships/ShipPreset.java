package com.ships;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

record ShipPreset(String id, String name, double width, double length, Map<String, Visual> visuals) {
    enum ShapeKind { OVAL, TRIANGLE, DIAMOND, HEXAGON }
    enum State { FULL, HALF, SUNKEN }
    record Visual(ShapeKind shape, int color) {}

    ShipPreset {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        width = Math.max(18, Math.min(28, width));
        length = Math.max(30, Math.min(48, length));
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
        Shape hull = new Ellipse2D.Double(x - length / 2, y - width / 2, length, width);
        return AffineTransform.getRotateInstance(angle, x, y).createTransformedShape(hull);
    }

    Map<String, Object> toJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("width", width);
        data.put("length", length);
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
            if (item.get("shape") != null) ShapeKind.valueOf(string(item, "shape"));
            visuals.put(state.name(), new Visual(ShapeKind.OVAL, (int) number(item, "color")));
        }
        double width = data.containsKey("width") ? number(data, "width") : Math.min(28, number(data, "size") * 0.5);
        double length = data.containsKey("length") ? number(data, "length") : Math.min(48, number(data, "size"));
        return new ShipPreset(string(data, "id"), string(data, "name"), width, length, visuals);
    }

    static List<ShipPreset> defaults() {
        return List.of(
                preset("Harbor Kite", 25, 46, 0x35B8A6, 0xC18B42, 0x62666C),
                preset("Ironclad", 28, 48, 0x4C86D9, 0xA96554, 0x555A66),
                preset("Corsair", 22, 40, 0xD9684D, 0xB78346, 0x5F6266));
    }

    private static ShipPreset preset(String name, double width, double length, int full, int half, int sunken) {
        return new ShipPreset(UUID.randomUUID().toString(), name, width, length, Map.of(
                State.FULL.name(), new Visual(ShapeKind.OVAL, full),
                State.HALF.name(), new Visual(ShapeKind.OVAL, half),
                State.SUNKEN.name(), new Visual(ShapeKind.OVAL, sunken)));
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

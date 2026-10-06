package com.ships;

import java.util.*;

final class Json {
    private Json() {}

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(",", "{", "}");
            map.forEach((key, item) -> joiner.add(quote(String.valueOf(key)) + ":" + stringify(item)));
            return joiner.toString();
        }
        if (value instanceof Iterable<?> items) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            items.forEach(item -> joiner.add(stringify(item)));
            return joiner.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    static Object parse(String input) {
        Parser parser = new Parser(input);
        Object result = parser.value();
        parser.space();
        if (!parser.end()) throw parser.error("Unexpected trailing input");
        return result;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) { this.text = text; }
        boolean end() { return index >= text.length(); }
        void space() { while (!end() && Character.isWhitespace(text.charAt(index))) index++; }
        IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at JSON offset " + index); }

        Object value() {
            space();
            if (end()) throw error("Expected a value");
            return switch (text.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            while (true) {
                space();
                if (end() || text.charAt(index) != '"') throw error("Expected an object key");
                String key = string();
                space();
                if (!take(':')) throw error("Expected ':'");
                result.put(key, value());
                space();
                if (take('}')) return result;
                if (!take(',')) throw error("Expected ',' or '}'");
            }
        }

        private List<Object> array() {
            index++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                space();
                if (take(']')) return result;
                if (!take(',')) throw error("Expected ',' or ']'");
            }
        }

        private String string() {
            index++;
            StringBuilder result = new StringBuilder();
            while (!end()) {
                char c = text.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') { result.append(c); continue; }
                if (end()) throw error("Incomplete escape");
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) throw error("Incomplete unicode escape");
                        try { result.append((char) Integer.parseInt(text.substring(index, index + 4), 16)); }
                        catch (NumberFormatException ex) { throw error("Invalid unicode escape"); }
                        index += 4;
                    }
                    default -> throw error("Invalid escape");
                }
            }
            throw error("Unterminated string");
        }

        private Object number() {
            int start = index;
            if (take('-')) { }
            while (!end() && Character.isDigit(text.charAt(index))) index++;
            if (!end() && text.charAt(index) == '.') {
                index++;
                while (!end() && Character.isDigit(text.charAt(index))) index++;
            }
            if (!end() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (!end() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++;
                while (!end() && Character.isDigit(text.charAt(index))) index++;
            }
            if (start == index) throw error("Expected a number");
            try { return Double.valueOf(text.substring(start, index)); }
            catch (NumberFormatException ex) { throw error("Invalid number"); }
        }

        private Object literal(String literal, Object value) {
            if (!text.startsWith(literal, index)) throw error("Invalid value");
            index += literal.length();
            return value;
        }

        private boolean take(char expected) {
            if (!end() && text.charAt(index) == expected) { index++; return true; }
            return false;
        }
    }
}

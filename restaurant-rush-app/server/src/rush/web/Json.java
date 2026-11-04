package rush.web;

import java.util.List;

/** Minimal JSON writer (the server has no external dependencies). */
final class Json {

    private Json() {
    }

    /** @return a quoted, escaped JSON string, or null */
    static String str(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** @return a JSON array of already-encoded values */
    static String arr(List<String> encoded) {
        return "[" + String.join(",", encoded) + "]";
    }

    /** Builds a JSON object key by key. */
    static final class Obj {
        private final StringBuilder out = new StringBuilder("{");

        private Obj key(String key) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append(str(key)).append(':');
            return this;
        }

        Obj put(String key, String value) {
            key(key).out.append(str(value));
            return this;
        }

        Obj put(String key, long value) {
            key(key).out.append(value);
            return this;
        }

        Obj put(String key, boolean value) {
            key(key).out.append(value);
            return this;
        }

        /** Puts an already-encoded JSON value (object, array, or "null"). */
        Obj raw(String key, String json) {
            key(key).out.append(json);
            return this;
        }

        @Override
        public String toString() {
            return out + "}";
        }
    }
}

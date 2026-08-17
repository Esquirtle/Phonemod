package esq.phonemod.security;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/** Converts an untrusted custom-UI BSON document into bounded-validator input. */
public final class UiEventDecoder {
    private UiEventDecoder() {}

    /**
     * Accepts the scalar types emitted by Hytale UI controls and converts them
     * to their canonical string representation. Compound values remain invalid;
     * field counts and lengths are enforced immediately afterwards by
     * {@link UiEventValidator}.
     */
    @Nonnull
    public static Map<String, String> decode(@Nonnull String rawData) {
        Map<String, String> result = new HashMap<>();
        BsonDocument document = BsonDocument.parse(rawData);
        for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
            BsonValue value = entry.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            result.put(normalizeKey(entry.getKey()), scalarString(value));
        }
        return result;
    }

    @Nonnull
    private static String scalarString(@Nonnull BsonValue value) {
        if (value.isString()) {
            return value.asString().getValue();
        }
        if (value.isBoolean()) {
            return Boolean.toString(value.asBoolean().getValue());
        }
        if (value.isInt32()) {
            return Integer.toString(value.asInt32().getValue());
        }
        if (value.isInt64()) {
            return Long.toString(value.asInt64().getValue());
        }
        if (value.isDouble()) {
            double number = value.asDouble().getValue();
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("UI event numbers must be finite");
            }
            return Double.toString(number);
        }
        throw new IllegalArgumentException("UI event values must be scalar primitives");
    }

    @Nonnull
    private static String normalizeKey(@Nonnull String key) {
        return key.startsWith("@") ? key.substring(1) : key;
    }
}

package esq.phonemod.device.routing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable bag of values produced by a route's resolvers and handed to its
 * handlers. Decouples handlers from service look-ups: a value is resolved once
 * and read by every handler in the chain.
 */
public final class RouteData {

    /** Shared empty instance for routes without resolvers. */
    public static final RouteData EMPTY = new RouteData(Collections.emptyMap());

    private final Map<String, Object> values;

    public RouteData(@Nonnull Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    @Nullable
    public Object get(@Nonnull String key) {
        return values.get(key);
    }

    @Nullable
    public String getString(@Nonnull String key) {
        Object value = values.get(key);
        return value instanceof String s ? s : (value != null ? value.toString() : null);
    }

    /** Typed read; returns {@code null} when absent or of a different type. */
    @Nullable
    public <T> T get(@Nonnull String key, @Nonnull Class<T> type) {
        Object value = values.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public boolean has(@Nonnull String key) {
        return values.containsKey(key);
    }

    @Nonnull
    public Map<String, Object> asMap() {
        return values;
    }
}

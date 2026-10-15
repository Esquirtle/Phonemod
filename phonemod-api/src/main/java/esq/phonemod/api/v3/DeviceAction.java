package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable command + primitive parameter map delivered to {@link DeviceApp#onAction}.
 * The app, not this type, validates command semantics (D-18).
 *
 * <p>Constructor validation (F4): {@code command} matches
 * {@code [a-z0-9_.:-]{1,64}} (v2 {@code DeviceAction.java:11}); at most 32 params;
 * non-blank keys up to 64 chars; String values up to 1024 chars; defensive copy.
 */
public record DeviceAction(@Nonnull String command, @Nonnull Map<String, Object> params) {

    /** Only these wrapper types may appear as parameter values. */
    public static final Set<Class<?>> ALLOWED_TYPES =
            Set.of(String.class, Integer.class, Long.class, Double.class, Boolean.class);

    public DeviceAction {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public static DeviceAction of(@Nonnull String command, @Nonnull Map<String, Object> params) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public Optional<String> string(@Nonnull String key) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public Optional<Integer> integer(@Nonnull String key) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public Optional<Long> longValue(@Nonnull String key) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public Optional<Double> decimal(@Nonnull String key) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public Optional<Boolean> bool(@Nonnull String key) {
        throw new UnsupportedOperationException("F4");
    }
}

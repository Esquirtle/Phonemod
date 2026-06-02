package esq.phonemod.device.routing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Descriptive, side-effect-free metadata attached to a {@link Route}: a name, an
 * optional human-readable description, and free-form attributes (for logging,
 * auditing, or third-party tooling). Never consulted by the routing algorithm.
 */
public final class RouteMetadata {

    public static final RouteMetadata EMPTY = new RouteMetadata("", "", Collections.emptyMap());

    private final String name;
    private final String description;
    private final Map<String, String> attributes;

    public RouteMetadata(@Nonnull String name,
            @Nonnull String description,
            @Nonnull Map<String, String> attributes) {
        this.name = name;
        this.description = description;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @Nonnull
    public static RouteMetadata of(@Nonnull String name, @Nonnull String description) {
        return new RouteMetadata(name, description, Collections.emptyMap());
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    @Nonnull
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Nullable
    public String getAttribute(@Nonnull String key) {
        return attributes.get(key);
    }
}

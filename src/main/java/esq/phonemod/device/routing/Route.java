package esq.phonemod.device.routing;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Declarative, immutable description of how one action is handled:
 * <em>guards &rarr; resolvers &rarr; handlers</em>, plus descriptive metadata.
 *
 * <p>Built via {@link #builder(String)} and frozen on {@link Builder#build()};
 * a built route shares no mutable state, so a single registry instance is safe
 * to use concurrently across all open phones.
 *
 * <p>Note: this type is intentionally non-generic. An earlier {@code Route<T>}
 * sketch carried no concrete payload type in this codebase; {@link RouteMetadata}
 * covers the extensibility that a type parameter would have.
 */
public final class Route {

    private final String action;
    private final List<RouteGuard> guards;
    private final List<RouteResolver> resolvers;
    private final List<RouteHandler> handlers;
    private final RouteMetadata metadata;

    private Route(@Nonnull Builder builder) {
        this.action = builder.action;
        this.guards = Collections.unmodifiableList(new ArrayList<>(builder.guards));
        this.resolvers = Collections.unmodifiableList(new ArrayList<>(builder.resolvers));
        this.handlers = Collections.unmodifiableList(new ArrayList<>(builder.handlers));
        this.metadata = builder.metadata;
    }

    @Nonnull
    public static Builder builder(@Nonnull String action) {
        return new Builder(action);
    }

    @Nonnull
    public String getAction() {
        return action;
    }

    @Nonnull
    public List<RouteGuard> getGuards() {
        return guards;
    }

    @Nonnull
    public List<RouteResolver> getResolvers() {
        return resolvers;
    }

    @Nonnull
    public List<RouteHandler> getHandlers() {
        return handlers;
    }

    @Nonnull
    public RouteMetadata getMetadata() {
        return metadata;
    }

    /** Fluent, single-use builder for a {@link Route}. */
    public static final class Builder {

        private final String action;
        private final List<RouteGuard> guards = new ArrayList<>();
        private final List<RouteResolver> resolvers = new ArrayList<>();
        private final List<RouteHandler> handlers = new ArrayList<>();
        private RouteMetadata metadata = RouteMetadata.EMPTY;

        private Builder(@Nonnull String action) {
            this.action = action;
        }

        @Nonnull
        public Builder guard(@Nonnull RouteGuard guard) {
            guards.add(guard);
            return this;
        }

        @Nonnull
        public Builder resolver(@Nonnull RouteResolver resolver) {
            resolvers.add(resolver);
            return this;
        }

        @Nonnull
        public Builder handler(@Nonnull RouteHandler handler) {
            handlers.add(handler);
            return this;
        }

        @Nonnull
        public Builder metadata(@Nonnull RouteMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        @Nonnull
        public Route build() {
            return new Route(this);
        }
    }
}

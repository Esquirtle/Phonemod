package esq.phonemod.device.routing;

import javax.annotation.Nonnull;

/**
 * Declarative router for phone UI interactions. Replaces the procedural
 * {@code if/else} chain in {@code DevicePage} with composable {@link Route}s.
 *
 * <p>For a given context the router: matches a route by action &rarr; runs its
 * guards &rarr; runs its resolvers &rarr; runs its handlers &rarr; applies the
 * resulting UI flush. A matched route whose guards fail, or an unmatched action,
 * is delegated to the fallback route.
 *
 * <p>Implementations register all routes up front and are then treated as
 * immutable, so a single instance is safe to share across all open devices.
 */
public interface PhoneRouter {

    /** Routes a single interaction and applies its UI result via the context's host. */
    void route(@Nonnull RouteContext context);

    /**
     * Registers (or replaces, by action) a route. Intended to be called during
     * setup, before the router serves traffic; not safe to call concurrently
     * with {@link #route}.
     */
    void registerRoute(@Nonnull Route route);
}

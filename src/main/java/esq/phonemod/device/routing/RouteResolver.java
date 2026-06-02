package esq.phonemod.device.routing;

import java.util.Map;

/**
 * Pre-loads data a route's handlers need, so look-ups are performed once and
 * shared instead of being duplicated across handlers. Resolvers must be pure
 * and read-only: they may query services but must not mutate session or UI
 * state. The returned maps are merged (in declaration order) into the
 * {@link RouteData} passed to handlers.
 */
@FunctionalInterface
public interface RouteResolver {

    Map<String, Object> resolve(RouteContext context);
}

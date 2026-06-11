package esq.phonemod.device.routing;

/**
 * Pre-activation predicate for a {@link Route}. Guards validate preconditions so
 * handlers stay free of defensive checks.
 *
 * <p><b>Failure semantics:</b> when any guard on a matched route returns
 * {@code false}, the router does <em>not</em> raise an error — it falls through
 * to the fallback route. This intentionally preserves the legacy {@code if/else}
 * behavior where, e.g., a call action on a device without the {@code calls}
 * capability simply flows on to the current app's event handler.
 */
@FunctionalInterface
public interface RouteGuard {

    boolean canActivate(RouteContext context);
}

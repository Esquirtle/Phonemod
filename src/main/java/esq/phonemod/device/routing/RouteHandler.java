package esq.phonemod.device.routing;

import javax.annotation.Nullable;

/**
 * Executes a route's business logic and returns the UI {@link RouteResult} for
 * the router to apply.
 *
 * <p>Handlers may perform the side effects inherent to the operation (opening an
 * app via {@link RouterHost}, call-registry mutations, etc.) but must not flush
 * UI themselves — they describe the flush by returning a {@link RouteResult}.
 * Returning {@code null} means "not handled"; the router then tries the next
 * handler in the chain, or finally falls back to an acknowledgement flush.
 */
@FunctionalInterface
public interface RouteHandler {

    @Nullable
    RouteResult handle(RouteContext context, RouteData resolvedData);
}

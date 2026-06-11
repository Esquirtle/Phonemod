package esq.phonemod.device.routing.guards;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteGuard;

/**
 * Ensures the action is present before a route activates.
 *
 * <p>Payload validation is intentionally left to handlers: the legacy flow
 * treated a missing payload (e.g. a blank {@code App} on {@code open_app}, or a
 * blank {@code Contact} on {@code open_chat}) as a no-op acknowledgement rather
 * than a fall-through, so rejecting it here would change behavior.
 */
public final class ValidationGuard implements RouteGuard {

    @Override
    public boolean canActivate(RouteContext context) {
        String action = context.getAction();
        return action != null && !action.isBlank();
    }
}

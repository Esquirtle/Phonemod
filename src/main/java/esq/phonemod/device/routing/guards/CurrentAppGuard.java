package esq.phonemod.device.routing.guards;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteGuard;

/**
 * Ensures a current app (and its id) exist on the session — i.e. an app is open
 * to receive the event. Used by routes that forward to the active app.
 */
public final class CurrentAppGuard implements RouteGuard {

    @Override
    public boolean canActivate(RouteContext context) {
        return context.getSession().getCurrentApp() != null
                && context.getSession().getCurrentAppId() != null;
    }
}

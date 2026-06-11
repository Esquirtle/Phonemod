package esq.phonemod.device.routing.guards;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteGuard;

import javax.annotation.Nonnull;

/**
 * Ensures the device shell declares a required capability (e.g. {@code "calls"}).
 * On failure the router falls through to the fallback route — matching the legacy
 * behavior where call actions on a non-call device flowed to the current app.
 */
public final class AppCapabilityGuard implements RouteGuard {

    private final String capability;

    public AppCapabilityGuard(@Nonnull String capability) {
        this.capability = capability;
    }

    @Override
    public boolean canActivate(RouteContext context) {
        return context.shell().hasCapability(capability);
    }
}

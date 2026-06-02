package esq.phonemod.device.routing.guards;

import esq.phonemod.device.core.DevicePageState;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteGuard;

import javax.annotation.Nonnull;

/**
 * Ensures the session is in a required call-related {@link DevicePageState}
 * (e.g. {@link DevicePageState#ACTIVE_CALL} or {@link DevicePageState#INCOMING_CALL}).
 *
 * <p>Reuses the existing {@code DevicePageState} enum rather than introducing a
 * parallel call-state type. Provided for new routes; it is intentionally
 * <em>not</em> attached to the built-in answer/decline/hang-up routes, because
 * the legacy code did not pre-check call state (it delegates the "no active
 * call" case to {@code CallRegistry}). Attaching it there would change behavior.
 */
public final class CallStateGuard implements RouteGuard {

    private final DevicePageState required;

    public CallStateGuard(@Nonnull DevicePageState required) {
        this.required = required;
    }

    @Override
    public boolean canActivate(RouteContext context) {
        return context.getSession().getCurrentState() == required;
    }
}

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
 * parallel UI state type. Server routes attach this guard to reject stale or
 * forged call controls before they reach the call state machine.
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

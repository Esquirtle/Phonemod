package esq.phonemod.device.routing.resolvers;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteResolver;
import esq.phonemod.phone.messaging.CallRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the current call situation for the session's device, keyed by phone
 * number. Pure read-only look-ups into {@link CallRegistry}.
 *
 * <p>Keys populated when present: {@code activePartner}, {@code pendingCallee},
 * {@code pendingCaller}; {@code callState} always reflects the session's page state.
 */
public final class CallStateResolver implements RouteResolver {

    public static final String ACTIVE_PARTNER = "activePartner";
    public static final String PENDING_CALLEE = "pendingCallee";
    public static final String PENDING_CALLER = "pendingCaller";
    public static final String CALL_STATE = "callState";

    @Override
    public Map<String, Object> resolve(RouteContext context) {
        String deviceId = context.getSession().getDeviceId();
        Map<String, Object> data = new HashMap<>();

        String activePartner = CallRegistry.getActivePartner(deviceId);
        if (activePartner != null) {
            data.put(ACTIVE_PARTNER, activePartner);
        }
        String pendingCallee = CallRegistry.getPendingCallee(deviceId);
        if (pendingCallee != null) {
            data.put(PENDING_CALLEE, pendingCallee);
        }
        String pendingCaller = CallRegistry.getPendingCaller(deviceId);
        if (pendingCaller != null) {
            data.put(PENDING_CALLER, pendingCaller);
        }
        data.put(CALL_STATE, context.getSession().getCurrentState());
        return data;
    }
}

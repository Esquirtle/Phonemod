package esq.phonemod.device.routing.resolvers;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves shell/navigation state for the session: the current page state, the
 * current app id (when an app is open), and the device id. Pure read-only.
 *
 * <p>Keys populated: {@code pageState}, {@code deviceId}, and {@code currentAppId}
 * (only when an app is open).
 */
public final class ShellStateResolver implements RouteResolver {

    public static final String PAGE_STATE = "pageState";
    public static final String CURRENT_APP_ID = "currentAppId";
    public static final String DEVICE_ID = "deviceId";

    @Override
    public Map<String, Object> resolve(RouteContext context) {
        Map<String, Object> data = new HashMap<>();
        data.put(PAGE_STATE, context.getSession().getCurrentState());
        data.put(DEVICE_ID, context.getSession().getDeviceId());
        String currentAppId = context.getSession().getCurrentAppId();
        if (currentAppId != null) {
            data.put(CURRENT_APP_ID, currentAppId);
        }
        return data;
    }
}

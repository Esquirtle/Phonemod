package esq.phonemod.device.routing.resolvers;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteResolver;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.core.PhoneService;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the target {@link PhoneApp} for the event — the {@code App} payload
 * value if given, otherwise the current app — along with its display name. Pure
 * read-only look-up into {@link PhoneService}.
 *
 * <p>Keys populated when the app resolves: {@code app}, {@code appId},
 * {@code appDisplayName}.
 */
public final class AppResolver implements RouteResolver {

    public static final String APP = "app";
    public static final String APP_ID = "appId";
    public static final String APP_DISPLAY_NAME = "appDisplayName";

    @Override
    public Map<String, Object> resolve(RouteContext context) {
        Map<String, Object> data = new HashMap<>();

        String appId = context.getEventData().get("App");
        if (appId == null || appId.isBlank()) {
            appId = context.getSession().getCurrentAppId();
        }
        if (appId == null || appId.isBlank()) {
            return data;
        }

        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app != null) {
            data.put(APP, app);
            data.put(APP_ID, appId);
            data.put(APP_DISPLAY_NAME, app.getDisplayName());
        }
        return data;
    }
}

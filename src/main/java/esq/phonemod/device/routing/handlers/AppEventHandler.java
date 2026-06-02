package esq.phonemod.device.routing.handlers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;

/**
 * Fallback handler: forwards the event to the current app's
 * {@link PhoneApp#handleEvent}. Mirrors the legacy tail of {@code handleEventMap}
 * — when the current app handles the event its render is flushed, otherwise an
 * acknowledgement is sent. This is also where guard-rejected and unknown actions
 * land, reproducing the original {@code if/else} fall-through.
 */
public final class AppEventHandler implements RouteHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public RouteResult handle(RouteContext context, RouteData resolvedData) {
        PhoneApp<?> app = context.getSession().getCurrentApp();
        String appId = context.getSession().getCurrentAppId();
        if (app != null && appId != null) {
            PhoneAppContext ctx = context.getHost().createContext(
                    context.getRef(), context.getStore(), appId);
            if (ctx != null) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder evb = new UIEventBuilder();
                if (app.handleEvent(ctx, context.getHost().toPhoneEvent(context.getEventData()), cmd, evb)) {
                    return RouteResult.update(cmd, evb);
                }
            }
        }
        LOGGER.atFine().log("[PhoneRouter] Unhandled action: %s", context.getAction());
        return RouteResult.empty();
    }
}

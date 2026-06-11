package esq.phonemod.device.routing.handlers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;
import esq.phonemod.phone.api.PhoneApp;

/**
 * Opens the app named by the {@code App} payload. Mirrors the legacy
 * {@code open_app} branch: an unknown/blank app yields an acknowledgement flush.
 */
public final class OpenAppHandler implements RouteHandler {

    @Override
    public RouteResult handle(RouteContext context, RouteData resolvedData) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        PhoneApp<?> app = context.getHost().openApp(
                context.getRef(), context.getStore(),
                context.getEventData().get("App"), cmd, evb);
        return app == null ? RouteResult.empty() : RouteResult.update(cmd, evb);
    }
}

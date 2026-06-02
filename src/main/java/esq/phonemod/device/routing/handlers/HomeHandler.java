package esq.phonemod.device.routing.handlers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;

/**
 * Returns to the home screen: resets the session and rebuilds the app grid.
 * Mirrors the legacy {@code home} branch.
 */
public final class HomeHandler implements RouteHandler {

    @Override
    public RouteResult handle(RouteContext context, RouteData resolvedData) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evb = new UIEventBuilder();
        context.getHost().loadHomeState(cmd, evb);
        return RouteResult.update(cmd, evb);
    }
}

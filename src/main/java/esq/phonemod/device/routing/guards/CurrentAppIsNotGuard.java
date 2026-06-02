package esq.phonemod.device.routing.guards;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteGuard;

import javax.annotation.Nonnull;

/**
 * Activates only when the current app is <em>not</em> the given id. Encodes the
 * legacy {@code open_chat} condition ({@code currentAppId != "whatgram"}): when
 * Whatgram is already open the guard fails and the event falls through to the
 * fallback route, letting Whatgram's own handler process {@code open_chat}.
 *
 * <p>A {@code null} current app id counts as "not the given id" (guard passes),
 * mirroring {@code !appId.equals(session.getCurrentAppId())}.
 */
public final class CurrentAppIsNotGuard implements RouteGuard {

    private final String appId;

    public CurrentAppIsNotGuard(@Nonnull String appId) {
        this.appId = appId;
    }

    @Override
    public boolean canActivate(RouteContext context) {
        return !appId.equals(context.getSession().getCurrentAppId());
    }
}

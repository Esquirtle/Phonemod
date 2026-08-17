package esq.phonemod.device.routing;

import esq.phonemod.device.routing.handlers.AppEventHandler;
import esq.phonemod.device.routing.handlers.CallActionsHandler;
import esq.phonemod.device.routing.handlers.HomeHandler;
import esq.phonemod.device.routing.handlers.OpenAppHandler;
import esq.phonemod.device.routing.handlers.OpenChatHandler;
import esq.phonemod.device.routing.guards.AppCapabilityGuard;
import esq.phonemod.device.routing.guards.CurrentAppIsNotGuard;
import esq.phonemod.device.routing.guards.CallStateGuard;
import esq.phonemod.device.routing.guards.ValidationGuard;
import esq.phonemod.device.routing.resolvers.AppResolver;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.device.core.DevicePageState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link PhoneRouter}: O(1) action lookup over a {@link HashMap}, with a
 * dedicated fallback route for guard-rejected and unmatched actions.
 *
 * <p>Routes are immutable once built, and registration happens entirely inside
 * {@link #createDefault()} before the instance is shared — so the only mutable
 * state at runtime is the per-call {@link RouteContext}, keeping the router safe
 * across concurrently open phones.
 */
public final class DefaultPhoneRouter implements PhoneRouter {

    private final Map<String, Route> routes = new HashMap<>();
    private volatile boolean frozen;
    private final Route fallbackRoute;

    public DefaultPhoneRouter(@Nonnull Route fallbackRoute) {
        this.fallbackRoute = fallbackRoute;
    }

    /**
     * Builds the router with all built-in phone routes registered. The route
     * table reproduces the legacy {@code DevicePage.handleEventMap} chain.
     */
    @Nonnull
    public static PhoneRouter createDefault() {
        Route fallback = Route.builder("*")
                .handler(new AppEventHandler())
                .metadata(RouteMetadata.of("app-fallback",
                        "Forwards unmatched/guard-rejected actions to the current app"))
                .build();

        DefaultPhoneRouter router = new DefaultPhoneRouter(fallback);

        router.registerRoute(Route.builder(PhoneEventActions.HOME)
                .guard(new ValidationGuard())
                .handler(new HomeHandler())
                .metadata(RouteMetadata.of("home", "Return to the app grid"))
                .build());

        router.registerRoute(Route.builder(PhoneEventActions.OPEN_APP)
                .guard(new ValidationGuard())
                .resolver(new AppResolver())
                .handler(new OpenAppHandler())
                .metadata(RouteMetadata.of("open-app", "Open a registered app by id"))
                .build());

        router.registerRoute(Route.builder(PhoneEventActions.OPEN_CHAT)
                .guard(new ValidationGuard())
                .guard(new CurrentAppIsNotGuard("whatgram"))
                .handler(new OpenChatHandler())
                .metadata(RouteMetadata.of("open-chat", "Jump directly into a Whatgram conversation"))
                .build());

        // answer / decline / hang-up share one handler; all gated on the calls capability.
        CallActionsHandler callActions = new CallActionsHandler();
        router.registerRoute(Route.builder(PhoneEventActions.ANSWER_CALL)
                .guard(new AppCapabilityGuard("calls"))
                .guard(new CallStateGuard(DevicePageState.INCOMING_CALL))
                .handler(callActions)
                .metadata(RouteMetadata.of("answer-call", "Answer an incoming call"))
                .build());
        router.registerRoute(Route.builder(PhoneEventActions.DECLINE_CALL)
                .guard(new AppCapabilityGuard("calls"))
                .guard(new CallStateGuard(DevicePageState.INCOMING_CALL))
                .handler(callActions)
                .metadata(RouteMetadata.of("decline-call", "Decline a ringing call"))
                .build());
        router.registerRoute(Route.builder(PhoneEventActions.HANG_UP)
                .guard(new AppCapabilityGuard("calls"))
                .guard(new CallStateGuard(DevicePageState.ACTIVE_CALL))
                .handler(callActions)
                .metadata(RouteMetadata.of("hang-up", "End an active call"))
                .build());
        router.registerRoute(Route.builder(PhoneEventActions.SPEAKER_ON)
                .guard(new AppCapabilityGuard("calls"))
                .guard(new CallStateGuard(DevicePageState.ACTIVE_CALL))
                .handler(callActions)
                .metadata(RouteMetadata.of("speaker-on", "Enable speaker audio for a toggleable call"))
                .build());
        router.registerRoute(Route.builder(PhoneEventActions.SPEAKER_OFF)
                .guard(new AppCapabilityGuard("calls"))
                .guard(new CallStateGuard(DevicePageState.ACTIVE_CALL))
                .handler(callActions)
                .metadata(RouteMetadata.of("speaker-off", "Return a toggleable call to private audio"))
                .build());

        router.frozen = true;
        return router;
    }

    @Override
    public void registerRoute(@Nonnull Route route) {
        if (frozen) {
            throw new IllegalStateException("Router is frozen");
        }
        routes.put(route.getAction(), route);
    }

    @Override
    public void route(@Nonnull RouteContext context) {
        Route route = routes.get(context.getAction());
        // A matched route whose guards all pass handles the action directly.
        // If no route matches, OR the matched route's guards reject the action,
        // fall through to the app fallback so the current app can handle it. This
        // is the documented RouteGuard failure contract ("falls through to the
        // fallback route") and what the app-fallback route metadata advertises
        // ("guard-rejected actions"); e.g. open_chat fired from inside Whatgram is
        // rejected by CurrentAppIsNotGuard so WhatgramApp.handleEvent opens the chat.
        if (route != null && guardsPass(route, context)) {
            apply(context, execute(route, context));
            return;
        }
        if (context.getSession().getCurrentState() == DevicePageState.APP
                && context.getSession().getCurrentApp() != null) {
            apply(context, execute(fallbackRoute, context));
        } else {
            apply(context, RouteResult.empty());
        }
    }

    private static boolean guardsPass(@Nonnull Route route, @Nonnull RouteContext context) {
        for (RouteGuard guard : route.getGuards()) {
            if (!guard.canActivate(context)) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private static RouteResult execute(@Nonnull Route route, @Nonnull RouteContext context) {
        RouteData data = resolve(route, context);
        for (RouteHandler handler : route.getHandlers()) {
            RouteResult result = handler.handle(context, data);
            if (result != null) {
                return result;
            }
        }
        return RouteResult.empty();
    }

    @Nonnull
    private static RouteData resolve(@Nonnull Route route, @Nonnull RouteContext context) {
        if (route.getResolvers().isEmpty()) {
            return RouteData.EMPTY;
        }
        Map<String, Object> merged = new HashMap<>();
        for (RouteResolver resolver : route.getResolvers()) {
            Map<String, Object> resolved = resolver.resolve(context);
            if (resolved != null) {
                merged.putAll(resolved);
            }
        }
        return new RouteData(merged);
    }

    private static void apply(@Nonnull RouteContext context, @Nonnull RouteResult result) {
        if (result.isUpdate()) {
            context.getHost().sendUpdate(result.cmd(), result.evb());
        } else {
            context.getHost().sendUpdate(null, null);
        }
    }
}

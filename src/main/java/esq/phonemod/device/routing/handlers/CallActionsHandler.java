package esq.phonemod.device.routing.handlers;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.messaging.CallRegistry;

/**
 * Handles the call control actions {@code answer_call}, {@code decline_call} and
 * {@code hang_up}. Mirrors the legacy branches: answering promotes the pending
 * call; declining and hanging up both end the call. All flush an acknowledgement
 * only — the resulting UI is driven by {@code CallRegistry}'s page callbacks.
 *
 * <p>The {@code calls} capability is enforced upstream by
 * {@code AppCapabilityGuard("calls")}; without it these actions fall through to
 * the fallback route.
 */
public final class CallActionsHandler implements RouteHandler {

    @Override
    public RouteResult handle(RouteContext context, RouteData resolvedData) {
        String deviceId = context.getSession().getDeviceId();
        if (PhoneEventActions.ANSWER_CALL.equals(context.getAction())) {
            CallRegistry.answerCall(deviceId);
        } else {
            // decline_call and hang_up share the same end-call path.
            CallRegistry.hangUp(deviceId);
        }
        return RouteResult.empty();
    }
}

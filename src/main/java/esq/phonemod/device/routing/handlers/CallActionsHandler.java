package esq.phonemod.device.routing.handlers;

import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.messaging.CallRegistry;

/**
 * Handles answer, decline, hang-up, and toggleable speaker actions. All flush an
 * acknowledgement only; resulting UI changes are driven by {@code CallRegistry}'s
 * page callbacks.
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
        } else if (PhoneEventActions.SPEAKER_ON.equals(context.getAction())) {
            CallRegistry.setSpeaker(deviceId, true);
        } else if (PhoneEventActions.SPEAKER_OFF.equals(context.getAction())) {
            CallRegistry.setSpeaker(deviceId, false);
        } else {
            // decline_call and hang_up share the same end-call path.
            CallRegistry.hangUp(deviceId);
        }
        return RouteResult.empty();
    }
}

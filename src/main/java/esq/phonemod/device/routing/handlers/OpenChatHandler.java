package esq.phonemod.device.routing.handlers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.routing.RouteContext;
import esq.phonemod.device.routing.RouteData;
import esq.phonemod.device.routing.RouteHandler;
import esq.phonemod.device.routing.RouteResult;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;

/**
 * Opens a Whatgram conversation directly: opens Whatgram (if not already active)
 * and dispatches the {@code open_chat} event into it so the target conversation
 * loads. Mirrors the legacy {@code open_chat} branch exactly, including:
 * <ul>
 *   <li>blank {@code Contact} &rarr; acknowledgement flush;</li>
 *   <li>Whatgram's inner {@code handleEvent} result preferred when it handles
 *       the event, otherwise the outer open render is flushed.</li>
 * </ul>
 *
 * <p>The "Whatgram is already open" case is excluded upstream by
 * {@code CurrentAppIsNotGuard("whatgram")}, which routes such events to the
 * fallback (so Whatgram handles its own {@code open_chat}).
 */
public final class OpenChatHandler implements RouteHandler {

    private static final String WHATGRAM = "whatgram";

    @Override
    public RouteResult handle(RouteContext context, RouteData resolvedData) {
        String contact = context.param("Contact");
        if (contact != null && !contact.isBlank()) {
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evb = new UIEventBuilder();
            PhoneApp<?> app = context.getHost().openApp(
                    context.getRef(), context.getStore(), WHATGRAM, cmd, evb);
            if (app != null) {
                PhoneAppContext ctx = context.getHost().createContext(
                        context.getRef(), context.getStore(), WHATGRAM);
                if (ctx != null) {
                    UICommandBuilder innerCmd = new UICommandBuilder();
                    UIEventBuilder innerEvb = new UIEventBuilder();
                    if (app.handleEvent(ctx, context.getHost().toPhoneEvent(context.getEventData()),
                            innerCmd, innerEvb)) {
                        return RouteResult.update(innerCmd, innerEvb);
                    }
                }
                return RouteResult.update(cmd, evb);
            }
        }
        return RouteResult.empty();
    }
}

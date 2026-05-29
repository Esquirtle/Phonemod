package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.StatefulPhoneApp;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import esq.phonemod.phone.messaging.CallRegistry;

public final class CallsApp extends StatefulPhoneApp<CallsApp.State> {

    public enum State {
        DIALER
    }

    public CallsApp() {
        super(State.DIALER);
    }

    @Override
    public String getId() {
        return "calls";
    }

    @Override
    public String getDisplayName() {
        return "Calls";
    }

    @Override
    public String getAppButtonUI() {
        return "Pages/Phone/Components/CallsButton.ui";
    }

    @Override
    public String getUIPath() {
        return "Pages/Phone/Calls.ui";
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);
        cmd.clear("#CallHistoryList");
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#DialButton",
                EventData.of("Action", "start_call").append("@DialNumber", "#DialInput.Value"),
                false);

        var history = ctx.getStore().ensureAndGetComponent(ctx.getRef(), esq.phonemod.phone.components.CallHistoryComponent.getComponentType());
        var records = history.getHistory(ctx.getPhoneNumber());
        int i = 0;
        for (int r = records.size() - 1; r >= 0; r--) {
            var rec = records.get(r);
            cmd.append("#CallHistoryList", "Pages/Phone/Components/CallHistoryEntry.ui");
            cmd.set("#CallHistoryList[" + i + "] #ContactName.Text",
                    rec.displayName + " (" + rec.contactNumber + ")");
            String status = rec.missed ? "Missed"
                    : (rec.outgoing ? "Outgoing" : "Incoming");
            cmd.set("#CallHistoryList[" + i + "] #CallStatus.Text",
                    status + "  " + java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(java.time.Instant.ofEpochMilli(rec.timestamp)));
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    "#CallHistoryList[" + i + "] #CallBackButton",
                    EventData.of("Action", "start_call").append("Contact", rec.contactNumber),
                    false);
            i++;
        }
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();
        if ("start_call".equals(action)) {
            String target = event.getParams().get("Contact");
            if (target == null || target.isBlank()) {
                target = event.getParams().get("DialNumber");
            }
            if (target != null && !target.isBlank()) {
                CallRegistry.initiateCall(ctx.getPhoneNumber(), target.trim(), target.trim());
            }
            build(ctx, cmd, evb);
            return true;
        }

        if ("answer_call".equals(action)) {
            CallRegistry.answerCall(ctx.getPhoneNumber());
            return true;
        }

        if ("decline_call".equals(action) || "hang_up".equals(action)) {
            CallRegistry.hangUp(ctx.getPhoneNumber());
            return true;
        }

        return false;
    }

    @Override
    public void onIncomingCall(PhoneAppContext ctx, String callerNumber, String callerName) {
        // no-op; PhonePage will rebuild the active app if needed.
    }
}


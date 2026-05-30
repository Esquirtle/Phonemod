package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.services.DeviceCallService;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.apps.helpers.Calls;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Calls — dialer and call-history app.
 *
 * <p>Demonstrates the preferred A4 pattern:
 * <ul>
 *   <li>All call operations go through {@link DeviceCallService} via
 *       {@link DeviceService#get()} — no direct {@code CallRegistry} calls.</li>
 *   <li>All UI bindings use {@link PhoneUi} helpers with
 *       {@link PhoneEventActions} constants.</li>
 * </ul>
 */
public final class CallsApp extends StatefulPhoneApp<CallsApp.State> {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

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
        return PhoneAssetPaths.CALLS_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_CALLS_UI;
    }

    @Override
    public String getIconPath() {
        return "Pages/Phone/Calls.png";
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        return Map.of(
                "appHeader", Calls.SEL_HEADER,
                "appPanel", Calls.SEL_PANEL,
                "appContent", Calls.SEL_HISTORY_LIST);
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);

        // Bind the dial pad.
        PhoneUi.bindCapturedNumber(evb, Calls.SEL_DIAL_BUTTON,
                PhoneEventActions.START_CALL, "DialNumber", Calls.SEL_DIAL_INPUT_VALUE);

        // Populate call history (most recent first).
        PhoneUi.safeClear(cmd, Calls.SEL_HISTORY_LIST);
        CallHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), CallHistoryComponent.getComponentType());
        List<CallRecord> records = history.getHistory(ctx.getPhoneNumber());

        int i = 0;
        for (int r = records.size() - 1; r >= 0; r--) {
            CallRecord rec = records.get(r);
            String row = PhoneUi.appendListItem(cmd, Calls.SEL_HISTORY_LIST, Calls.HISTORY_ENTRY_UI, i);
            PhoneUi.setText(cmd, PhoneUi.child(row, Calls.SEL_ENTRY_CONTACT_NAME_TEXT),
                    rec.displayName + " (" + rec.contactNumber + ")");
            String status = rec.missed ? "Missed" : (rec.outgoing ? "Outgoing" : "Incoming");
            PhoneUi.setText(cmd, PhoneUi.child(row, Calls.SEL_ENTRY_CALL_STATUS_TEXT),
                    status + "  " + TIME_FMT.format(Instant.ofEpochMilli(rec.timestamp)));
            PhoneUi.bindAction(evb, PhoneUi.child(row, Calls.SEL_ENTRY_CALLBACK_BUTTON),
                    PhoneEventActions.START_CALL,
                    PhoneUi.params("Contact", rec.contactNumber));
            i++;
        }
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();
        DeviceCallService callService = DeviceService.get().getCallService();

        if (PhoneEventActions.START_CALL.equals(action)) {
            String target = event.getParam("Contact");
            if (target == null || target.isBlank()) {
                target = event.getParam("DialNumber");
            }
            if (target != null && !target.isBlank()) {
                callService.initiateCall(ctx.getPhoneNumber(), target.trim(), target.trim());
            }
            build(ctx, cmd, evb);
            return true;
        }

        // answer_call / decline_call / hang_up are shell-level actions intercepted
        // by DevicePage (for any device with the `calls` capability) before they
        // reach an app, so this app only owns start_call.
        return false;
    }

    @Override
    public void onIncomingCall(PhoneAppContext ctx, String callerNumber, String callerName) {
        // No-op — DevicePage handles the incoming-call overlay directly.
    }
}

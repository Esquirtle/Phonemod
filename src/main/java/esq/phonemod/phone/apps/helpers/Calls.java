package esq.phonemod.phone.apps.helpers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Selector constants and static helpers for the Calls (dialer) UI.
 *
 * <p>Selector constants are {@code public static final} so that {@code CallsApp}
 * and {@code DevicePage} can reference them without duplicating strings.
 */
public final class Calls {

    // ── UI file paths ─────────────────────────────────────────────────────────

    public static final String CALLS_UI          = "Pages/Phone/DustCalls.ui";
    public static final String HISTORY_ENTRY_UI  = "Pages/Phone/Components/DustCallHistoryEntry.ui";
    public static final String INCOMING_UI       = "Pages/Phone/Components/DustIncomingCall.ui";
    public static final String ACTIVE_UI         = "Pages/Phone/Components/DustActiveCall.ui";

    // ── Selectors ─────────────────────────────────────────────────────────────

    /** Themeable header surface (role: appHeader). */
    public static final String SEL_HEADER                   = "#CallsHeader";
    /** Themeable panel surface (role: appPanel). */
    public static final String SEL_PANEL                    = "#CallsPanel";

    /** The scrollable call history list. Themeable (role: appContent). */
    public static final String SEL_HISTORY_LIST             = "#CallHistoryList";

    /** Dial pad input field — used as capture source for the dial button. */
    public static final String SEL_DIAL_INPUT_VALUE         = "#DialInput.Value";

    /** Dial button that fires {@code start_call} with the input value. */
    public static final String SEL_DIAL_BUTTON              = "#DialButton";

    /** Relative: contact name label inside a call-history entry row. */
    public static final String SEL_ENTRY_CONTACT_NAME_TEXT  = "#ContactName.Text";

    /** Relative: call-status label inside a call-history entry row. */
    public static final String SEL_ENTRY_CALL_STATUS_TEXT   = "#CallStatus.Text";

    /** Relative: callback button inside a call-history entry row. */
    public static final String SEL_ENTRY_CALLBACK_BUTTON    = "#CallBackButton";

    /** Caller name label on the incoming-call overlay. */
    public static final String SEL_INCOMING_CALLER_NAME_TEXT = "#CallerName.Text";

    /** Answer button on the incoming-call overlay. */
    public static final String SEL_INCOMING_ANSWER_BUTTON   = "#AnswerButton";

    /** Decline button on the incoming-call overlay. */
    public static final String SEL_INCOMING_DECLINE_BUTTON  = "#DeclineButton";

    /** Partner label on the active-call overlay. */
    public static final String SEL_ACTIVE_PARTNER_TEXT      = "#PartnerLabel.Text";

    /** Hang-up button on the active-call overlay. */
    public static final String SEL_ACTIVE_HANGUP_BUTTON     = "#HangUpButton";

    // ─────────────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

    private Calls() {}

    /**
     * Clears {@code #AppContent} and renders the calls screen: a dial pad and call history.
     * Uses {@link DeviceService} for call operations and {@link PhoneUi} helpers for bindings.
     */
    public static void loadCallsState(@Nonnull String phoneNumber,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String contentSelector,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        PhoneUi.safeClear(cmd, contentSelector);
        cmd.append(contentSelector, CALLS_UI);

        PhoneUi.bindCapturedNumber(evb, SEL_DIAL_BUTTON,
                PhoneEventActions.START_CALL, "DialNumber", SEL_DIAL_INPUT_VALUE);

        CallHistoryComponent history =
                store.ensureAndGetComponent(ref, CallHistoryComponent.getComponentType());
        List<CallRecord> records = history.getHistory(phoneNumber);

        int i = 0;
        for (int r = records.size() - 1; r >= 0; r--) {
            CallRecord rec = records.get(r);
            String row = PhoneUi.appendListItem(cmd, SEL_HISTORY_LIST, HISTORY_ENTRY_UI, i);
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ENTRY_CONTACT_NAME_TEXT),
                    rec.displayName + " (" + rec.contactNumber + ")");
            String status = rec.missed ? "Missed" : (rec.outgoing ? "Outgoing" : "Incoming");
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ENTRY_CALL_STATUS_TEXT),
                    status + "  " + TIME_FMT.format(Instant.ofEpochMilli(rec.timestamp)));
            PhoneUi.bindAction(evb, PhoneUi.child(row, SEL_ENTRY_CALLBACK_BUTTON),
                    PhoneEventActions.START_CALL,
                    PhoneUi.params("Contact", rec.contactNumber));
            i++;
        }
    }

    /**
     * Clears {@code #AppContent} and renders the incoming-call overlay.
     * {@code #AnswerButton} fires {@code answer_call}; {@code #DeclineButton} fires
     * {@code decline_call}.
     */
    public static void loadIncomingCallState(@Nonnull String callerNumber,
            @Nonnull String callerName,
            @Nonnull String contentSelector,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        PhoneUi.safeClear(cmd, contentSelector);
        cmd.append(contentSelector, INCOMING_UI);
        PhoneUi.setText(cmd, SEL_INCOMING_CALLER_NAME_TEXT, callerName + " (" + callerNumber + ")");

        PhoneUi.bindAction(evb, SEL_INCOMING_ANSWER_BUTTON, PhoneEventActions.ANSWER_CALL);
        PhoneUi.bindAction(evb, SEL_INCOMING_DECLINE_BUTTON, PhoneEventActions.DECLINE_CALL);
    }

    /**
     * Clears {@code #AppContent} and renders the active-call overlay.
     * {@code #HangUpButton} fires {@code hang_up}.
     */
    public static void loadActiveCallState(@Nonnull String partnerNumber,
            @Nonnull String partnerName,
            @Nonnull String contentSelector,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        PhoneUi.safeClear(cmd, contentSelector);
        cmd.append(contentSelector, ACTIVE_UI);
        PhoneUi.setText(cmd, SEL_ACTIVE_PARTNER_TEXT, partnerName + " (" + partnerNumber + ")");

        PhoneUi.bindAction(evb, SEL_ACTIVE_HANGUP_BUTTON, PhoneEventActions.HANG_UP);
    }
}

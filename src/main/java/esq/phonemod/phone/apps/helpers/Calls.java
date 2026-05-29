package esq.phonemod.phone.apps.helpers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Static helpers that build the Calls (phone dialer) app UI state. */
public final class Calls {

    static final String CALLS_UI         = "Pages/Phone/Calls.ui";
    static final String HISTORY_ENTRY_UI = "Pages/Phone/Components/CallHistoryEntry.ui";
    static final String INCOMING_UI      = "Pages/Phone/Components/IncomingCall.ui";
    static final String ACTIVE_UI        = "Pages/Phone/Components/ActiveCall.ui";

    private static final String CONTENT = "#AppContent";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

    private Calls() {}

    /**
     * Clears {@code #AppContent} and renders the calls screen: a dial pad and call history list.
     * {@code #DialButton} fires {@code start_call} with {@code @DialNumber → #DialInput.Value}.
     * Each history entry's callback button fires {@code start_call} with {@code Contact=number}.
     */
    public static void loadCallsState(@Nonnull String phoneNumber,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Ref<EntityStore> ref,
                                       @Nonnull UICommandBuilder cmd,
                                       @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, CALLS_UI);

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DialButton",
                EventData.of("Action", "start_call").append("@DialNumber", "#DialInput.Value"),
                false);

        CallHistoryComponent history =
                store.ensureAndGetComponent(ref, CallHistoryComponent.getComponentType());
        List<CallRecord> records = history.getHistory(phoneNumber);

        // Display most-recent first
        int i = 0;
        for (int r = records.size() - 1; r >= 0; r--) {
            CallRecord rec = records.get(r);
            cmd.append("#CallHistoryList", HISTORY_ENTRY_UI);
            cmd.set("#CallHistoryList[" + i + "] #ContactName.Text",
                    rec.displayName + " (" + rec.contactNumber + ")");
            String status = rec.missed ? "Missed"
                    : (rec.outgoing ? "Outgoing" : "Incoming");
            cmd.set("#CallHistoryList[" + i + "] #CallStatus.Text",
                    status + "  " + TIME_FMT.format(Instant.ofEpochMilli(rec.timestamp)));
            evb.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#CallHistoryList[" + i + "] #CallBackButton",
                    EventData.of("Action", "start_call").append("Contact", rec.contactNumber),
                    false);
            i++;
        }
    }

    /**
     * Clears {@code #AppContent} and renders the incoming-call screen.
     * {@code #AnswerButton} fires {@code answer_call}; {@code #DeclineButton} fires {@code decline_call}.
     */
    public static void loadIncomingCallState(@Nonnull String callerNumber,
                                              @Nonnull String callerName,
                                              @Nonnull UICommandBuilder cmd,
                                              @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, INCOMING_UI);
        cmd.set("#CallerName.Text", callerName + " (" + callerNumber + ")");

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AnswerButton",
                EventData.of("Action", "answer_call"),
                false);
        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DeclineButton",
                EventData.of("Action", "decline_call"),
                false);
    }

    /**
     * Clears {@code #AppContent} and renders the active-call screen.
     * {@code #HangUpButton} fires {@code hang_up}.
     */
    public static void loadActiveCallState(@Nonnull String partnerNumber,
                                            @Nonnull String partnerName,
                                            @Nonnull UICommandBuilder cmd,
                                            @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, ACTIVE_UI);
        cmd.set("#PartnerLabel.Text", partnerName + " (" + partnerNumber + ")");

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#HangUpButton",
                EventData.of("Action", "hang_up"),
                false);
    }
}

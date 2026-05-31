package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.core.DeviceService;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.device.events.PhoneMessageSentEvent;
import esq.phonemod.device.services.DeviceCallService;
import esq.phonemod.device.services.DeviceContactService;
import esq.phonemod.device.services.DeviceMessagingService;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.apps.helpers.Whatgram;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.messaging.ChatMessage;
import esq.phonemod.phone.messaging.TextMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Whatgram — the in-game messaging app.
 *
 * <p>Demonstrates the preferred A4 pattern:
 * <ul>
 *   <li>All data access goes through {@link DeviceService} services — no direct
 *       {@code PhoneRegistry} / {@code CallRegistry} / {@code PhoneOwnerComponent}
 *       calls.</li>
 *   <li>All UI bindings use {@link PhoneUi} helpers with {@link PhoneEventActions}
 *       constants — no raw {@code EventData} construction.</li>
 *   <li>Message-sent events are emitted via {@link PhoneEvents}.</li>
 * </ul>
 */
public final class WhatgramApp extends StatefulPhoneApp<WhatgramApp.State> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public enum State {
        CONVERSATION_LIST,
        CHAT
    }

    /** App-owned payload key — the contact phone number for the open chat. */
    private static final String KEY_CONTACT = "currentContact";

    public WhatgramApp() {
        super(State.CONVERSATION_LIST);
    }

    @Override
    public String getId() {
        return "whatgram";
    }

    @Override
    public String getDisplayName() {
        return "Whatgram";
    }

    @Override
    public String getAppButtonUI() {
        return PhoneAssetPaths.WHATGRAM_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_WHATGRAM_UI;
    }

    @Override
    public String getIconPath() {
        return "Pages/Phone/Whatgram.png";
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        // Background-themeable surfaces on the Dust Whatgram root. These live in
        // the single root and are present in BOTH views, so theme re-application
        // never targets a dead selector.
        return Map.of(
                "appHeader", Whatgram.SEL_HEADER,
                "appPanel", Whatgram.SEL_PANEL,
                "appContent", Whatgram.SEL_CONTENT_LIST);
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);

        String currentContact = ctx.getState(KEY_CONTACT);
        if (getState(ctx) == State.CHAT && currentContact != null) {
            renderChat(ctx, currentContact, cmd, evb);
        } else {
            setState(ctx, State.CONVERSATION_LIST);
            renderConversationList(ctx, cmd, evb);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderConversationList(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        // List view: plain "Whatgram" title, no chat chrome.
        PhoneUi.setText(cmd, Whatgram.SEL_HEADER_TITLE_TEXT, "Whatgram");
        PhoneUi.setVisible(cmd, Whatgram.SEL_HEADER_BACK_BUTTON, false);
        PhoneUi.setVisible(cmd, Whatgram.SEL_HEADER_ACTION_BUTTON, false);
        PhoneUi.setVisible(cmd, Whatgram.SEL_COMPOSER, false);

        // Clear only THIS app's list container — never the shell content area
        // (#AppContent), which holds the app root we just appended.
        PhoneUi.safeClear(cmd, Whatgram.SEL_CONTENT_LIST);

        DeviceContactService contactService = DeviceService.get().getContactService();
        DeviceMessagingService messagingService = DeviceService.get().getMessagingService();

        Map<String, String> contacts = contactService.getContacts(
                ctx.getStore(), ctx.getRef(), ctx.getPhoneNumber());

        LinkedHashMap<String, String> uniqueSenders = new LinkedHashMap<>();
        ConversationHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), ConversationHistoryComponent.getComponentType());

        for (String contact : history.getContacts(ctx.getPhoneNumber())) {
            uniqueSenders.putIfAbsent(contact, contacts.getOrDefault(contact, contact));
        }

        for (TextMessage msg : messagingService.getInbox(ctx.getPhoneNumber())) {
            String displayName = contacts.getOrDefault(msg.fromNumber(), msg.fromName());
            uniqueSenders.putIfAbsent(msg.fromNumber(), displayName);
        }

        int i = 0;
        for (Map.Entry<String, String> sender : uniqueSenders.entrySet()) {
            String fromNumber = sender.getKey();
            String displayName = sender.getValue();
            String row = PhoneUi.appendListItem(cmd, Whatgram.SEL_CONTENT_LIST, Whatgram.ENTRY_UI, i);
            PhoneUi.setText(cmd, PhoneUi.child(row, Whatgram.SEL_ENTRY_NAME_TEXT),
                    displayName + " (" + fromNumber + ")");
            PhoneUi.bindAction(evb, PhoneUi.child(row, Whatgram.SEL_ENTRY_BUTTON),
                    PhoneEventActions.OPEN_CHAT,
                    PhoneUi.params("Contact", fromNumber));
            i++;
        }
    }

    private void renderChat(PhoneAppContext ctx, String contactNumber, UICommandBuilder cmd, UIEventBuilder evb) {
        DeviceContactService contactService = DeviceService.get().getContactService();
        Map<String, String> contacts = contactService.getContacts(
                ctx.getStore(), ctx.getRef(), ctx.getPhoneNumber());
        String displayName = contacts.getOrDefault(contactNumber, contactNumber);

        // Chat view reuses the SAME root: retitle the header, reveal the chat
        // chrome (back/call/composer), and refill the shared content list with
        // bubbles. The shell content area is never touched here.
        PhoneUi.setText(cmd, Whatgram.SEL_HEADER_TITLE_TEXT, displayName);
        PhoneUi.setVisible(cmd, Whatgram.SEL_HEADER_BACK_BUTTON, true);
        PhoneUi.setVisible(cmd, Whatgram.SEL_HEADER_ACTION_BUTTON, true);
        PhoneUi.setVisible(cmd, Whatgram.SEL_COMPOSER, true);

        renderMessages(ctx, contactNumber, cmd);

        PhoneUi.bindCapturedText(evb, Whatgram.SEL_SEND_BUTTON,
                PhoneEventActions.SEND_MESSAGE, "MessageValue", Whatgram.SEL_MESSAGE_INPUT_VALUE);
        PhoneUi.bindAction(evb, Whatgram.SEL_HEADER_ACTION_BUTTON,
                PhoneEventActions.START_CALL,
                PhoneUi.params("Contact", contactNumber));
        PhoneUi.bindBack(evb, Whatgram.SEL_HEADER_BACK_BUTTON);
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();

        if (PhoneEventActions.OPEN_CHAT.equals(action)) {
            String contact = event.getParam("Contact");
            LOGGER.atFine().log("[WhatgramApp] open_chat phone=%s contact=%s state=%s",
                    ctx.getPhoneNumber(), contact, getState(ctx));
            if (contact != null && !contact.isBlank()) {
                ctx.setState(KEY_CONTACT, contact);
                setState(ctx, State.CHAT);
                build(ctx, cmd, evb);
                return true;
            }
            return false;
        }

        if (PhoneEventActions.START_CALL.equals(action)) {
            String target = event.getParam("Contact");
            LOGGER.atFine().log("[WhatgramApp] start_call phone=%s target=%s",
                    ctx.getPhoneNumber(), target);
            if (target != null && !target.isBlank()) {
                DeviceService.get().getCallService()
                        .initiateCall(ctx.getPhoneNumber(), target.trim(), target.trim());
            }
            build(ctx, cmd, evb);
            return true;
        }

        if (PhoneEventActions.BACK.equals(action)) {
            LOGGER.atFine().log("[WhatgramApp] back phone=%s state=%s",
                    ctx.getPhoneNumber(), getState(ctx));
            ctx.clearState(KEY_CONTACT);
            setState(ctx, State.CONVERSATION_LIST);
            build(ctx, cmd, evb);
            return true;
        }

        if (PhoneEventActions.SEND_MESSAGE.equals(action)) {
            return handleSendMessage(ctx, event, cmd, evb);
        }

        return false;
    }

    private boolean handleSendMessage(PhoneAppContext ctx, PhoneEvent event,
            UICommandBuilder cmd, UIEventBuilder evb) {
        String currentContact = ctx.getState(KEY_CONTACT);
        if (currentContact == null) {
            build(ctx, cmd, evb);
            return true;
        }
        String body = event.getParam("MessageValue");
        if (body == null || body.isBlank()) {
            build(ctx, cmd, evb);
            return true;
        }

        String trimmedBody = body.trim();
        String myNumber = ctx.getPhoneNumber();
        LOGGER.atFine().log("[WhatgramApp] send_message phone=%s contact=%s body=%s",
                myNumber, currentContact, trimmedBody);

        // Persist outgoing message locally.
        Ref<EntityStore> ref = ctx.getRef();
        Store<EntityStore> store = ctx.getStore();
        ConversationHistoryComponent history =
                store.ensureAndGetComponent(ref, ConversationHistoryComponent.getComponentType());
        history.addMessage(myNumber, currentContact,
                new ChatMessage(true, myNumber, trimmedBody));
        store.putComponent(ref, ConversationHistoryComponent.getComponentType(), history);

        // Deliver to recipient via messaging service.
        DeviceService.get().getMessagingService().deliver(currentContact,
                new TextMessage(myNumber, myNumber, trimmedBody));

        // Emit typed event for framework observers.
        PhoneEvents.post(new PhoneMessageSentEvent(myNumber, currentContact, trimmedBody));

        // Re-render the message list from history — single source of truth, so the
        // bubble indices can never drift from the displayed list.
        renderMessages(ctx, currentContact, cmd);
        return true;
    }

    // ── Incoming message ───────────────────────────────────────────────────────

    @Override
    public boolean onIncomingMessage(PhoneAppContext ctx, String fromNumber,
            UICommandBuilder cmd, UIEventBuilder evb) {
        String currentContact = ctx.getState(KEY_CONTACT);
        LOGGER.atFine().log("[WhatgramApp] onIncomingMessage phone=%s from=%s currentContact=%s state=%s",
                ctx.getPhoneNumber(), fromNumber, currentContact, getState(ctx));
        if (getState(ctx) == State.CHAT && fromNumber.equals(currentContact)) {
            // Rebuild the whole list from history (a partial update to
            // #WhatgramContent — the root is already on the client). This avoids
            // any index drift between the displayed bubbles and history.
            renderMessages(ctx, currentContact, cmd);
            return true;
        }
        return false;
    }

    @Override
    public void onIncomingMessage(PhoneAppContext ctx, String fromNumber) {
        // No-op — the page will rebuild after this hook via the default delegate.
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Clears {@code #WhatgramContent} and re-appends every message bubble from
     * history, in order. This is the single render path for the chat list, used by
     * the full chat view, message send, and live incoming-message push — so the
     * bubble at row {@code i} is always exactly the {@code i}-th history message and
     * the {@code #WhatgramContent[i]} selectors can never drift.
     */
    private void renderMessages(PhoneAppContext ctx, String contactNumber, UICommandBuilder cmd) {
        ConversationHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), ConversationHistoryComponent.getComponentType());
        List<ChatMessage> messages = history.getMessages(ctx.getPhoneNumber(), contactNumber);
        Map<String, String> contacts = DeviceService.get().getContactService()
                .getContacts(ctx.getStore(), ctx.getRef(), ctx.getPhoneNumber());

        PhoneUi.safeClear(cmd, Whatgram.SEL_CONTENT_LIST);
        int i = 0;
        for (ChatMessage msg : messages) {
            appendMessageBubble(ctx, contactNumber, msg, i, cmd, contacts);
            i++;
        }
    }

    private void appendMessageBubble(PhoneAppContext ctx,
            String contactNumber,
            ChatMessage msg,
            int index,
            UICommandBuilder cmd,
            Map<String, String> contacts) {
        LOGGER.atFine().log("[WhatgramApp] appendBubble phone=%s contact=%s index=%d fromMe=%s",
                ctx.getPhoneNumber(), contactNumber, index, msg.fromMe());
        String row = PhoneUi.appendListItem(cmd, Whatgram.SEL_CONTENT_LIST, Whatgram.BUBBLE_UI, index);
        String senderName = msg.fromMe() ? "You"
                : contacts.getOrDefault(contactNumber, msg.fromName() != null ? msg.fromName() : contactNumber);
        PhoneUi.setText(cmd, PhoneUi.child(row, Whatgram.SEL_BUBBLE_SENDER_TEXT), senderName);
        PhoneUi.setText(cmd, PhoneUi.child(row, Whatgram.SEL_BUBBLE_BODY_TEXT), msg.body());
        PhoneUi.setText(cmd, row + ".Background",
                msg.fromMe() ? Whatgram.BUBBLE_COLOR_SENT : Whatgram.BUBBLE_COLOR_RECV);
    }
}

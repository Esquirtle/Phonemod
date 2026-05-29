package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.StatefulPhoneApp;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.messaging.CallRegistry;
import esq.phonemod.phone.messaging.ChatMessage;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class WhatgramApp extends StatefulPhoneApp<WhatgramApp.State> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public enum State {
        CONVERSATION_LIST,
        CHAT
    }

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
        return "Pages/Phone/Components/WhatgramButton.ui";
    }

    @Override
    public String getUIPath() {
        return "Pages/Phone/Whatgram.ui";
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);

        String currentContact = ctx.getState(KEY_CONTACT);
        if (getState(ctx) == State.CHAT && currentContact != null) {
            renderChat(ctx, currentContact, cmd, evb);
        } else {
            setState(ctx, State.CONVERSATION_LIST);
            renderConversationList(ctx, cmd, evb);
        }
    }

    private void renderConversationList(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.clear("#WhatgramContent");

        PhoneOwnerComponent owner = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), PhoneOwnerComponent.getComponentType());
        Map<String, String> contacts = owner.getContacts(ctx.getPhoneNumber());

        LinkedHashMap<String, String> uniqueSenders = new LinkedHashMap<>();
        ConversationHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), ConversationHistoryComponent.getComponentType());

        for (String contact : history.getContacts(ctx.getPhoneNumber())) {
            uniqueSenders.putIfAbsent(contact, contacts.getOrDefault(contact, contact));
        }

        for (TextMessage msg : PhoneRegistry.getInbox(ctx.getPhoneNumber())) {
            String displayName = contacts.getOrDefault(msg.fromNumber(), msg.fromName());
            uniqueSenders.putIfAbsent(msg.fromNumber(), displayName);
        }

        int i = 0;
        for (Map.Entry<String, String> sender : uniqueSenders.entrySet()) {
            String fromNumber = sender.getKey();
            String displayName = sender.getValue();
            cmd.append("#WhatgramContent", "Pages/Phone/Components/WhatgramChatEntry.ui");
            cmd.set("#WhatgramContent[" + i + "] #Name.Text", displayName + " (" + fromNumber + ")");
            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    "#WhatgramContent[" + i + "] #Button",
                    EventData.of("Action", "open_chat").append("Contact", fromNumber),
                    false);
            i++;
        }
    }

    private void renderChat(PhoneAppContext ctx, String contactNumber, UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.clear("#AppContent");
        cmd.append("#AppContent", "Pages/Phone/Components/WhatgramChat.ui");
        String displayName = resolveContactName(ctx, contactNumber, contactNumber);
        cmd.set("#ContactTopbar #ContactName.Text", displayName);

        ConversationHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), ConversationHistoryComponent.getComponentType());
        List<ChatMessage> messages = history.getMessages(ctx.getPhoneNumber(), contactNumber);
        cmd.clear("#MessageList");
        int i = 0;
        for (ChatMessage msg : messages) {
            cmd.append("#MessageList", "Pages/Phone/Components/WhatgramMessageBubble.ui");
            String messageItem = "#MessageList[" + i + "]";
            String senderName = msg.fromMe() ? "You" : resolveContactName(ctx, contactNumber, msg.fromName());
            cmd.set(messageItem + " #Sender.Text", senderName);
            cmd.set(messageItem + " #Body.Text", msg.body());
            cmd.set(messageItem + ".Background",
                    msg.fromMe() ? "#43D69A" : "#99ae5e");
            i++;
        }

        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#SendButton",
                EventData.of("Action", "send_message").append("@MessageValue", "#MessageInput.Value"),
                false);
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#ContactTopbar #CallButton",
                EventData.of("Action", "start_call").append("Contact", contactNumber),
                false);
        evb.addEventBinding(CustomUIEventBindingType.Activating,
                "#ContactTopbar #BackButton",
                EventData.of("Action", "back"),
                false);
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        String action = event.getAction();
        if ("open_chat".equals(action)) {
            String contact = event.getParams().get("Contact");
            LOGGER.atInfo().log("[WhatgramApp] open_chat phone=%s contact=%s state=%s", ctx.getPhoneNumber(), contact, getState(ctx));
            if (contact != null && !contact.isBlank()) {
                ctx.setState(KEY_CONTACT, contact);
                setState(ctx, State.CHAT);
                build(ctx, cmd, evb);
                return true;
            }
            return false;
        }

        if ("start_call".equals(action)) {
            String target = event.getParams().get("Contact");
            LOGGER.atInfo().log("[WhatgramApp] start_call phone=%s target=%s", ctx.getPhoneNumber(), target);
            if (target != null && !target.isBlank()) {
                CallRegistry.initiateCall(ctx.getPhoneNumber(), target.trim(), target.trim());
            }
            build(ctx, cmd, evb);
            return true;
        }

        if ("back".equals(action)) {
            LOGGER.atInfo().log("[WhatgramApp] back phone=%s state=%s", ctx.getPhoneNumber(), getState(ctx));
            ctx.clearState(KEY_CONTACT);
            setState(ctx, State.CONVERSATION_LIST);
            build(ctx, cmd, evb);
            return true;
        }

        if ("send_message".equals(action)) {
            String currentContact = ctx.getState(KEY_CONTACT);
            if (currentContact == null) {
                build(ctx, cmd, evb);
                return true;
            }
            String body = event.getParams().get("MessageValue");
            if (body == null || body.isBlank()) {
                build(ctx, cmd, evb);
                return true;
            }

            String trimmedBody = body.trim();
            LOGGER.atInfo().log("[WhatgramApp] send_message phone=%s contact=%s body=%s", ctx.getPhoneNumber(), currentContact, trimmedBody);
            Ref<EntityStore> ref = ctx.getRef();
            Store<EntityStore> store = ctx.getStore();
            ConversationHistoryComponent history = store.ensureAndGetComponent(ref, ConversationHistoryComponent.getComponentType());
            history.addMessage(ctx.getPhoneNumber(), currentContact,
                    new ChatMessage(true, ctx.getPhoneNumber(), trimmedBody));
            store.putComponent(ref, ConversationHistoryComponent.getComponentType(), history);
            PhoneRegistry.deliver(currentContact,
                    new TextMessage(ctx.getPhoneNumber(), ctx.getPhoneNumber(), trimmedBody));

            ChatMessage outgoingMessage = new ChatMessage(true, ctx.getPhoneNumber(), trimmedBody);
            appendMessageBubble(ctx, currentContact, outgoingMessage, history.getMessages(ctx.getPhoneNumber(), currentContact).size() - 1, cmd);
            return true;
        }

        return false;
    }

    private void appendMessageBubble(PhoneAppContext ctx,
                                     String contactNumber,
                                     ChatMessage msg,
                                     int index,
                                     UICommandBuilder cmd) {
        LOGGER.atInfo().log("[WhatgramApp] appendBubble phone=%s contact=%s index=%d fromMe=%s", ctx.getPhoneNumber(), contactNumber, index, msg.fromMe());
        cmd.append("#MessageList", "Pages/Phone/Components/WhatgramMessageBubble.ui");
        String messageItem = "#MessageList[" + index + "]";
        String senderName = msg.fromMe() ? "You" : resolveContactName(ctx, contactNumber, msg.fromName());
        cmd.set(messageItem + " #Sender.Text", senderName);
        cmd.set(messageItem + " #Body.Text", msg.body());
        cmd.set(messageItem + ".Background",
                msg.fromMe() ? "#43D69A" : "#99ae5e");
    }

    @Override
    public boolean onIncomingMessage(PhoneAppContext ctx, String fromNumber, UICommandBuilder cmd, UIEventBuilder evb) {
        String currentContact = ctx.getState(KEY_CONTACT);
        LOGGER.atInfo().log("[WhatgramApp] onIncomingMessage phone=%s from=%s currentContact=%s state=%s", ctx.getPhoneNumber(), fromNumber, currentContact, getState(ctx));
        if (getState(ctx) == State.CHAT && fromNumber.equals(currentContact)) {
            ConversationHistoryComponent history = ctx.getStore().ensureAndGetComponent(
                    ctx.getRef(), ConversationHistoryComponent.getComponentType());
            List<ChatMessage> messages = history.getMessages(ctx.getPhoneNumber(), currentContact);
            if (!messages.isEmpty()) {
                ChatMessage incoming = messages.get(messages.size() - 1);
                appendMessageBubble(ctx, currentContact, incoming, messages.size() - 1, cmd);
                return true;
            }
        }
        return false;
    }

    private String resolveContactName(PhoneAppContext ctx, String contactNumber, String fallback) {
        PhoneOwnerComponent owner = ctx.getStore().ensureAndGetComponent(
                ctx.getRef(), PhoneOwnerComponent.getComponentType());
        String saved = owner.getContacts(ctx.getPhoneNumber()).get(contactNumber);
        if (saved != null && !saved.isBlank()) {
            return saved;
        }
        return fallback != null && !fallback.isBlank() ? fallback : contactNumber;
    }

    @Override
    public void onIncomingMessage(PhoneAppContext ctx, String fromNumber) {
        String currentContact = ctx.getState(KEY_CONTACT);
        if (getState(ctx) == State.CHAT && fromNumber.equals(currentContact)) {
            // no-op, page will rebuild after this hook
        }
    }
}

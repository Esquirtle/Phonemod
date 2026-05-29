package esq.phonemod.phone.apps.helpers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.messaging.ChatMessage;
import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static helpers that build Whatgram UI state — conversation list and chat view. */
public final class Whatgram {

    static final String WHATGRAM_UI        = "Pages/Phone/Whatgram.ui";
    static final String ENTRY_UI           = "Pages/Phone/Components/WhatgramChatEntry.ui";
    static final String WHATCHAT_UI        = "Pages/Phone/Components/WhatgramChat.ui";
    static final String BUBBLE_UI          = "Pages/Phone/Components/WhatgramMessageBubble.ui";
    static final String BUBBLE_COLOR_SENT  = "#43D69A";
    static final String BUBBLE_COLOR_RECV  = "#99ae5e";

    private static final String CONTENT = "#AppContent";

    private Whatgram() {}

    /**
     * Clears {@code #AppContent} and populates the Whatgram conversation list.
     * Merges persistent history contacts with the session inbox so every known
     * conversation appears even when no message arrived this session.
     */
    public static void loadConversationList(@Nonnull String phoneNumber,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Ref<EntityStore> ref,
                                             @Nonnull UICommandBuilder cmd,
                                             @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, WHATGRAM_UI);

        LinkedHashMap<String, String> uniqueSenders = new LinkedHashMap<>();

        ConversationHistoryComponent history =
                store.ensureAndGetComponent(ref, ConversationHistoryComponent.getComponentType());
        for (String contact : history.getContacts(phoneNumber)) {
            uniqueSenders.putIfAbsent(contact, contact);
        }

        for (TextMessage msg : PhoneRegistry.getInbox(phoneNumber)) {
            uniqueSenders.put(msg.fromNumber(), msg.fromName());
        }

        int i = 0;
        for (Map.Entry<String, String> sender : uniqueSenders.entrySet()) {
            String fromNumber = sender.getKey();
            String fromName   = sender.getValue();
            cmd.append("#WhatgramContent", ENTRY_UI);
            cmd.set("#WhatgramContent[" + i + "] #Name.Text", fromName + " (" + fromNumber + ")");
            evb.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#WhatgramContent[" + i + "] #Button",
                    EventData.of("Action", "open_chat").append("Contact", fromNumber),
                    false);
            i++;
        }
    }

    /**
     * Clears {@code #AppContent} and renders the chat view for the given conversation.
     * Populates message bubbles from persistent history and binds the send button.
     */
    public static void loadChat(@Nonnull String phoneNumber,
                                 @Nonnull String contactNumber,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref,
                                 @Nonnull UICommandBuilder cmd,
                                 @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, WHATCHAT_UI);
        cmd.set("#ContactTopbar #ContactName.Text", contactNumber);

        ConversationHistoryComponent history =
                store.ensureAndGetComponent(ref, ConversationHistoryComponent.getComponentType());
        List<ChatMessage> messages = history.getMessages(phoneNumber, contactNumber);
        int i = 0;
        for (ChatMessage msg : messages) {
            cmd.append("#MessageList", BUBBLE_UI);
            cmd.set("#MessageList[" + i + "] #Sender.Text", msg.fromName());
            cmd.set("#MessageList[" + i + "] #Body.Text", msg.body());
            cmd.set("#MessageList[" + i + "].Background",
                    msg.fromMe() ? BUBBLE_COLOR_SENT : BUBBLE_COLOR_RECV);
            i++;
        }

        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SendButton",
                EventData.of("Action", "send_message").append("@MessageValue", "#MessageInput.Value"),
                false);
        evb.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ContactTopbar #CallButton",
                EventData.of("Action", "start_call").append("Contact", contactNumber),
                false);
    }
}

package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneEventActions;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.apps.groupchat.GroupChatState;
import esq.phonemod.phone.apps.groupchat.GroupMessage;
import esq.phonemod.phone.sync.SyncChannelHandle;
import esq.phonemod.phone.sync.SyncChannelType;

import java.util.Map;

/**
 * Reference app demonstrating the realtime sync layer: a single shared "global"
 * group chat. Every open Group Chat view subscribes to one persistent
 * {@link SyncChannelType}; sending a message publishes a mutation, which the
 * framework persists to JSON and broadcasts — re-rendering every subscriber's view
 * (including the sender) from the new shared state. Restarting the server reloads
 * the conversation from disk.
 *
 * <p>The part to copy is the <em>sync wiring</em>: {@link SyncChannelHandle#subscribe}
 * in {@link #build} and {@link SyncChannelHandle#update} in {@link #handleEvent}.
 * Rendering mirrors {@code WhatgramApp}'s chat view, composed from the Dust UI
 * library (see {@code Pages/Phone/DustGroupChat.ui}).
 */
public final class GroupChatApp extends StatefulPhoneApp<GroupChatApp.State> {

    public enum State {
        CHAT
    }

    /** One shared, durable channel kind. The id below selects the single global room. */
    public static final SyncChannelType<GroupChatState> CHATS = SyncChannelType.persistent(
            "chatgroup", GroupChatState.CODEC, GroupChatState::new, s -> s.id);

    private static final String GLOBAL_ROOM = "global";

    // ── Selectors (DustGroupChat.ui) ─────────────────────────────────────────
    private static final String SEL_HEADER = "#GroupChatHeader";
    private static final String SEL_PANEL = "#GroupChatPanel";
    private static final String SEL_CONTENT = "#GroupChatContent";
    private static final String SEL_SEND_BUTTON = "#GroupChatComposer #DustSendButton";
    private static final String SEL_MESSAGE_INPUT_VALUE = "#GroupChatComposer #DustMessageInput.Value";
    // Bubble children (relative to an appended GROUPCHAT_BUBBLE_UI row).
    private static final String SEL_BUBBLE_SENDER_TEXT = "#Sender.Text";
    private static final String SEL_BUBBLE_BODY_TEXT = "#Body.Text";

    public GroupChatApp() {
        super(State.CHAT);
    }

    @Override
    public String getId() {
        return "groupchat";
    }

    @Override
    public String getDisplayName() {
        return "Group Chat";
    }

    @Override
    public String getAppButtonUI() {
        return PhoneAssetPaths.GROUPCHAT_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_GROUPCHAT_UI;
    }

    @Override
    public String getIconPath() {
        return "Pages/Phone/Whatgram.png";
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        return Map.of(
                "appHeader", SEL_HEADER,
                "appPanel", SEL_PANEL,
                "appContent", SEL_CONTENT);
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(ctx, cmd);

        SyncChannelHandle<GroupChatState> channel = ctx.channel(CHATS, GLOBAL_ROOM);
        // Subscribe this view: any publish to the channel re-renders it via refreshApp.
        channel.subscribe();

        // Send: capture the composer input and fire SEND_MESSAGE.
        PhoneUi.bindCapturedText(evb, SEL_SEND_BUTTON, PhoneEventActions.SEND_MESSAGE,
                "MessageValue", SEL_MESSAGE_INPUT_VALUE);

        PhoneUi.safeClear(cmd, SEL_CONTENT);
        int index = 0;
        for (GroupMessage message : channel.state().messages()) {
            String row = PhoneUi.appendListItem(cmd, SEL_CONTENT, PhoneAssetPaths.GROUPCHAT_BUBBLE_UI, index);
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_BUBBLE_SENDER_TEXT), message.senderName);
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_BUBBLE_BODY_TEXT), message.body);
            index++;
        }
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
            UICommandBuilder cmd, UIEventBuilder evb) {
        if (!PhoneEventActions.SEND_MESSAGE.equals(event.getAction())) {
            return false;
        }
        String body = event.getParam("MessageValue");
        if (body != null && !body.isBlank()) {
            String sender = ctx.getPhoneNumber();
            GroupMessage message = new GroupMessage(sender, sender, body.trim(), System.currentTimeMillis());
            // Mutate -> persist -> broadcast. The broadcast re-renders every viewer
            // (including this sender) from the new state, so we don't build() here.
            ctx.channel(CHATS, GLOBAL_ROOM).update(state -> state.add(message));
        }
        return true;
    }
}

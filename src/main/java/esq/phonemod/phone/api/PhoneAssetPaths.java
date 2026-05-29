package esq.phonemod.phone.api;

/**
 * Constants for all UI asset paths used by the phone framework.
 *
 * <p>
 * Paths are relative to the game's asset root, as required by
 * {@code UICommandBuilder.append()} and {@code UICommandBuilder.clear()}.
 *
 * <p>
 * Image paths inside {@code .ui} files are resolved relative to the
 * {@code .ui} file's own location in the asset namespace.
 */
public final class PhoneAssetPaths {

    private PhoneAssetPaths() {
    }

    // ── Phone frame ───────────────────────────────────────────────────────────

    /** The outer phone shell UI (background, top bar, bottom bar). */
    public static final String PHONE_UI = "Pages/Phone/Phone.ui";

    // ── Built-in app UI roots (loaded into #AppContent) ───────────────────────

    public static final String WHATGRAM_UI = "Pages/Phone/Whatgram.ui";
    public static final String CONTACTS_UI = "Pages/Phone/Contacts.ui";
    public static final String CALLS_UI = "Pages/Phone/Calls.ui";
    public static final String SETTINGS_UI = "Pages/Phone/Settings.ui";

    // ── Component fragments (appended into list containers) ───────────────────

    public static final String WHATGRAM_CHAT_ENTRY_UI = "Pages/Phone/Components/WhatgramChatEntry.ui";
    public static final String WHATGRAM_CHAT_UI = "Pages/Phone/Components/WhatgramChat.ui";
    public static final String WHATGRAM_BUBBLE_UI = "Pages/Phone/Components/WhatgramMessageBubble.ui";

    public static final String CONTACTS_ENTRY_UI = "Pages/Phone/Components/ContactsEntry.ui";
    public static final String CONTACTS_ADD_UI = "Pages/Phone/Components/ContactsAdd.ui";

    public static final String CALL_HISTORY_ENTRY_UI = "Pages/Phone/Components/CallHistoryEntry.ui";
    public static final String INCOMING_CALL_UI = "Pages/Phone/Components/IncomingCall.ui";
    public static final String ACTIVE_CALL_UI = "Pages/Phone/Components/ActiveCall.ui";

}

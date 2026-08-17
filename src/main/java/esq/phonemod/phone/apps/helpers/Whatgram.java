package esq.phonemod.phone.apps.helpers;

/**
 * Selector and asset-path constants for the Whatgram UI.
 *
 * <p>Pure constants, referenced by {@link esq.phonemod.phone.apps.WhatgramApp}
 * so every Whatgram selector string lives in exactly one place.
 *
 * <p>Whatgram uses a <em>single-root</em> layout ({@code DustWhatgram.ui}): the
 * same document owns both the conversation-list view and the chat view. The app
 * switches between them by toggling child visibility and re-filling
 * {@link #SEL_CONTENT_LIST} — it never replaces the shell content area. None of
 * these selectors is the shell content area; apps get that from
 * {@code PhoneAppContext.getContentSelector()}.
 */
public final class Whatgram {

    private Whatgram() {}

    // ── UI file paths ─────────────────────────────────────────────────────────

    /** One conversation-list row. */
    public static final String ENTRY_UI  = "Pages/Phone/Components/DustWhatgramConvEntry.ui";
    /** One chat message bubble. */
    public static final String BUBBLE_UI = "Pages/Phone/Components/DustWhatgramBubble.ui";

    // ── Bubble colours (Dust palette) ─────────────────────────────────────────

    public static final String BUBBLE_COLOR_SENT = "#2f5d44";
    public static final String BUBBLE_COLOR_RECV = "#26303f";

    // ── Header (shared by both views) ─────────────────────────────────────────

    /** Title label; shows "Whatgram" in the list view, the contact in chat. */
    public static final String SEL_HEADER_TITLE_TEXT = "#WhatgramHeader #HeaderTitle.Text";
    /** Back button (@DustAppBar leading) — visible only in the chat view. */
    public static final String SEL_HEADER_BACK_BUTTON = "#WhatgramHeader #BackButton";
    /** Trailing action button (@DustAppBar, labelled "Call") — chat view only. */
    public static final String SEL_HEADER_ACTION_BUTTON = "#WhatgramHeader #ActionButton";

    // ── Themeable root surfaces (one source of truth; see getThemeableSelectors) ─

    /** Themeable header surface (role: appHeader). */
    public static final String SEL_HEADER = "#WhatgramHeader";
    /** Themeable panel surface (role: appPanel). */
    public static final String SEL_PANEL  = "#WhatgramPanel";

    // ── Panel content ─────────────────────────────────────────────────────────

    /** Scrolling list that holds conversation rows OR message bubbles. Themeable (role: appContent). */
    public static final String SEL_CONTENT_LIST = "#WhatgramContent";

    // Conversation-row children (relative to an appended ENTRY_UI row).
    /** Relative: name label inside a conversation row. */
    public static final String SEL_ENTRY_NAME_TEXT = "#Name.Text";
    /** Relative: open-chat button inside a conversation row. */
    public static final String SEL_ENTRY_BUTTON    = "#Button";

    // Bubble children (relative to an appended BUBBLE_UI row).
    /** Relative: sender label inside a message bubble. */
    public static final String SEL_BUBBLE_SENDER_TEXT = "#Sender.Text";
    /** Relative: body label inside a message bubble. */
    public static final String SEL_BUBBLE_BODY_TEXT   = "#Body.Text";

    // ── Composer (chat view only) ─────────────────────────────────────────────

    // The composer is a @DustShellMessageInput instance: children #DustSendButton
    // and #DustMessageInput.
    /** The composer row; hidden in the list view, shown in chat. */
    public static final String SEL_COMPOSER          = "#WhatgramComposer";
    public static final String SEL_SEND_BUTTON       = "#WhatgramComposer #DustSendButton";
    public static final String SEL_MESSAGE_INPUT_VALUE = "#WhatgramComposer #DustMessageInput.Value";
}

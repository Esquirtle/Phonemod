package esq.phonemod.phone.apps.helpers;

/**
 * Selector and asset-path constants for the Contacts UI.
 *
 * <p>Pure constants, referenced by {@link esq.phonemod.phone.apps.ContactsApp}.
 * None of these is the shell content area — apps get that from
 * {@code PhoneAppContext.getContentSelector()}.
 */
public final class Contacts {

    private Contacts() {}

    // ── UI file paths ─────────────────────────────────────────────────────────

    public static final String CONTACTS_ENTRY_UI = "Pages/Phone/Components/DustContactsEntry.ui";

    // ── Themeable root surfaces (one source of truth; see getThemeableSelectors) ─

    /** Themeable header surface (role: appHeader). */
    public static final String SEL_HEADER                  = "#ContactsHeader";
    /** Themeable panel surface (role: appPanel). */
    public static final String SEL_PANEL                   = "#ContactsPanel";

    // ── Contacts-list view (root page selectors) ──────────────────────────────

    /** List-view container (holds #ContactsList); toggled against the add form. */
    public static final String SEL_LIST_VIEW               = "#ContactsListView";
    /** Add-contact form container; a hidden sibling revealed in ADD_CONTACT state. */
    public static final String SEL_ADD_VIEW                = "#ContactsAddView";
    /** Scrollable list that contact-entry rows are appended into. Themeable (role: appContent). */
    public static final String SEL_CONTACTS_LIST           = "#ContactsList";
    public static final String SEL_ADD_CONTACT_BUTTON      = "#AddContactButton";
    /** Relative: contact name label inside a contacts-entry row. */
    public static final String SEL_ENTRY_CONTACT_NAME_TEXT = "#ContactName.Text";
    /** Relative: chat button inside a contacts-entry row. */
    public static final String SEL_ENTRY_CHAT_BUTTON       = "#ChatButton";
    /** Relative: remove button inside a contacts-entry row. */
    public static final String SEL_ENTRY_REMOVE_BUTTON     = "#RemoveButton";
    /** Relative: call button inside a contacts-entry row. */
    public static final String SEL_ENTRY_CALL_BUTTON       = "#CallButton";

    // ── Add-contact form (hidden sibling #ContactsAddView inside DustContacts.ui) ─

    public static final String SEL_SAVE_BUTTON        = "#SaveButton";
    public static final String SEL_CANCEL_BUTTON      = "#CancelButton";
    public static final String SEL_NUMBER_INPUT_VALUE = "#NumberInput.Value";
    public static final String SEL_NAME_INPUT_VALUE   = "#NameInput.Value";
}

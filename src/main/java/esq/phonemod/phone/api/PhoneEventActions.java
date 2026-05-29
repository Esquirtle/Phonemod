package esq.phonemod.phone.api;

/**
 * String constants for all action values used in phone UI event bindings.
 *
 * <p>
 * Use these in {@link PhoneApp#handleEvent} switch/if blocks and in
 * {@code EventData.of("Action", PhoneEventActions.X)} bindings to avoid
 * magic strings and typos across plugins.
 */
public final class PhoneEventActions {

    private PhoneEventActions() {
    }

    // ── Phone-level — handled by PhonePage before reaching any app ────────────

    /**
     * Return to the home screen (app menu). Fired by the bottom-bar home button.
     */
    public static final String HOME = "home";

    /**
     * Open a registered app by ID.
     * Payload key: {@code App} → app ID string.
     */
    public static final String OPEN_APP = "open_app";

    /**
     * Open a Whatgram conversation directly (opens Whatgram if not already active).
     * Payload key: {@code Contact} → phone number.
     */
    public static final String OPEN_CHAT = "open_chat";

    /** Answer an incoming call. */
    public static final String ANSWER_CALL = "answer_call";

    /** Decline an incoming call (ringing state). */
    public static final String DECLINE_CALL = "decline_call";

    /** Hang up an active call. */
    public static final String HANG_UP = "hang_up";

    // ── Whatgram app ──────────────────────────────────────────────────────────

    /** Navigate back from a chat view to the conversation list. */
    public static final String BACK = "back";

    /**
     * Send a message in the currently open Whatgram conversation.
     * Payload key: {@code MessageValue} (captured via {@code @MessageValue} from
     * {@code #MessageInput.Value}).
     */
    public static final String SEND_MESSAGE = "send_message";

    /**
     * Initiate a phone call.
     * Payload key: {@code Contact} → target phone number, or
     * {@code DialNumber} (captured via {@code @DialNumber} from
     * {@code #DialInput.Value}).
     */
    public static final String START_CALL = "start_call";

    // ── Contacts app ─────────────────────────────────────────────────────────

    /** Navigate to the add-contact form. */
    public static final String OPEN_ADD_CONTACT = "open_add_contact";

    /** Navigate back to the contacts list from the add-contact form. */
    public static final String CONTACTS = "contacts";

    /**
     * Save a new contact from the add-contact form.
     * Payload keys: {@code ContactFormNumber}, {@code ContactFormName} (both
     * captured
     * via the {@code @} prefix from their respective input fields).
     */
    public static final String SAVE_CONTACT = "save_contact";

    /**
     * Remove a contact.
     * Payload key: {@code Contact} → phone number to remove.
     */
    public static final String REMOVE_CONTACT = "remove_contact";
}

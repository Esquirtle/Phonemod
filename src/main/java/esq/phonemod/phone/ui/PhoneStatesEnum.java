package esq.phonemod.phone.ui;

/**
 * Tracks which top-level state {@link PhonePage} is currently in.
 * App-internal navigation states (e.g. "which chat is open") are not
 * represented here — those live in {@link esq.phonemod.phone.api.PhoneAppContext}
 * per-player state via {@code ctx.getState()}.
 */
public enum PhoneStatesEnum {

    /** The app-launcher home screen is displayed. Set by {@code loadHomeState()} and the "home" action. */
    HOME,

    /** A registered phone app is open. Set by {@code openApp()}. */
    APP,

    /**
     * The calls screen (dialer + history) is displayed.
     * Set by {@code onCallEnded()} when no app was open before the call.
     */
    CALLS,

    /** An incoming-call prompt is displayed. Set by {@code onIncomingCall()} and during {@code build()} call-state restore. */
    INCOMING_CALL,

    /** The active-call screen is displayed. Set by {@code onCallAnswered()} and during {@code build()} call-state restore. */
    ACTIVE_CALL
}

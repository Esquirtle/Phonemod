package esq.phonemod.device.api;

import javax.annotation.Nonnull;

/**
 * Live callback surface for an opened device page.
 *
 * <p>This keeps registries from depending on a concrete page implementation —
 * they hold this handle, while {@code DevicePage} provides it.
 */
public interface DevicePageHandle {

    void onIncomingMessage(@Nonnull String fromNumber);

    void onIncomingCall(@Nonnull String callerNumber, @Nonnull String callerName);

    /**
     * Shows the caller a "calling…" / ringing view while a placed outgoing call waits
     * for the callee to answer. Default is a no-op for page implementations that don't
     * render an outgoing-call state.
     */
    default void onOutgoingCall(@Nonnull String calleeNumber, @Nonnull String calleeName) {
    }

    void onCallAnswered(@Nonnull String partnerNumber, @Nonnull String partnerName);

    void onCallEnded();

    /**
     * Re-applies theme colors to the live page after a settings change.
     * Default is a no-op so legacy page implementations need not support theming.
     */
    default void reapplyTheme(@Nonnull String themeId) {
    }
}


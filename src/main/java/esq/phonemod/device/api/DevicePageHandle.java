package esq.phonemod.device.api;

import javax.annotation.Nonnull;

/**
 * Live callback surface for an opened device page.
 *
 * <p>This keeps registries from depending on a concrete page implementation,
 * so the legacy {@code PhonePage} and the generic device page can coexist.
 */
public interface DevicePageHandle {

    void onIncomingMessage(@Nonnull String fromNumber);

    void onIncomingCall(@Nonnull String callerNumber, @Nonnull String callerName);

    void onCallAnswered(@Nonnull String partnerNumber, @Nonnull String partnerName);

    void onCallEnded();
}


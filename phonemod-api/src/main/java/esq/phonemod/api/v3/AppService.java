package esq.phonemod.api.v3;

import esq.phonemod.api.AppRegistration;

import javax.annotation.Nonnull;

/**
 * Plugin-facing entry point for registering {@link DeviceApp}s (R-API-9, D-28).
 *
 * <p>Registration is bound to the owning plugin's lifecycle: closing the
 * returned {@link AppRegistration} (or the plugin disabling) unregisters the
 * app. {@code AppRegistration} is reused from its v2 FQN as-is — it has no UI or
 * v2-app coupling ({@code esq.phonemod.api.AppRegistration}: {@code appId()},
 * {@code active()}, {@code close()}).
 */
public interface AppService {

    @Nonnull
    AppRegistration registerApp(@Nonnull DeviceApp<?> app);

    /** Requests a repaint of {@code session}'s currently open app, from outside it. */
    void refreshApp(@Nonnull DeviceSessionId session);
}

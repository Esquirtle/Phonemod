package esq.phonemod.api;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import esq.phonemod.phone.api.PhoneApp;

public interface AppService {
    @Nonnull AppRegistration registerApp(@Nonnull PluginBase owner, @Nonnull DeviceApp app);
    /** Owner-bound bridge for v1 apps retained throughout 2.x. */
    @Deprecated(since = "2.0", forRemoval = false)
    @Nonnull AppRegistration registerLegacyApp(@Nonnull PluginBase owner,
            @Nonnull String canonicalId, @Nonnull Set<String> legacyAliases,
            @Nonnull PhoneApp<?> app);
    @Nonnull Optional<DeviceApp> find(@Nonnull String canonicalIdOrAlias);
    @Nonnull Collection<AppDescriptor> snapshot();

    /**
     * Requests a repaint of {@code appId} on the given phone's currently-open page — safe to
     * call from <em>any</em> thread. The framework hops to that player's world thread and
     * re-renders the app; it is a no-op if the phone is offline or a different app is open.
     *
     * <p>This is the supported way for an async, data-backed app (e.g. one whose service
     * returns {@link java.util.concurrent.CompletionStage}) to refresh after a completion
     * fires off the world thread — the app renders synchronously from its own cache and calls
     * {@code refreshApp(getId(), phoneNumber)} when new data is ready, instead of wiring a
     * sync channel purely as a refresh signal.
     *
     * @param appId       the app's id (e.g. {@code app.getId()})
     * @param phoneNumber the target phone number ({@code PhoneAppContext.getPhoneNumber()})
     */
    void refreshApp(@Nonnull String appId, @Nonnull String phoneNumber);
}

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
}

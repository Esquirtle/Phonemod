package esq.phonemod.api;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface DeviceService {
    @Nonnull Optional<DeviceIdentity> find(@Nonnull UUID id);
    @Nonnull Optional<DeviceIdentity> findByAddress(@Nonnull String address);
    @Nonnull Collection<DeviceIdentity> snapshot();
    void openShared(@Nonnull PlayerRef player, @Nonnull Ref<EntityStore> ref,
                    @Nonnull Store<EntityStore> store, @Nonnull String deviceAssetId,
                    @Nonnull String stableKey);
    @Nonnull Map<String, String> contacts(@Nonnull Ref<EntityStore> ref,
                    @Nonnull Store<EntityStore> store, @Nonnull String address);
}

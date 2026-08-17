package esq.phonemod.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public interface DeviceAppContext {
    @Nonnull PlayerRef player();
    @Nonnull DeviceIdentity identity();
    @Nonnull default Optional<String> address() { return identity().optionalAddress(); }
    void executeOnWorld(@Nonnull Consumer<WorldAccess> work);
    @Nonnull Optional<String> state(@Nonnull String key);
    void state(@Nonnull String key, @Nonnull String value);
    void notify(@Nonnull String title, @Nonnull String body);
    @Nonnull SyncService sync();
}

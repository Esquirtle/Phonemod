package esq.phonemod.api.v3;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import esq.phonemod.api.DeviceIdentity;
import esq.phonemod.phone.sync.SyncChannelType;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/** Per-session context handed to a {@link DeviceApp}. */
public interface DeviceAppContext<M extends ViewModel> {

    @Nonnull
    PlayerRef player();

    @Nonnull
    DeviceIdentity identity();

    @Nonnull
    DeviceSessionId sessionId();

    @Nonnull
    DeviceType deviceType();

    @Nonnull
    String address();

    /** Safe from any thread; coalesces into a single world-thread flush (R-KIT-5). */
    void refresh();

    /** Safe from any thread; {@code mutator} runs on the world thread. */
    void onWorld(@Nonnull Consumer<M> mutator);

    @Nonnull
    <S> SyncChannels<S> sync(@Nonnull SyncChannelType<S> type);

    void notify(@Nonnull String title, @Nonnull String body);

    /** World-thread only. */
    @Nonnull
    AppState state();
}

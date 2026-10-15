package esq.phonemod.ui.kit.testing;

import esq.phonemod.api.v3.DeviceApp;
import esq.phonemod.ui.kit.host.AppHost;
import esq.phonemod.ui.kit.host.DeviceSessionInfo;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Wires a {@link FakeBackend} and a {@link FakeWorldThread} into a ready-to-use
 * {@link AppHost} for app-level tests. {@link #defaultSession} is a stock fixture
 * session. The {@code SyncGateway} it supplies throws {@code "F4: sync needs core"}.
 */
public final class FakeHost {

    public FakeHost() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public DeviceSessionInfo defaultSession() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public <M extends esq.phonemod.api.v3.ViewModel> AppHost<M> open(@Nonnull DeviceApp<M> app) {
        throw new UnsupportedOperationException("F4");
    }

    /** Notifications delivered through {@code DeviceAppContext.notify}, in order. */
    @Nonnull
    public List<String> notifications() {
        throw new UnsupportedOperationException("F4");
    }
}

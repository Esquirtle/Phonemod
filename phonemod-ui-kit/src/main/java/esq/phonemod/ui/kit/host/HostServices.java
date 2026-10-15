package esq.phonemod.ui.kit.host;

import javax.annotation.Nonnull;

/** Bundle of ports an {@link AppHost} needs, independent of any one app. */
public record HostServices(
        @Nonnull RenderBackend backend,
        @Nonnull WorldThread world,
        @Nonnull esq.phonemod.api.v3.AppState state,
        @Nonnull Notifier notifier,
        @Nonnull SyncGateway sync) {

    public HostServices {
        throw new UnsupportedOperationException("F4");
    }
}

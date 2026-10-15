package esq.phonemod.ui.kit.host;

import esq.phonemod.api.v3.Backend;
import esq.phonemod.api.v3.ViewRef;
import esq.phonemod.ui.kit.diff.DiffOp;

import javax.annotation.Nonnull;
import java.util.List;

/** Backend-specific sink for diff operations; implemented once per backend (legacy/Noesis). */
public interface RenderBackend {

    @Nonnull
    Backend backend();

    void open(@Nonnull DeviceSessionInfo session, @Nonnull Object app, @Nonnull ViewRef view);

    void apply(@Nonnull DeviceSessionInfo session, @Nonnull List<DiffOp> ops);

    void close(@Nonnull DeviceSessionInfo session);
}

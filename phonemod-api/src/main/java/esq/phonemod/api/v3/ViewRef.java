package esq.phonemod.api.v3;

import javax.annotation.Nonnull;

/** Reference to a backend-specific view document (e.g. a {@code .ui} path). */
public interface ViewRef {

    @Nonnull
    String document();

    @Nonnull
    static ViewRef of(@Nonnull String document) {
        throw new UnsupportedOperationException("F4");
    }
}

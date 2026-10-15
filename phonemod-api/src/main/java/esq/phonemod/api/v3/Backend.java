package esq.phonemod.api.v3;

/**
 * Rendering backend a device app view may target.
 *
 * <p>{@link #LEGACY} is the CustomUIPage + {@code .ui} backend (production).
 * {@link #NOESIS} is the experimental backend, off by default.
 */
public enum Backend {
    LEGACY,
    NOESIS
}

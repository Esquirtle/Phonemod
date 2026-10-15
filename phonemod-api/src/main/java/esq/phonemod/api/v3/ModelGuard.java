package esq.phonemod.api.v3;

/**
 * Attached to a {@link ViewModel} to enforce where it may be read/written
 * (R-KIT-5: the world thread). Thrown exceptions from {@link #checkAccess()}
 * propagate to the caller of the {@link Prop}/{@link ListProp} accessor.
 */
@FunctionalInterface
public interface ModelGuard {
    void checkAccess();
}

package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Base class for an app's typed, dirty-tracked model. Fields register themselves
 * from instance initializers (no reflection); {@link #attach(ModelGuard)} wires in
 * the thread guard exactly once.
 */
public abstract class ViewModel {

    protected ViewModel() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    protected final <T> Prop<T> prop(@Nonnull String name, @Nullable T initial) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    protected final <K, I> ListProp<K, I> list(@Nonnull String name,
            @Nonnull Function<? super I, ? extends K> keyOf) {
        throw new UnsupportedOperationException("F4");
    }

    /** Declared fields, in declaration order. */
    @Nonnull
    public final List<ModelField> fields() {
        throw new UnsupportedOperationException("F4");
    }

    /** @throws IllegalStateException on a second call */
    public final void attach(@Nonnull ModelGuard guard) {
        throw new UnsupportedOperationException("F4");
    }
}

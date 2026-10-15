package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single typed, dirty-tracked field of a {@link ViewModel}. Setting a value
 * {@code Objects.equals} to the current one does not mark it dirty (R-API-4).
 */
public final class Prop<T> implements ModelField {

    Prop(@Nonnull String name, @Nullable T initial) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    @Nonnull
    public String name() {
        throw new UnsupportedOperationException("F4");
    }

    @Nullable
    public T get() {
        throw new UnsupportedOperationException("F4");
    }

    public void set(@Nullable T value) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public boolean dirty() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void markClean() {
        throw new UnsupportedOperationException("F4");
    }
}

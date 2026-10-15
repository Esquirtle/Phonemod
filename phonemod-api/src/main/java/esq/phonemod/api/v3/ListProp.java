package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

/**
 * A dirty-tracked, key-identified list field of a {@link ViewModel}. Items are
 * identified by a stable key ({@code keyOf}), never by index (R-API-4).
 */
public final class ListProp<K, I> implements ModelField {

    ListProp(@Nonnull String name, @Nonnull Function<? super I, ? extends K> keyOf) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    @Nonnull
    public String name() {
        throw new UnsupportedOperationException("F4");
    }

    /** Unmodifiable current contents, in insertion/declared order. */
    @Nonnull
    public List<I> items() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public java.util.Optional<I> find(@Nonnull K key) {
        throw new UnsupportedOperationException("F4");
    }

    /** Upsert by key; a brand-new key is appended at the end. */
    public void put(@Nonnull I item) {
        throw new UnsupportedOperationException("F4");
    }

    /** @throws IllegalArgumentException if the item's key already exists */
    public void insert(int index, @Nonnull I item) {
        throw new UnsupportedOperationException("F4");
    }

    public void remove(@Nonnull K key) {
        throw new UnsupportedOperationException("F4");
    }

    /** @throws IllegalArgumentException on duplicate keys within {@code items} */
    public void setAll(@Nonnull List<I> items) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public K keyOf(@Nonnull I item) {
        throw new UnsupportedOperationException("F4");
    }

    public int size() {
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

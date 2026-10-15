package esq.phonemod.ui.kit.diff;

import javax.annotation.Nonnull;

/**
 * One neutral mutation produced by {@link DiffEngine}, applied by a
 * {@code RenderBackend} without knowing the concrete {@code ViewModel} type.
 */
public sealed interface DiffOp permits DiffOp.SetProp, DiffOp.InsertItem, DiffOp.RemoveItem, DiffOp.UpdateItem {

    @Nonnull
    String field();

    record SetProp(@Nonnull String field, Object value) implements DiffOp {}

    record InsertItem(@Nonnull String field, int index, @Nonnull Object key, @Nonnull Object item) implements DiffOp {}

    record RemoveItem(@Nonnull String field, @Nonnull Object key) implements DiffOp {}

    record UpdateItem(@Nonnull String field, @Nonnull Object key, @Nonnull Object item) implements DiffOp {}
}

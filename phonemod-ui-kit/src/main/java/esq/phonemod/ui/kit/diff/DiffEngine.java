package esq.phonemod.ui.kit.diff;

import esq.phonemod.api.v3.ViewModel;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Converts a {@link ViewModel}'s dirty fields into a minimal, canonically
 * ordered list of {@link DiffOp} (R-KIT-1).
 *
 * <p>The first diff of a model emits its full state; later diffs emit only
 * changed keys. Within a list field, order is Remove (old order) then Insert
 * (ascending index) then Update; a reorder is expressed as remove + insert.
 * Calls {@link ViewModel#fields()}'s {@code markClean()} on every field at the
 * end of a successful diff.
 */
public final class DiffEngine {

    private DiffEngine() {
    }

    @Nonnull
    public static List<DiffOp> diff(@Nonnull ViewModel model) {
        throw new UnsupportedOperationException("F4");
    }

    /** Forgets whether any model has been diffed before, so the next diff is a full state. */
    public static void reset() {
        throw new UnsupportedOperationException("F4");
    }
}

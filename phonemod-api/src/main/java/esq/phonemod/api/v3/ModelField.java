package esq.phonemod.api.v3;

import javax.annotation.Nonnull;

/** One declared field of a {@link ViewModel}: either a {@link Prop} or a {@link ListProp}. */
public sealed interface ModelField permits Prop, ListProp {

    @Nonnull
    String name();

    boolean dirty();

    void markClean();
}

package esq.phonemod.api.v3;

import javax.annotation.Nonnull;

/**
 * Public v3 app contract (R-API-3). The app never receives a builder or a
 * selector (D-5) — it only ever sees its own {@link ViewModel}.
 */
public interface DeviceApp<M extends ViewModel> {

    @Nonnull
    AppDescriptor descriptor();

    @Nonnull
    M createModel(@Nonnull DeviceAppContext<M> context);

    void onOpen(@Nonnull DeviceAppContext<M> context, @Nonnull M model);

    void onAction(@Nonnull DeviceAppContext<M> context, @Nonnull M model, @Nonnull DeviceAction action);

    default void onClose(@Nonnull DeviceAppContext<M> context, @Nonnull M model) {}
}

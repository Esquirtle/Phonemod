package esq.phonemod.api;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import javax.annotation.Nonnull;

/** Public v2 app contract. */
public interface DeviceApp {
    @Nonnull AppDescriptor descriptor();
    void build(@Nonnull DeviceAppContext context, @Nonnull UICommandBuilder commands,
               @Nonnull UIEventBuilder events);
    default void onOpen(@Nonnull DeviceAppContext context) {}
    default void onClose(@Nonnull DeviceAppContext context) {}
    @Nonnull default DeviceActionResult handle(@Nonnull DeviceAppContext context,
                                                @Nonnull DeviceAction action) {
        return DeviceActionResult.REJECTED;
    }
}

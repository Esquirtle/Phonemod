package esq.phonemod.ui.kit.host;

import javax.annotation.Nonnull;

/** Port for {@code DeviceAppContext.notify}. */
public interface Notifier {
    void notify(@Nonnull DeviceSessionInfo session, @Nonnull String title, @Nonnull String body);
}

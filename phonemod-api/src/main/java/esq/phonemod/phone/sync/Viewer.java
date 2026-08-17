package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;

/**
 * Identifies one subscriber of a {@link SyncChannel}: a specific app view open on a
 * specific phone. The {@code phoneNumber} is the device id used everywhere else in
 * the framework; pairing it with {@code appId} lets a broadcast re-render only the
 * relevant view and skip a player who has navigated elsewhere.
 *
 * <p>Deliberately stores no live page/world handle — the broadcast resolves the
 * current {@code OnlineEntry} from {@code PhoneRegistry} at send time, so a
 * re-opened page is never stale.
 */
public record Viewer(@Nonnull String phoneNumber, @Nonnull String appId) {
}

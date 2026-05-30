package esq.phonemod.device.events;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Typed notification request for the phone/device framework.
 *
 * <p>Build instances using the static factory methods:
 * <pre>{@code
 * PhoneNotification n = PhoneNotification.info(
 *         Message.translation("myapp.msg_received"),
 *         Message.raw(senderName));
 * PhoneNotifications.send(playerRef, n);
 * }</pre>
 */
public final class PhoneNotification {

    private final Message title;
    private final Message body;
    private final NotificationStyle style;
    private final String soundId;
    private final String targetDeviceId;

    private PhoneNotification(@Nonnull Message title,
            @Nullable Message body,
            @Nonnull NotificationStyle style,
            @Nullable String soundId,
            @Nullable String targetDeviceId) {
        this.title = title;
        this.body = body;
        this.style = style;
        this.soundId = soundId;
        this.targetDeviceId = targetDeviceId;
    }

    // ── Factory methods ────────────────────────────────────────────────────────

    /** Creates an info-style notification. */
    @Nonnull
    public static PhoneNotification info(@Nonnull Message title, @Nullable Message body) {
        return new PhoneNotification(title, body, NotificationStyle.Default, null, null);
    }

    /** Creates a success-style notification. */
    @Nonnull
    public static PhoneNotification success(@Nonnull Message title, @Nullable Message body) {
        return new PhoneNotification(title, body, NotificationStyle.Success, null, null);
    }

    /** Creates a warning-style notification. */
    @Nonnull
    public static PhoneNotification warning(@Nonnull Message title, @Nullable Message body) {
        return new PhoneNotification(title, body, NotificationStyle.Warning, null, null);
    }

    /** Creates a danger/error-style notification. */
    @Nonnull
    public static PhoneNotification danger(@Nonnull Message title, @Nullable Message body) {
        return new PhoneNotification(title, body, NotificationStyle.Danger, null, null);
    }

    // ── Fluent builder ─────────────────────────────────────────────────────────

    /** Returns a copy of this notification with the given sound event ID. */
    @Nonnull
    public PhoneNotification withSound(@Nullable String soundId) {
        return new PhoneNotification(title, body, style, soundId, targetDeviceId);
    }

    /** Returns a copy of this notification targeting a specific device ID. */
    @Nonnull
    public PhoneNotification forDevice(@Nullable String deviceId) {
        return new PhoneNotification(title, body, style, soundId, deviceId);
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    @Nonnull
    public Message getTitle() {
        return title;
    }

    @Nullable
    public Message getBody() {
        return body;
    }

    @Nonnull
    public NotificationStyle getStyle() {
        return style;
    }

    @Nullable
    public String getSoundId() {
        return soundId;
    }

    @Nullable
    public String getTargetDeviceId() {
        return targetDeviceId;
    }
}

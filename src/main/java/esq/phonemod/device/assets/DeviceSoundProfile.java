package esq.phonemod.device.assets;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nullable;

/**
 * Optional sound event IDs used by a device shell.
 */
public final class DeviceSoundProfile {

    public static final BuilderCodec<DeviceSoundProfile> CODEC = BuilderCodec
            .builder(DeviceSoundProfile.class, DeviceSoundProfile::new)
            .append(new KeyedCodec<>("Open", Codec.STRING),
                    (profile, value) -> profile.open = value,
                    profile -> profile.open)
            .add()
            .append(new KeyedCodec<>("Close", Codec.STRING),
                    (profile, value) -> profile.close = value,
                    profile -> profile.close)
            .add()
            .append(new KeyedCodec<>("MessageReceived", Codec.STRING),
                    (profile, value) -> profile.messageReceived = value,
                    profile -> profile.messageReceived)
            .add()
            .append(new KeyedCodec<>("MessageSent", Codec.STRING),
                    (profile, value) -> profile.messageSent = value,
                    profile -> profile.messageSent)
            .add()
            .append(new KeyedCodec<>("Notification", Codec.STRING),
                    (profile, value) -> profile.notification = value,
                    profile -> profile.notification)
            .add()
            .append(new KeyedCodec<>("Ringtone", Codec.STRING),
                    (profile, value) -> profile.ringtone = value,
                    profile -> profile.ringtone)
            .add()
            .append(new KeyedCodec<>("Error", Codec.STRING),
                    (profile, value) -> profile.error = value,
                    profile -> profile.error)
            .add()
            .build();

    private String open;
    private String close;
    private String messageReceived;
    private String messageSent;
    private String notification;
    private String ringtone;
    private String error;

    @Nullable
    public String getOpen() {
        return open;
    }

    @Nullable
    public String getClose() {
        return close;
    }

    @Nullable
    public String getMessageReceived() {
        return messageReceived;
    }

    @Nullable
    public String getMessageSent() {
        return messageSent;
    }

    @Nullable
    public String getNotification() {
        return notification;
    }

    @Nullable
    public String getRingtone() {
        return ringtone;
    }

    @Nullable
    public String getError() {
        return error;
    }
}


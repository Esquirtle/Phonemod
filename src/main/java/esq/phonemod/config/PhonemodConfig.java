package esq.phonemod.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import esq.phonemod.security.UiSafetyLimits;
import esq.telemetry.hytale.TelemetrySettings;

import javax.annotation.Nonnull;

/**
 * Server-enforced safety, rate, and retention limits.
 *
 * <p>Every field is codec-validated so an unsafe value prevents configuration
 * loading instead of silently weakening the server-side boundary.</p>
 */
public final class PhonemodConfig implements UiSafetyLimits {

    public static final BuilderCodec<PhonemodConfig> CODEC =
            BuilderCodec.builder(PhonemodConfig.class, PhonemodConfig::new)
                    .append(new KeyedCodec<>("MaxUiEventBytes", Codec.INTEGER),
                            (c, v) -> c.maxUiEventBytes = v, c -> c.maxUiEventBytes)
                    .addValidator(Validators.range(1024, 65536)).add()
                    .append(new KeyedCodec<>("MaxUiParameters", Codec.INTEGER),
                            (c, v) -> c.maxUiParameters = v, c -> c.maxUiParameters)
                    .addValidator(Validators.range(4, 128)).add()
                    .append(new KeyedCodec<>("MaxUiKeyCharacters", Codec.INTEGER),
                            (c, v) -> c.maxUiKeyCharacters = v, c -> c.maxUiKeyCharacters)
                    .addValidator(Validators.range(8, 256)).add()
                    .append(new KeyedCodec<>("MaxUiValueCharacters", Codec.INTEGER),
                            (c, v) -> c.maxUiValueCharacters = v, c -> c.maxUiValueCharacters)
                    .addValidator(Validators.range(64, 8192)).add()
                    .append(new KeyedCodec<>("MaxMessageCharacters", Codec.INTEGER),
                            (c, v) -> c.maxMessageCharacters = v, c -> c.maxMessageCharacters)
                    .addValidator(Validators.range(1, 4096)).add()
                    .append(new KeyedCodec<>("MaxContactNameCharacters", Codec.INTEGER),
                            (c, v) -> c.maxContactNameCharacters = v, c -> c.maxContactNameCharacters)
                    .addValidator(Validators.range(1, 256)).add()
                    .append(new KeyedCodec<>("UiActionsPerSecond", Codec.INTEGER),
                            (c, v) -> c.uiActionsPerSecond = v, c -> c.uiActionsPerSecond)
                    .addValidator(Validators.range(1, 100)).add()
                    .append(new KeyedCodec<>("UiActionBurst", Codec.INTEGER),
                            (c, v) -> c.uiActionBurst = v, c -> c.uiActionBurst)
                    .addValidator(Validators.range(1, 200)).add()
                    .append(new KeyedCodec<>("MessagesPerSecond", Codec.INTEGER),
                            (c, v) -> c.messagesPerSecond = v, c -> c.messagesPerSecond)
                    .addValidator(Validators.range(1, 20)).add()
                    .append(new KeyedCodec<>("MessageBurst", Codec.INTEGER),
                            (c, v) -> c.messageBurst = v, c -> c.messageBurst)
                    .addValidator(Validators.range(1, 100)).add()
                    .append(new KeyedCodec<>("CallAttemptIntervalSeconds", Codec.INTEGER),
                            (c, v) -> c.callAttemptIntervalSeconds = v, c -> c.callAttemptIntervalSeconds)
                    .addValidator(Validators.range(1, 300)).add()
                    .append(new KeyedCodec<>("MessagesPerConversation", Codec.INTEGER),
                            (c, v) -> c.messagesPerConversation = v, c -> c.messagesPerConversation)
                    .addValidator(Validators.range(1, 10000)).add()
                    .append(new KeyedCodec<>("CallRecordsPerDevice", Codec.INTEGER),
                            (c, v) -> c.callRecordsPerDevice = v, c -> c.callRecordsPerDevice)
                    .addValidator(Validators.range(1, 10000)).add()
                    .append(new KeyedCodec<>("PersistentChannelMessages", Codec.INTEGER),
                            (c, v) -> c.persistentChannelMessages = v, c -> c.persistentChannelMessages)
                    .addValidator(Validators.range(1, 50000)).add()
                    .append(new KeyedCodec<>("OfflineMessagesPerAddress", Codec.INTEGER),
                            (c, v) -> c.offlineMessagesPerAddress = v, c -> c.offlineMessagesPerAddress)
                    .addValidator(Validators.range(1, 10000)).add()
                    .append(new KeyedCodec<>("OfflineMessageRetentionDays", Codec.INTEGER),
                            (c, v) -> c.offlineMessageRetentionDays = v, c -> c.offlineMessageRetentionDays)
                    .addValidator(Validators.range(1, 3650)).add()
                    .append(new KeyedCodec<>("AddressBindingMode",
                                    new EnumCodec<>(AddressBindingMode.class)),
                            (c, v) -> c.addressBindingMode = v, c -> c.addressBindingMode)
                    .documentation("Whether addresses belong to durable devices or player identities.")
                    .add()
                    .append(new KeyedCodec<>("CallsEnabled", Codec.BOOLEAN),
                            (c, v) -> c.callsEnabled = v, c -> c.callsEnabled)
                    .documentation("Globally allow new device calls.")
                    .add()
                    .append(new KeyedCodec<>("CallAudioMode", new EnumCodec<>(CallAudioMode.class)),
                            (c, v) -> c.callAudioMode = v, c -> c.callAudioMode)
                    .documentation("Override device call privacy, speaker, or toggle behavior; Asset preserves the device asset.")
                    .add()
                    .append(new KeyedCodec<>("AllowCrossWorldCalls", Codec.BOOLEAN),
                            (c, v) -> c.allowCrossWorldCalls = v, c -> c.allowCrossWorldCalls)
                    .documentation("Allow call endpoints in different worlds.")
                    .add()
                    .append(new KeyedCodec<>("RequireOpenDeviceForIncomingCalls", Codec.BOOLEAN),
                            (c, v) -> c.requireOpenDeviceForIncomingCalls = v,
                            c -> c.requireOpenDeviceForIncomingCalls)
                    .documentation("Require the callee device page to be open instead of merely present.")
                    .add()
                    .append(new KeyedCodec<>("CallRingTimeoutSeconds", Codec.INTEGER),
                            (c, v) -> c.callRingTimeoutSeconds = v, c -> c.callRingTimeoutSeconds)
                    .addValidator(Validators.range(5, 300))
                    .documentation("Seconds before an unanswered call becomes missed.")
                    .add()
                    .append(new KeyedCodec<>("SpeakerRadiusBlocks", Codec.INTEGER),
                            (c, v) -> c.speakerRadiusBlocks = v, c -> c.speakerRadiusBlocks)
                    .addValidator(Validators.range(1, 64))
                    .documentation("Radius around the receiving endpoint for speaker audio.")
                    .add()
                    .append(new KeyedCodec<>("OfflineMessagingEnabled", Codec.BOOLEAN),
                            (c, v) -> c.offlineMessagingEnabled = v, c -> c.offlineMessagingEnabled)
                    .documentation("Queue bounded offline messages for later delivery.")
                    .add()
                    .append(new KeyedCodec<>("Telemetry", TelemetrySettings.CODEC),
                            (c, v) -> c.telemetry = v, c -> c.telemetry)
                    .documentation("esq-telemetry settings (usage counts + sanitised error fingerprints; never player identity).")
                    .add()
                    .build();

    private int maxUiEventBytes = 8 * 1024;
    private int maxUiParameters = 32;
    private int maxUiKeyCharacters = 64;
    private int maxUiValueCharacters = 1024;
    private volatile int maxMessageCharacters = 512;
    private int maxContactNameCharacters = 64;
    private int uiActionsPerSecond = 10;
    private int uiActionBurst = 20;
    private int messagesPerSecond = 2;
    private int messageBurst = 5;
    private volatile int callAttemptIntervalSeconds = 5;
    private int messagesPerConversation = 500;
    private int callRecordsPerDevice = 200;
    private int persistentChannelMessages = 1000;
    private volatile int offlineMessagesPerAddress = 200;
    private volatile int offlineMessageRetentionDays = 30;
    private volatile AddressBindingMode addressBindingMode = AddressBindingMode.DEVICE;
    private volatile boolean callsEnabled = true;
    private volatile CallAudioMode callAudioMode = CallAudioMode.ASSET;
    private volatile boolean allowCrossWorldCalls = true;
    private volatile boolean requireOpenDeviceForIncomingCalls;
    private volatile int callRingTimeoutSeconds = 30;
    private volatile int speakerRadiusBlocks = 8;
    private volatile boolean offlineMessagingEnabled = true;
    private volatile TelemetrySettings telemetry = new TelemetrySettings();

    public int maxUiEventBytes() { return maxUiEventBytes; }
    public int maxUiParameters() { return maxUiParameters; }
    public int maxUiKeyCharacters() { return maxUiKeyCharacters; }
    public int maxUiValueCharacters() { return maxUiValueCharacters; }
    public int maxMessageCharacters() { return maxMessageCharacters; }
    public int maxContactNameCharacters() { return maxContactNameCharacters; }
    public int uiActionsPerSecond() { return uiActionsPerSecond; }
    public int uiActionBurst() { return uiActionBurst; }
    public int messagesPerSecond() { return messagesPerSecond; }
    public int messageBurst() { return messageBurst; }
    public int callAttemptIntervalSeconds() { return callAttemptIntervalSeconds; }
    public int messagesPerConversation() { return messagesPerConversation; }
    public int callRecordsPerDevice() { return callRecordsPerDevice; }
    public int persistentChannelMessages() { return persistentChannelMessages; }
    public int offlineMessagesPerAddress() { return offlineMessagesPerAddress; }
    public int offlineMessageRetentionDays() { return offlineMessageRetentionDays; }
    @Nonnull public AddressBindingMode addressBindingMode() { return addressBindingMode; }
    public boolean callsEnabled() { return callsEnabled; }
    @Nonnull public CallAudioMode callAudioMode() { return callAudioMode; }
    public boolean allowCrossWorldCalls() { return allowCrossWorldCalls; }
    public boolean requireOpenDeviceForIncomingCalls() { return requireOpenDeviceForIncomingCalls; }
    public int callRingTimeoutSeconds() { return callRingTimeoutSeconds; }
    public int speakerRadiusBlocks() { return speakerRadiusBlocks; }
    public boolean offlineMessagingEnabled() { return offlineMessagingEnabled; }
    /** esq-telemetry settings (usage counts + sanitised error fingerprints; never player identity). */
    @Nonnull public TelemetrySettings telemetry() { return telemetry; }

    public void setTelemetry(@Nonnull TelemetrySettings value) {
        telemetry = java.util.Objects.requireNonNull(value, "value");
    }

    public void setAddressBindingMode(@Nonnull AddressBindingMode value) {
        addressBindingMode = java.util.Objects.requireNonNull(value, "value");
    }

    public void setCallsEnabled(boolean value) { callsEnabled = value; }

    public void setCallAudioMode(@Nonnull CallAudioMode value) {
        callAudioMode = java.util.Objects.requireNonNull(value, "value");
    }

    public void setAllowCrossWorldCalls(boolean value) { allowCrossWorldCalls = value; }

    public void setRequireOpenDeviceForIncomingCalls(boolean value) {
        requireOpenDeviceForIncomingCalls = value;
    }

    public void setCallRingTimeoutSeconds(int value) {
        requireRange("CallRingTimeoutSeconds", value, 5, 300);
        callRingTimeoutSeconds = value;
    }

    public void setSpeakerRadiusBlocks(int value) {
        requireRange("SpeakerRadiusBlocks", value, 1, 64);
        speakerRadiusBlocks = value;
    }

    public void setOfflineMessagingEnabled(boolean value) { offlineMessagingEnabled = value; }

    public void setMaxMessageCharacters(int value) {
        requireRange("MaxMessageCharacters", value, 1, 4096);
        maxMessageCharacters = value;
    }

    private static void requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
    }
}

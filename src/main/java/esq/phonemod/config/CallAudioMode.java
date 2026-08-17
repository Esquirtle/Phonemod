package esq.phonemod.config;

import esq.phonemod.device.assets.CallAudioPolicy;

import javax.annotation.Nonnull;

/** Server-wide override for the call policy declared by a device asset. */
public enum CallAudioMode {
    ASSET,
    PRIVATE,
    SPEAKER,
    TOGGLEABLE;

    @Nonnull
    public CallAudioPolicy resolve(@Nonnull CallAudioPolicy assetPolicy) {
        return switch (this) {
            case ASSET -> assetPolicy;
            case PRIVATE -> CallAudioPolicy.PRIVATE;
            case SPEAKER -> CallAudioPolicy.SPEAKER;
            case TOGGLEABLE -> CallAudioPolicy.TOGGLEABLE;
        };
    }
}

package esq.phonemod.phone.messaging;

import javax.annotation.Nonnull;

/**
 * Pure, side-effect-free classification of an inbound voice packet on the Netty thread.
 * Mirrors the gate ladder previously inlined in {@code CallRegistry.VOICE_FILTER}
 * (no-route drop, then voice-disabled / muted / opus-absent / oversize consume, then the
 * private-vs-speaker relay decision). The caller still owns all channel I/O; this helper
 * only decides. Does not touch MUTEX, singletons, or shared state.
 */
public final class VoiceRelayDecision {
    public enum Relay { DROP_NO_ROUTE, CONSUME_NO_RELAY, RELAY_PRIVATE, RELAY_PRIVATE_AND_SPEAKER }

    private VoiceRelayDecision() {}

    @Nonnull
    public static Relay decide(boolean hasRoute, boolean voiceEnabled, boolean speakerMuted,
                               boolean opusPresent, int opusLen, int maxLen,
                               boolean speakerEnabled) {
        if (!hasRoute) return Relay.DROP_NO_ROUTE;
        if (!voiceEnabled || speakerMuted || !opusPresent || opusLen > maxLen) {
            return Relay.CONSUME_NO_RELAY;
        }
        return speakerEnabled ? Relay.RELAY_PRIVATE_AND_SPEAKER : Relay.RELAY_PRIVATE;
    }
}

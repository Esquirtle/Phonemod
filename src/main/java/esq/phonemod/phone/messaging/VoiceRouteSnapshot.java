package esq.phonemod.phone.messaging;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Pure builder for the voice-route topology. Mirrors the loop previously inlined in
 * {@code CallRegistry.rebuildVoiceRoutesLocked}: only ACTIVE sessions produce routes,
 * both endpoints are mapped symmetrically, RINGING/ENDED are excluded, and the speaker
 * flag propagates to both directions. The endpoint-&gt;UUID resolver returns null when an
 * endpoint has no presence, in which case the whole session is skipped (matching the
 * original {@code if (caller == null || callee == null) continue}). The caller keeps
 * wrapping the receiver UUID back into a live PlayerRef.
 */
public final class VoiceRouteSnapshot {
    public record RouteView(@Nonnull UUID receiverUuid, boolean speakerEnabled) {}

    private VoiceRouteSnapshot() {}

    @Nonnull
    public static Map<UUID, RouteView> build(@Nonnull Collection<CallSession> sessions,
                                             @Nonnull Function<String, UUID> resolver) {
        Map<UUID, RouteView> routes = new HashMap<>();
        for (CallSession session : sessions) {
            if (session.state() != CallSession.State.ACTIVE) continue;
            UUID callerUuid = resolver.apply(session.caller());
            UUID calleeUuid = resolver.apply(session.callee());
            if (callerUuid == null || calleeUuid == null) continue;
            boolean speaker = session.speakerEnabled();
            routes.put(callerUuid, new RouteView(calleeUuid, speaker));
            routes.put(calleeUuid, new RouteView(callerUuid, speaker));
        }
        return routes;
    }
}

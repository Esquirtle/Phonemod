package esq.phonemod.phone.messaging;

import esq.phonemod.device.assets.CallAudioPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceRouteSnapshotTest {
    private static final UUID UUID_A = UUID.randomUUID();
    private static final UUID UUID_B = UUID.randomUUID();
    private static final Function<String, UUID> RESOLVER =
            Map.of("a", UUID_A, "b", UUID_B)::get;

    @Test
    void ringingSessionProducesNoRoutes() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        assertTrue(VoiceRouteSnapshot.build(List.of(call), RESOLVER).isEmpty());
    }

    @Test
    void endedSessionProducesNoRoutes() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        call.answer("b");
        call.end(CallSession.EndReason.HANGUP);
        assertTrue(VoiceRouteSnapshot.build(List.of(call), RESOLVER).isEmpty());
    }

    @Test
    void activeSessionMapsBothEndpointsSymmetrically() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        call.answer("b");
        Map<UUID, VoiceRouteSnapshot.RouteView> routes =
                VoiceRouteSnapshot.build(List.of(call), RESOLVER);
        assertEquals(2, routes.size());
        assertEquals(UUID_B, routes.get(UUID_A).receiverUuid());
        assertEquals(UUID_A, routes.get(UUID_B).receiverUuid());
        assertFalse(routes.get(UUID_A).speakerEnabled());
        assertFalse(routes.get(UUID_B).speakerEnabled());
    }

    @Test
    void speakerFlagPropagatesToBothDirections() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.SPEAKER);
        call.answer("b");
        Map<UUID, VoiceRouteSnapshot.RouteView> routes =
                VoiceRouteSnapshot.build(List.of(call), RESOLVER);
        assertTrue(routes.get(UUID_A).speakerEnabled());
        assertTrue(routes.get(UUID_B).speakerEnabled());
    }

    @Test
    void unresolvedEndpointSkipsWholeSession() {
        CallSession call = new CallSession("a", "ghost", "Ghost", 1, CallAudioPolicy.PRIVATE);
        call.answer("ghost");
        assertTrue(VoiceRouteSnapshot.build(List.of(call), RESOLVER).isEmpty());
    }
}

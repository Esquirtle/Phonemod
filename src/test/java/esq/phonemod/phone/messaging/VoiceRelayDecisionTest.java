package esq.phonemod.phone.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceRelayDecisionTest {
    @Test
    void noRouteDrops() {
        assertEquals(VoiceRelayDecision.Relay.DROP_NO_ROUTE,
                VoiceRelayDecision.decide(false, true, false, true, 10, 100, false));
    }

    @Test
    void voiceDisabledConsumesWithoutRelay() {
        assertEquals(VoiceRelayDecision.Relay.CONSUME_NO_RELAY,
                VoiceRelayDecision.decide(true, false, false, true, 10, 100, false));
    }

    @Test
    void mutedSpeakerConsumesWithoutRelay() {
        assertEquals(VoiceRelayDecision.Relay.CONSUME_NO_RELAY,
                VoiceRelayDecision.decide(true, true, true, true, 10, 100, false));
    }

    @Test
    void absentOpusConsumesWithoutRelay() {
        assertEquals(VoiceRelayDecision.Relay.CONSUME_NO_RELAY,
                VoiceRelayDecision.decide(true, true, false, false, 0, 100, false));
    }

    @Test
    void oversizeOpusConsumesWithoutRelay() {
        assertEquals(VoiceRelayDecision.Relay.CONSUME_NO_RELAY,
                VoiceRelayDecision.decide(true, true, false, true, 101, 100, false));
    }

    @Test
    void speakerOffRelaysPrivateOnly() {
        assertEquals(VoiceRelayDecision.Relay.RELAY_PRIVATE,
                VoiceRelayDecision.decide(true, true, false, true, 100, 100, false));
    }

    @Test
    void speakerOnRelaysPrivateAndSpeaker() {
        assertEquals(VoiceRelayDecision.Relay.RELAY_PRIVATE_AND_SPEAKER,
                VoiceRelayDecision.decide(true, true, false, true, 100, 100, true));
    }
}

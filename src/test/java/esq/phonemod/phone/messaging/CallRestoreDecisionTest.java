package esq.phonemod.phone.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CallRestoreDecisionTest {
    @Test
    void activePartnerWinsOverEverything() {
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve("partner", "callee", "caller");
        assertEquals(CallRestoreDecision.Kind.ACTIVE, r.kind());
        assertEquals("partner", r.value());
    }

    @Test
    void outgoingPendingWinsWhenNoActive() {
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve(null, "callee", "caller");
        assertEquals(CallRestoreDecision.Kind.OUTGOING, r.kind());
        assertEquals("callee", r.value());
    }

    @Test
    void incomingPendingWinsWhenNoActiveOrOutgoing() {
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve(null, null, "caller");
        assertEquals(CallRestoreDecision.Kind.INCOMING, r.kind());
        assertEquals("caller", r.value());
    }

    @Test
    void noneWhenAllNull() {
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve(null, null, null);
        assertEquals(CallRestoreDecision.Kind.NONE, r.kind());
        assertNull(r.value());
    }

    @Test
    void reopenIntoActiveCallIgnoresStalePendingInputs() {
        // page closed mid-call then reopened: active present alongside residual pending values
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve("partner", null, "caller");
        assertEquals(CallRestoreDecision.Kind.ACTIVE, r.kind());
        assertEquals("partner", r.value());
    }

    @Test
    void reopenIntoIncomingCallWhenOnlyCallerPending() {
        CallRestoreDecision.Restore r = CallRestoreDecision.resolve(null, null, "caller");
        assertEquals(CallRestoreDecision.Kind.INCOMING, r.kind());
    }
}

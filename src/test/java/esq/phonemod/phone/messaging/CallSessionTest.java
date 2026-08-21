package esq.phonemod.phone.messaging;

import esq.phonemod.device.assets.CallAudioPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CallSessionTest {
    @Test
    void ringingActiveEndedTransitionsAreValidatedAndIdempotent() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        assertFalse(call.answer("a"));
        assertTrue(call.answer("b"));
        assertFalse(call.answer("b"));
        assertFalse(call.setSpeaker("a", true));
        assertTrue(call.end(CallSession.EndReason.HANGUP));
        assertFalse(call.end(CallSession.EndReason.HANGUP));
        assertEquals(CallSession.State.ENDED, call.state());
    }

    @Test
    void toggleableStartsPrivateAndOnlyActiveEndpointsMayToggle() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.TOGGLEABLE);
        assertFalse(call.speakerEnabled());
        assertFalse(call.setSpeaker("a", true));
        assertTrue(call.answer("b"));
        assertFalse(call.setSpeaker("c", true));
        assertTrue(call.setSpeaker("a", true));
        assertTrue(call.speakerEnabled());
    }

    @Test
    void concurrentAnswersProduceOneTransition() throws InterruptedException {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        Thread first = new Thread(() -> answer(call, start, successes));
        Thread second = new Thread(() -> answer(call, start, successes));
        first.start(); second.start(); start.countDown(); first.join(); second.join();
        assertEquals(1, successes.get());
    }

    @Test
    void endRecordsReasonAndClearsSpeaker() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.SPEAKER);
        assertNull(call.endReason());
        assertTrue(call.answer("b"));
        assertTrue(call.speakerEnabled());
        assertTrue(call.end(CallSession.EndReason.DISCONNECT));
        assertEquals(CallSession.EndReason.DISCONNECT, call.endReason());
        assertFalse(call.speakerEnabled());
    }

    @Test
    void endReasonIsPinnedToFirstEnd() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        assertTrue(call.end(CallSession.EndReason.TIMEOUT));
        assertFalse(call.end(CallSession.EndReason.HANGUP));
        assertEquals(CallSession.EndReason.TIMEOUT, call.endReason());
    }

    @Test
    void speakerCannotBeToggledAfterEnd() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.TOGGLEABLE);
        assertTrue(call.answer("b"));
        assertTrue(call.end(CallSession.EndReason.HANGUP));
        assertFalse(call.setSpeaker("a", true));
        assertFalse(call.setSpeaker("b", true));
        assertFalse(call.speakerEnabled());
    }

    @Test
    void partnerOfResolvesBothEndpointsAndRejectsStrangers() {
        CallSession call = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        assertEquals("b", call.partnerOf("a"));
        assertEquals("a", call.partnerOf("b"));
        assertNull(call.partnerOf("c"));
        assertTrue(call.contains("a"));
        assertTrue(call.contains("b"));
        assertFalse(call.contains("c"));
    }

    @Test
    void speakerDefaultFollowsPolicyMatrix() {
        assertTrue(new CallSession("a", "b", "Bee", 1, CallAudioPolicy.SPEAKER).speakerEnabled());
        assertFalse(new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE).speakerEnabled());
        assertFalse(new CallSession("a", "b", "Bee", 1, CallAudioPolicy.TOGGLEABLE).speakerEnabled());
    }

    @Test
    void speakerPolicyGovernsWhoMayToggle() {
        CallSession privateCall = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.PRIVATE);
        assertTrue(privateCall.answer("b"));
        assertFalse(privateCall.setSpeaker("a", true));
        assertFalse(privateCall.setSpeaker("b", true));

        CallSession speakerCall = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.SPEAKER);
        assertTrue(speakerCall.answer("b"));
        assertFalse(speakerCall.setSpeaker("a", false));
        assertTrue(speakerCall.speakerEnabled());

        CallSession toggle = new CallSession("a", "b", "Bee", 1, CallAudioPolicy.TOGGLEABLE);
        assertTrue(toggle.answer("b"));
        assertTrue(toggle.setSpeaker("a", true));
        assertTrue(toggle.setSpeaker("b", false));
        assertFalse(toggle.speakerEnabled());
    }

    private static void answer(CallSession call, CountDownLatch start, AtomicInteger successes) {
        try {
            start.await();
            if (call.answer("b")) successes.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

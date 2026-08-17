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

    private static void answer(CallSession call, CountDownLatch start, AtomicInteger successes) {
        try {
            start.await();
            if (call.answer("b")) successes.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

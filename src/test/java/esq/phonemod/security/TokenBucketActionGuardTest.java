package esq.phonemod.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the TokenBucket primitive behind DeviceSession.tryAcquireAction: a stale/spam
 * action is rejected until the bucket refills. Tests TokenBucket directly because
 * DeviceSession needs live runtime types.
 */
class TokenBucketActionGuardTest {
    @Test
    void firstActionSucceedsImmediateReplaySpamsAndRefillRestores() {
        AtomicLong clock = new AtomicLong();
        // one action per second, burst of one — the DeviceSession action-guard shape
        TokenBucket guard = new TokenBucket(1.0, 1, clock::get);
        assertTrue(guard.tryAcquire());
        assertFalse(guard.tryAcquire());
        clock.addAndGet(1_000_000_000L);
        assertTrue(guard.tryAcquire());
        assertFalse(guard.tryAcquire());
    }
}

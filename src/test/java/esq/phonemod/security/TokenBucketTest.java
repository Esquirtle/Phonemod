package esq.phonemod.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {
    @Test
    void enforcesBurstAndRefillsDeterministically() {
        AtomicLong clock = new AtomicLong();
        TokenBucket bucket = new TokenBucket(2, 3, clock::get);
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
        clock.addAndGet(500_000_000L);
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
    }

    @Test
    void backwardsClockCannotMintTokens() {
        AtomicLong clock = new AtomicLong(100);
        TokenBucket bucket = new TokenBucket(1, 1, clock::get);
        assertTrue(bucket.tryAcquire());
        clock.set(0);
        assertFalse(bucket.tryAcquire());
    }
}

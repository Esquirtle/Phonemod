package esq.phonemod.security;

import java.util.function.LongSupplier;

/** Thread-safe token bucket with a monotonic clock. */
public final class TokenBucket {
    private final double tokensPerNano;
    private final double capacity;
    private final LongSupplier clock;
    private double tokens;
    private long lastRefill;

    public TokenBucket(double tokensPerSecond, int capacity) {
        this(tokensPerSecond, capacity, System::nanoTime);
    }

    TokenBucket(double tokensPerSecond, int capacity, LongSupplier clock) {
        if (tokensPerSecond <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("Rate and capacity must be positive");
        }
        this.tokensPerNano = tokensPerSecond / 1_000_000_000.0;
        this.capacity = capacity;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.getAsLong();
    }

    public synchronized boolean tryAcquire() {
        long now = clock.getAsLong();
        long elapsed = Math.max(0L, now - lastRefill);
        tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
        lastRefill = now;
        if (tokens < 1.0) {
            return false;
        }
        tokens -= 1.0;
        return true;
    }
}

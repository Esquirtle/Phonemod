package esq.phonemod.operations;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.metrics.MetricProvider;
import com.hypixel.hytale.metrics.MetricResults;
import com.hypixel.hytale.metrics.MetricsRegistry;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Runtime counters and gauges exposed through Hytale's metrics contract. */
public final class OperationalMetrics implements MetricProvider {
    private static final OperationalMetrics INSTANCE = new OperationalMetrics();
    private static final MetricsRegistry<Snapshot> REGISTRY = new MetricsRegistry<Snapshot>()
            .register("activeSessions", Snapshot::activeSessions, Codec.LONG)
            .register("activeCalls", Snapshot::activeCalls, Codec.LONG)
            .register("registrations", Snapshot::registrations, Codec.LONG)
            .register("rejectedActions", Snapshot::rejectedActions, Codec.LONG)
            .register("rateLimits", Snapshot::rateLimits, Codec.LONG)
            .register("mailboxDepth", Snapshot::mailboxDepth, Codec.LONG)
            .register("syncQueueDepth", Snapshot::syncQueueDepth, Codec.LONG)
            .register("saveFailures", Snapshot::saveFailures, Codec.LONG)
            .register("migrationConflicts", Snapshot::migrationConflicts, Codec.LONG);

    private final AtomicLong registrations = new AtomicLong();
    private final AtomicLong rejectedActions = new AtomicLong();
    private final AtomicLong rateLimits = new AtomicLong();
    private final AtomicLong saveFailures = new AtomicLong();
    private volatile LongSupplier activeSessions = () -> 0;
    private volatile LongSupplier activeCalls = () -> 0;
    private volatile LongSupplier mailboxDepth = () -> 0;
    private volatile LongSupplier syncQueueDepth = () -> 0;
    private volatile LongSupplier migrationConflicts = () -> 0;

    private OperationalMetrics() {}

    public static OperationalMetrics get() { return INSTANCE; }

    public void bind(LongSupplier activeSessions, LongSupplier activeCalls,
                     LongSupplier mailboxDepth, LongSupplier syncQueueDepth,
                     LongSupplier migrationConflicts) {
        this.activeSessions = activeSessions;
        this.activeCalls = activeCalls;
        this.mailboxDepth = mailboxDepth;
        this.syncQueueDepth = syncQueueDepth;
        this.migrationConflicts = migrationConflicts;
    }

    public void registration() { registrations.incrementAndGet(); }
    public void rejectedAction() { rejectedActions.incrementAndGet(); }
    public void rateLimited() { rateLimits.incrementAndGet(); }
    public void saveFailure() { saveFailures.incrementAndGet(); }

    public Snapshot snapshot() {
        return new Snapshot(activeSessions.getAsLong(), activeCalls.getAsLong(), registrations.get(),
                rejectedActions.get(), rateLimits.get(), mailboxDepth.getAsLong(),
                syncQueueDepth.getAsLong(), saveFailures.get(), migrationConflicts.getAsLong());
    }

    @Override public MetricResults toMetricResults() { return REGISTRY.toMetricResults(snapshot()); }

    public record Snapshot(long activeSessions, long activeCalls, long registrations,
                           long rejectedActions, long rateLimits, long mailboxDepth,
                           long syncQueueDepth, long saveFailures, long migrationConflicts) {}
}

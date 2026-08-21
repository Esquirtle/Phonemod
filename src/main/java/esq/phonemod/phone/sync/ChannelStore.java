package esq.phonemod.phone.sync;

import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.StorageManager;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JSON persistence for sync channels, under {@code <dataDir>/channels/<typeId>/}.
 *
 * <p>Reuses the engine's own config serialization primitives: encode state to a
 * {@link BsonDocument} via the type's codec and write it with
 * {@link BsonUtil#writeDocument} (which renders JSON and keeps a {@code .bak});
 * read back with {@link RawJsonReader#readSyncWithBak}. Filenames are the SHA-256
 * of the channel id (filesystem-safe); the id itself lives inside the document, so
 * channels can be rehydrated by scanning a type's directory.
 */
final class ChannelStore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Total wall-clock budget for the shutdown flush of all persistent channels.
     * Bounds {@link #saveBlocking} so a held Universe saving lock (whose queued
     * {@code doSave} only completes on release) can never hang shutdown indefinitely.
     */
    static final Duration SHUTDOWN_FLUSH_TIMEOUT = Duration.ofSeconds(5);

    /** Write seam: how a {@link BsonDocument} is rendered to a path (real impl {@link BsonUtil#writeDocument}). */
    @FunctionalInterface
    interface BsonWriter {
        @Nonnull
        CompletableFuture<Void> write(@Nonnull Path path, @Nonnull BsonDocument document);
    }

    private final Path baseDir;
    private final StorageManager storage;
    private final BsonWriter writer;

    ChannelStore(@Nonnull Path dataDir) {
        this(dataDir, Universe.get().getStorageManager(), BsonUtil::writeDocument);
    }

    ChannelStore(@Nonnull Path dataDir, @Nonnull StorageManager storage, @Nonnull BsonWriter writer) {
        this.baseDir = dataDir.resolve("channels");
        this.storage = storage;
        this.writer = writer;
    }

    @Nonnull
    private <S> Path pathFor(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        return baseDir.resolve(type.typeId()).resolve(Hashing.sha256Hex(id) + ".json");
    }

    /**
     * Encodes and writes the channel's state if it has changed since the last save.
     * Returns the new content hash to record, or {@code null} when unchanged/failed.
     */
    @Nullable
    <S> CompletableFuture<SaveResult> save(@Nonnull SyncChannel<S> channel) {
        SyncChannelType<S> type = channel.type();
        try {
            SyncChannel.Snapshot snapshot = channel.snapshot();
            if (snapshot.hash().equals(channel.savedHash())) {
                return null;
            }
            Path path = pathFor(type, channel.id());
            return storage
                    .doSave(path, () -> writer.write(path, snapshot.document()))
                    .thenApply(ignored -> new SaveResult(snapshot.sequence(), snapshot.hash()));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log(
                    "[ChannelStore] Failed to save channel %s/%s", type.typeId(), channel.id());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Synchronously flushes a channel's state (used on shutdown), bounding the wait by
     * {@link #SHUTDOWN_FLUSH_TIMEOUT}. Delegates to {@link #saveBlocking(SyncChannel, long)}
     * with a fresh per-channel deadline; callers flushing many channels should share one
     * deadline via the overload so the total shutdown delay stays bounded.
     */
    <S> void saveBlocking(@Nonnull SyncChannel<S> channel) {
        saveBlocking(channel, System.nanoTime() + SHUTDOWN_FLUSH_TIMEOUT.toNanos());
    }

    /**
     * Synchronously flushes a channel's state, waiting at most until {@code deadlineNanos}
     * (a {@link System#nanoTime()} stamp). If the write does not complete within the
     * remaining budget — e.g. the Universe saving lock is held at shutdown, leaving the
     * queued {@code doSave} to complete only on release — this logs a warning and returns
     * WITHOUT calling {@link SyncChannel#markSaved}. The channel's {@code savedHash} stays
     * un-advanced, so the state remains dirty and is retried passively on the next save;
     * the queued write still lands later when the lock releases.
     */
    <S> void saveBlocking(@Nonnull SyncChannel<S> channel, long deadlineNanos) {
        CompletableFuture<SaveResult> future;
        try {
            future = save(channel);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log(
                    "[ChannelStore] Failed to flush channel %s/%s",
                    channel.type().typeId(), channel.id());
            return;
        }
        if (future == null) {
            return;
        }
        long remaining = Math.max(0L, deadlineNanos - System.nanoTime());
        try {
            SaveResult result = future.get(remaining, TimeUnit.NANOSECONDS);
            channel.markSaved(result.sequence(), result.hash());
        } catch (TimeoutException e) {
            LOGGER.atWarning().log(
                    "[ChannelStore] Shutdown flush timed out for channel %s/%s after %ds;"
                            + " leaving it dirty for passive retry (queued write completes on lock release)",
                    channel.type().typeId(), channel.id(), SHUTDOWN_FLUSH_TIMEOUT.toSeconds());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log(
                    "[ChannelStore] Failed to flush channel %s/%s",
                    channel.type().typeId(), channel.id());
        }
    }

    /** Loads a single channel's state by id, or {@code null} if no file exists. */
    @Nullable
    <S> S loadOne(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        Path path = pathFor(type, id);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return RawJsonReader.readSyncWithBak(path, type.codec(), LOGGER);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to read %s", path);
            return null;
        }
    }

    /** Loads all persisted states for a type by scanning its directory. */
    @Nonnull
    <S> List<S> loadAll(@Nonnull SyncChannelType<S> type) {
        Path dir = baseDir.resolve(type.typeId());
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<S> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                try {
                    result.add(RawJsonReader.readSyncWithBak(p, type.codec(), LOGGER));
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to read %s", p);
                }
            });
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to list %s", dir);
        }
        return result;
    }

    /** Deletes a single channel's JSON (and its {@code .bak}). */
    <S> void delete(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        Path path = pathFor(type, id);
        deleteQuietly(path);
        deleteQuietly(path.resolveSibling(path.getFileName() + ".bak"));
    }

    /** Removes every JSON file for a type — used to clear stale ephemeral channels on startup. */
    <S> void deleteAllFor(@Nonnull SyncChannelType<S> type) {
        Path dir = baseDir.resolve(type.typeId());
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(this::deleteQuietly);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to clear %s", dir);
        }
    }

    private void deleteQuietly(@Nonnull Path path) {
        try {
            storage.doSave(path, () -> {
                try {
                    Files.deleteIfExists(path);
                    return CompletableFuture.completedFuture(null);
                } catch (IOException error) {
                    return CompletableFuture.failedFuture(error);
                }
            }).join();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to delete %s", path);
        }
    }

    record SaveResult(long sequence, @Nonnull String hash) {}
}

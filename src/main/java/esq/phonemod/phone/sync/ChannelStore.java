package esq.phonemod.phone.sync;

import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    private final Path baseDir;

    ChannelStore(@Nonnull Path dataDir) {
        this.baseDir = dataDir.resolve("channels");
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
    <S> String save(@Nonnull SyncChannel<S> channel) {
        SyncChannelType<S> type = channel.type();
        try {
            BsonDocument doc = type.codec().encode(channel.read()).asDocument();
            String hash = Hashing.sha256Hex(doc.toJson());
            if (hash.equals(channel.savedHash())) {
                return null;
            }
            BsonUtil.writeDocument(pathFor(type, channel.id()), doc);
            return hash;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log(
                    "[ChannelStore] Failed to save channel %s/%s", type.typeId(), channel.id());
            return null;
        }
    }

    /** Synchronously flushes a channel's state (used on shutdown). */
    <S> void saveBlocking(@Nonnull SyncChannel<S> channel) {
        SyncChannelType<S> type = channel.type();
        try {
            BsonDocument doc = type.codec().encode(channel.read()).asDocument();
            BsonUtil.writeDocument(pathFor(type, channel.id()), doc).join();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log(
                    "[ChannelStore] Failed to flush channel %s/%s", type.typeId(), channel.id());
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
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[ChannelStore] Failed to delete %s", path);
        }
    }
}

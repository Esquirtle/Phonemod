package esq.phonemod.phone.messaging;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.StorageManager;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.BsonUtil;
import esq.phonemod.PhoneMod;
import esq.phonemod.operations.OperationalMetrics;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded, lazy-loaded, per-address persistent offline mailboxes. */
final class OfflineMailboxStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Write seam mirroring {@code BsonUtil::writeDocument}; injectable for tests. */
    @FunctionalInterface
    interface BsonWriter {
        @Nonnull
        CompletableFuture<Void> write(@Nonnull Path path, @Nonnull BsonDocument document);
    }

    private final Map<String, Mailbox> mailboxes = new ConcurrentHashMap<>();
    private Path baseDir;
    private StorageManager storage;
    private BsonWriter writer;

    void init() {
        init(Universe.get().getPath().resolve("Phonemod").resolve("mailboxes"),
                Universe.get().getStorageManager(), BsonUtil::writeDocument);
    }

    void init(@Nonnull Path baseDir, @Nonnull StorageManager storage, @Nonnull BsonWriter writer) {
        this.baseDir = baseDir;
        this.storage = storage;
        this.writer = writer;
    }

    void enqueue(@Nonnull String address, @Nonnull TextMessage message, long queuedAtMillis) {
        Mailbox box = box(address);
        synchronized (box) {
            prune(box.messages);
            box.messages.add(new OfflineMessage(message.fromNumber(), message.fromName(),
                    message.body(), queuedAtMillis));
            int excess = box.messages.size() - PhoneMod.config().offlineMessagesPerAddress();
            if (excess > 0) box.messages.subList(0, excess).clear();
            saveLocked(address, box);
        }
    }

    @Nonnull List<OfflineMessage> drain(@Nonnull String address) {
        Mailbox box = box(address);
        synchronized (box) {
            prune(box.messages);
            List<OfflineMessage> result = List.copyOf(box.messages);
            if (!box.messages.isEmpty()) {
                box.messages.clear();
                saveLocked(address, box);
            }
            return result;
        }
    }

    @Nonnull List<TextMessage> peek(@Nonnull String address) {
        Mailbox box = box(address);
        synchronized (box) {
            prune(box.messages);
            return box.messages.stream().map(OfflineMessage::message).toList();
        }
    }

    int depth() {
        int total = 0;
        for (Mailbox box : mailboxes.values()) {
            synchronized (box) { prune(box.messages); total += box.messages.size(); }
        }
        return total;
    }

    void flush() {
        for (Mailbox box : mailboxes.values()) box.pending.join();
    }

    private Mailbox box(String address) {
        if (baseDir == null) throw new IllegalStateException("Offline mailbox store not initialized");
        return mailboxes.computeIfAbsent(address, this::load);
    }

    private Mailbox load(String address) {
        Mailbox box = new Mailbox();
        Path path = pathFor(address);
        if (!Files.exists(path)) return box;
        try {
            MailboxDocument document = RawJsonReader.readSyncWithBak(path, MailboxDocument.CODEC, LOGGER);
            if (address.equals(document.address)) {
                box.messages.addAll(List.of(document.messages));
                prune(box.messages);
            } else {
                LOGGER.atWarning().log("[Mailbox] Address mismatch in %s", path);
            }
        } catch (Exception error) {
            LOGGER.atWarning().withCause(error).log("[Mailbox] Failed to load %s", path);
        }
        return box;
    }

    private void saveLocked(String address, Mailbox box) {
        MailboxDocument document = new MailboxDocument();
        document.address = address;
        document.messages = box.messages.toArray(OfflineMessage[]::new);
        var encoded = MailboxDocument.CODEC.encode(document).asDocument();
        Path path = pathFor(address);
        box.pending = box.pending.handle((ignored, previousError) -> null)
                .thenCompose(ignored -> storage
                        .doSave(path, () -> writer.write(path, encoded)));
        box.pending.whenComplete((ignored, error) -> {
            if (error != null) {
                OperationalMetrics.get().saveFailure();
                LOGGER.atWarning().withCause(error).log("[Mailbox] Save failed address=%s", address);
            }
        });
    }

    private Path pathFor(String address) { return baseDir.resolve(sha256(address) + ".json"); }

    private static void prune(List<OfflineMessage> messages) {
        long cutoff = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS
                .toMillis(PhoneMod.config().offlineMessageRetentionDays());
        messages.removeIf(message -> message.queuedAtMillis < cutoff);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    private static final class Mailbox {
        final List<OfflineMessage> messages = new ArrayList<>();
        CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);
    }

    static final class OfflineMessage {
        static final BuilderCodec<OfflineMessage> CODEC = BuilderCodec
                .builder(OfflineMessage.class, OfflineMessage::new)
                .append(new KeyedCodec<>("FromAddress", Codec.STRING),
                        (o, v) -> o.fromAddress = v, o -> o.fromAddress).add()
                .append(new KeyedCodec<>("FromName", Codec.STRING),
                        (o, v) -> o.fromName = v, o -> o.fromName).add()
                .append(new KeyedCodec<>("Body", Codec.STRING),
                        (o, v) -> o.body = v, o -> o.body).add()
                .append(new KeyedCodec<>("QueuedAt", Codec.LONG),
                        (o, v) -> o.queuedAtMillis = v, o -> o.queuedAtMillis).add()
                .build();
        String fromAddress = "";
        String fromName = "";
        String body = "";
        long queuedAtMillis;
        OfflineMessage() {}
        OfflineMessage(String fromAddress, String fromName, String body, long queuedAtMillis) {
            this.fromAddress = fromAddress; this.fromName = fromName;
            this.body = body; this.queuedAtMillis = queuedAtMillis;
        }
        TextMessage message() { return new TextMessage(fromAddress, fromName, body); }
        long queuedAtMillis() { return queuedAtMillis; }
    }

    static final class MailboxDocument {
        static final ArrayCodec<OfflineMessage> MESSAGES =
                new ArrayCodec<>(OfflineMessage.CODEC, OfflineMessage[]::new);
        static final BuilderCodec<MailboxDocument> CODEC = BuilderCodec
                .builder(MailboxDocument.class, MailboxDocument::new)
                .append(new KeyedCodec<>("SchemaVersion", Codec.INTEGER),
                        (o, v) -> o.schemaVersion = v, o -> o.schemaVersion).add()
                .append(new KeyedCodec<>("Address", Codec.STRING),
                        (o, v) -> o.address = v, o -> o.address).add()
                .append(new KeyedCodec<>("Messages", MESSAGES),
                        (o, v) -> o.messages = v, o -> o.messages).add()
                .build();
        int schemaVersion = 2;
        String address = "";
        OfflineMessage[] messages = new OfflineMessage[0];
    }
}

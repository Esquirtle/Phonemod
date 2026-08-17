package esq.phonemod.identity;

import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.BsonUtil;
import esq.phonemod.api.DeviceIdentity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import esq.phonemod.operations.OperationalMetrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Authoritative universe-scoped device identity registry and derived address index. */
public final class IdentityRegistry {
    private static HytaleLogger logger() { return HytaleLogger.forEnclosingClass(); }
    private final Object lock = new Object();
    private final Map<UUID, IdentityRecord> byId = new HashMap<>();
    private final Map<String, UUID> byAddress = new HashMap<>();
    private Path path;
    private CompletableFuture<Void> pendingSave = CompletableFuture.completedFuture(null);
    private boolean backupCreated;
    private int conflicts;
    private final boolean persistenceEnabled;

    public IdentityRegistry() { this(true); }
    IdentityRegistry(boolean persistenceEnabled) { this.persistenceEnabled = persistenceEnabled; }

    public void load() {
        path = Universe.get().getPath().resolve("Phonemod").resolve("device-identities.json");
        if (!Files.exists(path)) return;
        try {
            IdentityDocument document = RawJsonReader.readSyncWithBak(path, IdentityDocument.CODEC, logger());
            synchronized (lock) {
                byId.clear(); byAddress.clear();
                for (IdentityRecord record : document.records) {
                    if (byId.putIfAbsent(record.id(), record) != null) {
                        conflicts++;
                        continue;
                    }
                    if (record.address() != null
                            && byAddress.putIfAbsent(record.address(), record.id()) != null) conflicts++;
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not load device identity registry", error);
        }
    }

    @Nonnull
    public Claim claimLegacy(@Nullable UUID embeddedId, @Nullable String legacyAddress,
                             @Nonnull UUID owner) {
        synchronized (lock) {
            if (embeddedId != null) {
                IdentityRecord existing = byId.get(embeddedId);
                if (existing != null && (existing.shared() || owner.equals(existing.owner()))) {
                    return new Claim(toApi(existing), false, false, legacyAddress);
                }
                if (existing != null) conflicts++;
            }
            String requested = normalizeAddress(legacyAddress);
            if (requested != null) {
                UUID occupiedId = byAddress.get(requested);
                if (occupiedId != null) {
                    IdentityRecord occupied = byId.get(occupiedId);
                    if (owner.equals(occupied.owner()) && embeddedId == null) {
                        return new Claim(toApi(occupied), false, false, requested);
                    }
                    conflicts++;
                    String replacement = nextAddress();
                    IdentityRecord created = create(replacement, owner, false);
                    saveLocked();
                    return new Claim(toApi(created), true, true, requested);
                }
            }
            IdentityRecord created = create(requested != null ? requested : nextAddress(), owner, false);
            saveLocked();
            return new Claim(toApi(created), true, false, requested);
        }
    }

    /**
     * Resolves one primary personal identity for a player. This mode intentionally ignores
     * an item's embedded identity: every phone the player opens is linked to the same oldest
     * personal identity, creating one when necessary. Existing device identities remain in
     * the registry so switching back to device-bound mode is non-destructive.
     */
    @Nonnull
    public Claim claimPlayer(@Nonnull UUID owner) {
        synchronized (lock) {
            IdentityRecord primary = byId.values().stream()
                    .filter(record -> !record.shared() && owner.equals(record.owner()))
                    .sorted(java.util.Comparator.comparingLong(IdentityRecord::createdAt)
                            .thenComparing(IdentityRecord::id))
                    .findFirst()
                    .orElse(null);
            if (primary == null) {
                IdentityRecord created = create(nextAddress(), owner, false);
                saveLocked();
                return new Claim(toApi(created), true, false, null);
            }
            if (primary.address() == null) {
                String address = nextAddress();
                primary = new IdentityRecord(primary.id(), address, primary.owner(), false,
                        primary.createdAt());
                byId.put(primary.id(), primary);
                byAddress.put(address, primary.id());
                saveLocked();
            }
            return new Claim(toApi(primary), false, false, null);
        }
    }

    @Nonnull
    public DeviceIdentity createShared(@Nullable String address) {
        synchronized (lock) {
            String normalized = normalizeAddress(address);
            if (normalized != null && byAddress.containsKey(normalized))
                throw new IllegalArgumentException("Address is already assigned");
            IdentityRecord record = create(normalized, null, true);
            saveLocked();
            return toApi(record);
        }
    }

    @Nonnull
    public DeviceIdentity ensureShared(@Nonnull String stableKey) {
        UUID id = UUID.nameUUIDFromBytes(("phonemod:shared:" + stableKey)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        synchronized (lock) {
            IdentityRecord existing = byId.get(id);
            if (existing != null) {
                if (!existing.shared()) throw new IllegalStateException("Stable key belongs to a personal device");
                return toApi(existing);
            }
            IdentityRecord record = new IdentityRecord(id, null, null, true, System.currentTimeMillis());
            byId.put(id, record);
            saveLocked();
            return toApi(record);
        }
    }

    public boolean transfer(@Nonnull UUID identityId, @Nonnull UUID currentOwner,
                            @Nonnull UUID newOwner) {
        synchronized (lock) {
            IdentityRecord current = byId.get(identityId);
            if (current == null || current.shared() || !currentOwner.equals(current.owner())) return false;
            byId.put(identityId, new IdentityRecord(current.id(), current.address(), newOwner,
                    false, current.createdAt()));
            saveLocked();
            return true;
        }
    }

    public boolean forceTransfer(@Nonnull UUID identityId, @Nonnull UUID newOwner) {
        synchronized (lock) {
            IdentityRecord current = byId.get(identityId);
            if (current == null || current.shared()) return false;
            byId.put(identityId, new IdentityRecord(current.id(), current.address(), newOwner,
                    false, current.createdAt()));
            saveLocked();
            return true;
        }
    }

    public boolean forceReleaseAddress(@Nonnull UUID identityId) {
        synchronized (lock) {
            IdentityRecord current = byId.get(identityId);
            if (current == null) return false;
            if (current.address() != null) byAddress.remove(current.address(), identityId);
            byId.put(identityId, new IdentityRecord(current.id(), null, current.owner(),
                    current.shared(), current.createdAt()));
            saveLocked();
            return true;
        }
    }

    public int repairAddressConflicts() {
        synchronized (lock) {
            byAddress.clear();
            int repaired = 0;
            List<IdentityRecord> ordered = byId.values().stream()
                    .sorted(java.util.Comparator.comparingLong(IdentityRecord::createdAt)
                            .thenComparing(IdentityRecord::id)).toList();
            for (IdentityRecord record : ordered) {
                if (record.address() == null) continue;
                if (byAddress.putIfAbsent(record.address(), record.id()) != null) {
                    String newAddress = nextAddress();
                    byId.put(record.id(), new IdentityRecord(record.id(), newAddress,
                            record.owner(), record.shared(), record.createdAt()));
                    byAddress.put(newAddress, record.id());
                    repaired++;
                }
            }
            if (repaired > 0) saveLocked();
            conflicts = Math.max(0, conflicts - repaired);
            return repaired;
        }
    }

    public boolean releaseAddress(@Nonnull UUID identityId, @Nonnull UUID owner) {
        synchronized (lock) {
            IdentityRecord current = byId.get(identityId);
            if (current == null || (!current.shared() && !owner.equals(current.owner()))) return false;
            if (current.address() != null) byAddress.remove(current.address(), identityId);
            byId.put(identityId, new IdentityRecord(current.id(), null, current.owner(),
                    current.shared(), current.createdAt()));
            saveLocked();
            return true;
        }
    }

    @Nonnull public Optional<DeviceIdentity> find(UUID id) {
        synchronized (lock) { return Optional.ofNullable(byId.get(id)).map(IdentityRegistry::toApi); }
    }
    @Nonnull public Optional<DeviceIdentity> findAddress(String address) {
        synchronized (lock) {
            UUID id = byAddress.get(address);
            return id == null ? Optional.empty() : Optional.of(toApi(byId.get(id)));
        }
    }
    @Nonnull public Collection<DeviceIdentity> snapshot() {
        synchronized (lock) { return byId.values().stream().map(IdentityRegistry::toApi).toList(); }
    }
    public int conflictCount() { synchronized (lock) { return conflicts; } }
    public int pendingSaves() { return pendingSave.isDone() ? 0 : 1; }
    public void flush() { pendingSave.join(); }

    private IdentityRecord create(String address, UUID owner, boolean shared) {
        IdentityRecord record = new IdentityRecord(UUID.randomUUID(), address, owner, shared,
                System.currentTimeMillis());
        byId.put(record.id(), record);
        if (address != null) byAddress.put(address, record.id());
        return record;
    }

    private String nextAddress() {
        String candidate;
        do {
            int value = java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000);
            candidate = "%03d-%04d".formatted(value / 10_000, value % 10_000);
        } while (byAddress.containsKey(candidate));
        return candidate;
    }

    private void saveLocked() {
        if (!persistenceEnabled) return;
        ensureBackupLocked();
        IdentityDocument document = new IdentityDocument();
        document.records = byId.values().stream().map(IdentityRecord::copy)
                .toArray(IdentityRecord[]::new);
        var encoded = IdentityDocument.CODEC.encode(document).asDocument();
        Path target = path;
        pendingSave = Universe.get().getStorageManager().doSave(
                target, () -> BsonUtil.writeDocument(target, encoded));
        pendingSave.whenComplete((ignored, error) -> {
            if (error != null) {
                OperationalMetrics.get().saveFailure();
                logger().atSevere().withCause(error).log("Identity registry save failed");
            }
        });
    }

    private void ensureBackupLocked() {
        if (backupCreated || path == null || !Files.exists(path)) {
            backupCreated = true;
            return;
        }
        try {
            String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
            Files.copy(path, path.resolveSibling(path.getFileName() + ".pre-v2-" + stamp + ".bak"),
                    StandardCopyOption.COPY_ATTRIBUTES);
            backupCreated = true;
        } catch (Exception error) {
            throw new IllegalStateException("Could not create pre-v2 identity backup", error);
        }
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String normalized = address.trim();
        if (normalized.length() > 64) throw new IllegalArgumentException("Invalid address");
        return normalized;
    }
    private static DeviceIdentity toApi(IdentityRecord record) {
        return new DeviceIdentity(record.id(), record.address(), record.shared());
    }

    public record Claim(@Nonnull DeviceIdentity identity, boolean created,
                        boolean conflict, @Nullable String legacyAddress) {}
}

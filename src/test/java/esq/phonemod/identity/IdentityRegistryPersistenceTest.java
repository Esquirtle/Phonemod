package esq.phonemod.identity;

import com.hypixel.hytale.server.core.universe.StorageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * On-disk persistence/versioning/migration behavior for {@link IdentityRegistry}: the
 * spec in {@code PROFESSIONALIZATION_PLAN.md} under "Persistence, versioning, and
 * migration" requires a timestamped backup before the first v2 mutation and byte-equivalent,
 * no-op double runs. Uses the package-private {@code IdentityRegistry(Path, StorageManager)}
 * test constructor to drive real disk I/O without a running {@link
 * com.hypixel.hytale.server.core.universe.Universe}.
 */
final class IdentityRegistryPersistenceTest {

    private static StorageManager unlockedStorageManager() {
        return new StorageManager(() -> false);
    }

    @Test
    void backupIsCreatedOnlyBeforeTheFirstMutationOfAPreexistingFile(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("device-identities.json");

        // First-ever session: the file does not exist yet, so no backup is expected — there
        // is nothing to protect.
        IdentityRegistry first = new IdentityRegistry(path, unlockedStorageManager());
        first.claimLegacy(null, "555-1111", UUID.randomUUID());
        first.flush();
        assertTrue(Files.exists(path), "identity document should be written after the first claim");
        assertTrue(backupFiles(dir).isEmpty(), "no backup should exist before any prior data was present");
        byte[] beforeSecondSessionMutation = Files.readAllBytes(path);

        // A new session loads the now-preexisting file. Its first v2 mutation must back up
        // the pre-mutation content exactly once.
        IdentityRegistry second = new IdentityRegistry(path, unlockedStorageManager());
        second.claimLegacy(null, "555-2222", UUID.randomUUID());
        second.flush();

        List<Path> backups = backupFiles(dir);
        assertEquals(1, backups.size(), "exactly one timestamped backup should be created");
        assertTrue(backups.get(0).getFileName().toString().contains(".pre-v2-"),
                "backup file name should carry the pre-v2 timestamp marker");
        assertArrayEquals(beforeSecondSessionMutation, Files.readAllBytes(backups.get(0)),
                "backup must capture the exact pre-mutation bytes");
    }

    @Test
    void repeatedIdenticalClaimIsIdempotentByteEquivalentAndCreatesNoAdditionalBackupOrMutation(
            @TempDir Path dir) throws Exception {
        Path path = dir.resolve("device-identities.json");
        UUID owner = UUID.randomUUID();

        IdentityRegistry seed = new IdentityRegistry(path, unlockedStorageManager());
        seed.claimLegacy(null, "555-3333", owner);
        seed.flush();

        IdentityRegistry registry = new IdentityRegistry(path, unlockedStorageManager());
        var firstRun = registry.claimLegacy(null, "555-4444", owner);
        registry.flush();

        List<Path> backupsAfterFirstMutation = backupFiles(dir);
        assertEquals(1, backupsAfterFirstMutation.size());
        byte[] bytesAfterFirstRun = Files.readAllBytes(path);

        // Running the exact same migration input twice must be a no-op: identical claim,
        // identical canonical bytes, no second backup or write.
        var secondRun = registry.claimLegacy(null, "555-4444", owner);
        registry.flush();

        assertEquals(firstRun.identity(), secondRun.identity());
        assertFalse(secondRun.created(), "the second run must not create a new identity");
        assertArrayEquals(bytesAfterFirstRun, Files.readAllBytes(path),
                "canonical data must be byte-equivalent after a no-op re-run");
        assertEquals(1, backupFiles(dir).size(), "a no-op re-run must not create a second backup");
    }

    @Test
    void concurrentDuplicateLegacyClaimsResolveExactlyOneConflictWithoutOverwrite(
            @TempDir Path dir) throws Exception {
        Path path = dir.resolve("device-identities.json");
        IdentityRegistry registry = new IdentityRegistry(path, unlockedStorageManager());

        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<IdentityRegistry.Claim> claimA = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return registry.claimLegacy(null, "555-5555", ownerA);
            });
            Future<IdentityRegistry.Claim> claimB = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return registry.claimLegacy(null, "555-5555", ownerB);
            });

            IdentityRegistry.Claim resultA = claimA.get(5, TimeUnit.SECONDS);
            IdentityRegistry.Claim resultB = claimB.get(5, TimeUnit.SECONDS);
            registry.flush();

            List<IdentityRegistry.Claim> results = List.of(resultA, resultB);
            long winners = results.stream().filter(claim -> !claim.conflict()).count();
            long losers = results.stream().filter(IdentityRegistry.Claim::conflict).count();
            assertEquals(1, winners, "exactly one claimant keeps the legacy address");
            assertEquals(1, losers, "exactly one claimant is diverted to a new address");

            IdentityRegistry.Claim winner = results.stream().filter(claim -> !claim.conflict()).findFirst().orElseThrow();
            IdentityRegistry.Claim loser = results.stream().filter(IdentityRegistry.Claim::conflict).findFirst().orElseThrow();
            assertEquals("555-5555", winner.identity().address());
            assertNotEquals("555-5555", loser.identity().address());
            assertNotNull(loser.identity().address());
            assertNotEquals(winner.identity().id(), loser.identity().id());

            assertEquals(1, registry.conflictCount());
            assertEquals(2, registry.snapshot().size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void firstV2UseConvertsLegacyPhoneNumberMetadataToAUuidIdentityThatSupersedesTheAddress(
            @TempDir Path dir) throws Exception {
        Path path = dir.resolve("device-identities.json");
        IdentityRegistry registry = new IdentityRegistry(path, unlockedStorageManager());
        UUID owner = UUID.randomUUID();

        // First open of a legacy phone item: only the human-readable PhoneNumber is known.
        var claimed = registry.claimLegacy(null, "555-0900", owner);
        registry.flush();
        assertEquals("555-0900", claimed.identity().address(),
                "the legitimate claimant keeps the legacy PhoneNumber as its address");
        assertNotNull(claimed.identity().id(), "a UUID DeviceIdentity must be minted on first v2 use");

        // Once the item is re-stamped with the embedded UUID (as OpenPhone does after the
        // first claim), later opens resolve purely from the UUID — the address string is no
        // longer the source of truth, proving the conversion took hold.
        var reopened = registry.claimLegacy(claimed.identity().id(), null, owner);
        assertEquals(claimed.identity(), reopened.identity());
        assertFalse(reopened.created());
        assertFalse(reopened.conflict());
    }

    /**
     * Only the explicit pre-v2 backups {@link IdentityRegistry} creates before its first
     * mutation of a session — not the routine {@code .bak} rotation file that {@code
     * BsonUtil.writeDocument}/{@code FileUtil.writeStringAtomic} keeps beside every save.
     */
    private static List<Path> backupFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().contains(".pre-v2-"))
                    .collect(Collectors.toList());
        }
    }
}

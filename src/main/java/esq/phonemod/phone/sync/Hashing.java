package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 hex helper used to derive filesystem-safe channel filenames from channel
 * ids and to content-hash serialized state for dirty detection.
 */
final class Hashing {

    private Hashing() {
    }

    @Nonnull
    static String sha256Hex(@Nonnull String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; treat absence as fatal.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

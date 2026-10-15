package esq.phonemod.api.v3;

/**
 * App visibility in the shell's app list.
 *
 * <p>Mirrors the v2 string values {@code "locked"}/{@code "hidden"}
 * ({@code PhoneApp.java:146-154}).
 */
public enum AppVisibility {
    LOCKED,
    HIDDEN
}

package esq.phonemod.phone.sync;

/**
 * Lifecycle/durability policy of a {@link SyncChannelType}.
 *
 * <ul>
 *   <li>{@link #PERSISTENT} — survives restarts; rehydrated from JSON on startup
 *       (e.g. a chat group).</li>
 *   <li>{@link #EPHEMERAL} — temporal; its JSON is written while alive but removed
 *       on dispose, and any leftover files are cleaned on startup (e.g. a game
 *       lobby).</li>
 * </ul>
 *
 * <p>This is engine-side configuration, not data sourced from a JSON asset, so an
 * enum is appropriate here.
 */
public enum ChannelPersistence {
    PERSISTENT,
    EPHEMERAL
}

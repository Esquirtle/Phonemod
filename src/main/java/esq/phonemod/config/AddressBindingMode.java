package esq.phonemod.config;

/** Determines whether a human-facing address follows a durable device or its player. */
public enum AddressBindingMode {
    /** Every durable device identity owns its own address. */
    DEVICE,
    /** Every personal device opened by one player resolves to that player's primary address. */
    PLAYER
}

package esq.phonemod.phone.messaging;

import javax.annotation.Nonnull;

/**
 * Immutable data object representing a single received text message.
 *
 * @param fromNumber the phone number of the sender (e.g. {@code "555-1234"})
 * @param fromName   the display name of the sender (e.g. {@code "Server"})
 * @param body       the message text
 */
public record TextMessage(
        @Nonnull String fromNumber,
        @Nonnull String fromName,
        @Nonnull String body
) {}

package esq.phonemod.ui.kit.action;

import esq.phonemod.api.v3.DeviceSessionId;

import javax.annotation.Nonnull;
import java.util.UUID;

/** One untrusted custom-UI event, before any guard has run. */
public record InboundEvent(@Nonnull DeviceSessionId session, @Nonnull UUID actor, @Nonnull String rawPayload) {

    public InboundEvent {
        throw new UnsupportedOperationException("F4");
    }
}

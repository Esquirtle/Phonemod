package esq.phonemod.ui.kit.action;

import esq.phonemod.api.v3.DeviceAction;

import javax.annotation.Nonnull;

/** Result of running an {@link InboundEvent} through {@link ActionPipeline}. */
public sealed interface ActionOutcome permits ActionOutcome.Delivered, ActionOutcome.Rejected {

    record Delivered(@Nonnull DeviceAction action) implements ActionOutcome {
        public Delivered {
            throw new UnsupportedOperationException("F4");
        }
    }

    record Rejected(@Nonnull String reason) implements ActionOutcome {
        public Rejected {
            throw new UnsupportedOperationException("F4");
        }
    }
}

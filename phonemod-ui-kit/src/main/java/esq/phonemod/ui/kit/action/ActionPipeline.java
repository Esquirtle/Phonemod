package esq.phonemod.ui.kit.action;

import javax.annotation.Nonnull;

/**
 * Runs an {@link InboundEvent} through the guards in the fixed order from
 * {@code DevicePage:176-203} (R-KIT-3): session &rarr; rate &rarr; validateRaw
 * &rarr; decode &rarr; validateFields &rarr; stale-app &rarr; deliver.
 */
public final class ActionPipeline {

    public static final String ENVELOPE_ACTION = "Action";
    public static final String ENVELOPE_APP = "App";

    public ActionPipeline(@Nonnull ActionGuards guards) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public ActionOutcome dispatch(@Nonnull InboundEvent event) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public ActionOutcome authorizeOpen(@Nonnull InboundEvent event, @Nonnull String appId) {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public ActionOutcome openIfAuthorized(@Nonnull InboundEvent event, @Nonnull String appId) {
        throw new UnsupportedOperationException("F4");
    }
}

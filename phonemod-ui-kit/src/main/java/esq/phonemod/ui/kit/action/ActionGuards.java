package esq.phonemod.ui.kit.action;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

/**
 * Reuses the existing security guards unchanged (D-20, R-KIT-3): one method per
 * real guard. ui-kit has no dependency on {@code esq.phonemod.security}, so a
 * concrete implementation (delegating to the real statics) is supplied by the
 * caller — in F2 that is {@code ActionPipelineContractTest}, in the runtime test
 * tree where {@code esq.phonemod.security} is visible.
 */
public interface ActionGuards {

    /** {@code DevicePage.isAuthorizedSession:512-518} / {@code DeviceSession.isOwnedBy:70}. */
    @Nonnull
    GuardVerdict sessionAuthorized(@Nonnull InboundEvent event);

    /** {@code DeviceSession.tryAcquireAction:74} over {@code TokenBucket.tryAcquire:28}. */
    @Nonnull
    GuardVerdict tryAcquire(@Nonnull InboundEvent event);

    /** {@code UiEventValidator.validateRaw:22}. */
    @Nonnull
    GuardVerdict validateRaw(@Nonnull String rawPayload);

    /** {@code UiEventDecoder.decode:21}. Empty means malformed. */
    @Nonnull
    Optional<Map<String, String>> decode(@Nonnull String rawPayload);

    /** {@code UiEventValidator.validateFields:30}. */
    @Nonnull
    GuardVerdict validateFields(@Nonnull Map<String, String> fields);

    /**
     * {@code AppOpenAuthorizer.authorize:43} ({@code Decision} at {@code :23}).
     * ui-kit has no visibility into {@code esq.phonemod.security}, so the
     * {@code Decision} enum is flattened here to a {@link GuardVerdict}: allowed
     * iff the decision is {@code ALLOW}; otherwise {@code reason} is the
     * decision's lower-snake-case name (e.g. {@code "missing_permission"}).
     */
    @Nonnull
    GuardVerdict authorizeOpen(@Nonnull InboundEvent event, @Nonnull String appId);
}

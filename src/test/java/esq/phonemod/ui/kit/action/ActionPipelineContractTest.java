package esq.phonemod.ui.kit.action;

import esq.phonemod.api.v3.DeviceSessionId;
import esq.phonemod.security.AppOpenAuthorizer;
import esq.phonemod.security.TokenBucket;
import esq.phonemod.security.UiEventDecoder;
import esq.phonemod.security.UiEventValidator;
import esq.phonemod.security.UiSafetyLimits;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link ActionPipeline} (R-KIT-3), in the runtime test
 * tree because that is where {@code esq.phonemod.security} is visible (plan
 * sec.1/6). {@link RealActionGuards} delegates to the real, unmodified
 * security statics (D-20) — it is what F4's pipeline will be wired to.
 *
 * <p>Every test asserts the real target outcome and fails on the F2 skeleton's
 * uncaught {@code UnsupportedOperationException("F4")} — the first hit is
 * always {@code new DeviceSessionId(...)} or {@code new ActionPipeline(...)}.
 */
@Tag("contract")
class ActionPipelineContractTest {

    /** Delegates to the real security statics, unchanged (D-20). */
    private static final class RealActionGuards implements ActionGuards {

        private static final UiSafetyLimits LIMITS = new UiSafetyLimits() {
            @Override public int maxUiEventBytes() { return 8192; }
            @Override public int maxUiParameters() { return 32; }
            @Override public int maxUiKeyCharacters() { return 64; }
            @Override public int maxUiValueCharacters() { return 1024; }
            @Override public int maxMessageCharacters() { return 512; }
            @Override public int maxContactNameCharacters() { return 64; }
        };

        private final TokenBucket bucket;
        private final Predicate<String> hasPermission;
        private final boolean sessionOwnedByActor;

        RealActionGuards(TokenBucket bucket, Predicate<String> hasPermission, boolean sessionOwnedByActor) {
            this.bucket = bucket;
            this.hasPermission = hasPermission;
            this.sessionOwnedByActor = sessionOwnedByActor;
        }

        @Override
        @Nonnull
        public GuardVerdict sessionAuthorized(@Nonnull InboundEvent event) {
            return sessionOwnedByActor ? GuardVerdict.ALLOWED : GuardVerdict.reject("session_not_authorized");
        }

        @Override
        @Nonnull
        public GuardVerdict tryAcquire(@Nonnull InboundEvent event) {
            return bucket.tryAcquire() ? GuardVerdict.ALLOWED : GuardVerdict.reject("rate_limited");
        }

        @Override
        @Nonnull
        public GuardVerdict validateRaw(@Nonnull String rawPayload) {
            UiEventValidator.ValidationResult r = UiEventValidator.validateRaw(rawPayload, LIMITS);
            return r.allowed() ? GuardVerdict.ALLOWED : GuardVerdict.reject(r.reason());
        }

        @Override
        @Nonnull
        public Optional<Map<String, String>> decode(@Nonnull String rawPayload) {
            try {
                return Optional.of(UiEventDecoder.decode(rawPayload));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        @Override
        @Nonnull
        public GuardVerdict validateFields(@Nonnull Map<String, String> fields) {
            UiEventValidator.ValidationResult r = UiEventValidator.validateFields(fields, LIMITS);
            return r.allowed() ? GuardVerdict.ALLOWED : GuardVerdict.reject(r.reason());
        }

        @Override
        @Nonnull
        public GuardVerdict authorizeOpen(@Nonnull InboundEvent event, @Nonnull String appId) {
            AppOpenAuthorizer.Decision decision = AppOpenAuthorizer.authorize(
                    true, Set.of(), "phone", sessionOwnedByActor, true, Set.of("some.permission"), hasPermission);
            if (decision == AppOpenAuthorizer.Decision.ALLOW) return GuardVerdict.ALLOWED;
            return GuardVerdict.reject(decision.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static InboundEvent event(String rawPayload) {
        // F4: DeviceSessionId can't even be constructed yet; this is where the
        // uncaught UnsupportedOperationException("F4") fails every test below.
        return new InboundEvent(new DeviceSessionId(UUID.randomUUID()), UUID.randomUUID(), rawPayload);
    }

    @Test
    void forgedActionNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, true));
        ActionOutcome outcome = pipeline.dispatch(event("{\"Action\":\"not a valid id!\"}"));
        assertInstanceOf(ActionOutcome.Rejected.class, outcome);
        assertEquals("invalid_action", ((ActionOutcome.Rejected) outcome).reason());
    }

    @Test
    void oversizedPayloadNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, true));
        String huge = "{\"Action\":\"ok.id\",\"Pad\":\"" + "x".repeat(9000) + "\"}";
        ActionOutcome outcome = pipeline.dispatch(event(huge));
        assertEquals(new ActionOutcome.Rejected("payload_too_large"), outcome);
    }

    @Test
    void foreignSessionNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, false));
        ActionOutcome outcome = pipeline.dispatch(event("{\"Action\":\"ok.id\"}"));
        assertEquals(new ActionOutcome.Rejected("session_not_authorized"), outcome);
    }

    @Test
    void rateLimitedActionNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(
                new RealActionGuards(new TokenBucket(0.001, 1), n -> true, true));
        pipeline.dispatch(event("{\"Action\":\"ok.id\"}"));
        ActionOutcome outcome = pipeline.dispatch(event("{\"Action\":\"ok.id\"}"));
        assertEquals(new ActionOutcome.Rejected("rate_limited"), outcome);
    }

    @Test
    void staleAppActionNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, true));
        ActionOutcome outcome = pipeline.dispatch(event("{\"Action\":\"ok.id\",\"App\":\"other:app\"}"));
        assertEquals(new ActionOutcome.Rejected("stale_app_state"), outcome);
    }

    @Test
    void malformedPayloadNeverReachesApp() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, true));
        ActionOutcome outcome = pipeline.dispatch(event("not json"));
        assertEquals(new ActionOutcome.Rejected("malformed_event"), outcome);
    }

    @Test
    void guardOrderMatchesDevicePage() {
        // Foreign session AND an oversized payload: session_not_authorized must
        // win, because it is checked first (DevicePage:176-203).
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, false));
        String huge = "{\"Action\":\"ok.id\",\"Pad\":\"" + "x".repeat(9000) + "\"}";
        ActionOutcome outcome = pipeline.dispatch(event(huge));
        assertEquals(new ActionOutcome.Rejected("session_not_authorized"), outcome);
    }

    @Test
    void validEventDeliveredOnceWithEnvelopeStripped() {
        ActionPipeline pipeline = new ActionPipeline(new RealActionGuards(new TokenBucket(10, 20), n -> true, true));
        ActionOutcome outcome = pipeline.dispatch(event("{\"Action\":\"ok.id\",\"App\":\"esq:app\",\"Value\":\"1\"}"));
        ActionOutcome.Delivered delivered = assertInstanceOf(ActionOutcome.Delivered.class, outcome);
        assertEquals("ok.id", delivered.action().command());
        assertFalse(delivered.action().params().containsKey(ActionPipeline.ENVELOPE_ACTION));
        assertFalse(delivered.action().params().containsKey(ActionPipeline.ENVELOPE_APP));
    }

    @Test
    void unauthorizedOpenNeverCallsCreateModel() {
        ActionPipeline pipeline = new ActionPipeline(
                new RealActionGuards(new TokenBucket(10, 20), n -> false, true));
        ActionOutcome outcome = pipeline.authorizeOpen(event("{\"Action\":\"open_app\"}"), "esq:app");
        assertEquals(new ActionOutcome.Rejected("missing_permission"), outcome);
    }
}

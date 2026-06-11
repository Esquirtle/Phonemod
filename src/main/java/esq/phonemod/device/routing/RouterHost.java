package esq.phonemod.device.routing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Privileged operations the router and its handlers need from the page that owns
 * the device session. Implemented by {@code DevicePage}.
 *
 * <p>The page deliberately remains the owner of every side-effecting primitive
 * (opening apps, building per-app contexts, flushing UI). Handlers stay pure
 * orchestration: they ask the host to perform an operation and return a
 * {@link RouteResult}; the router then asks the host to flush the result. This
 * keeps the engine's {@code protected sendUpdate(...)} encapsulated in the page
 * while letting routing live in its own, independently reasoned-about package.
 */
public interface RouterHost {

    /**
     * Opens the app with {@code appId}, mutating session state and emitting the
     * open/close transition events, exactly as the legacy {@code DevicePage}
     * flow did. Returns the opened app, or {@code null} when {@code appId} is
     * blank/unknown (UI builders left untouched in that case).
     */
    @Nullable
    PhoneApp<?> openApp(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String appId,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb);

    /** Builds a per-app context scoped to this session, or {@code null} if it cannot. */
    @Nullable
    PhoneAppContext createContext(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String appId);

    /** Translates a raw UI event map into the generic {@link PhoneEvent} apps consume. */
    @Nonnull
    PhoneEvent toPhoneEvent(@Nonnull Map<String, String> data);

    /** Resets the session to the home screen and builds the app grid into {@code cmd}/{@code evb}. */
    void loadHomeState(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb);

    /**
     * Flushes a UI update to the client. Passing {@code null} for both builders
     * mirrors the legacy {@code sendUpdate(null, false)} acknowledgement.
     */
    void sendUpdate(@Nullable UICommandBuilder cmd, @Nullable UIEventBuilder evb);
}

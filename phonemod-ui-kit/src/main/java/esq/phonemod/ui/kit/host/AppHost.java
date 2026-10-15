package esq.phonemod.ui.kit.host;

import esq.phonemod.api.v3.DeviceApp;
import esq.phonemod.api.v3.DeviceAppContext;
import esq.phonemod.api.v3.DeviceAction;
import esq.phonemod.api.v3.DeviceSessionId;
import esq.phonemod.api.v3.ViewModel;

import javax.annotation.Nonnull;

/**
 * Owns one open {@link DeviceApp} instance for one session: builds its model,
 * delivers actions, and coalesces refreshes onto the world thread (R-KIT-5).
 */
public final class AppHost<M extends ViewModel> {

    public AppHost(@Nonnull DeviceApp<M> app, @Nonnull HostServices services,
            @Nonnull DeviceSessionInfo session) {
        throw new UnsupportedOperationException("F4");
    }

    public void open() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public DeviceAppContext<M> context() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public M model() {
        throw new UnsupportedOperationException("F4");
    }

    /** World-thread only: delivers {@code action} to the app, then flushes the diff. */
    public void deliver(@Nonnull DeviceAction action) {
        throw new UnsupportedOperationException("F4");
    }

    /** Any thread: coalesces into a single {@code world.execute(flush)}. */
    public void refresh() {
        throw new UnsupportedOperationException("F4");
    }

    public void close() {
        throw new UnsupportedOperationException("F4");
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public DeviceSessionId sessionId() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public String appId() {
        throw new UnsupportedOperationException("F4");
    }
}

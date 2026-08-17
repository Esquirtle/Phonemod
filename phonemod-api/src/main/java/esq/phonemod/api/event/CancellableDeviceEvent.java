package esq.phonemod.api.event;

import com.hypixel.hytale.event.ICancellable;

abstract class CancellableDeviceEvent implements ICancellable {
    private boolean cancelled;
    public final boolean isCancelled() { return cancelled; }
    public final void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}

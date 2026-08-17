package esq.phonemod.api.event;

import com.hypixel.hytale.event.IEvent;
import esq.phonemod.api.DeviceIdentity;
import javax.annotation.Nonnull;
import java.util.UUID;

public abstract class DeviceOpenEvent implements IEvent<Void> {
    private final UUID playerId; private final DeviceIdentity device;
    protected DeviceOpenEvent(UUID playerId, DeviceIdentity device) { this.playerId = playerId; this.device = device; }
    public UUID playerId() { return playerId; } public DeviceIdentity device() { return device; }
    public static final class Pre extends CancellableDeviceEvent implements IEvent<Void> {
        private final UUID playerId; private final DeviceIdentity device;
        public Pre(@Nonnull UUID playerId, @Nonnull DeviceIdentity device) { this.playerId = playerId; this.device = device; }
        public UUID playerId() { return playerId; } public DeviceIdentity device() { return device; }
    }
    public static final class Post extends DeviceOpenEvent { public Post(UUID p, DeviceIdentity d) { super(p, d); } }
}

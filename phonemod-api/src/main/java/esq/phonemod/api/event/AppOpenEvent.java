package esq.phonemod.api.event;

import com.hypixel.hytale.event.IEvent;
import javax.annotation.Nonnull;
import java.util.UUID;

public abstract class AppOpenEvent implements IEvent<Void> {
    private final UUID playerId; private final String address; private final String appId;
    protected AppOpenEvent(UUID p,String a,String app){playerId=p;address=a;appId=app;}
    public UUID playerId(){return playerId;} public String address(){return address;} public String appId(){return appId;}
    public static final class Pre extends CancellableDeviceEvent implements IEvent<Void> {
        private final UUID playerId; private final String address; private final String appId;
        public Pre(@Nonnull UUID p,@Nonnull String a,@Nonnull String app){playerId=p;address=a;appId=app;}
        public UUID playerId(){return playerId;} public String address(){return address;} public String appId(){return appId;}
    }
    public static final class Post extends AppOpenEvent { public Post(UUID p,String a,String app){super(p,a,app);} }
}

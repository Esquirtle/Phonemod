package esq.phonemod.api.event;

import com.hypixel.hytale.event.IEvent;
import javax.annotation.Nonnull;
import java.util.UUID;

public abstract class AppInstallationEvent implements IEvent<Void> {
    private final UUID playerId; private final String address; private final String appId; private final boolean installing;
    protected AppInstallationEvent(UUID p, String a, String app, boolean installing) { playerId=p; address=a; appId=app; this.installing=installing; }
    public UUID playerId(){return playerId;} public String address(){return address;} public String appId(){return appId;} public boolean installing(){return installing;}
    public static final class Pre extends CancellableDeviceEvent implements IEvent<Void> {
        private final AppInstallationEvent data;
        public Pre(@Nonnull UUID p,@Nonnull String a,@Nonnull String app,boolean installing){data=new Post(p,a,app,installing);}
        public UUID playerId(){return data.playerId();} public String address(){return data.address();} public String appId(){return data.appId();} public boolean installing(){return data.installing();}
    }
    public static final class Post extends AppInstallationEvent { public Post(UUID p,String a,String app,boolean i){super(p,a,app,i);} }
}

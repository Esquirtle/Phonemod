package esq.phonemod.api.event;

import com.hypixel.hytale.event.IEvent;
import javax.annotation.Nonnull;

public abstract class MessageEvent implements IEvent<Void> {
    private final String from; private final String to; private final int bodyLength;
    protected MessageEvent(String f,String t,int length){from=f;to=t;bodyLength=length;}
    public String from(){return from;} public String to(){return to;} public int bodyLength(){return bodyLength;}
    public static final class Pre extends CancellableDeviceEvent implements IEvent<Void> {
        private final String from; private final String to; private final int bodyLength;
        public Pre(@Nonnull String f,@Nonnull String t,int l){from=f;to=t;bodyLength=l;}
        public String from(){return from;} public String to(){return to;} public int bodyLength(){return bodyLength;}
    }
    public static final class Post extends MessageEvent { public Post(String f,String t,int l){super(f,t,l);} }
}

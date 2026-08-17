package esq.phonemod.api.event;

import com.hypixel.hytale.event.IEvent;
import javax.annotation.Nonnull;

public abstract class CallTransitionEvent implements IEvent<Void> {
    public enum Transition { RINGING, ANSWERED, ENDED, MISSED }
    private final String caller; private final String callee; private final Transition transition;
    protected CallTransitionEvent(String a,String b,Transition t){caller=a;callee=b;transition=t;}
    public String caller(){return caller;} public String callee(){return callee;} public Transition transition(){return transition;}
    public static final class Pre extends CancellableDeviceEvent implements IEvent<Void> {
        private final String caller; private final String callee; private final Transition transition;
        public Pre(@Nonnull String a,@Nonnull String b,@Nonnull Transition t){caller=a;callee=b;transition=t;}
        public String caller(){return caller;} public String callee(){return callee;} public Transition transition(){return transition;}
    }
    public static final class Post extends CallTransitionEvent { public Post(String a,String b,Transition t){super(a,b,t);} }
}

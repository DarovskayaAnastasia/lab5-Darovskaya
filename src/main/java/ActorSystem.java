import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class ActorSystem extends AbstractActor {

    private Map<String, Long> keeper = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Request.class, (req) ->)
    }

}

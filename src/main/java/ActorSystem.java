import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class ActorSystem extends AbstractActor {

    private Map<String, Long> keeper = new HashMap<>();

    public ActorSystem() {}

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Request.class, (req) -> {
                    Long result = keeper.getOrDefault(req.getUrl(), 0L);
                    sender().tell(new Result(req.getUrl(), result), self());
                })
                .match(Result.class, (res) -> {
                    keeper.put(res.getUrl(), res.getAverageResTime());
                })
                .build();
    }

}

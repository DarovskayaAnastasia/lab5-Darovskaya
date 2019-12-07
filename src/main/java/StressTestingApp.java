
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.model.*;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.Supervision;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class StressTestingApp {

    public static void main(String[] args) throws IOException {
        System.out.println("start!");

        // exceptions
        final Function<Throwable, Supervision.Directive> decider = exc -> {
            if (exc instanceof ArithmeticException)
                return Supervision.resume();
            else
                return Supervision.stop();
        };

        ActorSystem system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Server server = new Server(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = server.getFlow(materializer);

        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost("localhost", 8080),
                materializer
        );

        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }
}

class Server {
    private AsyncHttpClient httpClient = Dsl.asyncHttpClient();
    private ActorRef storeActor;

    Server(ActorSystem system) {
        storeActor = system.actorOf(Props.create(ActorSystem.class));
    }

    Flow<HttpRequest, HttpResponse, NotUsed> getFlow(ActorMaterializer materializer) {
        return Flow
                .of(HttpRequest.class)
                .map((req) -> {
                    Query reqQuery = req.getUri().query();
                    String url = reqQuery.getOrElse("url", "");
                    int idx = Integer.parseInt(reqQuery.getOrElse("idx", "-1"));

                    return new Request(url, idx);
                })
                .mapAsync(6, (req) -> Patterns.ask(storeActor, req, Duration.ofMillis(3000))
                        .thenCompose((res) -> {
                            Result resultKeeper = (Result) res;

                            return resultKeeper.getAverageResTime() == -1 ? pingExecute(req, materializer) : CompletableFuture.completedFuture((resultKeeper));
                        }))
                .map((result) -> {
                    storeActor.tell(result, ActorRef.noSender());

                    return HttpResponse
                            .create()
                            .withStatus(StatusCodes.OK)
                            .withEntity(
                                    HttpEntities.create(
                                            result.getUrl() + " " + result.getAverageResTime()
                                    )
                            );
                });
    }

    private CompletionStage<Result> pingExecute(Request request, ActorMaterializer materializer) {
        return Source
                .from(Collections.singletonList(request))
                .toMat(pingSink(), Keep.right())
                .run(materializer)
                .thenApply((time) ->
                        new Result(request.getUrl(), time / request.getIndex() / 1_000_000L)
        );
    }

    private Sink<Request, CompletionStage<Long>> pingSink() {
        return Flow.<Request>create()
                .mapConcat((request) -> Collections.nCopies(request.getIndex(), request.getUrl()))
                .mapAsync(6, (url) -> {
                    long beginTime = System.nanoTime();

                    return httpClient
                            .prepareGet(url)
                            .execute()
                            .toCompletableFuture()
                            .thenApply((response) -> System.nanoTime() - beginTime);
                })
                .toMat(Sink.fold(0L, Long::sum), Keep.right());
    }
}
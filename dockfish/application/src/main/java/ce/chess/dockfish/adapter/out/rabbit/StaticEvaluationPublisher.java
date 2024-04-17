package ce.chess.dockfish.adapter.out.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluation;

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

@ApplicationScoped
@Log4j2
public class StaticEvaluationPublisher {

  @Inject
  @Channel(RabbitConfiguration.CHANNEL_STATIC_EVALUATION)
  Emitter<StaticEvaluation> emitter;

  public void publishFinalEvaluation(@Observes StaticEvaluation staticEvaluation) {
    log.info(" [x] Publishing '{}'", JsonObject.mapFrom(staticEvaluation));
    emitter.send(
        Message.of(staticEvaluation)
            .withMetadata(Metadata.of(
                OutgoingRabbitMQMetadata.builder()
                    .withDeliveryMode(2)
                    .build()
            ))
            .withAck(() -> {
              log.info("[x] acked");
              return CompletableFuture.completedFuture(null);
            })
            .withNack(reason -> {
              log.warn(" [x] nacked with reason", reason);
              return CompletableFuture.completedFuture(null);
            }));
  }

}

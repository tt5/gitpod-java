package ce.chess.dockfish.adapter.out.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.adapter.common.mapper.EvaluationMessageDtoMapper;
import ce.chess.dockfish.adapter.out.rabbit.fallback.PublishFailed;
import ce.chess.dockfish.domain.event.SubmitEvaluationMessage;

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
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
public class FinalResultPublisher {

  private static final String EXCHANGE_NAME = "evaluation.created";

  @Inject
  EvaluationMessageDtoMapper evaluationMessageDtoMapper;

  @Inject
  Event<PublishFailed> publishFailedEvent;

  @Inject
  @Channel(RabbitConfiguration.CHANNEL_EVALUATION)
  Emitter<EvaluationMessageDto> emitter;

  public void publishFinalEvaluation(@Observes SubmitEvaluationMessage event) {
    EvaluationMessageDto evaluationMessage = evaluationMessageDtoMapper.toDto(event.getEvaluationMessage());
    log.info(" [x] Publishing '{}'", JsonObject.mapFrom(evaluationMessage));
    try {
      emitter.send(
          Message.of(evaluationMessage)
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
                publishFailedEvent.fire(
                    new PublishFailed(
                        EXCHANGE_NAME,
                        JsonObject.mapFrom(evaluationMessage).toString()));
                return CompletableFuture.completedFuture(null);
              }));
    } catch (IllegalStateException ies) {
      log.warn(" sending message failed with", ies);
      publishFailedEvent.fire(
          new PublishFailed(
              EXCHANGE_NAME,
              JsonObject.mapFrom(evaluationMessage).toString()));
    }
  }
}

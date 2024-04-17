package ce.chess.dockfish.adapter.in.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;
import ce.chess.dockfish.usecase.in.ReceiveStaticEvaluationRequest;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
@Log4j2
public class StaticEvaluationRequestConsumer extends AbstractMessageConsumer {

  @Inject
  ReceiveStaticEvaluationRequest service;

  @Incoming(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST)
  @Blocking
  public CompletionStage<Void> consume(Message<JsonObject> message) {
    return super.consumeMessage(message);
  }

  @Override
  protected void handleContent(JsonObject jsonObject) {
    StaticEvaluationRequest request = jsonObject.mapTo(StaticEvaluationRequest.class);
    service.createAndPublishEvaluation(request);
  }
}

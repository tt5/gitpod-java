package ce.chess.dockfish.adapter.in.rabbit;

import ce.chess.dockfish.domain.model.RequeueException;

import io.smallrye.reactive.messaging.rabbitmq.RabbitMQRejectMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

@Log4j2
public abstract class AbstractMessageConsumer {
  @Inject
  IncomingRabbitMessageConverter incomingRabbitMessageConverter;

  protected CompletionStage<Void> consumeMessage(Message<JsonObject> incomingMessage) {
    IncomingRabbitMessageWrapper rabbitMessageWrapper;
    try {
      rabbitMessageWrapper = incomingRabbitMessageConverter.convert(incomingMessage);
    } catch (IllegalArgumentException ex) {
      log.error("Received invalid Rabbit Message: {}", incomingMessage.toString(), ex);
      return incomingMessage.nack(ex);
    }
    log.info("Consumer for {}: Received message [{}] with with headers [{}] and payload={}",
        rabbitMessageWrapper.getTopic(),
        rabbitMessageWrapper.correlationId(),
        rabbitMessageWrapper.headers(),
        rabbitMessageWrapper.jsonObject());

    try {
      handleContent(rabbitMessageWrapper.jsonObject());
      return incomingMessage.ack();
    } catch (RequeueException exception) { // NOPMD
      if (rabbitMessageWrapper.isRedeliver()) {
        log.error("Consumer {}: Nacking redelivered message {}",
            rabbitMessageWrapper.getTopic(), rabbitMessageWrapper.correlationId(),
            exception);
        return incomingMessage.nack(exception);
      } else {
        log.warn("Consumer {}: Requeue message {}",
            rabbitMessageWrapper.getTopic(), rabbitMessageWrapper.correlationId(),
            exception);
        return incomingMessage.nack(exception, Metadata.of(new RabbitMQRejectMetadata(true)));
      }
    } catch (RuntimeException exception) { // NOPMD
      log.error("Consumer {}: Nacking message {}",
          rabbitMessageWrapper.getTopic(), rabbitMessageWrapper.correlationId(),
          exception);
      return incomingMessage.nack(exception);
    }
  }

  protected abstract void handleContent(JsonObject jsonObject);
}


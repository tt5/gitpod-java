package ce.chess.dockfish.adapter.in.rabbit;

import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.reactive.messaging.Message;

@Log4j2
@ApplicationScoped
public class IncomingRabbitMessageConverter {
  public IncomingRabbitMessageWrapper convert(Message<JsonObject> incomingMessage) {
    IncomingRabbitMQMetadata metadata = incomingMessage.getMetadata(IncomingRabbitMQMetadata.class)
        .orElseThrow(() -> new IllegalArgumentException(
            "That was not a rabbit message: " + incomingMessage.getMetadata().getClass()));
    return new IncomingRabbitMessageWrapper(metadata.getExchange(),
        metadata.getRoutingKey(),
        metadata.getCorrelationId().orElse(""),
        metadata.getHeaders(),
        incomingMessage.getPayload(),
        metadata.isRedeliver());
  }
}

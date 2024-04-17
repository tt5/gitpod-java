package ce.chess.dockfish.adapter.in.rabbit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;

import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncomingRabbitMessageConverterTest {
  @Mock(answer = CALLS_REAL_METHODS)
  Message<JsonObject> message;

  @Mock(answer = CALLS_REAL_METHODS)
  IncomingRabbitMQMetadata metadata;

  private IncomingRabbitMessageConverter cut;

  @BeforeEach
  void setUp() {
    cut = new IncomingRabbitMessageConverter();
  }

  @Test
  void doesConvert() {
    doReturn(Optional.of(metadata)).when(message).getMetadata(IncomingRabbitMQMetadata.class);
    doReturn("exchange").when(metadata).getExchange();
    doReturn("routingKey").when(metadata).getRoutingKey();
    doReturn(JsonObject.of("attribute", "value")).when(message).getPayload();
    doReturn(Map.of()).when(metadata).getHeaders();
    doReturn(Optional.of("correlationId")).when(metadata).getCorrelationId();
    doReturn(true).when(metadata).isRedeliver();

    IncomingRabbitMessageWrapper result = cut.convert(message);

    IncomingRabbitMessageWrapper expected = new IncomingRabbitMessageWrapper(
        "exchange", "routingKey", "correlationId", Map.of(), JsonObject.of("attribute", "value"), true);
    assertThat(result, is(equalTo(expected)));
  }
}

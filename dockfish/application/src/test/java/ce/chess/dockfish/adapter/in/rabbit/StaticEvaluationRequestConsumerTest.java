package ce.chess.dockfish.adapter.in.rabbit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.domain.model.RequeueException;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;
import ce.chess.dockfish.usecase.in.ReceiveStaticEvaluationRequest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InOrder;

@QuarkusTest
@QuarkusTestResource(InMemoryTestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaticEvaluationRequestConsumerTest {
  private static final StaticEvaluationRequest request = new StaticEvaluationRequest("fen");

  private static final IllegalArgumentException exceptionForTest = new IllegalArgumentException("for test");
  private static final RequeueException requeueException = new RequeueException("for test", exceptionForTest);

  @Inject
  @Connector(InMemoryConnector.CONNECTOR)
  InMemoryConnector connector;

  @InjectMock
  IncomingRabbitMessageConverter incomingRabbitMessageConverter = mock(IncomingRabbitMessageConverter.class);

  @InjectMock
  ReceiveStaticEvaluationRequest service;

  Message<JsonObject> messageMock = spy(Message.class);

  @BeforeEach
  void setUp() {
    JsonObject jsonPayload = JsonObject.mapFrom(request);
    doReturn(
        new IncomingRabbitMessageWrapper("exchange", "routingKey", "correlationId", Map.of(), jsonPayload, false))
        .when(incomingRabbitMessageConverter).convert(any());
    doReturn(jsonPayload).when(messageMock).getPayload();
  }

  @Test
  void whenSendingMessagedThenServiceWillBeCalledAndMessageAcked() {

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(1000)).createAndPublishEvaluation(request);
    inOrder.verify(messageMock, timeout(100)).ack();
  }

  @Test
  void whenServiceFailsThenNack() {
    doThrow(exceptionForTest).when(service).createAndPublishEvaluation(request);

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(1000)).createAndPublishEvaluation(request);
    inOrder.verify(messageMock, timeout(100)).nack(exceptionForTest);
  }

  @Test
  void whenNotRequeuedThenNackWithRequeue() {
    doThrow(requeueException).when(service).createAndPublishEvaluation(request);

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(1000)).createAndPublishEvaluation(request);
    inOrder.verify(messageMock, timeout(100)).nack(eq(requeueException), any(Metadata.class));
  }

  @Test
  void whenRequeuedThenNackWithoutRequeue() {
    JsonObject jsonPayload = JsonObject.mapFrom(request);
    doReturn(
        new IncomingRabbitMessageWrapper("exchange", "routingKey", "correlationId", Map.of(), jsonPayload, true))
        .when(incomingRabbitMessageConverter).convert(any());
    doThrow(requeueException).when(service).createAndPublishEvaluation(request);

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(1000)).createAndPublishEvaluation(request);
    inOrder.verify(messageMock, timeout(100)).nack(requeueException);
  }

}

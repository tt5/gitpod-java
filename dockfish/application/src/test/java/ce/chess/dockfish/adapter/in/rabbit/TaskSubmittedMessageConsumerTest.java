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
import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;
import ce.chess.dockfish.domain.model.RequeueException;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.in.ReceiveAnalysisRequest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

@QuarkusTest
@QuarkusTestResource(InMemoryTestConfig.class)
class TaskSubmittedMessageConsumerTest {
  private static final SubmitTaskCommand request = SubmitTaskCommand.builder()
      .pgn("1.e4")
      .initialPv(3)
      .maxDepth(30)
      .build();
  private static final IllegalArgumentException exceptionForTest = new IllegalArgumentException("for test");
  private static final RequeueException requeueException = new RequeueException("for test", exceptionForTest);

  @Inject
  @Connector(InMemoryConnector.CONNECTOR)
  InMemoryConnector connector;

  @InjectMock
  IncomingRabbitMessageConverter incomingRabbitMessageConverter = mock(IncomingRabbitMessageConverter.class);

  @InjectMock
  ReceiveAnalysisRequest service;

  Message<JsonObject> messageMock = spy(Message.class);

  @BeforeEach
  void setUp() {
    JsonObject jsonPayload = JsonObject.mapFrom(request);
    doReturn(
        new IncomingRabbitMessageWrapper("exchange", "routingKey", "correlationId", Map.of(), jsonPayload, false))
        .when(incomingRabbitMessageConverter).convert(any());
    doReturn(jsonPayload).when(messageMock).getPayload();
    doReturn(Optional.of(new TaskId("task"))).when(service).startSync(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {RabbitConfiguration.CHANNEL_TASK_SUBMITTED, RabbitConfiguration.CHANNEL_TASK_SUBMITTED_2})
  void whenSendingMessagedThenServiceWillBeCalledAndMessageAcked(String channelName) {

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(channelName);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(4000)).startSync(any(AnalysisRun.class));
    inOrder.verify(messageMock, timeout(100)).ack();
  }

  @Test
  void whenServiceFailsThenNack() {
    doThrow(exceptionForTest).when(service).startSync(any(AnalysisRun.class));

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_TASK_SUBMITTED);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(4000)).startSync(any(AnalysisRun.class));
    inOrder.verify(messageMock, timeout(100)).nack(exceptionForTest);
  }

  @Test
  void whenNotRequeuedThenNackWithRequeue() {
    doThrow(requeueException).when(service).startSync(any(AnalysisRun.class));

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_TASK_SUBMITTED);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(4000)).startSync(any(AnalysisRun.class));
    inOrder.verify(messageMock, timeout(100)).nack(eq(requeueException), any(Metadata.class));
  }

  @Test
  void whenRequeuedThenNackWithoutRequeue() {
    JsonObject jsonPayload = JsonObject.mapFrom(request);
    doReturn(
        new IncomingRabbitMessageWrapper("exchange", "routingKey", "correlationId", Map.of(), jsonPayload, true))
        .when(incomingRabbitMessageConverter).convert(any());
    doThrow(requeueException).when(service).startSync(any(AnalysisRun.class));

    InMemorySource<Message<JsonObject>> requestSource =
        connector.source(RabbitConfiguration.CHANNEL_TASK_SUBMITTED);
    requestSource.send(messageMock);

    InOrder inOrder = inOrder(service, messageMock);
    inOrder.verify(service, timeout(4000)).startSync(any(AnalysisRun.class));
    inOrder.verify(messageMock, timeout(100)).nack(requeueException);
  }
}

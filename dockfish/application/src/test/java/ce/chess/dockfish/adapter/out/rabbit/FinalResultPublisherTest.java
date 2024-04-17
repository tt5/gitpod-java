package ce.chess.dockfish.adapter.out.rabbit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.adapter.common.mapper.EvaluationMessageDtoMapper;
import ce.chess.dockfish.adapter.out.rabbit.fallback.PublishFailed;
import ce.chess.dockfish.domain.event.SubmitEvaluationMessage;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@QuarkusTest
@QuarkusTestResource(InMemoryTestConfig.class)
class FinalResultPublisherTest {
  @Inject
  @Connector(InMemoryConnector.CONNECTOR)
  InMemoryConnector connector;

  @Inject
  Event<SubmitEvaluationMessage> resultPublisher;

  @Test
  void doesSendMessage() {
    InMemorySink<EvaluationMessageDto> sink = connector.sink(RabbitConfiguration.CHANNEL_EVALUATION);
    EvaluationMessage domainObject = EvaluationMessage.builder()
        .taskName("dummyMessage")
        .build();
    EvaluationMessageDto dtoObject = EvaluationMessageDto.builder()
        .taskName("dummyMessage")
        .build();

    resultPublisher.fire(new SubmitEvaluationMessage(domainObject));

    assertThat(sink.received(), hasSize(1));
    assertThat(sink.received().get(0).getPayload(), is(equalTo(dtoObject)));

  }

  @Test
  @SuppressWarnings("unchecked")
  void doesHandleErrorFromSend() {
    Emitter<EvaluationMessageDto> emitter = (Emitter<EvaluationMessageDto>) mock(Emitter.class);
    Event<PublishFailed> publishFailedEvent = (Event<PublishFailed>) mock(Event.class);
    FinalResultPublisher cut = new FinalResultPublisher();
    cut.evaluationMessageDtoMapper = Mappers.getMapper(EvaluationMessageDtoMapper.class);
    cut.publishFailedEvent = publishFailedEvent;
    cut.emitter = emitter;

    doThrow(IllegalStateException.class).when(emitter).send(any(Message.class));

    SubmitEvaluationMessage messageEvent = new SubmitEvaluationMessage(EvaluationMessage.builder().build());
    cut.publishFinalEvaluation(messageEvent);

    verify(publishFailedEvent).fire(any(PublishFailed.class));
  }

}

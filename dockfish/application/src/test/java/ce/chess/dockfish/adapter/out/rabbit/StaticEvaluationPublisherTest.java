package ce.chess.dockfish.adapter.out.rabbit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluation;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(InMemoryTestConfig.class)
class StaticEvaluationPublisherTest {

  @Inject
  @Connector(InMemoryConnector.CONNECTOR)
  InMemoryConnector connector;

  @Inject
  Event<StaticEvaluation> resultPublisher;

  @Test
  void doesSendMessage() {
    InMemorySink<StaticEvaluation> sink = connector.sink(RabbitConfiguration.CHANNEL_STATIC_EVALUATION);
    StaticEvaluation messageObject = StaticEvaluation.builder()
        .request(StaticEvaluationRequest.builder().fen("fen").build())
        .evaluation("evaluation")
        .build();

    resultPublisher.fire(messageObject);

    assertThat(sink.received(), hasSize(1));
    assertThat(sink.received().get(0).getPayload(), is(equalTo(messageObject)));

  }

}

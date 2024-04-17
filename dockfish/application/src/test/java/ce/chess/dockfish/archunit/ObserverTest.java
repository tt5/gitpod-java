package ce.chess.dockfish.archunit;

import static ce.chess.dockfish.archunit.Reflections.countObservingMethods;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.out.rabbit.FinalResultPublisher;
import ce.chess.dockfish.domain.event.AnalysisFinished;
import ce.chess.dockfish.domain.event.DepthReached;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.event.SubmitEvaluationMessage;
import ce.chess.dockfish.domain.service.run.AnalysisFinishedHandler;
import ce.chess.dockfish.domain.service.run.DepthReachedHandler;
import ce.chess.dockfish.domain.service.run.EngineInformationReceivedHandler;

import org.junit.jupiter.api.Test;

public class ObserverTest {
  @Test
  void checkObservers() {
    assertThat(countObservingMethods(DepthReachedHandler.class, DepthReached.class), is(1L));
    assertThat(countObservingMethods(AnalysisFinishedHandler.class, AnalysisFinished.class), is(1L));
    assertThat(countObservingMethods(EngineInformationReceivedHandler.class, EngineInformationReceived.class), is(1L));
    assertThat(countObservingMethods(FinalResultPublisher.class, SubmitEvaluationMessage.class), is(1L));
  }

}

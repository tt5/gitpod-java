package ce.chess.dockfish.domain.service.run;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluation;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;
import ce.chess.dockfish.usecase.out.engine.LockEngine;
import ce.chess.dockfish.usecase.out.engine.StartStaticEvaluation;

import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaticEvaluationServiceTest {
  @Mock
  LockEngine lockEngine;

  @Mock
  StartStaticEvaluation startStaticEvaluation;

  @Mock
  Event<StaticEvaluation> resultPublisher;

  @InjectMocks
  StaticEvaluationService cut;

  @Test
  void delegatesToEngineController() {
    given(startStaticEvaluation.retrieveStaticEvaluation(anyString())).willReturn("result");

    StaticEvaluationRequest evaluationRequest = new StaticEvaluationRequest("anyFen");
    cut.createAndPublishEvaluation(evaluationRequest);

    InOrder inOrder = Mockito.inOrder(lockEngine, startStaticEvaluation, resultPublisher);
    inOrder.verify(lockEngine).acquireLock();
    inOrder.verify(startStaticEvaluation).retrieveStaticEvaluation("anyFen");
    inOrder.verify(resultPublisher).fire(new StaticEvaluation(evaluationRequest, "result"));
    inOrder.verify(lockEngine).releaseLock();
  }

}

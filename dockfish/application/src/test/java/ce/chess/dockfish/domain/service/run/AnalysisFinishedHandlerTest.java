package ce.chess.dockfish.domain.service.run;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ce.chess.dockfish.domain.event.AnalysisFinished;
import ce.chess.dockfish.domain.event.SubmitEvaluationMessage;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.domain.service.query.EvaluationMessageService;

import jakarta.enterprise.event.Event;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisFinishedHandlerTest {
  private static final TaskId TASK_ID = new TaskId("taskid");
  private static final Instant NOW = Instant.now();

  @Mock
  private EvaluationMessageService evaluationMessageService;

  @Mock
  private Event<SubmitEvaluationMessage> submitEvaluationMessageEventPublisher;

  @Mock
  private EvaluationMessage evaluationMessage;

  @InjectMocks
  private AnalysisFinishedHandler cut;

  @BeforeEach
  void setUp() {
    Mockito.reset(evaluationMessageService, submitEvaluationMessageEventPublisher);
  }

  @Test
  void whenReceivingAnalysisFinishedEventThenFireSubmitEvaluationMessage() {
    given(evaluationMessageService.getLastEvaluationMessage(TASK_ID)).willReturn(Optional.of(evaluationMessage));

    cut.publishFinalEvaluation(new AnalysisFinished(TASK_ID, NOW));

    verify(evaluationMessageService).getLastEvaluationMessage(TASK_ID);
    verify(submitEvaluationMessageEventPublisher).fire(new SubmitEvaluationMessage(evaluationMessage));
  }

  @Test
  void doNotFireWhenQueryEmpty() {
    given(evaluationMessageService.getLastEvaluationMessage(TASK_ID)).willReturn(Optional.empty());

    cut.publishFinalEvaluation(new AnalysisFinished(TASK_ID, NOW));

    verify(evaluationMessageService).getLastEvaluationMessage(TASK_ID);
    verifyNoInteractions(submitEvaluationMessageEventPublisher);
  }

}

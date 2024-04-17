package ce.chess.dockfish.domain.service.run;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ce.chess.dockfish.domain.event.DepthReached;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.UciState;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;
import ce.chess.dockfish.usecase.out.db.TaskRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepthReachedHandlerTest {
  private static final DepthReached event = new DepthReached(new TaskId("task"), 40);
  private final UciState uciState = new UciState(0, 0, 0, Set.of());

  @Mock
  EvaluationRepository evaluationRepository;
  @Mock
  TaskRepository engineTaskRepository;
  @Mock
  AdaptPvService adaptPvService;

  @InjectMocks
  DepthReachedHandler cut;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Evaluation evaluation;


  @Mock
  private AnalysisRun engineTask;

  private static LogCaptor logCaptor;

  @BeforeAll
  public static void setupLogCaptor() {
    logCaptor = LogCaptor.forClass(DepthReachedHandler.class);
  }

  @AfterEach
  public void clearLogCaptor() {
    logCaptor.clearLogs();
  }

  @AfterAll
  public static void closeLogCaptor() {
    logCaptor.resetLogLevel();
    logCaptor.close();
  }

  @BeforeEach
  void setUp() {
    given(engineTaskRepository.findByTaskId(event.getTaskId())).willReturn(Optional.of(engineTask));
    given(evaluationRepository.findByTaskIdMaxCreated(event.getTaskId())).willReturn(Optional.of(evaluation));
    lenient().when(evaluation.getVariations())
        .thenReturn(List.of(mock(Variation.class), mock(Variation.class), mock(Variation.class)));
    lenient().when(evaluation.getUciState()).thenReturn(uciState);

  }

  @Test
  void observesEvent() {

    fire(event);

    assertThat(logCaptor.getInfoLogs(), hasItem(containsString("d0")));
    verify(engineTaskRepository).findByTaskId(event.getTaskId());
    verify(adaptPvService).adaptPv(evaluation, engineTask);
  }


  @Test
  void givenNoTaskThenJustLog() {
    given(engineTaskRepository.findByTaskId(event.getTaskId())).willReturn(Optional.empty());

    fire(event);

    assertThat(logCaptor.getWarnLogs(), hasItem(containsString("Task not found")));
    verifyNoInteractions(adaptPvService);
    verifyNoInteractions(evaluation);
  }

  private void fire(DepthReached event) {
    cut.newDepthReached(event);
  }

}

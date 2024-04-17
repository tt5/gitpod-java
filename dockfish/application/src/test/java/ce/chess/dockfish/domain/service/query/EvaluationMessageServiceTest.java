package ce.chess.dockfish.domain.service.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;
import ce.chess.dockfish.usecase.out.db.TaskRepository;
import ce.chess.dockfish.usecase.out.engine.QueryEngine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationMessageServiceTest {

  private static final TaskId TASK_ID = new TaskId("taskId");

  private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private EvaluationRepository evaluationRepository;

  @Mock
  private EngineInformationReceivedRepository engineInformationRepository;

  @Mock
  private QueryEngine queryEngine;

  @Mock
  Config config;

  @Mock
  private Evaluation evaluation;

  @Mock
  private Evaluation evaluation2;


  private final AnalysisRun task = AnalysisRun.builder()
      .taskId(TASK_ID)
      .reference("reference_id")
      .name("taskName")
      .engineProgramName("engineProgramName")
      .uciEngineName("uciEngineName")
      .hostname("testhost")
      .startingPosition(new GamePositionService().createFrom("1.e4 e5*"))
      .initialPv(3)
      .maxDuration(Duration.of(2, ChronoUnit.HOURS))
      .created(NOW)
      .build();

  private final EngineInformationReceived engineInformation = EngineInformationReceived.builder()
      .taskId(TASK_ID)
      .multiPv(3)
      .depth(20)
      .time(500)
      .score(99)
      .nodes(10000)
      .nodesPerSecond(6000)
      .tbHits(0)
      .calculatedPlies(12)
      .occurredOn(NOW)
      .pgn("pgn")
      .lineSan("lineSan")
      .build();

  @InjectMocks
  private EvaluationMessageService cut;

  @BeforeEach
  void setUpRepositories() {
    lenient().when(taskRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(task));
    lenient().when(taskRepository.findLatest()).thenReturn(Optional.of(task));
    lenient().when(evaluationRepository.findByTaskIdMaxCreated(TASK_ID)).thenReturn(Optional.of(evaluation));
    lenient().when(evaluationRepository.findByTaskId(TASK_ID)).thenReturn(List.of(evaluation, evaluation2));
    lenient().when(engineInformationRepository.findByTaskIdMaxOccurredOn(TASK_ID))
        .thenReturn(Optional.of(engineInformation));
    lenient().when(engineInformationRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(TASK_ID))
        .thenReturn(List.of(engineInformation));
    lenient().when(evaluation.maxDepth()).thenReturn(20);
    lenient().when(evaluation.shortForm()).thenReturn("h20");
    lenient().when(evaluation.getCreated()).thenReturn(NOW);
    lenient().when(evaluation2.maxDepth()).thenReturn(21);
    lenient().when(evaluation2.shortForm()).thenReturn("h21");
    lenient().when(evaluation2.getCreated()).thenReturn(NOW);
  }

  @Nested
  class WhenGettingLastEvaluationMessage {

    private EvaluationMessage lastEvaluationMessage;

    @BeforeEach
    void setUp() {
      lastEvaluationMessage = cut.getLastEvaluationMessage().orElse(null);
    }

    @Test
    void thenLookupLatestTaskId() {
      verify(taskRepository).findLatest();
      verify(taskRepository).findByTaskId(TASK_ID);
      verify(evaluationRepository).findByTaskIdMaxCreated(TASK_ID);
      verify(evaluationRepository).findByTaskId(TASK_ID);
      verify(engineInformationRepository).findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(TASK_ID);
      verify(engineInformationRepository).findByTaskIdMaxOccurredOn(TASK_ID);
    }

    @Test
    void thenReturnResult() {
      assertThat(lastEvaluationMessage, notNullValue());
    }
  }

  @Nested
  class WhenGettingLastEvaluationMessageForTaskId {

    private EvaluationMessage lastEvaluationMessage;

    @BeforeEach
    void setUp() {
      given(queryEngine.getJobStatus(TASK_ID)).willReturn(JobStatus.ACTIVE);
      given(config.getOptionalValue("hostname", String.class)).willReturn(Optional.of("testhost"));

      lastEvaluationMessage = cut.getLastEvaluationMessage(TASK_ID).orElseThrow();
    }

    @Test
    void thenLookupLatestTaskId() {
      verify(taskRepository).findByTaskId(TASK_ID);
      verify(evaluationRepository).findByTaskIdMaxCreated(TASK_ID);
      verify(evaluationRepository).findByTaskId(TASK_ID);
      verify(engineInformationRepository).findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(TASK_ID);
      verify(engineInformationRepository).findByTaskIdMaxOccurredOn(TASK_ID);
    }

    @Test
    void thenReturnExpectedResult() {
      EvaluationMessage expected = EvaluationMessage.builder()
          .taskName("taskName")
          .reference("reference_id")
          .analysedPgn(new GamePositionService().createFrom("1. e4 e5 *").getPgn())
          .analysedFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2")
          .analysedPly(2)
          .uciEngineName("uciEngineName")
          .taskDuration(Duration.of(2, ChronoUnit.HOURS))
          .hostname("testhost")
          .status(JobStatus.ACTIVE)
          .evaluation(evaluation)
          .taskStarted(NOW)
          .lastAlive(NOW)
          .lastEvaluation(NOW)
          .latestEvent(engineInformation.toModel())
          .history(List.of("h21", "h20"))
          .build();
      assertThat(lastEvaluationMessage, is(equalTo(expected)));
    }

    @Test
    void whenNoLastEvaluationMessageIsPresentThenReturnEmpty() {
      Optional<EvaluationMessage> lastEvaluationMessageOptional =
          cut.getLastEvaluationMessage(new TaskId("fantasy taks id"));

      assertThat(lastEvaluationMessageOptional, is(Optional.empty()));
    }

    @Nested
    class GivenNoEvaluationMessageIsPresent {

    }
  }

  @Nested
  class WhenGettingAllTaskIds {
    @Test
    void thenDelegateToEvaluationRepository() {
      cut.getAllTaskIds();

      verify(evaluationRepository).listTaskIds();
    }
  }

  @Nested
  class WhenGettingAllEvaluationsForTaskId {
    @Test
    void thenDelegateToEvaluationRepository() {
      cut.getAllEvaluations(TASK_ID);

      verify(evaluationRepository).findByTaskId(TASK_ID);
    }
  }

  @Nested
  class WhenGettingAllEvaluations {
    @Test
    void thenDelegateToEvaluationRepository() {
      given(taskRepository.findLatest()).willReturn(Optional.of(task));

      cut.getAllEvaluations();

      verify(evaluationRepository).findByTaskId(TASK_ID);
    }
  }

}

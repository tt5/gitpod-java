package ce.chess.dockfish.adapter.out.engine;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.event.AnalysisFinished;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import jakarta.enterprise.event.Event;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import raptor.engine.uci.UCIEngine;
import raptor.engine.uci.UCIOption;
import raptor.engine.uci.options.UCISpinner;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class EngineControllerTest {

  public static final String EXPECTED_FEN = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 2";
  public static final String EXPECTED_ENGINE_NAME = "uciEngine.getEngineName";
  private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
  @Mock
  private UCIEngine uciEngine;

  @Mock
  private UciEngineHolder uciEngineHolder;

  @Mock
  private EngineListener engineListener;

  @Mock
  private SimpleWatchdog simpleWatchdog;

  @Mock
  private Event<AnalysisFinished> analysisFinishedPublisher;

  @Mock
  private AnalysisRun analysisRun;

  @Captor
  private ArgumentCaptor<AnalysisRun> taskAssignedToEngineListener;

  @InjectMocks
  private EngineController cut;

  @Nested
  class StartAnalysis {
    @Nested
    class GivenLockIsAcquired {
      @BeforeEach
      void beforeEach() {
        given(uciEngineHolder.connect(anyString(), anyInt(), any())).willReturn(uciEngine);
        given(uciEngine.getEngineName()).willReturn("anything");

        cut.acquireLock();
      }

      @Test
      void doesStartEngineInOrder() {
        analysisRun = createAnalysisRun(null, null);

        cut.startAnalysis(analysisRun);

        InOrder inOrder = Mockito.inOrder(uciEngineHolder, engineListener, uciEngine, simpleWatchdog);
        inOrder.verify(uciEngineHolder).connect("engineName", 4, List.of());
        inOrder.verify(engineListener).assignTo(taskAssignedToEngineListener.capture());
        assertThat(taskAssignedToEngineListener.getValue(), is(not(equalTo(analysisRun))));
        inOrder.verify(uciEngine).setPosition(EXPECTED_FEN);
        inOrder.verify(uciEngine).go("infinite", engineListener);
        inOrder.verify(simpleWatchdog).watch(any(), eq(1), any());
      }

      @Test
      void doesRunMaxDepth() {
        analysisRun = createAnalysisRun(5, null);

        cut.startAnalysis(analysisRun);

        verify(uciEngine).go("depth 5", engineListener);
      }

      @Test
      void doesRunMaxDuration() {
        analysisRun = createAnalysisRun(null, Duration.ofMillis(200L));

        cut.startAnalysis(analysisRun);

        verify(uciEngine).go("movetime 200", engineListener);
      }

      @Test
      void doesReturnTaskWithUciEngineNameAndFen() {
        analysisRun = createAnalysisRun(0, Duration.ofMillis(200L));
        given(uciEngine.getEngineName()).willReturn(EXPECTED_ENGINE_NAME);

        AnalysisRun result = cut.startAnalysis(analysisRun);

        assertThat(result.uciEngineName().orElseThrow(AssertionError::new), is(equalTo(EXPECTED_ENGINE_NAME)));
        assertThat(result.startingPosition().getFen(), is(equalTo(EXPECTED_FEN)));

        verify(engineListener).assignTo(taskAssignedToEngineListener.capture());
        assertThat(taskAssignedToEngineListener.getValue().uciEngineName(), is(Optional.of(EXPECTED_ENGINE_NAME)));
        assertThat(taskAssignedToEngineListener.getValue().startingPosition().getFen(), is(equalTo(EXPECTED_FEN)));
      }

      @Test
      void doesKeepLockAfterSuccess() {
        analysisRun = createAnalysisRun(2, null);

        cut.startAnalysis(analysisRun);

        assertThat(cut.tryAcquireLock(), is(false));
      }

    }

    private AnalysisRun createAnalysisRun(Integer maxDepth, Duration maxDuration) {
      return AnalysisRun.builder()
          .taskId(new TaskId("taskId"))
          .engineProgramName("engineName")
          .hostname("testhost")
          .startingPosition(new GamePositionService().createFrom("1. d4 d5"))
          .created(NOW)
          .initialPv(4)
          .maxDepth(maxDepth)
          .maxDuration(maxDuration)
          .build();
    }

    @Test
    void startAnalysisThrowsIfNotLocked() {
      IllegalStateException illegalStateException =
          assertThrows(IllegalStateException.class, () -> cut.startAnalysis(analysisRun));
      assertThat(illegalStateException.getMessage(),
          containsString("startAnalysis should be called only after a Lock was acquired"));
      assertThat(cut.tryAcquireLock(), is(true));
    }
  }

  @Nested
  class Stop {
    @Test
    void doesCallStopOnEngine() {
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      given(uciEngine.isProcessingGo()).willReturn(true);

      cut.stop();

      verify(uciEngine).isProcessingGo();
      verify(uciEngine).stop();
    }
  }

  @Nested
  class Kill {
    @Test
    void doesCallStopOnEngine() {
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      given(uciEngine.isProcessingGo()).willReturn(true);

      cut.kill();

      verify(uciEngine).kill();
    }
  }

  @Nested
  class StaticEvaluation {
    @Test
    void returnsEvaluationFromEngine() {
      given(uciEngineHolder.connect(anyString())).willReturn(uciEngine);
      given(uciEngine.eval()).willReturn("anyEvaluation");

      cut.acquireLock();
      String staticEvaluation = cut.retrieveStaticEvaluation("anyFen");

      assertThat(staticEvaluation, is(equalTo("anyEvaluation")));
      verify(uciEngine).eval();
      verify(uciEngineHolder).disconnect();
    }

    @Test
    void staticEvaluationThrowsIfNotLocked() {
      IllegalStateException illegalStateException =
          assertThrows(IllegalStateException.class, () -> cut.retrieveStaticEvaluation("anyFen"));
      assertThat(illegalStateException.getMessage(),
          containsString("retrieveStaticEvaluation should be called only after a Lock was acquired"));
    }

  }

  @Nested
  class WhenAnalysisFinished {
    @Test
    void firesEventAndReleases() {
      TaskId taskId = new TaskId("42");
      given(analysisRun.taskId()).willReturn(taskId);
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      given(uciEngine.isProcessingGo()).willReturn(true);
      cut.acquireLock();

      cut.watchdogFinished();

      verify(analysisFinishedPublisher).fire(any());
      verify(uciEngine).stop();
      verify(uciEngineHolder).disconnect();
      assertThat(cut.tryAcquireLock(), is(true));
    }
  }

  @Nested
  class TestReducePv {
    private UCIOption multiPvOptionFixture(int value) {
      UCIOption oldOption = new UCISpinner();
      oldOption.setName("MultiPV");
      oldOption.setValue("" + value);
      return oldOption;
    }

    @Test
    void doesNothingIfNewPvIsZero() {

      cut.reducePvTo(0);

      verify(uciEngine, never()).getOption(any());
      verify(uciEngine, never()).setOption(any());
    }

    @Test
    void doesNotIncreasePv() {
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      UCIOption oldOption = multiPvOptionFixture(1);
      given(uciEngine.getOption("MultiPV")).willReturn(oldOption);

      cut.reducePvTo(2);

      verify(uciEngine, never()).setOption(any());
    }

    @Test
    void doesReducePv() {
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      UCIOption oldOption = multiPvOptionFixture(10);
      given(uciEngine.getOption("MultiPV")).willReturn(oldOption);

      cut.reducePvTo(2);

      ArgumentCaptor<UCIOption> optionCaptor = ArgumentCaptor.forClass(UCIOption.class);
      verify(uciEngine).stopSetOptionGo(optionCaptor.capture());
      assertThat(optionCaptor.getValue().getName(), is("MultiPV"));
      assertThat(optionCaptor.getValue().getValue(), is("2"));
    }
  }

  @Nested
  class BlocksParallelExecutions {
    @AfterEach
    void releaseLock() {
      cut.releaseLock();
    }

    @Test
    void whenLockIsAcquiredThenNoLockCanBeGained() {
      cut.acquireLock();
      assertThat(cut.tryAcquireLock(), is(false));

      cut.releaseLock();
      assertThat(cut.tryAcquireLock(), is(true));
    }

    @Test
    void whenLockIsAcquiredWithTryThenNoLockCanBeGained() {
      assertThat(cut.tryAcquireLock(), is(true));
      assertThat(cut.tryAcquireLock(), is(false));
    }

    @Test
    @SuppressWarnings("squid:S5778")
    void secondAcquireBlocks() {
      cut.acquireLock();
      Awaitility.pollExecutorService(Executors.newSingleThreadExecutor());
      assertThrows(ConditionTimeoutException.class,
          () -> await().atMost(500, MILLISECONDS).until(() -> {
            cut.acquireLock();
            return true;
          }));
    }

    @Test
    void secondReleaseDoesNothing() {
      cut.acquireLock();
      cut.releaseLock();
      // will finish immediately
      cut.releaseLock();
      assertThat(cut.tryAcquireLock(), is(true));
    }

    @Test
    void blocksWhileRunning() {
      cut.blockWhileActive();
      verify(simpleWatchdog).waitWhileConditionIsTrue();
    }
  }

  @Nested
  class DoesExposeMetrics {
    @BeforeEach
    void givenAnalysisIsRunning() {
      given(uciEngineHolder.getEngine()).willReturn(uciEngine);
      given(uciEngine.isProcessingGo()).willReturn(true);
      given(analysisRun.taskId()).willReturn(new TaskId("taskId"));
    }

    @Test
    void whenCompletionInFutureThenReturnSecondsRemaining() {
      given(analysisRun.estimatedCompletionTime()).willReturn(Optional.of(NOW.plusHours(1).plusMinutes(1)));

      Long expected = cut.getTaskTimeRemaining();

      assertThat(expected, is(greaterThan(3600L)));
    }

    @Test
    void whenCompletionInPastThenReturnZero() {
      given(analysisRun.estimatedCompletionTime()).willReturn(Optional.of(NOW.minusHours(1)));

      Long expected = cut.getTaskTimeRemaining();

      assertThat(expected, is(0L));
    }

    @Test
    void whenFutureButEngineNotRunningThenReturnZero() {
      given(uciEngine.isProcessingGo()).willReturn(false);
      lenient().when(analysisRun.estimatedCompletionTime()).thenReturn(Optional.of(NOW.plusHours(1).plusMinutes(1)));

      Long expected = cut.getTaskTimeRemaining();

      assertThat(expected, is(0L));
    }
  }
}

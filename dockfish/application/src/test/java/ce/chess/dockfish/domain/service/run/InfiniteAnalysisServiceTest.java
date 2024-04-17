package ce.chess.dockfish.domain.service.run;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.model.RequeueException;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.EngineOption;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.TaskRepository;
import ce.chess.dockfish.usecase.out.engine.LockEngine;
import ce.chess.dockfish.usecase.out.engine.QueryEngine;
import ce.chess.dockfish.usecase.out.engine.RunEngine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
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

@ExtendWith(MockitoExtension.class)
class InfiniteAnalysisServiceTest {

  public static final EngineOption LOCAL_OPTION_HASH = new EngineOption("Hash", "HashValue");
  private static final EngineOption CALLER_OPTION_DEFAULT = new EngineOption("name", "value");
  private static final EngineOption CALLER_OPTION_HASH = new EngineOption("Hash", "CallerHashValue");
  public static final EngineOption LOCAL_OPTION_THREADS = new EngineOption("Threads", "ThreadsValue");
  public static final EngineOption LOCAL_OPTION_SYZYGY_PATH = new EngineOption("SyzygyPath", "/syz");

  @Mock
  private LockEngine lockEngine;

  @Mock
  private QueryEngine queryEngine;

  @Mock
  private RunEngine runEngine;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private UciOptionsConfiguration uciOptionsConfiguration;

  @Mock
  Config config;

  @Captor
  private ArgumentCaptor<AnalysisRun> analysisRequest;

  @InjectMocks
  private InfiniteAnalysisService cut;

  private final AnalysisRun command = AnalysisRun.builder()
      .name("someName")
      .engineProgramName("engineId")
      .startingPosition(new GamePositionService().createFrom("1. e4 e5"))
      .initialPv(3)
      .maxDepth(2)
      .engineOptions(List.of(CALLER_OPTION_DEFAULT, CALLER_OPTION_HASH))
      .created(LocalDateTime.now(ZoneId.systemDefault()))
      .build();

  @Nested
  class WhenCalledViaRest {
    @Nested
    class AndEngineIsReady {
      @Mock
      private AnalysisRun analysisRunFromController;

      @BeforeEach
      void setUp() {
        given(lockEngine.tryAcquireLock()).willReturn(true);
        given(runEngine.startAnalysis(any())).willReturn(analysisRunFromController);
      }

      @Test
      void thenTaskIdIsReturned() {
        Optional<TaskId> taskId = cut.startAsync(command);

        assertThat(taskId.isPresent(), is(true));
      }

      @Test
      void thenControllerIsCalledInOrder() {
        cut.startAsync(command);

        InOrder inOrder = Mockito.inOrder(lockEngine, runEngine, taskRepository);
        inOrder.verify(lockEngine).tryAcquireLock();
        inOrder.verify(runEngine).startAnalysis(analysisRequest.capture());
        inOrder.verify(taskRepository).save(analysisRunFromController);
      }

      @Test
      void thenControllerIsCalledWithCorrectAnalysisRequest() {
        Optional<TaskId> taskId = cut.startAsync(command);

        verify(runEngine).startAnalysis(analysisRequest.capture());
        AnalysisRun actualAnalysisRequest = analysisRequest.getValue();
        assertThat(actualAnalysisRequest.taskId(), is(equalTo(taskId.orElseThrow(AssertionError::new))));
        assertThat(actualAnalysisRequest.initialPv(), is(equalTo(command.initialPv())));
        assertThat(actualAnalysisRequest.maxDepth(), is(equalTo(command.maxDepth())));
        assertThat(actualAnalysisRequest.engineOptions(), contains(CALLER_OPTION_DEFAULT, CALLER_OPTION_HASH));
        assertThat(actualAnalysisRequest.uciEngineName(), is(Optional.empty()));
      }

      @Test
      void doesReleaseLockAfterError() {
        given(runEngine.startAnalysis(any()))
            .willThrow(IllegalArgumentException.class);

        assertThrows(RequeueException.class, () -> cut.startAsync(command));

        verify(lockEngine).releaseLock();
      }
    }

    @Nested
    class AndEngineIsAlreadyRunning {

      @BeforeEach
      void setUp() {
        given(lockEngine.tryAcquireLock()).willReturn(false);
      }

      @Test
      void thenReturnEmptyTaskId() {

        Optional<TaskId> taskId = cut.startAsync(mock(AnalysisRun.class));

        assertThat(taskId.isPresent(), is(false));
        verify(lockEngine).tryAcquireLock();
        verifyNoMoreInteractions(lockEngine);
        verifyNoMoreInteractions(runEngine);
      }
    }

    @Nested
    class AndTaskIsDuplicate {

      @BeforeEach
      void setUp() {
        given(lockEngine.tryAcquireLock()).willReturn(true);
        given(taskRepository.hasDuplicate(any())).willReturn(true);
      }

      @Test
      void thenReturnEmptyTaskId() {

        Optional<TaskId> taskId = cut.startAsync(command);

        assertThat(taskId.isPresent(), is(false));
        verify(taskRepository).hasDuplicate(any());
        verify(lockEngine).tryAcquireLock();
        verify(lockEngine).releaseLock();
        verifyNoMoreInteractions(runEngine);
      }
    }

    @Nested
    class GivenLocalOptions {
      @BeforeEach
      void setUp() {
        given(uciOptionsConfiguration.getLocalEngineOptions())
            .willReturn(List.of(LOCAL_OPTION_HASH, LOCAL_OPTION_THREADS, LOCAL_OPTION_SYZYGY_PATH));
      }

      @Test
      void thenDelegateWithMergedOptions() {
        given(lockEngine.tryAcquireLock()).willReturn(true);

        Optional<TaskId> taskId = cut.startAsync(command);

        assertThat(taskId.isPresent(), is(true));
        verify(runEngine).startAnalysis(analysisRequest.capture());
        assertThat(analysisRequest.getValue().engineOptions(),
            containsInAnyOrder(CALLER_OPTION_DEFAULT, LOCAL_OPTION_HASH, LOCAL_OPTION_THREADS));
      }

      @Test
      void thenDelegateWithMergedOptionsWithSyzygyPath() {
        given(lockEngine.tryAcquireLock()).willReturn(true);

        Optional<TaskId> taskId = cut.startAsync(command.toBuilder().useSyzygyPath(true).build());

        assertThat(taskId.isPresent(), is(true));
        verify(runEngine).startAnalysis(analysisRequest.capture());
        assertThat(analysisRequest.getValue().engineOptions(),
            containsInAnyOrder(CALLER_OPTION_DEFAULT, LOCAL_OPTION_HASH, LOCAL_OPTION_THREADS,
                LOCAL_OPTION_SYZYGY_PATH));
      }
    }

    @Test
    void whenCalledWithGivenTaskIdThenReturnTaskId() {
      given(lockEngine.tryAcquireLock()).willReturn(true);
      TaskId taskId = new TaskId("TASK_ID");
      AnalysisRun withTaskId = command.toBuilder()
          .taskId(taskId)
          .build();

      Optional<TaskId> actual = cut.startAsync(withTaskId);

      assertThat(actual, is(Optional.of(taskId)));
    }
  }

  @Nested
  class WhenCalledViaMessaging {

    @Nested
    class AndTaskIsNotDuplicate {
      @Test
      void thenDelegateToController() {
        AnalysisRun resultFromController = mock(AnalysisRun.class);
        given(runEngine.startAnalysis(any(AnalysisRun.class))).willReturn(resultFromController);
        given(config.getOptionalValue("hostname", String.class)).willReturn(Optional.of("testhost"));

        Optional<TaskId> taskId = cut.startSync(command);

        assertThat(taskId.isPresent(), is(true));

        InOrder inOrder = Mockito.inOrder(lockEngine, runEngine, taskRepository);
        inOrder.verify(lockEngine).acquireLock();
        inOrder.verify(runEngine).startAnalysis(analysisRequest.capture());
        inOrder.verify(taskRepository).save(resultFromController);
        inOrder.verify(lockEngine).blockWhileActive();
        assertThat(analysisRequest.getValue().taskId(), is(equalTo(taskId.orElseThrow(AssertionError::new))));
        assertThat(analysisRequest.getValue().taskId(), is(equalTo(taskId.get())));
        assertThat(analysisRequest.getValue().initialPv(), is(equalTo(command.initialPv())));
        assertThat(analysisRequest.getValue().maxDepth(), is(equalTo(command.maxDepth())));
        assertThat(analysisRequest.getValue().engineOptions(), is(equalTo(command.engineOptions())));
        assertThat(analysisRequest.getValue().uciEngineName(), is(Optional.empty()));
        assertThat(analysisRequest.getValue().hostname(), is("testhost"));
      }

      @Test
      void doesReleaseLockAfterError() {
        given(runEngine.startAnalysis(any()))
            .willThrow(IllegalArgumentException.class);

        assertThrows(RequeueException.class, () -> cut.startSync(command));

        verify(lockEngine).releaseLock();
      }
    }

    @Nested
    class AndTaskIsKnown {

      @BeforeEach
      void setUp() {
        given(taskRepository.hasDuplicate(any())).willReturn(true);
      }

      @Test
      void thenReturnEmptyTaskId() {

        Optional<TaskId> taskId = cut.startSync(command);

        assertThat(taskId.isPresent(), is(false));
        verify(taskRepository).hasDuplicate(any());
        verifyNoInteractions(lockEngine);
        verifyNoInteractions(runEngine);
      }
    }
  }

  @Test
  void stopAnalysisDelegatesToEngineController() {
    given(queryEngine.uciEngineIsRunning()).willReturn(false);

    boolean result = cut.stop();

    verify(runEngine).stop();
    assertThat(result, is(true));
  }

  @Test
  void killAnalysisDelegatesToEngineController() {
    given(queryEngine.uciEngineIsRunning()).willReturn(false);

    boolean result = cut.kill();

    verify(runEngine).kill();
    assertThat(result, is(true));
  }

  @Test
  void getTaskDetailsDelegatesToRepository() {
    TaskId taskId = new TaskId("42");
    AnalysisRun analysisRun = mock(AnalysisRun.class);
    given(taskRepository.findByTaskId(taskId)).willReturn(Optional.of(analysisRun));

    AnalysisRun taskDetails = cut.getTaskDetails(taskId);

    verify(taskRepository).findByTaskId(taskId);
    assertThat(taskDetails, is(equalTo(analysisRun)));
  }

}

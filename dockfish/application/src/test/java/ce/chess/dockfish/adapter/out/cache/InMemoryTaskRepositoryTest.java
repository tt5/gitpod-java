package ce.chess.dockfish.adapter.out.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryTaskRepositoryTest {
  private static final TaskId taskId1 = new TaskId("TASK1");
  private static final TaskId taskId2 = new TaskId("TASK2");
  private static final TaskId taskIdx = new TaskId("TASKX");
  private static final TaskId someOtherTaskId = new TaskId("some other TaskId");
  private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
  private static final GamePosition GAME_POSITION = new GamePositionService().createFrom("1. e4");

  private AnalysisRun task1;

  private AnalysisRun task2;

  private AnalysisRun task3;

  private InMemoryTaskRepository cut;


  @BeforeEach
  void setUp() {
    cut = new InMemoryTaskRepository();

    task1 = createAnalysisRun(taskId1, NOW, null);
    task2 = createAnalysisRun(taskId2, NOW, null);
    task3 = createAnalysisRun(someOtherTaskId, NOW.plusHours(1L), null);
    Stream.of(task1, task2, task3).forEach(cut::save);
    assertThat(cut.getCacheSize(), is(3L));
  }

  private AnalysisRun createAnalysisRun(TaskId taskId, LocalDateTime createdDate, String name) {
    return AnalysisRun.builder()
        .taskId(taskId)
        .created(NOW.minusHours(1L))
        .engineProgramName("prog1")
        .hostname("testhost")
        .startingPosition(GAME_POSITION)
        .initialPv(23)
        .created(createdDate)
        .name(name)
        .build();
  }

  @Nested
  class FindByTaskId {
    @Test
    void returnsElements() {

      Optional<AnalysisRun> result = cut.findByTaskId(taskId1);

      assertThat(result, is(Optional.of(task1)));
    }

    @Test
    void searchesByPartialTaskId() {

      Optional<AnalysisRun> result = cut.findByTaskId(new TaskId("some"));

      assertThat(result, is(Optional.of(task3)));
    }
  }

  @Nested
  class FindLatest {
    @Test
    void returnsElementWithLatestCreationDate() {

      Optional<AnalysisRun> result = cut.findLatest();

      assertThat(result, is(Optional.of(task3)));
    }
  }

  @Nested
  class HasDuplicates {
    @Test
    void isTrueForSimilarTasks() {
      AnalysisRun other = createAnalysisRun(taskIdx, NOW, null);

      assertThat(cut.hasDuplicate(other), is(true));
    }

    @Test
    void isFalseForDifferentTasks() {
      AnalysisRun other = createAnalysisRun(taskIdx, NOW, "differentName");
      assertThat(cut.hasDuplicate(other), is(false));
    }

  }

}

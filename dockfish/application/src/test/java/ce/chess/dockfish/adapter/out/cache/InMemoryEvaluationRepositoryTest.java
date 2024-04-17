package ce.chess.dockfish.adapter.out.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InMemoryEvaluationRepositoryTest {
  private static final TaskId taskId = new TaskId("TASK");
  private static final TaskId someOtherTaskId = new TaskId("some other TaskId");

  @Mock
  private Evaluation evaluation1;

  @Mock
  private Evaluation evaluation2;

  @Mock
  private Evaluation evaluation3;

  @InjectMocks
  private InMemoryEvaluationRepository cut;


  @BeforeEach
  void setUp() {
    given(evaluation1.getTaskId()).willReturn(taskId);
    given(evaluation2.getTaskId()).willReturn(taskId);
    given(evaluation3.getTaskId()).willReturn(someOtherTaskId);
    given(evaluation1.taskIdAndMaxDepth()).willReturn(taskId.getRawId() + 20);
    given(evaluation2.taskIdAndMaxDepth()).willReturn(taskId.getRawId() + 21);
    given(evaluation3.taskIdAndMaxDepth()).willReturn(someOtherTaskId.getRawId() + 21);
    given(evaluation1.getCreated()).willReturn(LocalDateTime.now(ZoneId.systemDefault()).minusHours(1L));
    given(evaluation2.getCreated()).willReturn(LocalDateTime.now(ZoneId.systemDefault()));
    given(evaluation3.getCreated()).willReturn(LocalDateTime.now(ZoneId.systemDefault()));

    Arrays.asList(evaluation1, evaluation2, evaluation3).forEach(cut::save);
    assertThat(cut.getCacheSize(), is(3L));
  }

  @Nested
  class Save {
    @Test
    void doesUpdateElementWithSameTaskAndMaxDepth() {
      Evaluation evaluation4 = mock(Evaluation.class);
      given(evaluation4.getTaskId()).willReturn(taskId);
      given(evaluation4.getCreated()).willReturn(LocalDateTime.now(ZoneId.systemDefault()));
      given(evaluation4.taskIdAndMaxDepth()).willReturn(taskId.getRawId() + 21);

      cut.save(evaluation4);
      Collection<Evaluation> result = cut.findByTaskId(taskId);

      assertThat(result, contains(evaluation1, evaluation4));
    }
  }

  @Nested
  class FindByTaskId {
    @Test
    void returnElements() {

      Collection<Evaluation> result = cut.findByTaskId(taskId);

      assertThat(result, contains(evaluation1, evaluation2));
    }
  }

  @Nested
  class FindLatest {
    @Test
    void returnsNewestElement() {

      Optional<Evaluation> result = cut.findByTaskIdMaxCreated(taskId);

      assertThat(result, is(Optional.of(evaluation2)));
    }
  }

  @Nested
  class ListTaskIds {
    @Test
    void returnsListOfDistinctTaskIds() {

      Collection<TaskId> result = cut.listTaskIds();

      assertThat(result, containsInAnyOrder(taskId, someOtherTaskId));
    }
  }
}

package ce.chess.dockfish.domain.model.task;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskIdTest {

  TaskId cut;

  @Test
  void testMatches() {
    cut = new TaskId("abcdefghij");

    assertThat(cut.matches(new TaskId("abc")), is(true));
    assertThat(cut.matches(new TaskId("abv")), is(false));
    assertThat(cut.matches(cut), is(true));
    assertThat(cut.matches(new TaskId("")), is(false));
    assertThat(new TaskId("abc").matches(cut), is(false));
  }

  @Test
  void testCreateNew() {
    cut = TaskId.createNew();

    assertThat(cut.getRawId(), is(not(blankOrNullString())));
    assertThat(UUID.fromString(cut.getRawId()), isA(UUID.class));
  }

}

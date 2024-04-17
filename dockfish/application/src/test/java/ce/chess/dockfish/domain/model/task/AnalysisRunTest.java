package ce.chess.dockfish.domain.model.task;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnalysisRunTest {
  public static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
  private AnalysisRun cut;

  @BeforeEach
  void setup() {
    cut = AnalysisRun.builder()
        .name("someName")
        .engineProgramName("engineId")
        .startingPosition(new GamePositionService().createFrom("1. e4 e5"))
        .initialPv(3)
        .maxDepth(2)
        .created(NOW)
        .build();
  }

  @Test
  void doesCompareWithSame() {
    assertThat(cut.isSameAs(cut), is(true));
    assertThat(cut.isSameAs(cut.toBuilder().build()), is(true));
    assertThat(cut.isSameAs(cut.toBuilder().created(NOW.plusSeconds(5)).build()), is(true));
    assertThat(cut.isSameAs(cut.toBuilder().taskId(new TaskId("other")).build()), is(true));
    assertThat(cut.isSameAs(cut.toBuilder().uciEngineName("other").build()), is(true));

    assertThat(cut.isSameAs(cut.toBuilder().maxDuration(Duration.ZERO).build()), is(false));
    assertThat(cut.isSameAs(cut.toBuilder().engineProgramName("other").build()), is(false));
  }

  @Nested
  class SetsOptions {
    EngineOption newOption = new EngineOption("newOption", "newValue");
    EngineOption oldOption = new EngineOption("newOption", "oldValue");
    EngineOption otherOption = new EngineOption("otherOption", "newValue");

    @Nested
    class WhenNoOptionsArePresent {

      @Test
      void newOptionsWillBeCreated() {
        AnalysisRun result = cut.addOrReplaceOption(newOption);
        assertThat(result.engineOptions(), contains(newOption));
      }
    }

    @Nested
    class WhenOptionIsAlreadyPresent {
      @Test
      void theNewOptionWillReplaceTheExistingOne() {
        cut = cut.toBuilder().engineOptions(List.of(oldOption)).build();

        AnalysisRun result = cut.addOrReplaceOption(newOption);

        assertThat(result.engineOptions(), contains(newOption));
      }

    }

    @Nested
    class WhenDifferentOptionsArePresent {
      @Test
      void theNewOptionWillBeAdded() {
        cut = cut.toBuilder().engineOptions(List.of(otherOption)).build();

        AnalysisRun result = cut.addOrReplaceOption(newOption);

        assertThat(result.engineOptions(), containsInAnyOrder(otherOption, newOption));
      }

    }
  }

}

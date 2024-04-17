package ce.chess.dockfish.adapter.in.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SubmitTaskCommandTest {

  private SubmitTaskCommand cut;

  @Test
  void mustHaveDurationXorDepth() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> SubmitTaskCommand.builder()
            .initialPv(3)
            .engineId("engineId")
            .name("name")
            .pgn("pgn")
            .build()
            .validate());
    assertThat(thrown.getMessage(), containsString("Either Depth or Duration must be given"));
  }

  @Test
  void mustHavePgnXorFen() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> SubmitTaskCommand.builder()
            .maxDuration(Duration.parse("PT2H30M"))
            .initialPv(3)
            .engineId("engineId")
            .name("name")
            .pgn("pgn")
            .fen("fen")
            .build()
            .validate());
    assertThat(thrown.getMessage(), containsString("Either PGN or FEN must be given"));
  }

  @Test
  void parsesDuration() {
    cut = SubmitTaskCommand.builder()
        .maxDuration(Duration.parse("PT2H30M"))
        .initialPv(3)
        .engineId("engineId")
        .name("name")
        .pgn("pgn")
        .build();
    assertThat(cut.getMaxDuration(), is(Duration.ofMinutes(2 * 60 + 30)));
  }

}

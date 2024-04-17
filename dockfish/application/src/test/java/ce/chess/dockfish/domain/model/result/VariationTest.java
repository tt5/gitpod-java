package ce.chess.dockfish.domain.model.result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;

import org.junit.jupiter.api.Test;

class VariationTest {

  @Test
  void testLogRepresentation() {
    Variation cut = Variation.builder()
        .depth(2)
        .moves("1. d4 d5")
        .gamePosition(new GamePositionService().createFrom("1. d4 d5"))
        .pvId(4)
        .score(Score.fromCentiPawns(23))
        .time(AnalysisTime.fromMinutes(42))
        .build();

    assertThat(cut.shortRepresentation(), is(not(blankOrNullString())));
  }

  @Test
  void firstMoveWhite() {
    String moveList = "22. Bxd7 Nxd7 23. Nb3 *";
    assertThat(Variation.firstMove(moveList), is(equalTo("22. Bxd7")));
  }

  @Test
  void firstMoveBlack() {
    String moveList = "28. ... Nf7 29. Rh4 e5 30. Ba5 Qe7 31. Rxh3";
    assertThat(Variation.firstMove(moveList), is(equalTo("28. ... Nf7")));
  }
}

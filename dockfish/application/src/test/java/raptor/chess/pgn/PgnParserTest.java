package raptor.chess.pgn;

import static java.util.function.Predicate.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import raptor.chess.Game;

class PgnParserTest {
  private static final String HEADER =
      "[Event \"Final\"]\n"
          + "[Site \"ICCF\"]\n"
          + "[Date \"2021.01.01\"]\n"
          + "[Round \"-\"]\n"
          + "[White \"Whiteman, Dummy\"]\n"
          + "[Black \"Black, Foo\"]\n"
          + "[Result \"\"]\n"
          + "[WhiteElo \"2306\"]\n"
          + "[BlackElo \"2350\"]\n"
          + "[ECO \"B99l\"]\n"
          + "\n";

  @ParameterizedTest
  @ValueSource(strings = {
      "1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 a6 6.Bg5 e6 7.f4 Be7 8.Qf3 Nbd7 9.O-O-O Qc7 10.Bd3 b5 "
          + "11.Rhe1 Bb7 12.Qg3 h6 13.Bxf6 Bxf6 14.Bxb5 axb5 15.Ndxb5 Qb6 16.Rxd6 Bc6 17.e5 Be7 18.Qxg7 Rf8 "
          + "19.f5 Qf2 20.Rdd1 Rc8 21.fxe6 Bxb5 22.Rf1 Bxf1 23.exd7+ Kd8 24.dxc8=Q+ Kxc8 25.Qg4+ Kb8 26.Nd5 Be2 "
          + "27.Qd7 Bg5+ 28.Kb1 Qc5 29.b4 Rd8 30.Qxf7 Qc8 31.Rd4 Bc4 32.Kb2 Bxa2 33.h4 Bxh4 *\n",
      "1.e4 d5 2.e5 f5 3.exf6 *",
      "1.e4 d5 2.e5 { strange move } f5 {good move!} 3.exf6 *",
      "1.e4 d5 2.e5 {f5 looks good here} f5 (and now a4!) 3.exf6 *",
      "1.e4 {1.d4 was slighly better} d5 2.e5 (strange move) f5 {2...a5 was also possible} 3.exf6 a6 4.d3 a5 5.d4 *",
      "1.e4 d5 2.f3 d4 3.c4 dxc3 4.dxc3 Qxd1+ 5.Kxd1 *\n",
      "1.d4 d5 2.e3 Bf5 3.f3 Nc6 4.c3 Qd7 5.b3 O-O-O *\n",
      "1.e4 e5 2.Nf3 Nf6 3.Bc4 Bc5 4.O-O O-O 5.Nxe5 d5 6.Nc6 Nxc6 7.e5 d4 8.e6 d3 9.e7 dxc2 10.e8=R cxb1=B *\n"
  })
  void checkValidPgn(String notation) {
    String pgnString = HEADER + notation;

    Game game = gameFromPgn(pgnString);

    assertThat(stripped(game.toPgn()), is(stripped(pgnString.replace("(","{").replace(")","}"))));
  }

  @Test
  void checkComplexPgnNoSublines() {
    String pgnIn = "[Event \"Ch World (match) (PCA)\"]\n"
        + "[Site \"New York (USA)\"]\n"
        + "[Date \"1995.??.??\"]\n"
        + "[Round \"9\"]\n"
        + "[White \"Anand Viswanathan (IND)\"]\n"
        + "[Black \"Kasparov Garry (RUS)\"]\n"
        + "[Result \"1-0\"]\n"
        + "[WhiteElo \"2725\"]\n"
        + "[BlackElo \"2795\"]\n"
        + "[ECO \"B85\"]\n"
        + "\n"
        + "1. e4 c5 2. Nf3 d6 {[%t bMrk] X ANCc8g42C SNDd502 SNDf502} 3. d4 {[%t Ctrl]\n"
        + "^254 ^00 @^03 ^06 ^06 ^00 ^07 ^00 ^15 ^00 ^16 ^00 ^18 ^00 } cxd4 4. Nxd4\n"
        + "Nf6 5. Nc3 a6 {?+} {!} {maybe 6. a4 } 6. Be2 e6 7. O-O Be7 8. a4 Nc6 9. Be3 O-O 10. f4 Qc7 11. Kh1 Re8\n"
        + "12. Bf3 Bd7 13. Nb3 Na5 14. Nxa5 Qxa5 15. Qd3 Rad8 16. Rfd1 { Fishbein\n"
        + "- Gruenberg, Moscow 1989 } Bc6 {!} 17. b4 1-0";

    Game game = gameFromPgn(pgnIn);
    assertThat(stripped(game.toPgn()), is(stripped(pgnIn)));

  }

  private static Game gameFromPgn(String pgnString) {
    PgnParser parser = new SimplePgnParser(pgnString);
    ListMaintainingPgnParserListener listener = new ListMaintainingPgnParserListener();
    parser.addPgnParserListener(listener);

    parser.parse();

    if (errorsOccurred(listener)) {
      throw new IllegalArgumentException("Invalid pgn: " + listener.getErrors());
    }

    assertThat(listener.getGames(), hasSize(1));
    return listener.getGames().get(0);
  }

  private static boolean errorsOccurred(ListMaintainingPgnParserListener listener) {
    return listener.getErrors().stream()
        .map(PgnParserError::getType)
        .anyMatch(not(PgnParserError.Type.UNEXPECTED_GAME_END::equals));
  }

  private static String stripped(String notation) {
    return notation.replace("*", "").replaceAll("\\s", "");
  }

}

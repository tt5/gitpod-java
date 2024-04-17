package ce.chess.dockfish.adapter.common.chess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ce.chess.dockfish.domain.model.result.GamePosition;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import raptor.chess.Game;
import raptor.chess.pgn.LenientPgnParserListener;
import raptor.chess.pgn.PgnHeader;

class GamePositionServiceTest {
  private static LogCaptor logCaptor;

  @BeforeAll
  public static void setupLogCaptor() {
    logCaptor = LogCaptor.forClass(LenientPgnParserListener.class);
  }

  @AfterEach
  public void clearLogCaptor() {
    logCaptor.clearLogs();
  }

  @AfterAll
  public static void closeLogCaptor() {
    logCaptor.resetLogLevel();
    logCaptor.close();
  }

  private static final String DEFAULT_HEADER = """
      [Event "?"]
      [Site "?"]
      [Date "?"]
      [Round "?"]
      [White "?"]
      [Black "?"]
      [Result "*"]

      """;

  private GamePositionService cut;

  @BeforeEach
  void setUp() {
    cut = new GamePositionService();
  }

  @ParameterizedTest
  @CsvSource({
      "1. Nf3 Nf6 2. c4 g6 3. d4 Bg7 4. g3 O-O 5. Bg2 c6 6. Nc3 d5 7. cxd5 cxd5 8. Ne5 e6 *, true, 16",
      "1. Nf3 Nf6 2. c4 g6 3. d4 Bg7 4. g3 O-O 5. Bg2 c6 6. Nc3 d5 7. cxd5 cxd5 8. Ne5 e6*, true, 16",
      "1. Nf3 Nf6 2. c4 g6 3. d4 Bg7 4. g3 O-O 5. Bg2 c6 6. Nc3 d5 7. cxd5 cxd5 8. Ne5 e6, true, 16",
      "*, true, 0",
      "1. e4 , false, 1",
      "1.e4, false, 1"
  })
  void givenValidPgnThenCreate(String pgn, boolean isWhite, int plies) {
    GamePosition gamePosition = cut.createFrom(pgn);

    assertThat(stripped(gamePosition.getNotation()), is(stripped(pgn)));
    assertThat(gamePosition.isWhitesMove(), is(isWhite));
    assertThat(gamePosition.getLastMovePly(), is(plies));
    assertThat(gamePosition.getPgn(), is(equalTo(DEFAULT_HEADER + gamePosition.getNotation())));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      """
          [Event "ICCF World Cup 23 Pre 12"]
          [Site "ICCF"]
          [Date "2020.9.30"]
          [Round "?"]
          [White "Němec, Zdeněk (*1960)"]
          [Black "Ernst, Christoph"]
          [Result ""]
          [WhiteElo "2170"]
          [BlackElo "2265"]
          1.c4 Nf6 2.g3 c6 3.Bg2 d5 4.Nf3 g6 5.b3 Bg7 6.Bb2 O-O 7.O-O Re8 8.d4 Ne4 9.e3 a5 10.Nbd2 Bf5 11.Nh4 Nxd2
          12.Qxd2 Be6 13.Rfc1 a4 14.cxd5 Bxd5 15.e4 Be6 16.Nf3 Qb6 17.Bc3 axb3 18.Rab1 Ra3 19.Bf1 Nd7 20.axb3 Rxb3
          21.Ba5 Qa7 22.d5 Rxf3 23.dxe6 Ne5 24.Bb6 Qa4 25.Qc2 *""",
      """
          [Event "ICCF World Cup 23 Pre 12"]
          [Site "ICCF"]
          [Date "2020.9.30"]
          [Round "?"]
          [White "Andersen, Lars Kirstein"]
          [Black "Ernst, Christoph"]
          [Result ""]
          [WhiteElo "2022"]
          [BlackElo "2265"]
          1.Nf3 Nf6 2.c4 e6 3.g3 d5 4.Bg2 dxc4 5.Qa4+ Bd7 6.Qxc4 c5 7.Ne5 Qc8 8.Qd3 Nc6 9.Nxd7 Nxd7 10.O-O Be7
          11.b3 Qc7 12.Bb2 O-O 13.Nc3 Rfd8 14.Rfd1 Nf6 15.Qc2 Nb4 16.Qc1 Rac8 17.d3 Rd7 18.Qd2 b6 19.Rac1 Nc6
          20.e3 Nb4 21.Qe2 Qd8 22.Bf1 Nfd5 23.a3 Nxc3 24.Rxc3 Nd5 25.Rc4 b5 26.Rc2 c4 27.bxc4 bxc4 28.Ba1 Nb6
          29.d4 *""",
      """
          [Event "ICCF World Cup 23 Pre 12"]
          [Site "ICCF"]
          [Date "2020.9.30"]
          [Round "?"]
          [White "Ernst, Christoph"]
          [Black "McCartney, Patrick J."]
          [Result ""]
          [WhiteElo "2265"]
          [BlackElo "1922"]
          1.d4 e6 2.c4 Nf6 3.g3 Bb4+ 4.Bd2 a5 5.Bg2 d6 6.Nf3 Nbd7 7.O-O e5 8.Bxb4 axb4 9.Qb3 exd4 10.Nxd4 O-O
          11.Nc2 c5 12.a3 Ne5 13.axb4 Rxa1 14.Nxa1 Be6 15.bxc5 Nxc4 16.Qb4 dxc5 17.Qxc5 Nxb2 18.Bxb7 Qb8 19.Qb4 Nc4
          20.Nc2 Qe5 21.Nc3 h5 22.Ra1 h4 23.Ne1 Qd6 24.Qxd6 Nxd6 25.Bg2 hxg3 26.hxg3 Rc8 27.Nd1 g6 *""",
      """
          [Event "ICCF World Cup 23 Pre 12"]
          [Site "ICCF"]
          [Date "2020.9.30"]
          [Round "?"]
          [White "Ernst, Christoph"]
          [Black "Crapulli, Giuseppe"]
          [Result ""]
          [WhiteElo "2265"]
          [BlackElo "2133"]
          1.d4 Nf6 2.c4 e6 3.Nf3 b6 4.g3 c6 5.Bg2 Be7 6.Nc3 d5 7.Ne5 O-O 8.O-O Ba6 9.Re1 Bxc4 10.Nxc4 dxc4
          11.e3 Nd5 12.a4 Nb4 13.Qe2 Nd3 14.Rd1 a6 15.Rb1 Qc7 16.b3 Nxc1 17.Rdxc1 cxb3 18.Qb2 Qb7 19.Qxb3 Nd7
          20.Ne2 Rac8 21.Nf4 Kh8 22.Rc3 b5 23.Qc2 b4 24.a5 Nf6 *"""
  })
  void checkSomeIccfGames(String pgnString) {
    Game game = cut.raptorGameFor(pgnString);
    assertThat(stripped(game.toPgn()), is(stripped(pgnString)));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "1. Nf3 Nf6 2. c4 g6 3. d4 Bg7 4. g3 O-O 5. Bg2 c6 6. Nc3 d5 7. cxd5 cxd5 8. Ne5 e6 9. Kh8 *",
      "1 e4",
      "1. e4 2. d4 *",
      "1. e4 *\n 1. d4 *"
  })
  void givenInvalidPgnThenThrow(String pgn) {
    logCaptor.disableLogs();
    assertThrows(IllegalArgumentException.class, () -> cut.createFrom(pgn));
    logCaptor.resetLogLevel();
  }

  @Nested
  class GivenComplexGameWithCastlesCheckConversion {
    String header =
        """
            [Event "Cro/Cup9/Final (CRO)"]
            [Site "ICCF"]
            [Date "2018.11.15"]
            [Round "-"]
            [White "xxx, yyy"]
            [Black "ccc, ddd"]
            [Result "1/2-1/2"]
            [WhiteElo "2306"]
            [BlackElo "2350"]
            [ECO "B99l"]

            """;
    String pgn =
        "1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 a6 6.Bg5 e6 7.f4 Be7 8.Qf3 Nbd7 9.O-O-O Qc7 10.Bd3 b5 "
            + "11.Rhe1 Bb7 12.Qg3 h6 13.Bxf6 Bxf6 14.Bxb5 axb5 15.Ndxb5 Qb6 16.Rxd6 Bc6 17.e5 Be7 18.Qxg7 Rf8 "
            + "19.f5 Qf2 20.Rdd1 Rc8 21.fxe6 Bxb5 22.Rf1 Bxf1 23.exd7+ Kd8 24.dxc8=Q+ Kxc8 25.Qg4+ Kb8 26.Nd5 Be2 "
            + "27.Qd7 Bg5+ 28.Kb1 Qc5 29.b4 Rd8 30.Qxf7 Qc8 31.Rd4 Bc4 32.Kb2 Bxa2 33.h4 Bxh4 1/2-1/2\n";

    @Test
    void canBeParsedToGameNotation() {
      GamePosition gamePosition = cut.createFrom(header + pgn);

      assertThat(stripped(gamePosition.getNotation()), is(stripped(pgn)));
      assertThat(gamePosition.isWhitesMove(), is(true));
      assertThat(gamePosition.getLastMovePly(), is(66));
      assertThat(gamePosition.getFen(), is("1kqr4/5Q2/7p/3NP3/1P1R3b/8/bKP3P1/8 w - - 0 34"));
    }

    @Test
    void doesSetUpdateSanState() {
      assertThat(cut.raptorGameFor(header + pgn).isInState(Game.UPDATING_SAN_STATE), is(true));
    }

    @Test
    void canBeParsedToPgn() {
      GamePosition gamePosition = cut.createFrom(header + pgn);

      String actualPgn = gamePosition.getPgn();
      assertThat(stripped(actualPgn), is(stripped(header + pgn)));
    }

  }

  @ParameterizedTest
  @CsvSource({
      "r2q1rk1/p3bppp/bp2p3/3pP3/5Bn1/5BP1/PPQ1PP1P/RN1R2K1 b - - 2 14, false, 27",
      "r2q1rk1/p3b1pp/bp2p3/3pPp2/5Bn1/5BP1/PPQ1PP1P/RN1R2K1 w - f6 0 15, true, 28"
  })
  void givenValidFenThenCreate(String fen, boolean whitesMove, int ply) {
    GamePosition gamePosition = cut.createFromFen(fen);

    assertThat(gamePosition.getPgn(), containsString("*"));
    assertThat(gamePosition.getFen(), is(equalTo(fen)));
    assertThat(gamePosition.getLastMovePly(), is(equalTo(ply)));
    assertThat(gamePosition.isWhitesMove(), is(equalTo(whitesMove)));
  }

  @Test
  void givenFenThenSetFenHeader() {
    GamePosition gamePosition = cut.createFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
    String gamePositionPgn = gamePosition.getPgn();

    assertThat(gamePositionPgn,
        containsString("[FEN \"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1\"]"));

    Game raptorGame = cut.raptorGameFor(gamePositionPgn);
    assertThat(raptorGame.getHeader(PgnHeader.FEN),
        is(equalTo("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")));
    assertThat(raptorGame.toPgn(), is(equalTo(gamePositionPgn)));

  }

  private static String stripped(String notation) {
    return notation.replace("*", "").replaceAll("\\s", "");
  }

}

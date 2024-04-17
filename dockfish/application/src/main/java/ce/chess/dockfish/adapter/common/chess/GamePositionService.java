package ce.chess.dockfish.adapter.common.chess;

import static java.util.function.Predicate.not;

import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.usecase.out.chess.CreateGamePosition;

import com.google.common.base.Strings;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import raptor.chess.Game;
import raptor.chess.GameFactory;
import raptor.chess.Result;
import raptor.chess.pgn.ListMaintainingPgnParserListener;
import raptor.chess.pgn.PgnHeader;
import raptor.chess.pgn.PgnParser;
import raptor.chess.pgn.PgnParserError;
import raptor.chess.pgn.SimplePgnParser;

@ApplicationScoped
@Log4j2
public class GamePositionService implements CreateGamePosition {

  @Override
  public GamePosition createFrom(String pgn) {
    Game raptorGame = raptorGameFor(pgn);
    return GamePosition.builder()
        .pgn(raptorGame.toPgn())
        .fen(raptorGame.toFen())
        .lastMovePly(raptorGame.getHalfMoveCount())
        .whitesMove(raptorGame.isWhitesMove())
        .build();
  }

  public GamePosition createFromFen(String fen) {
    Game raptorGame = raptorGameForFen(fen);
    raptorGame.setHeader(PgnHeader.FEN, fen);
    return GamePosition.builder()
        .pgn(raptorGame.toPgn())
        .fen(raptorGame.toFen())
        .lastMovePly(raptorGame.getHalfMoveCount())
        .whitesMove(raptorGame.isWhitesMove())
        .build();
  }

  public Game raptorGameForFen(String fen) {
    Game raptorGame = GameFactory.createFromFen(fen);
    raptorGame.addState(Game.UPDATING_SAN_STATE);
    return raptorGame;
  }

  public Game raptorGameFor(String pgnIn) {
    String pgnString = fixPgnString(pgnIn);
    PgnParser parser = new SimplePgnParser(pgnString);
    ListMaintainingPgnParserListener listener = new ListMaintainingPgnParserListener();
    parser.addPgnParserListener(listener);

    parser.parse();

    if (errorsOccurred(listener)) {
      throw new IllegalArgumentException("Invalid pgn: " + listener.getErrors());
    }

    int countGames = listener.getGames().size();
    Game result = switch (countGames) {
      case 0 -> {
        log.warn("No game found. Create starting position");
        yield GameFactory.createStartingPosition();
      }
      case 1 -> listener.getGames().get(0);
      default -> {
        log.warn("Found games {}", listener.getGames());
        throw new IllegalArgumentException("Expected valid pgn for exactly one game, but found " + countGames);
      }
    };
    result.addState(Game.UPDATING_SAN_STATE);
    return result;
  }

  private static boolean errorsOccurred(ListMaintainingPgnParserListener listener) {
    return listener.getErrors().stream()
        .map(PgnParserError::getType)
        .anyMatch(not(PgnParserError.Type.UNEXPECTED_GAME_END::equals));
  }

  private static String fixPgnString(String pgnString) {
    if (Strings.isNullOrEmpty(pgnString)) {
      throw new IllegalArgumentException("Input PGN must not be empty");
    }
    String pgnOut = pgnString.strip().replaceAll("(\\b)\\*", " *");
    if (!pgnOut.startsWith("[")) {
      pgnOut = "[Event \" \"]\r\n" + pgnOut;
    }
    boolean hasResult = Arrays.stream(Result.values())
        .map(Result::getDescription)
        .anyMatch(pgnOut::endsWith);
    if (!hasResult) {
      pgnOut = pgnOut + " *\n";
    }
    return pgnOut;
  }

}

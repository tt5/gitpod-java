package ce.chess.dockfish.usecase.out.chess;

import ce.chess.dockfish.domain.model.result.GamePosition;

public interface CreateGamePosition {
  GamePosition createFrom(String pgn);
}

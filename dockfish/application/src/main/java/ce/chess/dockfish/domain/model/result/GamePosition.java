package ce.chess.dockfish.domain.model.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class GamePosition {

  String pgn;

  int lastMovePly;

  boolean whitesMove;

  String fen;

  public String getNotation() {
    return pgn.replaceAll("(?m)^\\[.*(?:\\r?\\n)?", "").replaceAll("\\r\\n|\\r|\\n", " ").trim();
  }
}

package ce.chess.dockfish.domain.model.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder
@AllArgsConstructor
public class Variation {
  int pvId;

  String moves;

  Score score;

  int depth;

  AnalysisTime time;

  GamePosition gamePosition;

  public String getPgn() {
    return gamePosition.getPgn();
  }

  public String shortRepresentation() {
    return "[p" + pvId + '=' + firstMove() + '/' + score.toString() + ']';
  }

  public String firstMove() {
    return firstMove(moves);
  }

  static String firstMove(String moveList) {
    boolean numberFound = false;
    boolean moveFound = false;
    StringBuilder builder = new StringBuilder();
    for (String part : moveList.split(" ")) {
      if (!numberFound && Character.isDigit(part.charAt(0))) {
        builder.append(part).append(' ');
        numberFound = true;
      }
      if ("...".equals(part)) {
        builder.append(part).append(' ');
      }
      if (!moveFound && Character.isAlphabetic(part.charAt(0))) {
        builder.append(part);
        moveFound = true;
      }
    }
    return builder.toString();
  }
}

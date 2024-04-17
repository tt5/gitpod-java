package ce.chess.dockfish.domain.model.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Score {

  int centiPawns;

  public static Score fromCentiPawns(int centiPawns) {
    return new Score(centiPawns);
  }

  @Override
  public String toString() {
    String score = BigDecimal.valueOf(centiPawns / 100.)
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString();
    return (centiPawns >= 0 ? "+" : "") + score;
  }


}

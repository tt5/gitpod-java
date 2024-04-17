package ce.chess.dockfish.domain.model.result;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class UciState {
  long kiloNodes;

  long kiloNodesPerSecond;

  long tbHits;

  @Singular
  Set<String> infoStrings;

  public String shortRepresentation() {
    return "[Uci: " + kiloNodes + "kN, " + kiloNodesPerSecond + "kN/s, " + tbHits + "tbHits]";
  }
}

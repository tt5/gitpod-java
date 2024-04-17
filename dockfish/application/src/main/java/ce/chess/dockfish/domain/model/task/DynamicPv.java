package ce.chess.dockfish.domain.model.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class DynamicPv {
  int requiredDepth;

  int cutOffCentiPawns;

  int keepMinPv;
}

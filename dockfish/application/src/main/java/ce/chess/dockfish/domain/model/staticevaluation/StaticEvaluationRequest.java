package ce.chess.dockfish.domain.model.staticevaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class StaticEvaluationRequest {
  String fen;
}

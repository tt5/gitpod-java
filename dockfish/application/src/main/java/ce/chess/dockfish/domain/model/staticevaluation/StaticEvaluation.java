package ce.chess.dockfish.domain.model.staticevaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class StaticEvaluation {
  StaticEvaluationRequest request;

  String evaluation;
}

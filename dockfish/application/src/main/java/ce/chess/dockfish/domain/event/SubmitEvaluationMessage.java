package ce.chess.dockfish.domain.event;

import ce.chess.dockfish.domain.model.result.EvaluationMessage;

import lombok.Value;

@Value
public class SubmitEvaluationMessage {
  EvaluationMessage evaluationMessage;
}

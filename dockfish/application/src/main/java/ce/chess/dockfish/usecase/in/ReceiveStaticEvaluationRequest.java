package ce.chess.dockfish.usecase.in;

import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;

public interface ReceiveStaticEvaluationRequest {
  void createAndPublishEvaluation(StaticEvaluationRequest request);
}

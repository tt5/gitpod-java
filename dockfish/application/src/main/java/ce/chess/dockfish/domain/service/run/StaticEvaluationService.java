package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluation;
import ce.chess.dockfish.domain.model.staticevaluation.StaticEvaluationRequest;
import ce.chess.dockfish.usecase.in.ReceiveStaticEvaluationRequest;
import ce.chess.dockfish.usecase.out.engine.LockEngine;
import ce.chess.dockfish.usecase.out.engine.StartStaticEvaluation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class StaticEvaluationService implements ReceiveStaticEvaluationRequest {

  @Inject
  LockEngine lockEngine;

  @Inject
  StartStaticEvaluation startStaticEvaluation;

  @Inject
  Event<StaticEvaluation> resultPublisher;

  @Override
  public void createAndPublishEvaluation(StaticEvaluationRequest request) {
    lockEngine.acquireLock();
    try {
      String evaluation = startStaticEvaluation.retrieveStaticEvaluation(request.getFen());
      resultPublisher.fire(new StaticEvaluation(request, evaluation));
    } finally {
      lockEngine.releaseLock();
    }
  }

}

package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.event.AnalysisFinished;
import ce.chess.dockfish.domain.event.SubmitEvaluationMessage;
import ce.chess.dockfish.domain.service.query.EvaluationMessageService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
public class AnalysisFinishedHandler {

  @Inject
  EvaluationMessageService evaluationMessageService;

  @Inject
  Event<SubmitEvaluationMessage> submitEvaluationMessageEvent;

  public void publishFinalEvaluation(@Observes AnalysisFinished event) {
    evaluationMessageService.getLastEvaluationMessage(event.getTaskId())
        .ifPresentOrElse(
            evaluation -> submitEvaluationMessageEvent.fire(new SubmitEvaluationMessage(evaluation)),
            () -> logNoEvaluationFound(event));
  }

  private void logNoEvaluationFound(AnalysisFinished event) {
    log.warn("No evaluation message found for taskId {}", event::getTaskId);
  }


}

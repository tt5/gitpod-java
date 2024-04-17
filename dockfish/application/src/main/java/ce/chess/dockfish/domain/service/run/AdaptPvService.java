package ce.chess.dockfish.domain.service.run;

import static java.lang.Math.max;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.DynamicPv;
import ce.chess.dockfish.usecase.out.engine.ReducePv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
public class AdaptPvService {
  @Inject
  ReducePv reducePv;

  public void adaptPv(Evaluation evaluation, AnalysisRun task) {
    task.dynamicPv()
        .filter(taskConfiguration -> taskConfiguration.getRequiredDepth() <= evaluation.maxDepth())
        .ifPresent(taskConfiguration -> apply(taskConfiguration, evaluation));
  }

  private void apply(DynamicPv taskConfiguration, Evaluation evaluation) {
    int keepAtLeastPvFromConfiguration = taskConfiguration.getKeepMinPv();
    int currentPv = evaluation.sizeOfCurrentVariations();
    if (currentPv > keepAtLeastPvFromConfiguration) {
      int cutoffConfiguration = taskConfiguration.getCutOffCentiPawns();
      int newPv = max(evaluation.determineNumberOfGoodPv(cutoffConfiguration), keepAtLeastPvFromConfiguration);
      if (newPv < currentPv) {
        doReducePv(newPv, evaluation);
      }
    }
  }

  private void doReducePv(int newPv, Evaluation evaluation) {
    evaluation.getVariations().stream()
        .skip(newPv)
        .forEach(this::logVariation);
    log.info("Keep running with {} variations", () -> evaluation.getVariations().size());

    reducePv.reducePvTo(newPv);
  }

  private void logVariation(Variation variation) {
    log.info("Stop analysis on {} with last variation: {}", variation::shortRepresentation, variation::getMoves);
  }

}

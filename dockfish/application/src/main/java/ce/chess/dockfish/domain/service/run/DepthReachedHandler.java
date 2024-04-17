package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.event.DepthReached;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;
import ce.chess.dockfish.usecase.out.db.TaskRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
public class DepthReachedHandler {

  private final EvaluationRepository evaluationRepository;
  private final TaskRepository taskRepository;
  private final AdaptPvService adaptPvService;

  @Inject
  DepthReachedHandler(EvaluationRepository evaluationRepository, TaskRepository taskRepository,
                      AdaptPvService adaptPvService) {
    this.evaluationRepository = Objects.requireNonNull(evaluationRepository);
    this.taskRepository = Objects.requireNonNull(taskRepository);
    this.adaptPvService = Objects.requireNonNull(adaptPvService);
  }

  public void newDepthReached(@Observes DepthReached event) {
    evaluationRepository.findByTaskIdMaxCreated(event.getTaskId()).ifPresentOrElse(
        evaluation -> taskRepository.findByTaskId(event.getTaskId()).ifPresentOrElse(
            task -> {
              log(evaluation, task);
              adaptPvService.adaptPv(evaluation, task);
            },
            () -> log.warn("Task not found")),
        () -> log.warn("Evaluation not found"));
  }

  private void log(Evaluation evaluation, AnalysisRun task) {
    List<Variation> variations = evaluation.getVariations();
    int currentDepth = variations.getFirst().getDepth();
    String variationLog = variations.stream()
        .filter(variation -> variation.getDepth() == currentDepth)
        .map(Variation::shortRepresentation)
        .collect(Collectors.joining());
    log.info("d{}{},{},{},{},{} {}",
        evaluation::maxDepth,
        () -> variationLog,
        () -> evaluation.getUciState().shortRepresentation(),
        () -> task.name().orElse(""),
        task::engineProgramName,
        evaluation::analysisTime,
        () -> task.estimatedCompletionTime()
            .map(t -> "finishes at " + DateTimeFormatter.ISO_LOCAL_TIME.format(t)).orElse("")
    );
  }
}

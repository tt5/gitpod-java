package ce.chess.dockfish.domain.service.query;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.EngineInformation;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.in.QueryEvaluation;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;
import ce.chess.dockfish.usecase.out.db.TaskRepository;
import ce.chess.dockfish.usecase.out.engine.QueryEngine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class EvaluationMessageService implements QueryEvaluation {
  @Inject
  TaskRepository taskRepository;

  @Inject
  EvaluationRepository evaluationRepository;

  @Inject
  EngineInformationReceivedRepository engineInformationRepository;

  @Inject
  QueryEngine queryEngine;

  @Inject
  Config config;

  @Override
  public Optional<EvaluationMessage> getLastEvaluationMessage() {
    return taskRepository.findLatest()
        .map(AnalysisRun::taskId)
        .flatMap(this::getLastEvaluationMessage);
  }

  @Override
  public Optional<EvaluationMessage> getLastEvaluationMessage(TaskId taskId) {
    return evaluationRepository.findByTaskIdMaxCreated(taskId)
        .map(latestEvaluation -> {
          AnalysisRun task = taskRepository.findByTaskId(taskId)
              .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

          LocalDateTime lastAlive = engineInformationRepository.findByTaskIdMaxOccurredOn(taskId)
              .map(EngineInformationReceived::getOccurredOn)
              .orElseGet(latestEvaluation::getCreated);

          List<EngineInformation> latestEvents =
              engineInformationRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(taskId).stream()
                  .map(EngineInformationReceived::toModel)
                  .toList();

          List<String> history = evaluationRepository.findByTaskId(taskId).stream()
              .sorted(Comparator.comparingInt(Evaluation::maxDepth).reversed())
              .limit(20)
              .filter(evaluation -> evaluation.maxDepth() > 15)
              .map(Evaluation::shortForm)
              .toList();

          return EvaluationMessage.builder()
              .taskName(task.name().orElse(null))
              .reference(task.reference())
              .analysedPgn(task.startingPosition().getPgn())
              .analysedFen(task.startingPosition().getFen())
              .analysedPly(task.startingPosition().getLastMovePly())
              .uciEngineName(task.uciEngineName().orElseGet(task::engineProgramName))
              .taskDepth(task.maxDepth().orElse(null))
              .taskDuration(task.maxDuration().orElse(null))
              .hostname(config.getOptionalValue("hostname", String.class).orElse("hostname"))
              .status(queryEngine.getJobStatus(taskId))
              .evaluation(latestEvaluation)
              .taskStarted(task.created())
              .lastEvaluation(latestEvaluation.getCreated())
              .lastAlive(lastAlive)
              .latestEvents(latestEvents)
              .history(history)
              .build();
        });
  }

  @Override
  public List<TaskId> getAllTaskIds() {
    return evaluationRepository.listTaskIds();
  }

  @Override
  public List<Evaluation> getAllEvaluations() {
    return taskRepository.findLatest()
        .map(AnalysisRun::taskId)
        .map(this::getAllEvaluations)
        .orElseGet(List::of);
  }

  @Override
  public List<Evaluation> getAllEvaluations(TaskId taskId) {
    return evaluationRepository.findByTaskId(taskId);
  }
}

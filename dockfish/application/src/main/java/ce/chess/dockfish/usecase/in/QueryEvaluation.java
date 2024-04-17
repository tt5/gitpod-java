package ce.chess.dockfish.usecase.in;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.List;
import java.util.Optional;

public interface QueryEvaluation {
  Optional<EvaluationMessage> getLastEvaluationMessage();

  Optional<EvaluationMessage> getLastEvaluationMessage(TaskId taskId);

  List<TaskId> getAllTaskIds();

  List<Evaluation> getAllEvaluations();

  List<Evaluation> getAllEvaluations(TaskId taskId);
}

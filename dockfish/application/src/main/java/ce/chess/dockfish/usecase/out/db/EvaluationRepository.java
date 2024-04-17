package ce.chess.dockfish.usecase.out.db;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository {
  void save(Evaluation evaluation);

  List<TaskId> listTaskIds();

  Optional<Evaluation> findByTaskIdMaxCreated(TaskId taskId);

  List<Evaluation> findByTaskId(TaskId taskId);
}

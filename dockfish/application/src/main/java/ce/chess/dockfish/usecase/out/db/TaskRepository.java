package ce.chess.dockfish.usecase.out.db;

import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.Optional;

public interface TaskRepository {

  void save(AnalysisRun task);

  Optional<AnalysisRun> findLatest();

  Optional<AnalysisRun> findByTaskId(TaskId taskId);

  boolean hasDuplicate(AnalysisRun engineTask);
}

package ce.chess.dockfish.usecase.out.engine;

import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.TaskId;

public interface QueryEngine {
  JobStatus getJobStatus(TaskId taskId);

  boolean uciEngineIsRunning();
}

package ce.chess.dockfish.usecase.in;

import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

public interface QueryAnalysis {
  JobStatus getJobStatus(TaskId taskId);

  AnalysisRun getTaskDetails(TaskId taskId);
}

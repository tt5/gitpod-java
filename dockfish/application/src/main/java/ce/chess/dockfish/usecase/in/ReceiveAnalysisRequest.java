package ce.chess.dockfish.usecase.in;

import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.Optional;

public interface ReceiveAnalysisRequest {
  Optional<TaskId> startAsync(AnalysisRun task);

  Optional<TaskId> startSync(AnalysisRun task);
}

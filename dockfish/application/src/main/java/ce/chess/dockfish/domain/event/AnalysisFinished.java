package ce.chess.dockfish.domain.event;

import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class AnalysisFinished {
  @NonNull
  TaskId taskId;

  @NonNull
  Instant occurredOn;

}

package ce.chess.dockfish.domain.event;

import ce.chess.dockfish.domain.model.task.TaskId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class DepthReached {
  @NonNull
  TaskId taskId;

  @NonNull
  Integer depth;
}

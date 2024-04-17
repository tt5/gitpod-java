package ce.chess.dockfish.domain.model.task;

import com.google.common.base.Strings;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class TaskId {
  String rawId;

  public static TaskId createNew() {
    return new TaskId(UUID.randomUUID().toString().toLowerCase(Locale.getDefault()));
  }

  public boolean matches(TaskId other) {
    return !Strings.isNullOrEmpty(other.getRawId()) && getRawId().startsWith(other.getRawId());
  }

  public static boolean isPresent(TaskId taskId) {
    return taskId != null && taskId.getRawId() != null && !taskId.getRawId().isEmpty() && !taskId.getRawId().isBlank();
  }
}

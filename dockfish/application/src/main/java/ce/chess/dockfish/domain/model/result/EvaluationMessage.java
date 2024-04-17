package ce.chess.dockfish.domain.model.result;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder
@AllArgsConstructor
public class EvaluationMessage {

  String taskName;

  String reference;

  String analysedPgn;

  String analysedFen;

  Integer analysedPly;

  String uciEngineName;

  Integer taskDepth;

  Duration taskDuration;

  String hostname;

  JobStatus status;

  Evaluation evaluation;

  LocalDateTime taskStarted;

  LocalDateTime lastEvaluation;

  LocalDateTime lastAlive;

  @Singular
  List<EngineInformation> latestEvents;

  @Singular("historyEntry")
  List<String> history;

  public Optional<String> getTaskName() {
    return Optional.ofNullable(taskName);
  }

}

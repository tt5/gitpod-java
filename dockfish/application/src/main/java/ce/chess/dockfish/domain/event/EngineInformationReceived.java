package ce.chess.dockfish.domain.event;

import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.EngineInformation;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder(toBuilder = true)
@AllArgsConstructor
public class EngineInformationReceived {

  @NonNull
  TaskId taskId;

  int multiPv;

  int depth;

  long time;

  int score;

  long nodes;

  long nodesPerSecond;

  long tbHits;

  String pgn;

  String lineSan;

  int calculatedPlies;

  LocalDateTime occurredOn;

  @Singular
  Set<String> infoStrings;

  public long kiloNodes() {
    return nodes / 1000;
  }

  public long kiloNodesPerSecond() {
    return nodesPerSecond / 1000;
  }

  public boolean hasGame() {
    return pgn != null && !pgn.isEmpty();
  }

  public static Predicate<EngineInformationReceived> matching(TaskId taskId) {
    return i -> i.getTaskId().matches(taskId);
  }

  public EngineInformation toModel() {
    return EngineInformation.builder()
        .multiPv(this.getMultiPv())
        .lineSan(this.getLineSan())
        .score(this.getScore())
        .time(AnalysisTime.fromMilliSeconds(this.getTime()).formattedAsTime())
        .depth(this.getDepth())
        .occurredOn(this.getOccurredOn())
        .build();
  }

}

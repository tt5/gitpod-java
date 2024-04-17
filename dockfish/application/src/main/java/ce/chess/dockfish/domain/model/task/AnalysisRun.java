package ce.chess.dockfish.domain.model.task;

import ce.chess.dockfish.domain.model.result.GamePosition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder(toBuilder = true)
@AllArgsConstructor
@Accessors(fluent = true)
public class AnalysisRun {
  TaskId taskId;

  String name;

  String reference;

  @NonNull
  String engineProgramName;

  @With
  String uciEngineName;

  String hostname;

  @NonNull
  GamePosition startingPosition;

  @NonNull
  Integer initialPv;

  Integer maxDepth;

  Duration maxDuration;

  @Singular
  List<EngineOption> engineOptions;

  boolean useSyzygyPath;

  DynamicPv dynamicPv;

  LocalDateTime created;

  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  public Optional<String> uciEngineName() {
    return Optional.ofNullable(uciEngineName);
  }

  public Optional<Integer> maxDepth() {
    return Optional.ofNullable(maxDepth);
  }

  public Optional<Duration> maxDuration() {
    return Optional.ofNullable(maxDuration);
  }

  public Optional<DynamicPv> dynamicPv() {
    return Optional.ofNullable(dynamicPv);
  }

  public Optional<LocalDateTime> estimatedCompletionTime() {
    return Optional.ofNullable(maxDuration).map(duration -> created().plus(duration));
  }

  public boolean isSameAs(AnalysisRun engineTask) {
    // equals ignoring taskId, uciEngineName, created date
    return Objects.equals(this.name(), engineTask.name())
        && Objects.equals(this.engineProgramName(), engineTask.engineProgramName())
        && Objects.equals(this.startingPosition().getNotation(), engineTask.startingPosition().getNotation())
        && Objects.equals(this.startingPosition().getLastMovePly(), engineTask.startingPosition().getLastMovePly())
        && Objects.equals(this.initialPv(), engineTask.initialPv())
        && Objects.equals(this.maxDepth(), engineTask.maxDepth())
        && Objects.equals(this.maxDuration(), engineTask.maxDuration())
        && Objects.equals(this.engineOptions(), engineTask.engineOptions())
        && this.useSyzygyPath() == engineTask.useSyzygyPath()
        && Objects.equals(this.dynamicPv(), engineTask.dynamicPv());
  }

  public AnalysisRun addOrReplaceOption(EngineOption newOption) {
    Map<String, String> optionMap = engineOptions.stream()
        .collect(Collectors.toMap(EngineOption::getName, EngineOption::getValue));
    optionMap.put(newOption.getName(), newOption.getValue());
    List<EngineOption> newEngineOptions = optionMap.entrySet().stream()
        .map(entry -> new EngineOption(entry.getKey(), entry.getValue()))
        .toList();
    return this.toBuilder().clearEngineOptions().engineOptions(newEngineOptions).build();
  }
}

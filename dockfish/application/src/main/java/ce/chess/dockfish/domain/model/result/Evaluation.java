package ce.chess.dockfish.domain.model.result;

import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder
@AllArgsConstructor
public class Evaluation {

  TaskId taskId;

  LocalDateTime created;

  @Singular
  List<Variation> variations;

  UciState uciState;

  public int sizeOfCurrentVariations() {
    return currentVariations().size();
  }

  public List<Variation> currentVariations() {
    int maxDepth = maxDepth();
    return getVariations().stream()
        .filter(v -> v.getDepth() > maxDepth - 2)
        .toList();
  }

  public boolean hasAllVariationsOfSameDepth() {
    return allVariationsHavingSameDepth().test(this);
  }

  public int maxDepth() {
    return getVariations().stream().mapToInt(Variation::getDepth).max().orElse(0);
  }

  public String analysisTime() {
    return variations.isEmpty() ? "00:00" : variations.get(0).getTime().formattedAsTime();
  }

  public int determineNumberOfGoodPv(int cutoffScore) {
    return determineNumberOfGoodPv(variations, cutoffScore);
  }

  public static int determineNumberOfGoodPv(List<Variation> variations, int cutoffScore) {
    boolean isWhitesMove = variations.get(0).getGamePosition().isWhitesMove();
    int bestScoreOfWhite = variations.stream()
        .mapToInt(v -> v.getScore().getCentiPawns())
        .max().orElse(0);
    int bestScoreOfBlack = variations.stream()
        .mapToInt(v -> v.getScore().getCentiPawns())
        .min().orElse(0);

    if (isWhitesMove) {
      return (int) variations.stream()
          .mapToInt(v -> v.getScore().getCentiPawns())
          .filter(s -> s >= bestScoreOfWhite - cutoffScore)
          .count();
    } else {
      return (int) variations.stream()
          .mapToInt(v -> v.getScore().getCentiPawns())
          .filter(s -> s <= bestScoreOfBlack + cutoffScore)
          .count();
    }
  }

  public static Predicate<Evaluation> matching(TaskId taskId) {
    return e -> e.getTaskId().matches(taskId);
  }

  static Predicate<Evaluation> allVariationsHavingSameDepth() {
    return e -> e.currentVariations().stream()
        .map(Variation::getDepth)
        .distinct()
        .limit(2)
        .count() == 1;
  }

  public String taskIdAndMaxDepth() {
    return taskId.getRawId() + maxDepth();
  }

  public String shortForm() {
    Variation bestVariation = currentVariations().get(0);
    return "d=" + bestVariation.getDepth() + ": "
        + bestVariation.firstMove() + " "
        + bestVariation.getScore().toString() + " "
        + bestVariation.getTime().formattedAsTime();
  }
}

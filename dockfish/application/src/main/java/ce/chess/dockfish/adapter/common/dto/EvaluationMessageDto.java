package ce.chess.dockfish.adapter.common.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Jacksonized
@Builder(toBuilder = true)
public class EvaluationMessageDto {
  String taskName;

  @Schema(description = "reference value from the submitted task")
  String reference;

  String analysedPgn;

  Integer analysedPly;

  String analysedFen;

  String uciEngineName;

  Integer taskDepth;

  String taskDuration;

  String hostname;

  String status;

  EvaluationDto evaluation;

  LocalDateTime taskStarted;

  LocalDateTime lastEvaluation;

  LocalDateTime lastAlive;

  @Singular
  List<EngineEventDto> latestEvents;

  @Singular(value = "addHistory", ignoreNullCollections = true)
  List<String> history;


  @Data
  @Jacksonized
  @Builder(toBuilder = true)
  public static class EvaluationDto {
    String taskId;

    LocalDateTime created;

    @Singular
    List<VariationDto> variations;

    UciStateDto uciState;


    @Data
    @Jacksonized
    @Builder(toBuilder = true)
    public static class VariationDto {
      int pvId;

      String moves;

      String score;

      int depth;

      String time;

      String pgn;

    }

    @Data
    @Jacksonized
    @Builder(toBuilder = true)
    public static class UciStateDto {
      long kiloNodes;

      long kiloNodesPerSecond;

      long tbHits;

      List<String> infoStrings;

    }

  }

  @Data
  @Jacksonized
  @Builder(toBuilder = true)
  public static class EngineEventDto {
    int multiPv;

    String lineSan;

    int score;

    String time;

    int depth;

    LocalDateTime occurredOn;

  }
}

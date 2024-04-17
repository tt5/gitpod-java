package ce.chess.dockfish.adapter.common.dto;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Value
@NonFinal
@Builder(toBuilder = true)
@AllArgsConstructor
@Jacksonized
public class SubmitTaskCommand {

  @Schema(example = "uuid", description = "Optionally assign a fixed TaskId. May be used to trace task status")
  String id;

  @Schema(example = "name")
  String name;

  @Schema(description = "will be passed as is in the answer")
  String reference;

  @Schema(example = "1.d4 d5 2.c4 *")
  String pgn;

  @Schema(description = "FEN position")
  String fen;

  @NonNull
  @Schema(example = "4")
  Integer initialPv;

  @Schema(example = "40")
  Integer maxDepth;

  @Schema(type = SchemaType.STRING,
      example = "PT2H",
      description = "ISO-8601 duration string")
  Duration maxDuration;

  @Singular
  @Schema
  List<EngineOptionDto> options;

  @Schema
  DynamicPvDto dynamicPv;

  @Builder.Default
  @Schema(nullable = true, defaultValue = "stockfish")
  String engineId = "stockfish";

  @Schema(defaultValue = "false")
  boolean useSyzygyPath;

  public void validate() {
    Preconditions.checkArgument(maxDuration != null ^ maxDepth != null,
        "Either Depth or Duration must be given");
    Preconditions.checkArgument(fen != null ^ pgn != null,
        "Either PGN or FEN must be given");
  }

  @Value
  @Builder
  @Jacksonized
  public static class EngineOptionDto {
    @NonNull
    @Schema(example = "Hash")
    String name;

    @NonNull
    @Schema(example = "1024")
    String value;

  }

  @Value
  @Builder
  @Jacksonized
  public static class DynamicPvDto {
    @NonNull
    @Schema(example = "30")
    Integer requiredDepth;

    @NonNull
    @Schema(example = "20")
    Integer cutOffCentiPawns;

    @NonNull
    @Schema(example = "2")
    Integer keepMinPv;

  }
}

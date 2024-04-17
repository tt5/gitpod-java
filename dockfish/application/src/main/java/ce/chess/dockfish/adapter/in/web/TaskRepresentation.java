package ce.chess.dockfish.adapter.in.web;

import ce.chess.dockfish.domain.model.result.JobStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Value
@Builder
@AllArgsConstructor
@Schema(name = "TaskRepresentation")
public class TaskRepresentation {
  String taskId;

  String taskName;

  @Schema(description = "reference attribute from the submitted task")
  String reference;

  LocalDateTime submitted;

  String startingPosition;

  Integer startingMoveNumber;

  String engineProgramName;

  String hostname;

  Integer initialPv;

  Integer maxDepth;

  Duration maxDuration;

  boolean useSyzygyPath;

  LocalDateTime estimatedCompletionTime;

  JobStatus status;

  String link;

}

package ce.chess.dockfish.adapter.common.mapper;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;
import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = QuarkusMappingConfig.class)
public abstract class SubmitTaskCommandMapper {
  @Inject
  GamePositionService gamePositionService;

  @Mapping(target = "engineProgramName", source = "submitTaskCommand.engineId")
  @Mapping(target = "startingPosition", source = "submitTaskCommand", qualifiedByName = "mapStartingPosition")
  @Mapping(target = "engineOptions", source = "submitTaskCommand.options")
  @Mapping(target = "taskId", source = "submitTaskCommand.id")
  @Mapping(target = "engineOption", ignore = true)
  @Mapping(target = "hostname", ignore = true)
  @Mapping(target = "uciEngineName", ignore = true)
  public abstract AnalysisRun toDomainObject(SubmitTaskCommand submitTaskCommand, LocalDateTime created);

  @Named("mapStartingPosition")
  public GamePosition mapStartingPosition(SubmitTaskCommand submitTaskCommand) {
    if (submitTaskCommand.getFen() != null) {
      return gamePositionService.createFromFen(submitTaskCommand.getFen());
    }
    return gamePositionService.createFrom(submitTaskCommand.getPgn());
  }

  public TaskId mapTaskId(String idValue) {
    return new TaskId(idValue);
  }
}

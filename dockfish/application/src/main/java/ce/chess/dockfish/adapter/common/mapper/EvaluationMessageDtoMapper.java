package ce.chess.dockfish.adapter.common.mapper;

import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.result.Score;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = QuarkusMappingConfig.class)
public interface EvaluationMessageDtoMapper {
  @Mapping(target = "latestEvent", ignore = true)
  @Mapping(target = "evaluation.variation", ignore = true)
  @Mapping(target = "taskName", qualifiedByName = "unwrapOptional")
  EvaluationMessageDto toDto(EvaluationMessage evaluationMessage);

  @Mapping(target = "pgn", source = "gamePosition.notation")
  EvaluationMessageDto.EvaluationDto.VariationDto toDto(Variation variation);

  default String map(TaskId taskId) {
    return taskId.getRawId();
  }

  default String map(Score value) {
    return value.toString();

  }

  default String map(AnalysisTime value) {
    return value.formattedAsTime();
  }

  @Named("unwrapOptional")
  default <T> T unwrapOptional(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> optional) {
    return optional.orElse(null);
  }
}

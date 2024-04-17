package ce.chess.dockfish.adapter.common.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.EngineInformation;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.result.Score;
import ce.chess.dockfish.domain.model.result.UciState;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EvaluationMessageDtoMapperTest {
  private static final LocalDateTime NOW = LocalDateTime.now();
  private static final EvaluationMessage evaluationMessage = EvaluationMessage.builder()
      .taskName("taskName")
      .reference("reference_id")
      .analysedPgn("analysedPgn")
      .analysedFen("analysedFen")
      .uciEngineName("uciEngineName")
      .taskDuration(Duration.of(10, ChronoUnit.MINUTES))
      .hostname("testhost")
      .status(JobStatus.NOT_ACTIVE)
      .evaluation(Evaluation.builder()
          .taskId(new TaskId("taskId"))
          .created(NOW)
          .variation(Variation.builder()
              .pvId(1)
              .moves("move")
              .score(Score.fromCentiPawns(-42))
              .depth(25)
              .time(AnalysisTime.fromMinutes(22))
              .gamePosition(new GamePositionService().createFrom("1. e4 e5"))
              .build())
          .uciState(UciState.builder()
              .kiloNodes(100)
              .kiloNodesPerSecond(10)
              .tbHits(1)
              .infoString("some infoString")
              .build())
          .build())
      .lastAlive(NOW)
      .lastEvaluation(NOW)
      .taskStarted(NOW)
      .latestEvent(EngineInformation.builder()
          .multiPv(1)
          .lineSan("lineSan")
          .occurredOn(NOW)
          .depth(22)
          .score(43)
          .time("00:10:00")
          .build())
      .history(List.of("history1", "history2"))
      .build();

  private EvaluationMessageDtoMapper cut;

  @BeforeEach
  void setUp() {
    cut = Mappers.getMapper(EvaluationMessageDtoMapper.class);
  }

  @Test
  void convertsToDomainObject() {
    EvaluationMessageDto actual = cut.toDto(evaluationMessage);

    EvaluationMessageDto expected = EvaluationMessageDto.builder()
        .taskName("taskName")
        .reference("reference_id")
        .analysedPgn("analysedPgn")
        .analysedFen("analysedFen")
        .uciEngineName("uciEngineName")
        .taskDuration("PT10M")
        .hostname("testhost")
        .status("NOT_ACTIVE")
        .evaluation(EvaluationMessageDto.EvaluationDto.builder()
            .taskId("taskId")
            .created(NOW)
            .variation(EvaluationMessageDto.EvaluationDto.VariationDto.builder()
                .pvId(1)
                .moves("move")
                .score("-0.42")
                .depth(25)
                .time("00:22:00")
                .pgn("1. e4 e5 *")
                .build())
            .uciState(EvaluationMessageDto.EvaluationDto.UciStateDto.builder()
                .kiloNodes(100)
                .kiloNodesPerSecond(10)
                .tbHits(1)
                .infoStrings(List.of("some infoString"))
                .build())
            .build())
        .lastAlive(NOW)
        .lastEvaluation(NOW)
        .taskStarted(NOW)
        .latestEvent(EvaluationMessageDto.EngineEventDto.builder()
            .multiPv(1)
            .lineSan("lineSan")
            .occurredOn(NOW)
            .depth(22)
            .score(43)
            .time("00:10:00")
            .build())
        .history(List.of("history1", "history2"))
        .build();

    assertThat(actual, is(equalTo(expected)));

  }


}

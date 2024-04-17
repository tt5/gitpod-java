package ce.chess.dockfish.adapter.common.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;
import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.DynamicPv;
import ce.chess.dockfish.domain.model.task.EngineOption;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SubmitTaskCommandMapperTest {

  private static final LocalDateTime NOW = LocalDateTime.now();

  private SubmitTaskCommandMapper cut;

  @BeforeEach
  void setUp() {
    cut = Mappers.getMapper(SubmitTaskCommandMapper.class);
    cut.gamePositionService = new GamePositionService();
  }

  @Test
  void convertsToDomainObject() {
    SubmitTaskCommand task = SubmitTaskCommand.builder()
        .id("taskId")
        .name("task_name")
        .reference("reference_id")
        .maxDuration(Duration.parse("PT2H30M"))
        .initialPv(3)
        .engineId("engineId")
        .name("name")
        .pgn("1. e4")
        .dynamicPv(SubmitTaskCommand.DynamicPvDto.builder()
            .cutOffCentiPawns(30)
            .keepMinPv(2)
            .requiredDepth(30)
            .build())
        .option(SubmitTaskCommand.EngineOptionDto.builder()
            .name("optionName")
            .value("value")
            .build())
        .build();

    AnalysisRun analysisRun = cut.toDomainObject(task, NOW);

    AnalysisRun expected = AnalysisRun.builder()
        .taskId(new TaskId("taskId"))
        .name("task_name")
        .reference("reference_id")
        .maxDuration(Duration.parse("PT2H30M"))
        .initialPv(3)
        .engineProgramName("engineId")
        .name("name")
        .startingPosition(new GamePositionService().createFrom("1. e4"))
        .dynamicPv(DynamicPv.builder()
            .cutOffCentiPawns(30)
            .keepMinPv(2)
            .requiredDepth(30)
            .build())
        .engineOption(EngineOption.builder()
            .name("optionName")
            .value("value")
            .build())
        .created(NOW)
        .build();
    assertThat(analysisRun, is(equalTo(expected)));
  }

  @Test
  void nameAndIdAndEngineMayBeEmpty() {
    SubmitTaskCommand task = SubmitTaskCommand.builder()
        .maxDuration(Duration.parse("PT2H30M"))
        .initialPv(3)
        .pgn("1. d4")
        .build();

    AnalysisRun analysisRun = cut.toDomainObject(task, NOW);

    assertThat(analysisRun.name(), is(Optional.empty()));
    assertThat(analysisRun.engineProgramName(), is("stockfish"));
  }

  @Test
  void fenWillBeMapped() {
    SubmitTaskCommand task = SubmitTaskCommand.builder()
        .maxDuration(Duration.parse("PT2H30M"))
        .initialPv(1)
        .fen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
        .build();

    AnalysisRun analysisRun = cut.toDomainObject(task, NOW);

    assertThat(analysisRun.startingPosition(), is(GamePosition.builder()
        .pgn("""
            [Event "?"]
            [Site "?"]
            [Date "?"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "*"]
            [FEN "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"]

                *""")
        .fen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
        .lastMovePly(4)
        .whitesMove(true)
        .build()));
  }

}

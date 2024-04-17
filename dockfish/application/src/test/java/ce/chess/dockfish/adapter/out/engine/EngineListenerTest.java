package ce.chess.dockfish.adapter.out.engine;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;

import jakarta.enterprise.event.Event;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import raptor.chess.Game;
import raptor.chess.GameFactory;
import raptor.engine.uci.UCIBestMove;
import raptor.engine.uci.UCIMove;
import raptor.engine.uci.info.BestLineFoundInfo;
import raptor.engine.uci.info.DepthInfo;
import raptor.engine.uci.info.MultiPV;
import raptor.engine.uci.info.NodesPerSecondInfo;
import raptor.engine.uci.info.NodesSearchedInfo;
import raptor.engine.uci.info.ScoreInfo;
import raptor.engine.uci.info.StringInfo;
import raptor.engine.uci.info.TableBaseHitsInfo;
import raptor.engine.uci.info.TimeInfo;

@ExtendWith(MockitoExtension.class)
class EngineListenerTest {

  private static final ScoreInfo SCORE_INFO_42 = new ScoreInfo();
  private static final MultiPV MULTI_PV_1 = new MultiPV("1");
  private static final DepthInfo DEPTH_INFO_32 = new DepthInfo();
  private static final TimeInfo TIME_INFO_2H = new TimeInfo();
  private static final String ENGINE_PROGRAM_NAME = "engine42";

  static {
    SCORE_INFO_42.setValueInCentipawns(42);
    DEPTH_INFO_32.setSearchDepthPlies(32);
    TIME_INFO_2H.setTimeMillis((int) Duration.of(2, HOURS).toMillis());
  }

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private GamePositionService gamePositionService;

  @Mock
  private Event<EngineInformationReceived> engineInformationPublisher;

  @Mock
  private AnalysisRun task;

  @Captor
  private ArgumentCaptor<EngineInformationReceived> eventCaptor;

  @InjectMocks
  private EngineListener cut;

  @Nested
  class WhenUciEngineSendsBestMove {

    @Test
    void nothingIsDoneAnymore() {
      UCIBestMove uciBestMove = new UCIBestMove();
      uciBestMove.setBestMove(new UCIMove("e4"));
      uciBestMove.setPonderMove(new UCIMove("e5"));

      cut.engineSentBestMove(uciBestMove);

      verifyNoInteractions(engineInformationPublisher);
    }
  }

  @Nested
  class WhenUciEngineSendsBestLineFoundInfo {
    private BestLineFoundInfo bestLine;

    @BeforeEach
    void setup() {
      when(task.startingPosition()).thenReturn(new GamePositionService().createFrom("1. e4 *"));
      when(task.taskId()).thenReturn(new TaskId("taskId"));
      when(task.uciEngineName()).thenReturn(Optional.of(ENGINE_PROGRAM_NAME));
      cut.assignTo(task);
    }

    @BeforeEach
    void prepareBestLineInfo() {
      bestLine = new BestLineFoundInfo();
      bestLine.setMoves(new UCIMove("e7e5"), new UCIMove("d2d4"), new UCIMove("d7d5"));
    }

    @Test
    void doesUpdateGame() {
      cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, SCORE_INFO_42, DEPTH_INFO_32, TIME_INFO_2H));

      verify(engineInformationPublisher).fire(eventCaptor.capture());

      EngineInformationReceived result = eventCaptor.getValue();
      assertThat(result.hasGame(), is(true));
      assertThat(result.getLineSan(), is(equalTo("1. ... e5 2. d4 d5")));
      assertThat(result.getPgn(),
          equalTo("""
              [Event "?"]
              [Site "?"]
              [Date "?"]
              [Round "?"]
              [White "?"]
              [Black "?"]
              [Result "*"]

              1. e4 e5 {pv1: -0.42;d32;02:00:00 engine42} 2. d4 d5 {pv1: -0.42;d32;02:00:00 engine42}
              *"""));
      assertThat(result.getCalculatedPlies(), is(equalTo(3)));
    }

    @Nested
    class GivenGameFromFen {
      @BeforeEach
      void setup() {
        GamePosition gamePosition = new GamePositionService().createFromFen(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
        when(task.startingPosition()).thenReturn(gamePosition);
      }

      @Test
      void doesUpdateGame() {
        cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, SCORE_INFO_42, DEPTH_INFO_32, TIME_INFO_2H));

        verify(engineInformationPublisher).fire(eventCaptor.capture());

        EngineInformationReceived result = eventCaptor.getValue();
        assertThat(result.hasGame(), is(true));
        assertThat(result.getLineSan(), is(equalTo("1. ... e5 2. d4 d5")));
        assertThat(result.getPgn(),
            equalTo("""
                [Event "?"]
                [Site "?"]
                [Date "?"]
                [Round "?"]
                [White "?"]
                [Black "?"]
                [Result "*"]
                [FEN "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"]

                1... e5 {pv1: -0.42;d32;02:00:00 engine42} 2. d4 d5 {pv1: -0.42;d32;02:00:00 engine42}
                 *"""));
        assertThat(result.getCalculatedPlies(), is(equalTo(3)));
      }

    }

    @Nested
    class GivenBlackToMove {
      @Test
      void doesCalculatePositiveScoreForBlackToMove() {
        ScoreInfo score = new ScoreInfo();
        score.setValueInCentipawns(42);

        cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, score));
        verify(engineInformationPublisher).fire(eventCaptor.capture());
        EngineInformationReceived result = eventCaptor.getValue();

        assertThat(result.getScore(), is(-42));
      }

      @Test
      void doesCalculateNegativeScoreForBlackToMove() {
        ScoreInfo score = new ScoreInfo();
        score.setValueInCentipawns(-72);

        cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, score));
        verify(engineInformationPublisher).fire(eventCaptor.capture());
        EngineInformationReceived result = eventCaptor.getValue();

        assertThat(result.getScore(), is(72));
      }
    }

    @Nested
    class GivenWhiteToMove {
      @BeforeEach
      void setup() {
        Game game = GameFactory.createStartingPosition();
        game.addState(Game.UPDATING_SAN_STATE);
        game.makeSanMove("e4");
        game.makeSanMove("e5");
        when(task.startingPosition()).thenReturn(new GamePositionService().createFrom(game.toPgn()));
        bestLine = new BestLineFoundInfo();
        bestLine.setMoves(new UCIMove("d2d4"), new UCIMove("d7d5"));

      }

      @Test
      void doesCalculatePositiveScoreForWhiteToMove() {
        ScoreInfo score = new ScoreInfo();
        score.setValueInCentipawns(42);

        cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, score));
        verify(engineInformationPublisher).fire(eventCaptor.capture());
        EngineInformationReceived result = eventCaptor.getValue();

        assertThat(result.getScore(), is(42));
      }

      @Test
      void doesCalculateNegativeScoreForWhiteToMove() {
        ScoreInfo score = new ScoreInfo();
        score.setValueInCentipawns(-72);

        cut.engineSentInfo(List.of(bestLine, MULTI_PV_1, score));
        verify(engineInformationPublisher).fire(eventCaptor.capture());
        EngineInformationReceived result = eventCaptor.getValue();

        assertThat(result.getScore(), is(-72));
      }

    }
  }

  @Nested
  class WhenReceivingUciInfo {
    @BeforeEach
    void prepareCut() {
      when(task.taskId()).thenReturn(new TaskId("taskId"));
      cut.assignTo(task);
    }

    @Test
    void shouldSetDepth() {
      DepthInfo info = new DepthInfo();
      info.setSearchDepthPlies(3);

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().currentDepth, is(3));
    }

    @Test
    void shouldSetTime() {
      TimeInfo info = new TimeInfo();
      info.setTimeMillis(5);

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().currentTime, is(5L));
    }

    @Test
    void shouldSetNodes() {
      NodesSearchedInfo info = new NodesSearchedInfo();
      info.setNodesSearched(100);

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().currentNodes, is(100L));
    }

    @Test
    void shouldSetNodesPerSecond() {
      NodesPerSecondInfo info = new NodesPerSecondInfo();
      info.setNodesPerSecond(200);

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().nodesPerSeconds, is(200L));
    }

    @Test
    void shouldSetTbHits() {
      TableBaseHitsInfo info = new TableBaseHitsInfo();
      info.setNumberOfHits(15);

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().tbHits, is(15L));
    }

    @Test
    void shouldSetStringInfo() {
      StringInfo info = new StringInfo();
      info.setValue("info for you");

      cut.engineSentInfo(List.of(info));

      assertThat(cut.getUciInformation().infoStrings, contains("info for you"));
    }
  }
}

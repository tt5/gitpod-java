package ce.chess.dockfish.adapter.out.engine;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.Score;
import ce.chess.dockfish.domain.model.task.AnalysisRun;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import raptor.chess.Game;
import raptor.chess.GameConstants;
import raptor.chess.Move;
import raptor.chess.MoveList;
import raptor.chess.pgn.Comment;
import raptor.chess.util.GameUtils;
import raptor.engine.uci.UCIBestMove;
import raptor.engine.uci.UCIInfo;
import raptor.engine.uci.UCIInfoListener;
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

@ApplicationScoped
@Log4j2
public class EngineListener implements UCIInfoListener {

  private final UciEngineInformation uciInformation = new UciEngineInformation();
  private AnalysisRun currentTask;

  @Inject
  GamePositionService gamePositionService;

  @Inject
  Event<EngineInformationReceived> engineInformationPublisher;

  public void assignTo(AnalysisRun engineTask) {
    this.currentTask = Objects.requireNonNull(engineTask);
    this.uciInformation.reset();
    log.info("Listening to task: [{}]", engineTask);
  }

  @Override
  public void engineSentBestMove(UCIBestMove uciBestmove) {
    log.info("engineSentBestMove: {}", uciBestmove);
  }

  @Override
  public void engineSentInfo(List<UCIInfo> uciInfos) {
    if (log.isDebugEnabled()) {
      log.debug("Info from engine: {}", uciInfos);
    }

    Objects.requireNonNull(currentTask);

    Optional<BestLineFoundInfo> bestLineFoundInfo = updateUciInformation(uciInfos);

    EngineInformationReceived uciInfoEvent = bestLineFoundInfo
        .map(this::uciEventWithGame)
        .orElseGet(this::uciEventWithoutGame);

    fireEvent(uciInfoEvent);
  }

  private Optional<BestLineFoundInfo> updateUciInformation(List<UCIInfo> uciInfos) {
    BestLineFoundInfo result = null;

    for (UCIInfo uciInfo : uciInfos) {
      if (uciInfo instanceof BestLineFoundInfo bestLineFoundInfo) {
        result = bestLineFoundInfo;
      } else if (uciInfo instanceof DepthInfo depthInfo) {
        uciInformation.currentDepth = depthInfo.getSearchDepthPlies();
      } else if (uciInfo instanceof TimeInfo timeInfo) {
        uciInformation.currentTime = timeInfo.getTimeMillis();
      } else if (uciInfo instanceof NodesSearchedInfo nodesSearchedInfo) {
        uciInformation.currentNodes = nodesSearchedInfo.getNodesSearched();
      } else if (uciInfo instanceof NodesPerSecondInfo npsInfo) {
        uciInformation.nodesPerSeconds = npsInfo.getNodesPerSecond();
      } else if (uciInfo instanceof TableBaseHitsInfo tbInfo) {
        uciInformation.tbHits = tbInfo.getNumberOfHits();
      } else if (uciInfo instanceof MultiPV multiPvInfo) {
        uciInformation.lastMultiPv = multiPvInfo.getId();
      } else if (uciInfo instanceof ScoreInfo scoreInfo) {
        uciInformation.lastScore = scoreInfo.getValueInCentipawns()
            + 9999 * Integer.signum(scoreInfo.getMateInMoves());
      } else if (uciInfo instanceof StringInfo stringInfo) {
        String infoString = stringInfo.getValue();
        log.info("StringInfo: [{}]", infoString);
        uciInformation.infoStrings.add(infoString);
      }
    }
    return Optional.ofNullable(result);
  }

  private EngineInformationReceived uciEventWithoutGame() {
    return EngineInformationReceived.builder()
        .taskId(currentTask.taskId())
        .occurredOn(LocalDateTime.now(ZoneId.systemDefault()))
        .multiPv(uciInformation.lastMultiPv)
        .depth(uciInformation.currentDepth)
        .time(uciInformation.currentTime)
        .nodes(uciInformation.currentNodes)
        .nodesPerSecond(uciInformation.nodesPerSeconds)
        .tbHits(uciInformation.tbHits)
        .score(uciInformation.lastScore)
        .infoStrings(uciInformation.infoStrings)
        .pgn("")
        .lineSan("")
        .calculatedPlies(0)
        .build();
  }

  private EngineInformationReceived uciEventWithGame(BestLineFoundInfo bestLineInfo) {
    Game game = gamePositionService.raptorGameFor(currentTask.startingPosition().getPgn());
    if (!game.isWhitesMove()) {
      uciInformation.lastScore *= -1;
    }
    MoveList raptorMoveList = makeMovesAndReturnMoveList(game, bestLineInfo.getMoves());
    return createBestLineInfoEvent(game, raptorMoveList);
  }

  private MoveList makeMovesAndReturnMoveList(Game raptorGame, List<UCIMove> uciMoves) {
    MoveList raptorMoveList = new MoveList();
    for (UCIMove uciMove : uciMoves) {
      String uciString = uciMove.getValue();
      int startSquare = GameUtils.getSquare(uciString.substring(0, 2));
      int endSquare = GameUtils.getSquare(uciString.substring(2, 4));
      int promotedPiece = 0;
      if (uciString.length() > 4) {
        char pieceChar = Character.toLowerCase(uciString.charAt(4));
        promotedPiece = GameConstants.PIECE_TO_SAN
            .toLowerCase(Locale.getDefault()).indexOf(pieceChar);
      }

      Move raptorMove;
      if (promotedPiece == 0) {
        raptorMove = raptorGame.makeMove(startSquare, endSquare);
      } else {
        raptorMove = raptorGame.makeMove(startSquare, endSquare, promotedPiece);
      }
      raptorMoveList.append(raptorMove);
    }
    Comment comment = createGameComment();
    raptorMoveList.get(0).addAnnotation(comment);
    raptorMoveList.getLast().addAnnotation(comment);

    return raptorMoveList;
  }

  private Comment createGameComment() {
    return new Comment(String.format("pv%d: %s;d%d;%s %s",
        uciInformation.lastMultiPv,
        Score.fromCentiPawns(uciInformation.lastScore),
        uciInformation.currentDepth,
        AnalysisTime.fromMilliSeconds(uciInformation.currentTime).formattedAsTime(),
        currentTask.uciEngineName().orElseGet(currentTask::engineProgramName)));
  }

  private EngineInformationReceived createBestLineInfoEvent(Game game, MoveList raptorMoveList) {
    return uciEventWithoutGame().toBuilder()
        .pgn(game.toPgn())
        .lineSan(moveList2SanString(raptorMoveList))
        .calculatedPlies(raptorMoveList.getSize())
        .build();
  }

  private void fireEvent(EngineInformationReceived uciInfoReceived) {
    engineInformationPublisher.fire(uciInfoReceived);
  }

  private static String moveList2SanString(MoveList raptorMoveList) {
    StringBuilder lineText = new StringBuilder();
    boolean isFirstMove = true;
    for (Move raptorMove : raptorMoveList.asList()) {
      lineText.append(move2String(isFirstMove, raptorMove));
      isFirstMove = false;
    }
    return lineText.toString();

  }

  private static String move2String(boolean isFirstMove, Move raptorMove) {
    StringBuilder lineText = new StringBuilder();
    String moveNumber;
    if (isFirstMove && !raptorMove.isWhitesMove()) {
      moveNumber = raptorMove.getFullMoveCount() + ". ... ";
    } else {
      moveNumber = raptorMove.isWhitesMove() ? raptorMove.getFullMoveCount() + ". " : "";
    }
    String san = raptorMove.getSan();

    if (!isFirstMove) {
      lineText.append(' ');
    }
    lineText.append(moveNumber).append(san);
    return lineText.toString();
  }

  UciEngineInformation getUciInformation() {
    return uciInformation;
  }

  static final class UciEngineInformation {
    int currentDepth;
    long currentTime;
    long currentNodes;
    long nodesPerSeconds;
    long tbHits;
    int lastMultiPv;
    int lastScore;
    Set<String> infoStrings;

    UciEngineInformation() {
      reset();
    }

    void reset() {
      this.currentDepth = 0;
      this.currentTime = 0;
      this.currentNodes = 0;
      this.nodesPerSeconds = 0;
      this.tbHits = 0;
      this.lastMultiPv = 0;
      this.lastScore = 0;
      this.infoStrings = new HashSet<>();
    }
  }

}

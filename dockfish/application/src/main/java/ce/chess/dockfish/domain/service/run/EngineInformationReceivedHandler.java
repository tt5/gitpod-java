package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.event.DepthReached;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.Score;
import ce.chess.dockfish.domain.model.result.UciState;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.usecase.out.chess.CreateGamePosition;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
public class EngineInformationReceivedHandler {
  private static final int VERBOSE_LOG_AFTER_MINUTES = 30;

  private final EngineInformationReceivedRepository eventRepository;
  private final EvaluationRepository evaluationRepository;
  private final Event<DepthReached> newDepthEvent;
  private final ShortLineReplacer shortLineFixer;
  private final CreateGamePosition gamePositionService;
  private int lastSentDepth;

  @Inject
  EngineInformationReceivedHandler(EngineInformationReceivedRepository eventRepository,
                                   EvaluationRepository evaluationRepository,
                                   Event<DepthReached> newDepthEvent, ShortLineReplacer shortLineFixer,
                                   CreateGamePosition gamePositionService) {
    this.eventRepository = Objects.requireNonNull(eventRepository);
    this.evaluationRepository = Objects.requireNonNull(evaluationRepository);
    this.newDepthEvent = Objects.requireNonNull(newDepthEvent);
    this.shortLineFixer = Objects.requireNonNull(shortLineFixer);
    this.gamePositionService = gamePositionService;
  }

  public void receive(@Observes EngineInformationReceived engineInformation) {
    EngineInformationReceived engineInformationNormalized = shortLineFixer.fillUpGameIfTooShort(engineInformation);

    eventRepository.save(engineInformationNormalized);

    if (engineInformationNormalized.hasGame()) {
      if (log.isDebugEnabled()
          || Duration.ofMillis(engineInformation.getTime()).toMinutes() > VERBOSE_LOG_AFTER_MINUTES) {
        log.info("pv-{} {}[{},{}d]",
            engineInformationNormalized.getMultiPv(),
            Score.fromCentiPawns(engineInformationNormalized.getScore()),
            String.format("%.45s", engineInformationNormalized.getLineSan()),
            engineInformationNormalized.getDepth());
      }
      createAndPublishEvaluationFor(engineInformationNormalized);
    }
  }

  private void createAndPublishEvaluationFor(EngineInformationReceived event) {
    Collection<EngineInformationReceived> deepestEvents =
        eventRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(event.getTaskId());
    Evaluation evaluation = createEvaluation(event, deepestEvents);

    if (evaluation.hasAllVariationsOfSameDepth()) {
      evaluationRepository.save(evaluation);
      if (lastSentDepth != evaluation.maxDepth()) {
        newDepthEvent.fire(new DepthReached(event.getTaskId(), evaluation.maxDepth()));
        lastSentDepth = evaluation.maxDepth();
      }
    }
  }

  private Evaluation createEvaluation(EngineInformationReceived event,
                                      Collection<EngineInformationReceived> deepestEvents) {
    return Evaluation.builder()
        .taskId(event.getTaskId())
        .created(event.getOccurredOn())
        .variations(createVariations(deepestEvents))
        .uciState(UciState.builder()
            .kiloNodes(event.kiloNodes())
            .kiloNodesPerSecond(event.kiloNodesPerSecond())
            .tbHits(event.getTbHits())
            .infoStrings(event.getInfoStrings())
            .build())
        .build();
  }

  private List<Variation> createVariations(Collection<EngineInformationReceived> deepestEvents) {
    return deepestEvents.stream()
        .map(this::createVariation)
        .toList();
  }

  private Variation createVariation(EngineInformationReceived event) {
    return Variation.builder()
        .pvId(event.getMultiPv())
        .moves(event.getLineSan())
        .score(Score.fromCentiPawns(event.getScore()))
        .depth(event.getDepth())
        .time(AnalysisTime.fromMilliSeconds(event.getTime()))
        .gamePosition(gamePositionService.createFrom(event.getPgn()))
        .build();
  }
}

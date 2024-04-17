package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
public class ShortLineReplacer {

  private final EngineInformationReceivedRepository eventRepository;

  @Inject
  ShortLineReplacer(EngineInformationReceivedRepository eventRepository) {
    this.eventRepository = Objects.requireNonNull(eventRepository);
  }

  public EngineInformationReceived fillUpGameIfTooShort(EngineInformationReceived engineEvent) {
    if (engineEvent.hasGame() && engineEvent.getCalculatedPlies() < 3) {
      return eventRepository
          .findByTaskIdAndStartingWithLineSanMaxOccurredOn(engineEvent.getTaskId(), engineEvent.getLineSan())
          .filter(storedEvent -> storedEvent.getCalculatedPlies() > engineEvent.getCalculatedPlies())
          .map(storedEvent -> copyWithGameFrom(engineEvent, storedEvent))
          .orElse(engineEvent);
    } else {
      return engineEvent;
    }
  }

  private EngineInformationReceived copyWithGameFrom(EngineInformationReceived event,
                                                     EngineInformationReceived storedEvent) {
    log.info("Replacing short line [{}] with [{}]", event::getLineSan, storedEvent::getLineSan);
    return event.toBuilder()
        .pgn(storedEvent.getPgn())
        .lineSan(storedEvent.getLineSan())
        .calculatedPlies(storedEvent.getCalculatedPlies())
        .build();
  }
}

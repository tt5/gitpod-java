package ce.chess.dockfish.domain.service.run;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;
import ce.chess.dockfish.domain.event.DepthReached;
import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;

import jakarta.enterprise.event.Event;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EngineInformationReceivedHandlerTest {

  private static final EngineInformationReceived infoEvent = EngineInformationReceived.builder()
      .calculatedPlies(5)
      .depth(20)
      .lineSan("2... Nf6 3. Nc3 e6 4. Qc2 g6")
      .multiPv(1)
      .nodes(2000)
      .nodesPerSecond(1000)
      .occurredOn(LocalDateTime.now(ZoneId.systemDefault()))
      .pgn("1.d4 d5 2.c4 Nf6 3. Nc3 e6 4. Qc2 g6*")
      .score(30)
      .taskId(new TaskId("task1"))
      .tbHits(0L)
      .time(1000 * 60 * 100L)
      .build();

  @Mock
  EngineInformationReceivedRepository eventRepository;

  @Mock
  EvaluationRepository evaluationRepository;

  @Mock
  ShortLineReplacer shortLineReplacer;

  @Mock
  Event<DepthReached> newDepthEvent;

  @Mock
  GamePositionService gamePositionService;

  @InjectMocks
  EngineInformationReceivedHandler cut;

  @Test
  void observesUciInformationReceivedEvent() {
    given(shortLineReplacer.fillUpGameIfTooShort(infoEvent)).willReturn(infoEvent);
    given(eventRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(infoEvent.getTaskId()))
        .willReturn(List.of(infoEvent));

    fire(infoEvent);

    InOrder inOrder = Mockito.inOrder(shortLineReplacer, eventRepository, evaluationRepository);
    inOrder.verify(shortLineReplacer).fillUpGameIfTooShort(infoEvent);
    inOrder.verify(eventRepository).save(infoEvent);
    inOrder.verify(evaluationRepository).save(any(Evaluation.class));
  }

  @Test
  void doesNotCreateEvaluationWhenNoGame() {
    EngineInformationReceived eventSansGame = infoEvent.toBuilder().pgn("").build();
    given(shortLineReplacer.fillUpGameIfTooShort(eventSansGame)).willReturn(eventSansGame);

    fire(eventSansGame);

    verify(eventRepository).save(eventSansGame);
    verify(evaluationRepository, never()).save(any(Evaluation.class));
  }

  @Test
  void whenAllVariationsHaveSameDepth_firesNewDepthEvent() {
    given(shortLineReplacer.fillUpGameIfTooShort(infoEvent)).willReturn(infoEvent);
    given(eventRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(infoEvent.getTaskId()))
        .willReturn(Arrays.asList(infoEvent, infoEvent));

    EngineInformationReceivedHandler cut = new EngineInformationReceivedHandler(
        eventRepository, evaluationRepository, newDepthEvent, shortLineReplacer, gamePositionService);
    cut.receive(infoEvent);

    verify(eventRepository).findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(infoEvent.getTaskId());
    verify(newDepthEvent).fire(new DepthReached(infoEvent.getTaskId(), infoEvent.getDepth()));
  }

  @Test
  void whenNotAllVariationsHaveSameDepth_firesNoNewDepthEvent() {
    given(shortLineReplacer.fillUpGameIfTooShort(infoEvent)).willReturn(infoEvent);
    EngineInformationReceived infoEvent2 = infoEvent.toBuilder().depth(22).build();
    given(eventRepository.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(infoEvent.getTaskId()))
        .willReturn(Arrays.asList(infoEvent, infoEvent2));

    EngineInformationReceivedHandler cut = new EngineInformationReceivedHandler(
        eventRepository, evaluationRepository, newDepthEvent, shortLineReplacer, gamePositionService);
    cut.receive(infoEvent);

    verify(eventRepository).findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(infoEvent.getTaskId());
    verify(newDepthEvent, never()).fire(new DepthReached(infoEvent.getTaskId(), infoEvent.getDepth()));
  }

  private void fire(EngineInformationReceived event) {
    cut.receive(event);
  }

}

package ce.chess.dockfish.domain.service.run;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLineReplacerTest {

  private static final EngineInformationReceived eventStored = EngineInformationReceived.builder()
      .calculatedPlies(5)
      .depth(19)
      .lineSan("2... Nf6 3. Nc3 Bb4 4. Qc2 g6")
      .multiPv(1)
      .nodes(1000)
      .nodesPerSecond(1000)
      .occurredOn(LocalDateTime.now(ZoneId.systemDefault()))
      .pgn("1.d4 d5 2.c4 Nf6 3. Nc3 Bb4 4. Qc2 g6*")
      .score(30)
      .taskId(new TaskId("task1"))
      .tbHits(0L)
      .time(234332L)
      .build();

  private static final EngineInformationReceived eventReceived = eventStored.toBuilder()
      .taskId(new TaskId("task2"))
      .calculatedPlies(2)
      .lineSan("2... Nf6 3. Nc3")
      .build();

  @Mock
  private EngineInformationReceivedRepository eventRepository;

  @InjectMocks
  private ShortLineReplacer cut;

  @Test
  void whenStoredEventIsNotFoundThenDontReplace() {
    given(eventRepository.findByTaskIdAndStartingWithLineSanMaxOccurredOn(any(), any())).willReturn(Optional.empty());

    EngineInformationReceived result = cut.fillUpGameIfTooShort(eventReceived);

    assertThat(result, is(equalTo(eventReceived)));
  }

  @Test
  void whenStoredEventIsSameThenDontReplace() {
    given(eventRepository.findByTaskIdAndStartingWithLineSanMaxOccurredOn(any(), any()))
        .willReturn(Optional.of(eventReceived));

    EngineInformationReceived result = cut.fillUpGameIfTooShort(eventReceived);

    assertThat(result, is(equalTo(eventReceived)));
  }

  @Test
  void whenReceivingGoodEventThenDontReplace() {
    EngineInformationReceived eventReceivedGood = eventStored.toBuilder()
        .taskId(new TaskId("task2"))
        .build();

    EngineInformationReceived result = cut.fillUpGameIfTooShort(eventReceivedGood);

    assertThat(result, is(equalTo(eventReceivedGood)));
    verifyNoInteractions(eventRepository);
  }

  @Test
  void replacesGameWithStoredValue() {
    given(eventRepository.findByTaskIdAndStartingWithLineSanMaxOccurredOn(
        eventReceived.getTaskId(), eventReceived.getLineSan()))
        .willReturn(Optional.of(eventStored));

    EngineInformationReceived result = cut.fillUpGameIfTooShort(eventReceived);

    EngineInformationReceived expected = eventReceived.toBuilder()
        .lineSan(eventStored.getLineSan())
        .calculatedPlies(eventStored.getCalculatedPlies())
        .pgn(eventStored.getPgn())
        .build();
    assertThat(result, is(equalTo(expected)));
  }

}

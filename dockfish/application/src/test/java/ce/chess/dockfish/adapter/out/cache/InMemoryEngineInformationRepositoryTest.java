package ce.chess.dockfish.adapter.out.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InMemoryEngineInformationRepositoryTest {
  private static final TaskId taskId = new TaskId("TASK");
  private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());

  @Mock
  private EngineInformationReceived event1;

  @Mock
  private EngineInformationReceived event2;

  @Mock
  private EngineInformationReceived event3;

  @InjectMocks
  private InMemoryEngineInformationRepository cut;

  @BeforeEach
  void setUp() {
    given(event1.getTaskId()).willReturn(taskId);
    given(event2.getTaskId()).willReturn(taskId);
    given(event3.getTaskId()).willReturn(taskId);
    given(event1.getOccurredOn()).willReturn(NOW);
    given(event2.getOccurredOn()).willReturn(NOW);
    given(event3.getOccurredOn()).willReturn(NOW);
    given(event1.getMultiPv()).willReturn(1);
    given(event2.getMultiPv()).willReturn(2);
    given(event3.getMultiPv()).willReturn(3);
    given(event1.getDepth()).willReturn(22);
    given(event2.getDepth()).willReturn(22);
    given(event3.getDepth()).willReturn(22);
    given(event1.hasGame()).willReturn(true);
    given(event2.hasGame()).willReturn(true);
    given(event3.hasGame()).willReturn(true);
    given(event1.getLineSan()).willReturn("e4 e5 Nf3 Nc6");
    given(event2.getLineSan()).willReturn("e4 e5 Nf3 Nc6 Bb5");
    given(event3.getLineSan()).willReturn("e4 e5 Nf3 Nc6 Bb5 a6");

    Stream.of(event1, event2, event3).forEach(cut::save);
    assertThat(cut.getCacheSize(), is(3L));
  }

  @Nested
  class FindDeepestEventsPerPv {
    @Test
    void ignoresEventsWithLowerDepthAndSamePv() {
      given(event3.getMultiPv()).willReturn(2);
      given(event3.getDepth()).willReturn(20);

      Collection<EngineInformationReceived> events = cut.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(taskId);

      assertThat(events, contains(event1, event2));
    }

    @Test
    void returnsAllEventsOfDifferentPv() {
      given(event1.getDepth()).willReturn(21);
      given(event2.getDepth()).willReturn(26);
      given(event3.getDepth()).willReturn(23);

      Collection<EngineInformationReceived> events = cut.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(taskId);

      assertThat(events, contains(event1, event2, event3));
    }

    @Test
    void doesReturnTheMostRecentEvents() {
      given(event1.getMultiPv()).willReturn(1);
      given(event2.getMultiPv()).willReturn(1);
      given(event3.getMultiPv()).willReturn(1);
      given(event1.getOccurredOn()).willReturn(NOW);
      given(event2.getOccurredOn()).willReturn(NOW.plusSeconds(2));
      given(event3.getOccurredOn()).willReturn(NOW.plusSeconds(1));

      Collection<EngineInformationReceived> events = cut.findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(taskId);

      assertThat(events, contains(event2));
    }
  }

  @Nested
  class FindLatestByTaskId {

    @Test
    void findsEventWithNewerTimeStamp() {
      given(event1.getOccurredOn()).willReturn(NOW.plusSeconds(20));

      Optional<EngineInformationReceived> latest = cut.findByTaskIdMaxOccurredOn(taskId);

      assertThat(latest, is(Optional.of(event1)));
    }
  }

  @Nested
  class FindLatestStartingWithSan {
    @Test
    void findsLatestWithSameSan() {
      given(event1.getOccurredOn()).willReturn(NOW.plusSeconds(10));
      given(event2.getOccurredOn()).willReturn(NOW.plusSeconds(20));
      given(event3.getOccurredOn()).willReturn(NOW.plusSeconds(30));

      Optional<EngineInformationReceived> latest = cut.findByTaskIdAndStartingWithLineSanMaxOccurredOn(taskId, "e4 e5");

      assertThat(latest, is(Optional.of(event3)));
    }
  }

}

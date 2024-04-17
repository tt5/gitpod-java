package ce.chess.dockfish.adapter.out.engine;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleWatchdogTest {

  private static Instant startTime;

  @Mock
  private BooleanSupplier checkCondition;

  @Spy
  private Runnable actionWhenFalse = new Runnable() {
    @Override
    public void run() {
      System.out.println("** Action performed after "
          + Duration.between(startTime, Instant.now()).truncatedTo(ChronoUnit.MILLIS));
    }
  };

  private SimpleWatchdog cut;

  @BeforeEach
  void setup() {
    cut = new SimpleWatchdog();
  }

  @Test
  void blocksWhileRunningThenRunsAction_AndCanBeReused() {
    given(checkCondition.getAsBoolean()).willReturn(true).willReturn(true).willReturn(false);

    startTime = Instant.now();
    cut.watch(checkCondition, 1, actionWhenFalse);
    Awaitility.await().atMost(3, TimeUnit.SECONDS)
        .until(cut::waitWhileConditionIsTrue); // blocks

    InOrder inOrder = inOrder(checkCondition, actionWhenFalse);
    inOrder.verify(checkCondition, times(3)).getAsBoolean();
    inOrder.verify(actionWhenFalse).run();

    canBeReused();
  }

  void canBeReused() {
    BooleanSupplier condition2 = mock(BooleanSupplier.class);
    given(condition2.getAsBoolean()).willReturn(true).willReturn(false);
    Runnable actionWhenFalse2 = mock(Runnable.class);

    cut.watch(condition2, 1, actionWhenFalse2);

    verify(actionWhenFalse2, timeout(1500)).run();
  }
}

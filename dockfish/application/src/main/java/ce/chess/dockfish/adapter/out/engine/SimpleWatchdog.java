package ce.chess.dockfish.adapter.out.engine;

import static java.util.concurrent.TimeUnit.SECONDS;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;

@ApplicationScoped
class SimpleWatchdog {

  private static final int INITIAL_DELAY_SECONDS = 0;

  private CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();

  boolean waitWhileConditionIsTrue() {
    try {
      return completionFuture.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return false;
    } catch (ExecutionException ex) {
      return false;
    }
  }

  void watch(BooleanSupplier condition, int monitoringIntervalInSeconds, Runnable performWhenFalse) {
    this.completionFuture = new CompletableFuture<>();
    // NOPMD because of false positive. scheduledExecutorService.close() is called when Scheduler terminates
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(); // NOPMD
    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
      if (!condition.getAsBoolean()) {
        scheduledExecutorService.shutdown();
        performWhenFalse.run();
        this.completionFuture.complete(true);
      }
    }, INITIAL_DELAY_SECONDS, monitoringIntervalInSeconds, SECONDS);

    this.completionFuture = this.completionFuture.whenComplete((result, thrown) -> {
      scheduledFuture.cancel(true);
      scheduledExecutorService.close();
    });
  }
}

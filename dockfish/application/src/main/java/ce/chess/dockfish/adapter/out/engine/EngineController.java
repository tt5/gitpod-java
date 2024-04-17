package ce.chess.dockfish.adapter.out.engine;

import ce.chess.dockfish.domain.event.AnalysisFinished;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.engine.LockEngine;
import ce.chess.dockfish.usecase.out.engine.QueryEngine;
import ce.chess.dockfish.usecase.out.engine.ReducePv;
import ce.chess.dockfish.usecase.out.engine.RunEngine;
import ce.chess.dockfish.usecase.out.engine.StartStaticEvaluation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import raptor.engine.uci.UCIBestMove;
import raptor.engine.uci.UCIEngine;
import raptor.engine.uci.UCIOption;

@ApplicationScoped
@Log4j2
public class EngineController implements ReducePv, StartStaticEvaluation, LockEngine, QueryEngine, RunEngine {

  private static final String STATIC_EVALUATION_ENGINE = "stockfish16";

  private final Semaphore singleProcessMutex = new Semaphore(1);

  @Inject
  EngineListener engineListener;

  @Inject
  UciEngineHolder uciEngineHolder;

  @Inject
  SimpleWatchdog simpleWatchdog;

  @Inject
  Event<AnalysisFinished> analysisFinishedPublisher;

  private AnalysisRun analysisRun;

  @Gauge(name = "task_time_remaining", absolute = true, unit = MetricUnits.SECONDS)
  public Long getTaskTimeRemaining() {
    return Optional.ofNullable(analysisRun)
        .filter(job -> uciEngineIsRunning(job.taskId()))
        .flatMap(AnalysisRun::estimatedCompletionTime)
        .map(completionDate -> Duration.between(LocalDateTime.now(ZoneId.systemDefault()), completionDate))
        .map(Duration::toSeconds)
        .filter(seconds -> seconds > 0)
        .orElse(0L);
  }

  @Override
  public AnalysisRun startAnalysis(final AnalysisRun analysisRun) {
    throwIfNotLocked("startAnalysis should be called only after a Lock was acquired");
    log.info("START Analysis");
    UCIEngine uciEngine = uciEngineHolder.connect(
        analysisRun.engineProgramName(),
        analysisRun.initialPv(),
        analysisRun.engineOptions());

    String currentFen = analysisRun.startingPosition().getFen();

    this.analysisRun = analysisRun
        .withUciEngineName(uciEngine.getEngineName());

    engineListener.assignTo(this.analysisRun);

    uciEngine.setPosition(currentFen);
    uciEngine.go(createGoParameter(this.analysisRun), engineListener);

    log.info("*** Engine {} is running: {}", uciEngine.getEngineName(), uciEngine.isProcessingGo());
    simpleWatchdog.watch(this::uciEngineIsRunning, 1, this::watchdogFinished);

    return this.analysisRun;
  }

  void watchdogFinished() {
    log.info("Watchdog: Engine finished");
    analysisFinishedPublisher.fire(new AnalysisFinished(analysisRun.taskId(), Instant.now()));

    stop();
    uciEngineHolder.disconnect();

    releaseLock();
  }

  @Override
  public String retrieveStaticEvaluation(String fen) {
    throwIfNotLocked("retrieveStaticEvaluation should be called only after a Lock was acquired");
    log.info("START eval");

    UCIEngine uciEngine = uciEngineHolder.connect(STATIC_EVALUATION_ENGINE);

    uciEngine.setPosition(fen);

    String eval = uciEngine.eval();
    log.info("Got static evaluation: {}", eval);
    uciEngineHolder.disconnect();
    return eval;
  }

  @Override
  public void reducePvTo(int newPv) {
    if (newPv > 0) {
      UCIEngine uciEngine = uciEngineHolder.getEngine();
      UCIOption uciOption = uciEngine.getOption("MultiPV");
      int pvCurrent = Integer.parseInt(uciOption.getValue());
      log.info("Set new MultiPV from {} to {}", pvCurrent, newPv);
      if (newPv < pvCurrent) {
        // + set new PV to engine
        uciOption.setValue(Integer.toString(newPv));
        uciEngine.stopSetOptionGo(uciOption);
      }
    }
  }

  @Override
  public void stop() {
    if (uciEngineIsRunning()) {
      log.info("stop");
      UCIBestMove bestMove = uciEngineHolder.getEngine().stop();
      log.info("Engine stopped. BestMove: {}", bestMove);
    }
  }

  @Override
  public void kill() {
    log.info("kill");
    uciEngineHolder.getEngine().kill();
    log.info("Engine killed.");
  }

  @Override
  public JobStatus getJobStatus(TaskId taskId) {
    if (uciEngineIsRunning(taskId)) {
      return JobStatus.ACTIVE;
    } else {
      return JobStatus.NOT_ACTIVE;
    }
  }

  @Override
  public boolean uciEngineIsRunning() {
    return uciEngineHolder.getEngine().isProcessingGo();
  }

  private boolean uciEngineIsRunning(TaskId taskId) {
    return uciEngineIsRunning()
        && Objects.nonNull(analysisRun)
        && Objects.nonNull(analysisRun.taskId())
        && analysisRun.taskId().matches(taskId);
  }

  private String createGoParameter(AnalysisRun analysisJob) {
    StringBuilder goCommandBuilder = new StringBuilder(20);

    analysisJob.maxDepth().ifPresent(p -> goCommandBuilder.append(" depth ").append(p));
    analysisJob.maxDuration().ifPresent(p -> goCommandBuilder.append(" movetime ").append(p.toMillis()));

    String goCommand = goCommandBuilder.toString().trim();
    if (goCommand.isEmpty()) {
      goCommand = "infinite";
    }
    return goCommand;
  }

  @Override
  public void acquireLock() {
    log.info("START wait for lock");
    singleProcessMutex.acquireUninterruptibly();
    log.info("END   wait for lock");
  }

  @Override
  public boolean tryAcquireLock() {
    log.info("Start try acquire lock");
    boolean acquire = singleProcessMutex.tryAcquire();
    log.info("End try acquire lock = {}", acquire);
    return acquire;
  }

  @Override
  public void releaseLock() {
    log.info("Release Lock");
    if (singleProcessMutex.availablePermits() == 0) {
      singleProcessMutex.release();
      log.info("Lock released");
    } else {
      log.warn("Lock was already released!");
    }
  }

  @Override
  public void blockWhileActive() {
    log.info("START Block while running");
    simpleWatchdog.waitWhileConditionIsTrue();
    log.info("END   Block while running");
  }

  private void throwIfNotLocked(String errorMessage) {
    if (singleProcessMutex.availablePermits() > 0) {
      throw new IllegalStateException(errorMessage);
    }
  }
}

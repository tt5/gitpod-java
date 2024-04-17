package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.model.RequeueException;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.EngineOption;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.in.QueryAnalysis;
import ce.chess.dockfish.usecase.in.ReceiveAnalysisRequest;
import ce.chess.dockfish.usecase.in.TerminateAnalysis;
import ce.chess.dockfish.usecase.out.db.TaskRepository;
import ce.chess.dockfish.usecase.out.engine.LockEngine;
import ce.chess.dockfish.usecase.out.engine.QueryEngine;
import ce.chess.dockfish.usecase.out.engine.RunEngine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
@Log4j2
public class InfiniteAnalysisService implements ReceiveAnalysisRequest, TerminateAnalysis, QueryAnalysis {

  @Inject
  LockEngine lockEngine;

  @Inject
  QueryEngine queryEngine;

  @Inject
  RunEngine runEngine;

  @Inject
  TaskRepository taskRepository;

  @Inject
  UciOptionsConfiguration uciOptionsConfiguration;

  @Inject
  Config config;

  @Override
  public Optional<TaskId> startAsync(AnalysisRun task) {
    // Use this for rest calls.
    // It will return with a task id immediately and block further analysis requests until finished.
    log.info("Start asynchronous analysis");
    if (!lockEngine.tryAcquireLock()) {
      log.info("Task rejected. Engine is still running");
      return Optional.empty();
    }
    return startAnalysis(task);
  }

  @Override
  public Optional<TaskId> startSync(AnalysisRun task) {
    // Use this for messaging.
    // It will block until calculation is finished.
    log.info("Start synchronous analysis");
    if (isTaskAlreadyAnalysed(task)) {
      log.info("Task rejected immediately. Has already been analysed.");
      return Optional.empty();
    }
    lockEngine.acquireLock();
    Optional<TaskId> taskId = startAnalysis(task);
    taskId.ifPresent(t -> lockEngine.blockWhileActive());
    return taskId;
  }

  private Optional<TaskId> startAnalysis(AnalysisRun task) {
    try {
      TaskId taskId;
      if (TaskId.isPresent(task.taskId())) {
        taskId = task.taskId();
      } else {
        taskId = TaskId.createNew();
      }
      AnalysisRun analysisRun = enrichAnalysisRun(task, taskId);

      if (taskRepository.hasDuplicate(analysisRun)) {
        log.info("Task rejected. Has already been analysed.");
        lockEngine.releaseLock();
        return Optional.empty();
      }
      analysisRun = startEngine(analysisRun);
      taskRepository.save(analysisRun);
      return Optional.of(taskId);
    } catch (Exception ex) { // NOPMD
      lockEngine.releaseLock();
      throw ex;
    }
  }

  private AnalysisRun startEngine(AnalysisRun engineTask) {
    try {
      return runEngine.startAnalysis(engineTask);
    } catch (Exception ex) { // NOPMD
      throw new RequeueException(ex.getMessage(), ex);
    }
  }

  @Override
  public JobStatus getJobStatus(TaskId taskId) {
    return queryEngine.getJobStatus(taskId);
  }

  @Override
  public boolean stop() {
    log.info("Stop analysis");
    runEngine.stop();
    return !queryEngine.uciEngineIsRunning();
  }

  @Override
  public boolean kill() {
    log.info("Kill analysis");
    runEngine.kill();
    return !queryEngine.uciEngineIsRunning();
  }

  @Override
  public AnalysisRun getTaskDetails(TaskId taskId) {
    return taskRepository.findByTaskId(taskId).orElseThrow(IllegalArgumentException::new);
  }

  private boolean isTaskAlreadyAnalysed(AnalysisRun task) {
    return taskRepository.hasDuplicate(task);
  }

  private AnalysisRun enrichAnalysisRun(AnalysisRun task, TaskId taskId) {
    return task.toBuilder()
        .taskId(taskId)
        .hostname(config.getOptionalValue("hostname", String.class).orElse("hostname"))
        .clearEngineOptions()
        .engineOptions(mergeLocalUciOptions(task).engineOptions())
        .created(LocalDateTime.now(ZoneId.systemDefault()))
        .build();

  }

  private AnalysisRun mergeLocalUciOptions(AnalysisRun task) {
    AnalysisRun result = task;
    for (EngineOption option : uciOptionsConfiguration.getLocalEngineOptions()) {
      log.info("Found custom engine option: {}", option);
      if ("SyzygyPath".equalsIgnoreCase(option.getName())) {
        if (task.useSyzygyPath()) {
          // add SyzygyPath only when Tablebases should be used
          result = result.addOrReplaceOption(option);
        }
      } else {
        result = result.addOrReplaceOption(option);
      }
    }
    return result;
  }
}

package ce.chess.dockfish.adapter.out.cache;

import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.TaskRepository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.Optional;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class InMemoryTaskRepository implements TaskRepository {

  private final Cache<TaskId, AnalysisRun> tasks = CacheBuilder.newBuilder().maximumSize(100).build();

  @Gauge(name = "guava_cache_size", absolute = true, unit = MetricUnits.NONE,
      tags = "cache=InMemoryTaskRepository")
  public long getCacheSize() {
    return tasks.size();
  }

  @Override
  public void save(AnalysisRun task) {
    tasks.put(task.taskId(), task);
  }

  @Override
  public Optional<AnalysisRun> findLatest() {
    return tasks.asMap().values().stream()
        .max(Comparator.comparing(AnalysisRun::created));
  }

  @Override
  public Optional<AnalysisRun> findByTaskId(TaskId taskId) {
    return tasks.asMap().keySet().stream()
        .filter(key -> key.matches(taskId))
        .findFirst()
        .map(tasks::getIfPresent);
  }

  @Override
  public boolean hasDuplicate(AnalysisRun engineTask) {
    return tasks.asMap().values().stream().anyMatch(engineTask::isSameAs);
  }

}

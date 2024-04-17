package ce.chess.dockfish.adapter.out.cache;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EvaluationRepository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class InMemoryEvaluationRepository implements EvaluationRepository {

  private final Cache<String, Evaluation> evaluations = CacheBuilder.newBuilder().maximumSize(2000).build();

  @Gauge(name = "guava_cache_size", absolute = true, unit = MetricUnits.NONE,
      tags = "cache=InMemoryEvaluationRepository")
  public long getCacheSize() {
    return evaluations.size();
  }

  @Override
  public void save(Evaluation evaluation) {
    evaluations.put(evaluation.taskIdAndMaxDepth(), evaluation);
  }

  @Override
  public List<TaskId> listTaskIds() {
    return getEvaluations().stream()
        .sorted(Comparator.comparing(Evaluation::getCreated))
        .map(Evaluation::getTaskId)
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Evaluation> findByTaskIdMaxCreated(TaskId taskId) {
    return getEvaluations().stream()
        .filter(Evaluation.matching(taskId))
        .max(Comparator.comparing(Evaluation::getCreated));
  }

  @Override
  public List<Evaluation> findByTaskId(TaskId taskId) {
    return getEvaluations().stream()
        .filter(Evaluation.matching(taskId))
        .sorted(Comparator.comparing(Evaluation::getCreated))
        .collect(Collectors.toList());
  }

  private Collection<Evaluation> getEvaluations() {
    return evaluations.asMap().values();
  }

}

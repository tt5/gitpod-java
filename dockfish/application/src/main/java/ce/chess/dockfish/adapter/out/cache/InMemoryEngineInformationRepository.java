package ce.chess.dockfish.adapter.out.cache;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.out.db.EngineInformationReceivedRepository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class InMemoryEngineInformationRepository implements EngineInformationReceivedRepository {

  private final Cache<EngineInformationReceived, String> events =
      CacheBuilder.newBuilder().maximumSize(10_000).build();

  @Gauge(name = "guava_cache_size", absolute = true, unit = MetricUnits.NONE,
      tags = "cache=InMemoryEngineInformationRepository")
  public long getCacheSize() {
    return events.size();
  }

  @Override
  public void save(EngineInformationReceived event) {
    refreshInCache(event);
  }

  @Override
  public Collection<EngineInformationReceived> findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(TaskId taskId) {
    Map<Integer, Optional<EngineInformationReceived>> deepestEventsByPv = getEvents().stream()
        .filter(EngineInformationReceived.matching(taskId))
        .filter(EngineInformationReceived::hasGame)
        .collect(
            Collectors.groupingBy(
                EngineInformationReceived::getMultiPv,
                Collectors.maxBy(
                    Comparator.comparingInt(EngineInformationReceived::getDepth)
                        .thenComparing(EngineInformationReceived::getOccurredOn))));
    return deepestEventsByPv.values().stream()
        .flatMap(Optional::stream)
        .map(this::refreshInCache)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<EngineInformationReceived> findByTaskIdMaxOccurredOn(TaskId taskId) {
    return getEvents().stream()
        .filter(EngineInformationReceived.matching(taskId))
        .max(Comparator.comparing(EngineInformationReceived::getOccurredOn));
  }

  @Override
  public Optional<EngineInformationReceived> findByTaskIdAndStartingWithLineSanMaxOccurredOn(TaskId taskId,
                                                                                             String lineSan) {
    return getEvents().stream()
        .filter(EngineInformationReceived.matching(taskId))
        .filter(EngineInformationReceived::hasGame)
        .filter(e -> e.getLineSan().startsWith(lineSan))
        .max(Comparator.comparing(EngineInformationReceived::getOccurredOn));
  }

  private EngineInformationReceived refreshInCache(EngineInformationReceived engineInformationReceived) {
    events.put(engineInformationReceived, "");
    return engineInformationReceived;
  }

  private Set<EngineInformationReceived> getEvents() {
    return events.asMap().keySet();
  }

}

package ce.chess.dockfish.usecase.out.db;

import ce.chess.dockfish.domain.event.EngineInformationReceived;
import ce.chess.dockfish.domain.model.task.TaskId;

import java.util.Collection;
import java.util.Optional;

public interface EngineInformationReceivedRepository {

  void save(EngineInformationReceived event);

  Collection<EngineInformationReceived> findByTaskIdGroupedByMultiPvMaxDepthAndMaxOccurredOn(TaskId taskId);

  Optional<EngineInformationReceived> findByTaskIdMaxOccurredOn(TaskId taskId);

  Optional<EngineInformationReceived> findByTaskIdAndStartingWithLineSanMaxOccurredOn(TaskId taskId, String lineSan);
}

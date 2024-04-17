package ce.chess.dockfish.domain.model.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class EngineOption {

  String name;

  String value;
}

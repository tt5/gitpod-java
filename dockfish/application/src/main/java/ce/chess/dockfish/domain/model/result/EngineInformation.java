package ce.chess.dockfish.domain.model.result;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class EngineInformation {

  int multiPv;

  String lineSan;

  int score;

  String time;

  int depth;

  LocalDateTime occurredOn;

}

package ce.chess.dockfish.adapter.out.rabbit.fallback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class PublishFailed {
  String exchangeName;

  String message;
}

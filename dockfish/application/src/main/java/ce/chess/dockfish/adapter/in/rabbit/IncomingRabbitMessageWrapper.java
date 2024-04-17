package ce.chess.dockfish.adapter.in.rabbit;

import io.vertx.core.json.JsonObject;

public record IncomingRabbitMessageWrapper(String exchange, String routingKey, String correlationId,
                                           java.util.Map<String, Object> headers,
                                           JsonObject jsonObject,
                                           boolean isRedeliver) {

  public String getTopic() {
    if (routingKey != null && !routingKey.isEmpty()) {
      return exchange + "#" + routingKey;
    }
    return exchange;
  }
}

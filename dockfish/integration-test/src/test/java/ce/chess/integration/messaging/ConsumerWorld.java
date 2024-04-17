package ce.chess.integration.messaging;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class ConsumerWorld {
  private final ConcurrentMap<String, ToStringConsumer> consumerMap = new ConcurrentHashMap<>();
  private ToStringConsumer currentConsumer;

  public ToStringConsumer get(String key) {
    currentConsumer = consumerMap.get(key);
    return currentConsumer;
  }

  public ToStringConsumer getCurrentConsumer() {
    return Objects.requireNonNull(currentConsumer);
  }

  public ToStringConsumer putOrGet(String key, Supplier<ToStringConsumer> supplier) {
    return consumerMap.computeIfAbsent(key, x -> supplier.get());
  }

  public void closeAll() {
    consumerMap.values().forEach(ToStringConsumer::close);
  }
}

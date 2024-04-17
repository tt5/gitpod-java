package ce.chess.dockfish.adapter.out.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.util.HashMap;
import java.util.Map;

public class InMemoryTestConfig implements QuarkusTestResourceLifecycleManager {

  @Override
  public Map<String, String> start() {
    Map<String, String> env = new HashMap<>();
    env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(RabbitConfiguration.CHANNEL_STATIC_EVALUATION));
    env.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(RabbitConfiguration.CHANNEL_EVALUATION));
    return env;
  }

  @Override
  public void stop() {
    InMemoryConnector.clear();
  }
}
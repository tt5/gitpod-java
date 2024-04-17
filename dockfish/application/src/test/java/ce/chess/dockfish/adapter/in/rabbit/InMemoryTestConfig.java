package ce.chess.dockfish.adapter.in.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import java.util.HashMap;
import java.util.Map;

public class InMemoryTestConfig implements QuarkusTestResourceLifecycleManager {

  @Override
  public Map<String, String> start() {
    Map<String, String> env = new HashMap<>();
    env.putAll(
        InMemoryConnector.switchIncomingChannelsToInMemory(RabbitConfiguration.CHANNEL_STATIC_EVALUATION_REQUEST));
    env.putAll(
        InMemoryConnector.switchIncomingChannelsToInMemory(RabbitConfiguration.CHANNEL_TASK_SUBMITTED));
    env.putAll(
        InMemoryConnector.switchIncomingChannelsToInMemory(RabbitConfiguration.CHANNEL_TASK_SUBMITTED_2));
    env.put("task_consumer_delay_seconds", "0");
    return env;
  }

  @Override
  public void stop() {
    InMemoryConnector.clear();
  }
}

package ce.chess.integration.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class TestMessagePublisher implements Closeable {
  private final Connection connection;

  public TestMessagePublisher(ConnectionFactory connectionFactory) throws IOException, TimeoutException {
    this.connection = connectionFactory.newConnection();
  }

  public void publish(String exchangeName, String payload) throws IOException, TimeoutException {
    publish(exchangeName, "", MessageProperties.BASIC, payload);
  }

  public void publish(String exchangeName, String routingKey,
                      AMQP.BasicProperties properties, String payload) throws IOException, TimeoutException {
    try (Channel channel = provideChannel()) {
      channel.exchangeDeclarePassive(exchangeName);
      channel.basicPublish(exchangeName, routingKey, properties.builder().contentType("application/json").build(),
          payload.getBytes(Charset.defaultCharset()));
    }
  }

  @Override
  public void close() {
    Optional.ofNullable(connection).ifPresent(c -> {
      try {
        c.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
  }

  private Channel provideChannel() throws IOException {
    return connection.createChannel();
  }

}

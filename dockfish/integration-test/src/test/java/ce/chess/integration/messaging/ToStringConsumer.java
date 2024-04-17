package ce.chess.integration.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ToStringConsumer extends DefaultConsumer {

  private final Deque<String> rabbitMessages;

  public ToStringConsumer(ConnectionFactory connectionFactory) {
    super(provideChannel(connectionFactory));
    rabbitMessages = new ArrayDeque<>();
  }

  public List<String> getMessages() {
    return List.copyOf(rabbitMessages);
  }

  public String getLastMessage() {
    return rabbitMessages.getLast();
  }

  public boolean hasMessages() {
    return !rabbitMessages.isEmpty();
  }

  @Override
  public void handleDelivery(String consumerTag, Envelope envelope,
                             AMQP.BasicProperties properties, byte[] body) {
    String rabbitMessage = new String(body, StandardCharsets.UTF_8);
    rabbitMessages.addLast(rabbitMessage);
  }

  public ToStringConsumer consumeFromQueue(String queueName) {
    rabbitMessages.clear();
    Channel channel = getChannel();

    try {
      channel.queueDeclarePassive(queueName);
      channel.queuePurge(queueName);
      channel.basicConsume(queueName, true, this);
    } catch (IOException ioex) {
      throw new UncheckedIOException(ioex);
    }
    return this;
  }

  public ToStringConsumer consumeFromExchange(String exchangeName, String routingKey, boolean declareExchange) {
    rabbitMessages.clear();
    Channel channel = getChannel();

    try {
      if (declareExchange) {
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true);
      } else {
        channel.exchangeDeclarePassive(exchangeName);
      }

      String queueName = channel.queueDeclare().getQueue();
      channel.queueBind(queueName, exchangeName, routingKey == null ? "" : routingKey);
      channel.queuePurge(queueName);
      channel.basicConsume(queueName, true, this);
    } catch (IOException ioex) {
      throw new UncheckedIOException(ioex);
    }
    return this;
  }

  public void close() {
    try {
      getChannel().close();
    } catch (IOException ioex) {
      throw new UncheckedIOException(ioex);
    } catch (TimeoutException tex) {
      throw new IllegalStateException(tex);
    }
  }

  private static Channel provideChannel(ConnectionFactory connectionFactory) {
    try {
      Connection connection = connectionFactory.newConnection();
      return connection.createChannel();
    } catch (IOException ioex) {
      throw new UncheckedIOException(ioex);
    } catch (TimeoutException tex) {
      throw new IllegalStateException(tex);
    }
  }

}

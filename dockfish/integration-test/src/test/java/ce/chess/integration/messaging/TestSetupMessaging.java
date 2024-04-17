package ce.chess.integration.messaging;

import com.rabbitmq.client.ConnectionFactory;

// non-spring
public class TestSetupMessaging {
  private static final ConnectionFactory connectionFactory;

  static {
    connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(getRabbitmqHost());
    connectionFactory.setPort(getRabbitmqPort());
    connectionFactory.setUsername(getRabbitmqUser());
    connectionFactory.setPassword(getRabbitmqPassword());
    connectionFactory.setVirtualHost(getRabbitmqVirtualHost());
  }

  public static ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  private static String getRabbitmqHost() {
    return System.getProperty("rabbitmq.host", "localhost");
  }

  private static int getRabbitmqPort() {
    return Integer.parseInt(System.getProperty("rabbitmq.port", "5672"));
  }

  private static String getRabbitmqVirtualHost() {
    return System.getProperty("rabbitmq.virtualhost", "/");
  }

  private static String getRabbitmqUser() {
    return System.getProperty("rabbitmq.user", "guest");
  }

  private static String getRabbitmqPassword() {
    return System.getProperty("rabbitmq.password", "guest");
  }

}

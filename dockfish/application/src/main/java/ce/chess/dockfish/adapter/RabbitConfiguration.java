package ce.chess.dockfish.adapter;

import static java.lang.Boolean.TRUE;

import io.smallrye.common.annotation.Identifier;
import io.vertx.rabbitmq.RabbitMQOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RabbitConfiguration {
  public static final String CHANNEL_TASK_SUBMITTED = "submittaskcommand";
  public static final String CHANNEL_TASK_SUBMITTED_2 = "submittaskcommand2";
  public static final String CHANNEL_EVALUATION = "evaluation";
  public static final String CHANNEL_STATIC_EVALUATION_REQUEST = "staticevaluationrequest";
  public static final String CHANNEL_STATIC_EVALUATION = "staticevaluation";

  private static final String RABBITMQ_HOST_PROPERTY = "rabbitmq_host";
  private static final String RABBITMQ_PORT_PROPERTY = "rabbitmq_port";
  private static final String RABBITMQ_USERNAME_PROPERTY = "rabbitmq_username";
  private static final String RABBITMQ_SECRET_PROPERTY = "rabbitmq_password";
  private static final String RABBITMQ_VIRTUALHOST_PROPERTY = "rabbitmq_virtualhost";
  private static final String RABBITMQ_USE_SSL_PROPERTY = "rabbitmq_use_ssl";

  @ConfigProperty(name = RABBITMQ_HOST_PROPERTY)
  Optional<String> host;

  @ConfigProperty(name = RABBITMQ_PORT_PROPERTY)
  Optional<Integer> port;

  @ConfigProperty(name = RABBITMQ_USERNAME_PROPERTY)
  Optional<String> username;

  @ConfigProperty(name = RABBITMQ_SECRET_PROPERTY)
  Optional<String> password;

  @ConfigProperty(name = RABBITMQ_VIRTUALHOST_PROPERTY)
  Optional<String> virtualhost;

  @ConfigProperty(name = RABBITMQ_USE_SSL_PROPERTY)
  Optional<Boolean> useSsl;

  @Produces
  @Identifier("rabbitClient")
  public RabbitMQOptions getNamedOptions() {
    RabbitMQOptions newOptions = new RabbitMQOptions();
    Objects.requireNonNullElseGet(host, Optional::<String>empty).ifPresent(newOptions::setHost);
    Objects.requireNonNullElseGet(port, Optional::<Integer>empty).ifPresent(newOptions::setPort);
    Objects.requireNonNullElseGet(virtualhost, Optional::<String>empty).ifPresent(newOptions::setVirtualHost);
    Objects.requireNonNullElseGet(username, Optional::<String>empty).ifPresent(newOptions::setUser);
    Objects.requireNonNullElseGet(password, Optional::<String>empty).ifPresent(newOptions::setPassword);
    Objects.requireNonNullElseGet(useSsl, Optional::<Boolean>empty).filter(TRUE::equals)
        .ifPresent(x -> newOptions.setSsl(true));
    newOptions.setAutomaticRecoveryEnabled(true);
    newOptions.setAutomaticRecoveryOnInitialConnection(true);
    newOptions.setRequestedHeartbeat(5);
    newOptions.setReconnectAttempts(100_000);
    newOptions.setReconnectInterval(5000);
    return newOptions;
  }
}

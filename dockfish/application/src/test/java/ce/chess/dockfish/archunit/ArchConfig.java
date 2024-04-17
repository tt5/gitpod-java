package ce.chess.dockfish.archunit;

public interface ArchConfig {
  String PROJECT_PACKAGE = "ce.chess.dockfish";

  String[] DOMAIN_EVENT_PERMISSIONS = new String[]
      {"com.google.common.."};
  String[] DOMAIN_MODEL_PERMISSIONS = new String[]
      {"com.google.common.base..",
          "raptor.chess.."}; // TODO Remove from here
  String[] DOMAIN_SERVICE_PERMISSIONS = new String[]
      {"com.google.common..", "jakarta.enterprise.event..", "org.eclipse.microprofile.config"};
  String[] ADAPTER_IN_PERMISSIONS = new String[]
      {"com.google.common.base..", "com.google.common.util..", "com.fasterxml.jackson..",
          "jakarta.ws.rs..",
          "org.eclipse.microprofile.reactive.messaging..", "io.smallrye.reactive.messaging..",
          "io.vertx.core.json..", "io.vertx.mutiny.core..", "io.vertx.mutiny.rabbitmq..",
          "com.rabbitmq.client..",
          "org.eclipse.microprofile.config.."};
  String[] ADAPTER_OUT_PERMISSIONS = new String[]
      {"com.google.common.base..", "com.google.common.cache..", "com.fasterxml.jackson..",
          "jakarta.enterprise.event..",
          "org.eclipse.microprofile.reactive.messaging..", "io.smallrye.reactive.messaging..",
          "io.vertx.core.json..",
          "raptor..",
          "org.eclipse.microprofile.config.."};
  String[] USECASE_IN_PERMISSIONS = new String[]
      {};
  String[] USECASE_OUT_PERMISSIONS = new String[]
      {};
}

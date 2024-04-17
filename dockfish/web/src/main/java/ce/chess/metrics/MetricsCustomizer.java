package ce.chess.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MetricsCustomizer {

  @ConfigProperty(name = "quarkus.application.name")
  String applicationName;

  @Produces
  @Singleton
  @MeterFilterConstraint(applyTo = PrometheusMeterRegistry.class)
  public MeterFilter configurePrometheusRegistries() {
    return MeterFilter.commonTags(List.of(Tag.of("application", applicationName)));
  }
}

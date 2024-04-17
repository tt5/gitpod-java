package ce.chess.dockfish.domain.service.run;

import ce.chess.dockfish.domain.model.task.EngineOption;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class UciOptionsConfiguration {

  private static final String UCI_OPTION_PREFIX = "uci_option_";

  @Inject
  Config config;

  public List<EngineOption> getLocalEngineOptions() {
    return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
        .filter(property -> property.startsWith(UCI_OPTION_PREFIX))
        .map(this::createEngineOption)
        .collect(Collectors.toList());
  }

  private EngineOption createEngineOption(String property) {
    return EngineOption.builder()
        .name(property.replace(UCI_OPTION_PREFIX, "").replace("_", " "))
        .value(config.getValue(property, String.class))
        .build();
  }

}

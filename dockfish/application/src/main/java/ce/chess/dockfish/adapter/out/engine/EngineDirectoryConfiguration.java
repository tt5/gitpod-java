package ce.chess.dockfish.adapter.out.engine;

import ce.chess.dockfish.usecase.out.engine.ListEngines;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
@Log4j2
public class EngineDirectoryConfiguration implements ListEngines {

  private static final String ENGINE_DIR_PROPERTY = "engine_directory";
  private static final String DEFAULT_ENGINE_DIR = "/engines";
  private static final String ADDITIONAL_ENGINE_DIR_PROPERTY = "additional_engine_directory";

  @Inject
  Config config;

  public String validatedProcessPathFor(String engineName) {
    if (engineName.startsWith("..") || engineName.startsWith("/") || engineName.startsWith("\\")) {
      throw new IllegalArgumentException("Illegal engine program name: " + engineName);
    }
    return Optional.ofNullable(enginePathsByEngineName().get(engineName))
        .orElseThrow(() -> new IllegalArgumentException("Engine not found: " + engineName));
  }

  @Override
  public Set<String> listEngineNames() {
    return enginePathsByEngineName().keySet();
  }

  public void contextInitialized(@Observes StartupEvent sce) {
    log.info("Available engines: {}", listEngineNames());
  }

  private Map<String, String> enginePathsByEngineName() {
    Map<String, String> result = new ConcurrentHashMap<>();
    appendFilesToMap(getEngineDirectory(), result);
    getAdditionalEngineDirectory().ifPresent(
        directory -> appendFilesToMap(directory, result));
    return result;
  }

  private void appendFilesToMap(String directory, Map<String, String> result) {
    Path dir = Paths.get(directory);
    if (Files.exists(dir)) {
      try (Stream<Path> stream = Files.list(dir)) {
        stream
            .filter(Files::isRegularFile)
            .filter(Files::isExecutable)
            .forEach(p -> result.put(p.getFileName().toString(), p.toString()));
      } catch (IOException ioe) {
        throw new UncheckedIOException(ioe);
      }
    }
  }

  String getEngineDirectory() {
    return config.getOptionalValue(ENGINE_DIR_PROPERTY, String.class)
        .orElse(DEFAULT_ENGINE_DIR);
  }

  private Optional<String> getAdditionalEngineDirectory() {
    return config.getOptionalValue(ADDITIONAL_ENGINE_DIR_PROPERTY, String.class);
  }
}

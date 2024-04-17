package ce.chess.dockfish.adapter.out.engine;

import ce.chess.dockfish.domain.model.task.EngineOption;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.log4j.Log4j2;
import raptor.engine.uci.UCIEngine;
import raptor.engine.uci.UCIOption;
import raptor.engine.uci.options.UCISpinner;

@ApplicationScoped
@Log4j2
public class UciEngineHolder {

  private final EngineDirectoryConfiguration engineDirConfiguration;

  private final UCIEngine uciEngine;

  private final AtomicBoolean isReleasing;

  @Inject
  public UciEngineHolder(EngineDirectoryConfiguration engineDirConfiguration, UCIEngine uciEngine) {
    this.engineDirConfiguration = engineDirConfiguration;
    this.uciEngine = uciEngine;
    this.isReleasing = new AtomicBoolean(false);
  }

  public UCIEngine getEngine() {
    return uciEngine;
  }

  public UCIEngine connect(String engineName) {
    return connect(engineName, 0, List.of());
  }

  public UCIEngine connect(String engineName, int initialPVs, List<EngineOption> options) {
    if (uciEngine.isProcessingGo()) {
      throw new IllegalStateException("UCIEngine is already active");
    }

    disconnect();

    uciEngine.setProcessPath(engineDirConfiguration.validatedProcessPathFor(engineName));

    log.info("Connecting to {}", uciEngine.getProcessPath());
    if (!uciEngine.connect()) {
      throw new IllegalStateException("Failed to connect engine " + uciEngine.getProcessPath());
    }

    setMultiPv(initialPVs);
    options.forEach(option -> setOption(option.getName(), option.getValue()));
    Arrays.stream(uciEngine.getOptionNames())
        .map(uciEngine::getOption)
        .sorted(Comparator.comparing(UCIOption::getName))
        .forEach(log::info);
    return uciEngine;
  }

  public void disconnect() {
    if (!isConnected()) {
      return;
    }
    if (isReleasing.compareAndSet(false, true)) {
      log.info("Disconnecting from {}", uciEngine.getProcessPath());
      try {
        uciEngine.quit();
      } finally {
        isReleasing.compareAndSet(true, false);
      }
    }
  }

  public boolean isConnected() {
    return uciEngine.isConnected();
  }

  private void setMultiPv(int initialPVs) {
    UCIOption multiPv = new UCISpinner();
    multiPv.setName("MultiPV");
    multiPv.setValue(Integer.toString(initialPVs));
    uciEngine.setOption(multiPv);
  }

  private void setOption(String optionKey, String optionValue) {
    if (uciEngine.hasOption(optionKey)) {
      UCIOption uciOption = uciEngine.getOption(optionKey);
      uciOption.setName(optionKey);
      uciOption.setValue(optionValue);
      uciEngine.setOption(uciOption);
    } else {
      log.warn("Engine option ignored: {}", optionKey);
    }
  }

}

package ce.chess.dockfish.domain.service.query;

import ce.chess.dockfish.usecase.in.QueryConfiguration;
import ce.chess.dockfish.usecase.out.engine.ListEngines;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class EnginePathService implements QueryConfiguration {
  private final ListEngines listEngines;

  @Inject
  public EnginePathService(ListEngines listEngines) {
    this.listEngines = Objects.requireNonNull(listEngines);
  }

  @Override
  public Set<String> listEngineNames() {
    return listEngines.listEngineNames();
  }
}

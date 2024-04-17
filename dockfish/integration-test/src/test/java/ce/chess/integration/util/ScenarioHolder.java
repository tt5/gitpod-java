package ce.chess.integration.util;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class ScenarioHolder {
  private Scenario scenario;

  public Scenario getScenario() {
    return scenario;
  }

  @Before
  public void setupScenario(Scenario newScenario) {
    scenario = newScenario;
  }
}

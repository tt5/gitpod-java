package ce.chess.integration.model;

public class SubmitTaskCommand {
  public String name;
  public String pgn;
  public String engineId;
  public Integer initialPv;
  public String maxDuration;
  public boolean useSyzygyPath;

  SubmitTaskCommand() {
  }

  public SubmitTaskCommand(String name, String pgn, String engineId, Integer initialPv, String maxDuration,
                           boolean useSyzygyPath) {
    this.name = name;
    this.pgn = pgn;
    this.engineId = engineId;
    this.initialPv = initialPv;
    this.maxDuration = maxDuration;
    this.useSyzygyPath = useSyzygyPath;
  }
}

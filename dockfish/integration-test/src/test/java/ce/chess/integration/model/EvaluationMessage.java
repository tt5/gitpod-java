package ce.chess.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public class EvaluationMessage {
  public String taskName;
  public String analysedPgn;
  public String analysedFen;
  public Integer analysedPly;
  public String uciEngineName;
  public int taskDepth;
  public String taskDuration;
  public String hostname;
  public String status;
  public Evaluation evaluation;
  public LocalDateTime taskStarted;
  public LocalDateTime lastEvaluation;
  public LocalDateTime lastAlive;
  public List<EngineInformation> latestEvents;
  public List<String> history;

}

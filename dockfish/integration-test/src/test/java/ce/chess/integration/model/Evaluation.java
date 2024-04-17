package ce.chess.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public class Evaluation {
  public String taskId;
  public LocalDateTime created;
  public List<Variation> variations;
  public UciState uciState;

}

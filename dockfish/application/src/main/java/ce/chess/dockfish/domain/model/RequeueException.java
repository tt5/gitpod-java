package ce.chess.dockfish.domain.model;

import java.io.Serial;

public class RequeueException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 42L;

  public RequeueException(String message, Throwable cause) {
    super(message, cause);
  }
}

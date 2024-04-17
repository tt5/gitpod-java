package ce.chess.dockfish.usecase.out.engine;

public interface LockEngine {
  void acquireLock();

  boolean tryAcquireLock();

  void releaseLock();

  void blockWhileActive();
}

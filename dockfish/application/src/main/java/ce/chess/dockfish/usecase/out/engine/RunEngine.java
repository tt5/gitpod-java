package ce.chess.dockfish.usecase.out.engine;

import ce.chess.dockfish.domain.model.task.AnalysisRun;

public interface RunEngine {
  AnalysisRun startAnalysis(AnalysisRun analysisRun);

  void stop();

  void kill();
}

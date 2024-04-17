package ce.chess.integration;

import ce.chess.integration.model.SubmitTaskCommand;

import java.util.Objects;

public class TaskWorld {
  private String taskId;
  private SubmitTaskCommand submitTaskCommand;

  public void set(String taskId) {
    this.taskId = Objects.requireNonNull(taskId);
  }

  public void set(SubmitTaskCommand submitTaskCommand) {
    this.submitTaskCommand = Objects.requireNonNull(submitTaskCommand);
  }

  public String getTaskId() {
    return taskId;
  }

  public SubmitTaskCommand getSubmittedTaskCommand() {
    return submitTaskCommand;
  }

}

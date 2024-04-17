package ce.chess.dockfish.adapter.in.rabbit;

import ce.chess.dockfish.adapter.RabbitConfiguration;
import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;
import ce.chess.dockfish.adapter.common.mapper.SubmitTaskCommandMapper;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.in.ReceiveAnalysisRequest;

import com.google.common.util.concurrent.Uninterruptibles;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
@Log4j2
public class TaskSubmittedMessageConsumer extends AbstractMessageConsumer {

  @Inject
  Config config;

  @Inject
  SubmitTaskCommandMapper submitTaskCommandMapper;
  @Inject
  ReceiveAnalysisRequest service;

  @Incoming(RabbitConfiguration.CHANNEL_TASK_SUBMITTED)
  @Incoming(RabbitConfiguration.CHANNEL_TASK_SUBMITTED_2)
  @Blocking
  public CompletionStage<Void> consume(Message<JsonObject> message) {
    return super.consumeMessage(message);
  }

  @Override
  protected void handleContent(JsonObject jsonObject) {
    SubmitTaskCommand request = jsonObject.mapTo(SubmitTaskCommand.class);
    request.validate();

    // be gentle to StaticEvaluationRequestsConsumer
    Long initialDelay = config.getOptionalValue("task_consumer_delay_seconds", Long.class).orElse(3L);
    Uninterruptibles.sleepUninterruptibly(initialDelay, TimeUnit.SECONDS);

    AnalysisRun analysisRun =
        submitTaskCommandMapper.toDomainObject(request, LocalDateTime.now(ZoneId.systemDefault()));
    TaskId taskId = service.startSync(analysisRun)
        .orElseThrow(() -> new IllegalStateException("No Task Id. Engine not running"));
    log.info("Finished calculation of taskID {}", taskId);
  }

}

package ce.chess.dockfish.adapter.out.rabbit.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.log4j.Log4j2;

@ApplicationScoped
@Log4j2
class FallbackToFilePublisher {

  private static final String PARENT_FOLDER = "unpublished";
  private static final String FILE_PREFIX = "fallback_";
  private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

  void writeToFile(@Observes PublishFailed publishFailed) {
    String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(DATE_PATTERN);
    String fileName = FILE_PREFIX + publishFailed.getExchangeName() + "_" + timestamp + ".json";

    File file = Paths.get(PARENT_FOLDER, fileName).toFile();
    log.warn("Write message to {}", file.getAbsolutePath());

    boolean directoryExists = file.getParentFile().exists() || file.getParentFile().mkdirs();
    if (!directoryExists) {
      log.error("Directory does not exist {}", file.getAbsolutePath());
    }

    try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
      out.write(publishFailed.getMessage());
    } catch (IOException ex) {
      log.error("Failed to write fallback file", ex);
    }
  }
}

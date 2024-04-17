package ce.chess.dockfish.adapter.out.rabbit.fallback;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.google.common.io.Files;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FallbackToFilePublisherTest {
  private File expectedFile;

  @Inject
  Event<PublishFailed> publishFailedEvent;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    expectedFile = new File(
        "unpublished/fallback_" + "exchange_" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".json");
  }

  @AfterEach
  void removeFile() {
    if (expectedFile != null && expectedFile.exists()) {
      expectedFile.delete();
    }
  }

  @Test
  void doesObserveEventAndWritesFile() throws IOException {
    fire(new PublishFailed("exchange", "message"));

    assertThat(expectedFile.exists(), is(true));
    assertThat(Files.asCharSource(expectedFile, StandardCharsets.UTF_8).read(), is(equalTo("message")));
  }

  private void fire(PublishFailed event) {
    publishFailedEvent.fire(event);
  }

}

package ce.chess.dockfish.adapter.out.rabbit.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
@Log4j2
class FallbackHttpPublisher {
  private static final String FALLBACK_POST_URL = "fallback_post_url";

  @Inject
  Config config;

  void postToHttpServer(@Observes PublishFailed publishFailed) {
    getRestUrl().ifPresent(url -> doPost(url, publishFailed));
  }

  private void doPost(String url, PublishFailed publishFailed) {
    log.warn("Post message to {}", url);
    try {
      URL apiUrl = URI.create(url).toURL();
      HttpURLConnection httpConnection = (HttpURLConnection) apiUrl.openConnection();
      httpConnection.setDoOutput(true);
      httpConnection.setRequestMethod("POST");
      httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      try (DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream())) {
        out.write(publishFailed.getMessage().getBytes(StandardCharsets.UTF_8));
      }
      log.info("Response code: {}", httpConnection.getResponseCode());
    } catch (IOException ex) {
      log.error("Failed to post to fallback server", ex);
    }
  }

  private Optional<String> getRestUrl() {
    return config.getOptionalValue(FALLBACK_POST_URL, String.class);
  }

}

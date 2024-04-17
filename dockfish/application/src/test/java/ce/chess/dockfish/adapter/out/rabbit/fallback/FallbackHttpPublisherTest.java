package ce.chess.dockfish.adapter.out.rabbit.fallback;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.BDDMockito.given;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FallbackHttpPublisherTest {

  @Mock
  Config config;

  @InjectMocks
  FallbackHttpPublisher cut;

  private static final int PORT_FOR_TEST = 8015;
  public static final String FALLBACK_URL = "http://localhost:" + PORT_FOR_TEST + "/evaluations";

  private final WireMockServer wireMockServer = new WireMockServer(options()
      .port(PORT_FOR_TEST));

  @BeforeEach
  void setup() {
    wireMockServer.start();
    configureFor("localhost", PORT_FOR_TEST);
    stubFor(post(urlPathEqualTo("/evaluations"))
        .withRequestBody(equalTo("message"))
        .willReturn(aResponse()
            .withStatus(202)));
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }


  @Test
  void givenUrlThenPost() {
    given(config.getOptionalValue("fallback_post_url", String.class)).willReturn(Optional.of(FALLBACK_URL));

    cut.postToHttpServer(new PublishFailed("exchange", "message"));

    verify(postRequestedFor(urlEqualTo("/evaluations"))
        .withHeader("Content-Type", containing("application/json"))
        .withRequestBody(containing("message")));
  }

}

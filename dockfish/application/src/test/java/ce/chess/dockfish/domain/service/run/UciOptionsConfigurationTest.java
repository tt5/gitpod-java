package ce.chess.dockfish.domain.service.run;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.domain.model.task.EngineOption;

import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class UciOptionsConfigurationTest {

  @Nested
  @QuarkusComponentTest
  class GivenLocalConfigurationIsPresent {
    @Inject
    UciOptionsConfiguration cut;

    @Test
    void thenReturnLocalConfiguration() {
      // read from microprofile-config.properties
      try {
        List<EngineOption> expected = List.of(
            new EngineOption("Hash", "anyHash"),
            new EngineOption("Threads", "anyThreads"),
            new EngineOption("Contempt", "anyContempt"),
            new EngineOption("SyzygyPath", "C:\\tablebases\\3-4-5"),
            new EngineOption("Use NNUE", "true")
        );

        List<EngineOption> actual = cut.getLocalEngineOptions();
        assertThat(actual, containsInAnyOrder(expected.toArray()));
      } catch (Exception ex) { // NOPMD
        ex.printStackTrace(); // NOPMD
      }
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class GivenLocalConfigurationIsNotPresent {
    @Mock
    private Config config;

    @InjectMocks
    private UciOptionsConfiguration cutMocked;

    @Test
    void thenReturnEmptyConfigurations() {
      given(config.getPropertyNames()).willReturn(List.of());

      assertThat(cutMocked.getLocalEngineOptions(), is(empty()));
    }
  }
}

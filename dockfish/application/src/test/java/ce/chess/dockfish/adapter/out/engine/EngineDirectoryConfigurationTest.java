package ce.chess.dockfish.adapter.out.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EngineDirectoryConfigurationTest {

  @Mock
  private Config config;

  @InjectMocks
  private EngineDirectoryConfiguration cut;

  @Nested
  class CanBeConfigured {
    @Test
    void givenNoEnvironmentThenReturnDefaultDirectory() {
      given(config.getOptionalValue("engine_directory", String.class)).willReturn(Optional.empty());

      assertThat(cut.getEngineDirectory(), is(equalTo("/engines")));
    }

    @Test
    void givenEnvironmentThenReturnConfiguredDirectory() {
      given(config.getOptionalValue("engine_directory", String.class)).willReturn(Optional.of("something"));

      assertThat(cut.getEngineDirectory(), is(equalTo("something")));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"\\engine.exe", "..engine", "/engine"})
  void throwsWhenCalledWithInvalidFilename(String engineName) {
    assertThrows(IllegalArgumentException.class, () -> cut.validatedProcessPathFor(engineName));
  }

  @Nested
  class GivenEngineDirectory {
    private File engineExe;
    private File engineExe2;

    private File subdirectory;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @BeforeEach
    void setUp() throws IOException {
      given(config.getOptionalValue("engine_directory", String.class)).willReturn(Optional.of("engineDir"));

      engineExe = new File("engineDir/engine.exe");
      engineExe.setExecutable(true);
      engineExe.getParentFile().mkdir();
      engineExe.createNewFile();
      engineExe2 = new File("engineDir/engine2.exe");
      engineExe2.setExecutable(true);
      engineExe2.getParentFile().mkdir();
      engineExe2.createNewFile();
      try {
        Files.setPosixFilePermissions(engineExe.toPath(), Set.of(
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_EXECUTE));
        Files.setPosixFilePermissions(engineExe2.toPath(), Set.of(
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_EXECUTE));
      } catch (UnsupportedOperationException ex) {
        // may happen on windows
      }
      subdirectory = new File("engineDir/subdirectory");
      subdirectory.setExecutable(true);
      subdirectory.mkdirs();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() {
      engineExe.delete();
      engineExe2.delete();
      subdirectory.delete();
      engineExe.getParentFile().delete();
    }

    @Test
    void returnPathWhenValidEngineName() {
      String processPath = cut.validatedProcessPathFor("engine.exe");

      assertThat(processPath, is(equalTo("engineDir" + File.separator + "engine.exe")));
    }

    @Test
    void listEnginesContainsEngineExe() {
      Set<String> engines = cut.listEngineNames();

      assertThat(engines, contains("engine.exe", "engine2.exe"));
    }

    @Test
    void throwsWhenExecutableEngineIsMissing() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> cut.validatedProcessPathFor("engineNotExisting.exe"));
      assertThat(exception.getMessage(), containsStringIgnoringCase("Engine not found"));
    }

    @Test
    void throwsWhenEngineIsNotaSingleFile() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> cut.validatedProcessPathFor("subdirectory"));

      assertThat(exception.getMessage(), containsStringIgnoringCase("Engine not found"));
    }

    @Nested
    class AndAdditionalEngineDirIsConfigured {
      private File additionalEngineExe;

      @SuppressWarnings("ResultOfMethodCallIgnored")
      @BeforeEach
      void setUp() throws IOException {
        given(config.getOptionalValue("additional_engine_directory", String.class))
            .willReturn(Optional.of("additionalEngineDir"));

        additionalEngineExe = new File("additionalEngineDir/engine.exe");
        additionalEngineExe.setExecutable(true);
        additionalEngineExe.getParentFile().mkdir();
        additionalEngineExe.createNewFile();
        try {
          Files.setPosixFilePermissions(additionalEngineExe.toPath(), new HashSet<>(Arrays.asList(
              PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE,
              PosixFilePermission.OTHERS_EXECUTE)));
        } catch (UnsupportedOperationException ex) {
          // may happen on windows
        }
      }

      @SuppressWarnings("ResultOfMethodCallIgnored")
      @AfterEach
      void tearDown() {
        additionalEngineExe.delete();
        additionalEngineExe.getParentFile().delete();
      }

      @Test
      void listEnginesContainsAdditionalEngineExe() {
        Set<String> engines = cut.listEngineNames();
        assertThat(engines.size(), is(2));
        assertThat(engines, contains("engine.exe", "engine2.exe"));
      }

      @Test
      void returnPathWhenValidEngineName() {
        String processPath = cut.validatedProcessPathFor("engine.exe");

        assertThat(processPath, is(equalTo("additionalEngineDir" + File.separator + "engine.exe")));
      }

      @Test
      void logEngineNamesOnStartup() {
        // add maybe sometimes: LogCaptor, fire Weld event
        cut.contextInitialized(null);
      }
    }
  }
}

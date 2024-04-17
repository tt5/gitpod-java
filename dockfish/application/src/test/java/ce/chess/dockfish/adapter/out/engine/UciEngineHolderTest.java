package ce.chess.dockfish.adapter.out.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.domain.model.task.EngineOption;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import raptor.engine.uci.UCIEngine;
import raptor.engine.uci.UCIOption;
import raptor.engine.uci.options.UCISpinner;

@ExtendWith(MockitoExtension.class)
class UciEngineHolderTest {

  @Captor
  private ArgumentCaptor<UCIOption> optionCaptor;

  @Mock
  private EngineDirectoryConfiguration engineDirectoryConfiguration;

  @Mock
  private UCIEngine uciEngine;

  @InjectMocks
  private UciEngineHolder cut;

  @Test
  void doesReturnUciEngine() {
    given(engineDirectoryConfiguration.validatedProcessPathFor("engine.exe")).willReturn("engineDir/engine.exe");
    given(uciEngine.isProcessingGo()).willReturn(false);
    given(uciEngine.isConnected()).willReturn(true);
    given(uciEngine.connect()).willReturn(true);
    given(uciEngine.hasOption("Name")).willReturn(true);
    given(uciEngine.getOption("Name")).willAnswer(iom -> new UCISpinner());
    given(uciEngine.getOptionNames()).willAnswer(iom -> new String[]{"Name"});

    UCIEngine result =
        cut.connect("engine.exe", 3, List.of(new EngineOption("Name", "Value")));

    assertThat(result, is(notNullValue()));

    InOrder inOrder = Mockito.inOrder(uciEngine);
    inOrder.verify(uciEngine).quit();
    inOrder.verify(uciEngine).setProcessPath("engineDir/engine.exe");
    inOrder.verify(uciEngine).connect();
    inOrder.verify(uciEngine).setOption(optionCaptor.capture());

    assertThat(optionCaptor.getValue().getName(), is("MultiPV"));
    assertThat(optionCaptor.getValue().getValue(), is("3"));

    inOrder.verify(uciEngine).hasOption("Name");
    inOrder.verify(uciEngine).setOption(optionCaptor.capture());
    assertThat(optionCaptor.getValue().getValue(), is("Value"));
  }

  @Test
  void throwsWhenEngineIsRunning() {
    given(uciEngine.isProcessingGo()).willReturn(true);

    Exception exception = assertThrows(IllegalStateException.class,
        () -> cut.connect("engine.exe", 3, List.of()));
    assertThat(exception.getMessage(), containsStringIgnoringCase("is already active"));
  }

  @Test
  void throwsWhenConnectFails() {
    given(uciEngine.isProcessingGo()).willReturn(false);
    given(uciEngine.connect()).willReturn(false);

    Exception exception = assertThrows(IllegalStateException.class,
        () -> cut.connect("engine.exe", 3, List.of()));
    assertThat(exception.getMessage(), containsStringIgnoringCase("Failed to connect engine"));
  }

}

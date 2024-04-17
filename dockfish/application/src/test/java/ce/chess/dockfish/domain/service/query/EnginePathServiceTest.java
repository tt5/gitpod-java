package ce.chess.dockfish.domain.service.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.usecase.out.engine.ListEngines;

import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnginePathServiceTest {

  @Mock
  private ListEngines listEngines;

  @InjectMocks
  private EnginePathService cut;

  @Test
  void delegatesToAdapter() {
    given(listEngines.listEngineNames()).willReturn(Set.of("e1", "e2"));

    Set<String> result = cut.listEngineNames();

    assertThat(result, CoreMatchers.hasItems("e1", "e2"));
  }
}

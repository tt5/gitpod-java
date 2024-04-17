package ce.chess.dockfish.domain.service.run;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.DynamicPv;
import ce.chess.dockfish.usecase.out.engine.ReducePv;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdaptPvServiceTest {
  private static final int CURRENT_EVENT_DEPTH = 40;

  @Mock
  private ReducePv reducePv;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Evaluation evaluation;

  @Mock
  private AnalysisRun engineTask;

  @InjectMocks
  private AdaptPvService cut;

  @BeforeEach
  void setupEvaluationWithThreeVariations() {
    when(evaluation.getVariations())
        .thenReturn(List.of(mock(Variation.class), mock(Variation.class), mock(Variation.class)));
    lenient().when(evaluation.sizeOfCurrentVariations()).thenReturn(3);
    lenient().when(evaluation.maxDepth()).thenReturn(CURRENT_EVENT_DEPTH);
  }

  @Test
  void givenExistingDepthIsHigherThenDoNothing() {
    given(engineTask.dynamicPv()).willReturn(Optional.of(new DynamicPv(CURRENT_EVENT_DEPTH + 1, 30, 1)));

    cut.adaptPv(evaluation, engineTask);

    verify(evaluation, never()).sizeOfCurrentVariations();
    verifyNoInteractions(reducePv);
  }

  @Test
  void givenMinimumPvReachedThenDoNotReduce() {
    given(engineTask.dynamicPv()).willReturn(Optional.of(new DynamicPv(CURRENT_EVENT_DEPTH - 1, 30, 3)));

    cut.adaptPv(evaluation, engineTask);

    verify(evaluation).sizeOfCurrentVariations();
    verifyNoInteractions(reducePv);
  }

  @Test
  void givenAllVariationsAreGoodThenDoNotReduce() {
    given(engineTask.dynamicPv()).willReturn(Optional.of(new DynamicPv(CURRENT_EVENT_DEPTH - 1, 30, 1)));
    given(evaluation.determineNumberOfGoodPv(30)).willReturn(3);

    cut.adaptPv(evaluation, engineTask);

    verify(evaluation).determineNumberOfGoodPv(30);
    verifyNoInteractions(reducePv);
  }

  @Test
  void givenKeepOnePvThenReduceToTwo() {
    given(engineTask.dynamicPv()).willReturn(Optional.of(new DynamicPv(CURRENT_EVENT_DEPTH - 1, 30, 1)));
    given(evaluation.determineNumberOfGoodPv(30)).willReturn(2);

    cut.adaptPv(evaluation, engineTask);

    verify(reducePv).reducePvTo(2);
  }

  @Test
  void givenKeepTwoPvThenReduceToTwo() {
    given(engineTask.dynamicPv()).willReturn(Optional.of(new DynamicPv(CURRENT_EVENT_DEPTH - 1, 30, 2)));
    given(evaluation.determineNumberOfGoodPv(30)).willReturn(1);

    cut.adaptPv(evaluation, engineTask);

    verify(reducePv).reducePvTo(2);
  }
}

package ce.chess.dockfish.domain.model.result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

import ce.chess.dockfish.adapter.common.chess.GamePositionService;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EvaluationTest {
  @Mock
  Variation variation1;

  @Mock
  Variation variation2;

  @Mock
  Variation variation3;

  @Mock
  Evaluation evaluation;

  Score score100 = Score.fromCentiPawns(100);
  Score score50 = Score.fromCentiPawns(50);
  Score scoreMinus100 = Score.fromCentiPawns(-100);
  Score scoreMinus50 = Score.fromCentiPawns(-50);

  @BeforeEach
  void setUp() {
    given(variation1.getDepth()).willReturn(25);
    given(variation2.getDepth()).willReturn(25);
    given(variation3.getDepth()).willReturn(25);

    given(evaluation.getVariations()).willReturn(List.of(variation1, variation2, variation3));

  }

  @Nested
  class WhenWhiteIsToMove {
    @BeforeEach
    void whitesMove() {
      given(variation1.getGamePosition()).willReturn(new GamePositionService().createFrom("1. e4 e5 *"));
    }

    @Test
    void hasOneGoodVariation() {
      given(variation1.getScore()).willReturn(score100);
      given(variation2.getScore()).willReturn(score50);
      given(variation3.getScore()).willReturn(scoreMinus50);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(1)));
    }

    @Test
    void hasTwoGoodVariation() {
      given(variation1.getScore()).willReturn(scoreMinus50);
      given(variation2.getScore()).willReturn(scoreMinus50);
      given(variation3.getScore()).willReturn(scoreMinus100);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(2)));

    }

    @Test
    void hasAllGoodVariation() {
      given(variation1.getScore()).willReturn(score100);
      given(variation2.getScore()).willReturn(score100);
      given(variation3.getScore()).willReturn(score100);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(3)));
    }

  }

  @Nested
  class WhenBlackIsToMove {
    @BeforeEach
    void blackMove() {
      given(variation1.getGamePosition()).willReturn(new GamePositionService().createFrom("1. e4 *"));
    }

    @Test
    void hasOneGoodVariation() {
      given(variation1.getScore()).willReturn(scoreMinus100);
      given(variation2.getScore()).willReturn(scoreMinus50);
      given(variation3.getScore()).willReturn(score100);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(1)));
    }

    @Test
    void hasTwoGoodVariation() {
      given(variation1.getScore()).willReturn(scoreMinus50);
      given(variation2.getScore()).willReturn(scoreMinus50);
      given(variation3.getScore()).willReturn(score50);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(2)));

    }

    @Test
    void hasAllGoodVariation() {
      given(variation1.getScore()).willReturn(score100);
      given(variation2.getScore()).willReturn(score100);
      given(variation3.getScore()).willReturn(score100);

      int result = Evaluation.determineNumberOfGoodPv(List.of(variation1, variation2, variation3), 20);
      assertThat(result, is(equalTo(3)));
    }
  }

  @Nested
  class TestAllVariationsHaveSameDepth {

    @BeforeEach
    void setup() {
      given(evaluation.currentVariations()).willCallRealMethod();
      given(evaluation.maxDepth()).willCallRealMethod();
    }

    @Test
    void whenAllHaveSameDepthReturnTrue() {
      given(variation1.getDepth()).willReturn(25);
      given(variation2.getDepth()).willReturn(25);
      given(variation3.getDepth()).willReturn(25);
      assertThat(Evaluation.allVariationsHavingSameDepth().test(evaluation), is(true));
    }

    @Test
    void whenOneHasDifferentDepthReturnFalse() {
      given(variation1.getDepth()).willReturn(25);
      given(variation2.getDepth()).willReturn(25);
      given(variation3.getDepth()).willReturn(24);

      assertThat(Evaluation.allVariationsHavingSameDepth().test(evaluation), is(false));
    }

    @Test
    void whenOnlyOneIsOldReturnTrue() {
      given(variation1.getDepth()).willReturn(25);
      given(variation2.getDepth()).willReturn(25);
      given(variation3.getDepth()).willReturn(23);

      assertThat(Evaluation.allVariationsHavingSameDepth().test(evaluation), is(true));
    }

    @Test
    void whenOneIsOldReturnFalse() {
      given(variation1.getDepth()).willReturn(25);
      given(variation2.getDepth()).willReturn(24);
      given(variation3.getDepth()).willReturn(23);

      assertThat(Evaluation.allVariationsHavingSameDepth().test(evaluation), is(false));
    }
  }
}

package ce.chess.dockfish.adapter.in.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;


@QuarkusComponentTest
class ObjectMapperContextResolverTest {

  @Inject
  ObjectMapperContextResolver cut;

  @Inject
  ObjectMapper objectMapper;

  @Test
  void producesObjectMapper() {
    assertThat(objectMapper, is(notNullValue()));
  }

  @Test
  void isJaxrsProvider() {
    assertThat(cut.getClass().isAnnotationPresent(Provider.class), is(true));
  }

  @Test
  void producedMapperSerializesDate() throws JsonProcessingException {
    String dateString = cut.getContext(LocalDate.class)
        .writeValueAsString(LocalDate.of(2017, Month.FEBRUARY, 7));
    assertThat(dateString, is(equalTo("\"2017-02-07\"")));
  }

  @Test
  void producedMapperSerializesDuration() throws JsonProcessingException {
    String dateString = cut.getContext(Duration.class)
        .writeValueAsString(Duration.of(3662, ChronoUnit.SECONDS));
    assertThat(dateString, is(equalTo("\"PT1H1M2S\"")));
  }

  @Test
  void producedMapperDoesNotSerializeEmptyValues() throws JsonProcessingException {
    WithEmptyValues withEmptyValues = new WithEmptyValues();

    String json = objectMapper.writeValueAsString(withEmptyValues);

    assertThat(json, not(containsString("null")));
    assertThat(json, containsString("nonEmpty"));
    assertThat(json, containsString("emptyList"));
    assertThat(json, containsString("blank"));
  }

  private class WithEmptyValues {
    public String getNonEmpty() {
      return "value";
    }

    public String getBlank() {
      return "";
    }

    public Optional<Object> getEmpty() {
      return Optional.empty();
    }

    public Object getNullValue() {
      return null;
    }

    public List<Object> getEmptyList() {
      return Collections.emptyList();
    }
  }
}

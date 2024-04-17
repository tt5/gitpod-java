package ce.chess.dockfish.adapter.in.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonInputExceptionMapperTest {

  JsonInputExceptionMapper cut;
  private Response response;

  private static LogCaptor logCaptor;

  @BeforeAll
  public static void setupLogCaptor() {
    logCaptor = LogCaptor.forClass(JsonInputExceptionMapper.class);
  }

  @AfterEach
  public void clearLogCaptor() {
    logCaptor.clearLogs();
  }

  @AfterAll
  public static void closeLogCaptor() {
    logCaptor.resetLogLevel();
    logCaptor.close();
  }

  @BeforeEach
  void setUp() {
    cut = new JsonInputExceptionMapper();
  }

  @Test
  void shouldBeAJaxRsProvider() {
    assertThat(cut.getClass().isAnnotationPresent(Provider.class), is(true));
  }

  @Nested
  class GivenJsonMappingException {

    JsonMappingException exception = new JsonMappingException(null, "irrelevant json mapping message");

    @BeforeEach
    void setUp() {
      response = cut.toResponse(exception);
    }

    @Test
    void shouldReturnBadRequestStatusCode() {
      assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }

    @Test
    void shouldLogWarning() {
      assertThat(logCaptor.getWarnLogs(), hasItem(containsString(
          "JsonProcessingException occurred: com.fasterxml.jackson.databind.JsonMappingException: "
              + "irrelevant json mapping message")));
    }
  }

}

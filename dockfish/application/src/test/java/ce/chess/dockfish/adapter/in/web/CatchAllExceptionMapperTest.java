package ce.chess.dockfish.adapter.in.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CatchAllExceptionMapperTest {

  private CatchAllExceptionMapper cut;
  private Response response;

  private static LogCaptor logCaptor;

  @BeforeAll
  public static void setupLogCaptor() {
    logCaptor = LogCaptor.forClass(CatchAllExceptionMapper.class);
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
    cut = new CatchAllExceptionMapper();
  }

  @Test
  void shouldBeAJaxRsProvider() {
    assertThat(cut.getClass().isAnnotationPresent(Provider.class), is(true));
  }

  @Nested
  class GivenRuntimeException {
    Exception exception = new RuntimeException("Testing the CatchAllExceptionMapper");

    @BeforeEach
    void setUp() {
      response = cut.toResponse(exception);
    }

    @Test
    void shouldReturnHttpStatusInternalServerError() {
      assertThat(response.getStatus(), is(equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
    }

    @Test
    void shouldLogError() {
      assertThat(logCaptor.getErrorLogs(), hasItem(containsString("Exception occurred")));
    }
  }

  @Nested
  class GivenExceptionIsOfTypeWebApplicationException {

    int httpStatusCode = Response.Status.NOT_FOUND.getStatusCode();
    Exception exception = new WebApplicationException("irrelevant message", httpStatusCode);

    @BeforeEach
    void setUp() {
      response = cut.toResponse(exception);
    }

    @Test
    void shouldReturnWebApplicationExceptionStatusCode() {
      assertThat(response.getStatus(), is(equalTo(httpStatusCode)));
    }

    @Test
    void shouldLogInfo() {
      assertThat(logCaptor.getInfoLogs(), hasItem(containsString("WebApplicationException occurred")));
    }
  }

  @Nested
  class GivenIllegalArgumentException {

    Exception exception = new IllegalArgumentException("irrelevant message");

    @BeforeEach
    void setUp() {
      response = cut.toResponse(exception);
    }

    @Test
    void shouldReturnWebApplicationExceptionStatusCode() {
      assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }
  }

  @Nested
  class GivenCauseIsIllegalArgumentException {

    class MyException extends Exception {
      MyException(Throwable cause) {
        super(cause);
      }
    }

    Exception exception = new MyException(new IllegalArgumentException("wrappedIAE for test"));

    @BeforeEach
    void setUp() {
      response = cut.toResponse(exception);
    }

    @Test
    void shouldReturnWebApplicationExceptionStatusCode() {
      assertThat(response.getStatus(), is(equalTo(Response.Status.BAD_REQUEST.getStatusCode())));
    }

    @Test
    void shouldLogInfo() {
      assertThat(logCaptor.getInfoLogs(), hasItem(containsString("Illegal Argument occurred")));
    }
  }
}

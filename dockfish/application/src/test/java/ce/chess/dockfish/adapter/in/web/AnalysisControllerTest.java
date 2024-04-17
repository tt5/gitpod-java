package ce.chess.dockfish.adapter.in.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.domain.model.result.AnalysisTime;
import ce.chess.dockfish.domain.model.result.EngineInformation;
import ce.chess.dockfish.domain.model.result.Evaluation;
import ce.chess.dockfish.domain.model.result.EvaluationMessage;
import ce.chess.dockfish.domain.model.result.GamePosition;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.result.Score;
import ce.chess.dockfish.domain.model.result.UciState;
import ce.chess.dockfish.domain.model.result.Variation;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.domain.service.run.InfiniteAnalysisService;
import ce.chess.dockfish.usecase.in.QueryConfiguration;
import ce.chess.dockfish.usecase.in.QueryEvaluation;

import com.google.common.io.Resources;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class AnalysisControllerTest {
  private static final String BASE_URI = "/api/tasks";
  private static final String POST_URI = BASE_URI;
  private static final String GET_URI = BASE_URI;

  private static final TaskId taskId = new TaskId("taskId");
  private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());

  private static final EvaluationMessage evaluationMessage = EvaluationMessage.builder()
      .taskName("taskName")
      .reference("reference_id")
      .analysedPgn("analysedPgn")
      .analysedFen("analysedFen")
      .uciEngineName("uciEngineName")
      .hostname("testhost")
      .status(JobStatus.NOT_ACTIVE)
      .evaluation(Evaluation.builder()
          .taskId(taskId)
          .created(NOW)
          .variation(Variation.builder()
              .pvId(1)
              .moves("move")
              .score(Score.fromCentiPawns(-42))
              .depth(25)
              .time(AnalysisTime.fromMilliSeconds(2222))
              .gamePosition(GamePosition.builder()
                  .pgn("pgn")
                  .build())
              .build())
          .uciState(UciState.builder()
              .kiloNodes(100)
              .kiloNodesPerSecond(10)
              .tbHits(1)
              .infoString("some infoString")
              .build())
          .build())
      .lastAlive(NOW)
      .taskStarted(NOW)
      .history(List.of("history1", "history2"))
      .latestEvent(EngineInformation.builder()
          .multiPv(1)
          .lineSan("lineSan")
          .occurredOn(NOW)
          .depth(22)
          .score(43)
          .time("00:10:00")
          .build())
      .build();

  private static final AnalysisRun ANALYSIS_RUN = AnalysisRun.builder()
      .taskId(taskId)
      .name("name")
      .created(NOW)
      .startingPosition(GamePosition.builder()
          .pgn("pgn")
          .lastMovePly(42)
          .build())
      .engineProgramName("taskDetails.engineProgramName()")
      .hostname("testhost")
      .initialPv(3)
      .maxDepth(30)
      .build();

  @InjectMock
  private InfiniteAnalysisService analysisService;

  @InjectMock
  private QueryEvaluation messageService;

  @InjectMock
  private QueryConfiguration queryConfiguration;

  private final ArgumentCaptor<AnalysisRun> analysisRunCaptor = ArgumentCaptor.forClass(AnalysisRun.class);

  @BeforeEach
  void setUp() {
    when(analysisService.startAsync(any())).thenReturn(Optional.of(taskId));
    when(analysisService.getJobStatus(taskId)).thenReturn(JobStatus.NOT_ACTIVE).thenReturn(JobStatus.ACTIVE);
    when(analysisService.getTaskDetails(taskId)).thenReturn(ANALYSIS_RUN);
  }

  @Test
  void listEngineReturnsResult() {
    when(queryConfiguration.listEngineNames()).thenReturn(Set.of("engine1", "engine2"));

    given()
        .accept(ContentType.JSON)
        .when()
        .get("/api/engines")
        .then()
        .log().ifValidationFails()
        .statusCode(HttpStatus.SC_OK)
        .body("$", contains("engine1", "engine2"));
  }

  @Test
  void postTask_depthOrDurationMustBePresent() {
    String payload = "{\"name\": \"someName\", \"pgn\": \"value\", \"initialPv\": 3}";

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(payload.getBytes(StandardCharsets.UTF_8))
        .when()
        .post(POST_URI)
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void postTask_invalidDuration() {
    String payload = "{\"name\": \"someName\", \"pgn\": \"value\", \"maxDuration\": \"invalid\", \"initialPv\": 3}";

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(payload.getBytes(StandardCharsets.UTF_8))
        .when()
        .post(POST_URI)
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void postTask_acceptsValidMessageAndDelegatesToService() throws IOException {
    String payload = resourceToString(AnalysisControllerTest.class.getSimpleName() + ".json");

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(payload.getBytes(StandardCharsets.UTF_8))
        .when()
        .post(POST_URI)
        .then()
        .statusCode(HttpStatus.SC_ACCEPTED);

    verify(analysisService).startAsync(analysisRunCaptor.capture());
    AnalysisRun analysisRun = analysisRunCaptor.getValue();
    assertThat(analysisRun.maxDuration().orElseThrow().toHours(), is(equalTo(5L)));
    assertThat(analysisRun.engineOptions(), hasSize(2));
    assertThat(analysisRun.name(), is(notNullValue()));

  }

  @Test
  void getResult_returnsResult() {
    when(messageService.getLastEvaluationMessage(taskId)).thenReturn(Optional.of(evaluationMessage));

    EvaluationMessageDto expected = EvaluationMessageDto.builder()
        .taskName("taskName")
        .reference("reference_id")
        .analysedPgn("analysedPgn")
        .analysedFen("analysedFen")
        .uciEngineName("uciEngineName")
        .hostname("testhost")
        .status("NOT_ACTIVE")
        .evaluation(EvaluationMessageDto.EvaluationDto.builder()
            .taskId(taskId.getRawId())
            .created(NOW)
            .variation(EvaluationMessageDto.EvaluationDto.VariationDto.builder()
                .pvId(1)
                .moves("move")
                .score("-0.42")
                .depth(25)
                .time("00:00:02")
                .pgn("pgn")
                .build())
            .uciState(EvaluationMessageDto.EvaluationDto.UciStateDto.builder()
                .kiloNodes(100)
                .kiloNodesPerSecond(10)
                .tbHits(1)
                .infoStrings(List.of("some infoString"))
                .build())
            .build())
        .lastAlive(NOW)
        .taskStarted(NOW)
        .latestEvent(EvaluationMessageDto.EngineEventDto.builder()
            .multiPv(1)
            .lineSan("lineSan")
            .occurredOn(NOW)
            .depth(22)
            .score(43)
            .time("00:10:00")
            .build())
        .history(List.of("history1", "history2"))
        .build();

    EvaluationMessageDto actual = given()
        .accept(ContentType.JSON)
        .when()
        .get(GET_URI + "/" + taskId.getRawId())
        .then()
        .statusCode(HttpStatus.SC_OK)
        .and()
        .extract().body().as(EvaluationMessageDto.class);

    assertThat(actual, is(equalTo(expected)));
  }

  @Test
  void getResultCurrent_returnsResult() {
    when(messageService.getLastEvaluationMessage()).thenReturn(Optional.of(evaluationMessage));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .when()
        .get(GET_URI + "/current")
        .then()
        .statusCode(HttpStatus.SC_OK);

    verify(messageService).getLastEvaluationMessage();
  }

  @Test
  void getTaskList_returnsList() {
    when(messageService.getAllTaskIds())
        .thenReturn(List.of(new TaskId("task1"), new TaskId("task2")));
    when(analysisService.getJobStatus(any())).thenReturn(JobStatus.NOT_ACTIVE).thenReturn(JobStatus.ACTIVE);
    when(analysisService.getTaskDetails(any())).thenReturn(ANALYSIS_RUN);

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .when()
        .get(GET_URI)
        .then()
        .statusCode(HttpStatus.SC_OK);

    verify(messageService).getAllTaskIds();
  }

  @Test
  void postStopTask_delegates() {
    when(messageService.getLastEvaluationMessage(taskId)).thenReturn(Optional.of(evaluationMessage));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .when()
        .post(POST_URI + "/" + taskId.getRawId() + "/stop")
        .then()
        .statusCode(HttpStatus.SC_OK);

    verify(analysisService).stop();
  }

  @Test
  void getStopTask_delegates() {
    when(analysisService.stop()).thenReturn(true);
    when(messageService.getLastEvaluationMessage()).thenReturn(Optional.of(evaluationMessage));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .when()
        .get(GET_URI + "/stop")
        .then()
        .statusCode(HttpStatus.SC_OK);

    verify(analysisService).stop();
  }

  @Test
  void getKill_delegates() {
    when(analysisService.kill()).thenReturn(true);
    when(messageService.getLastEvaluationMessage()).thenReturn(Optional.of(evaluationMessage));

    given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .when()
        .get(GET_URI + "/kill")
        .then()
        .statusCode(HttpStatus.SC_OK);

    verify(analysisService).kill();
  }

  private static String resourceToString(String filename) throws IOException {
    URL url = Resources.getResource(filename);
    return Resources.toString(url, StandardCharsets.UTF_8);
  }

}

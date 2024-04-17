package ce.chess.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import ce.chess.integration.api.RequestResponseDump;
import ce.chess.integration.api.ResponseWorld;
import ce.chess.integration.model.EngineInformation;
import ce.chess.integration.model.Evaluation;
import ce.chess.integration.model.EvaluationMessage;
import ce.chess.integration.model.SubmitTaskCommand;
import ce.chess.integration.model.UciState;
import ce.chess.integration.model.Variation;
import ce.chess.integration.util.OutputObjectMapper;
import ce.chess.integration.util.ResourceUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;

public class EvaluationStepDef {
  private final ResponseWorld responseWorld;
  private final TaskWorld taskWorld;
  private final RequestResponseDump requestResponseDump;

  public EvaluationStepDef(ResponseWorld responseWorld, TaskWorld taskWorld, RequestResponseDump requestResponseDump) {
    this.responseWorld = responseWorld;
    this.taskWorld = taskWorld;
    this.requestResponseDump = requestResponseDump;
  }

  @When("I start an evaluation for {string}")
  public void startAnEvaluationFor(String filename) throws Exception {
    String taskContent = ResourceUtils.resourceAsString(filename);
    SubmitTaskCommand submitTaskCommand = OutputObjectMapper.getObjectMapper()
        .readValue(taskContent, SubmitTaskCommand.class);
    sendTask(taskContent, submitTaskCommand);
  }

  @When("I submit a task for game {string} with {int} variations and the name {string} "
      + "and the duration {string} to engine {string}")
  public void submitATaskFormGameName(String pgn, int initialPv, String name,
      String duration, String engine) throws Exception {
    SubmitTaskCommand submitTaskCommand = new SubmitTaskCommand(name,
        pgn, engine, initialPv, duration, false);
    String taskContent = OutputObjectMapper.getObjectMapper().writeValueAsString(submitTaskCommand);
    sendTask(taskContent, submitTaskCommand);
  }

  private void sendTask(String taskContent, SubmitTaskCommand submitTaskCommand) {
    Response response = startTask(taskContent);
    String taskId = response.body().path("taskId");
    System.out.println("Analysis running with taskId = " + taskId);
    taskWorld.set(submitTaskCommand);
    taskWorld.set(taskId);
  }

  @Then("I will find this task in the list of tasks")
  public void canFindThisTaskInTheListOfTasks() {
    String taskId = taskWorld.getTaskId();
    SubmitTaskCommand submitTaskCommand = taskWorld.getSubmittedTaskCommand();

    Awaitility.await()
        .with().pollDelay(200, TimeUnit.MILLISECONDS)
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> restGetTaskList()
            .then().extract().path("taskId"), hasItem(taskId));

    Response response = restGetTaskList();
    String taskIdElement = "find{it.taskId=='%s'}".formatted(taskId);

    response.then().assertThat().body(taskIdElement + ".taskName", equalTo(submitTaskCommand.name));
    response.then().assertThat().body(taskIdElement + ".startingPosition", containsString(submitTaskCommand.pgn));
    response.then().assertThat().body(taskIdElement + ".engineProgramName", equalTo(submitTaskCommand.engineId));
    response.then().assertThat().body(taskIdElement + ".hostname", not(blankOrNullString()));
    response.then().assertThat().body(taskIdElement + ".initialPv", equalTo(submitTaskCommand.initialPv));
    response.then().assertThat().body(taskIdElement + ".maxDuration", equalTo(submitTaskCommand.maxDuration));
    response.then().assertThat().body(taskIdElement + ".useSyzygyPath", equalTo(submitTaskCommand.useSyzygyPath));

    response.then().assertThat().body(taskIdElement + ".startingMoveNumber", notNullValue());
    response.then().assertThat().body(taskIdElement + ".estimatedCompletionTime", notNullValue());
    response.then().assertThat().body(taskIdElement + ".status", notNullValue());
    response.then().assertThat().body(taskIdElement + ".link", notNullValue());
  }

  @Then("I can get details for this task")
  public void canGetDetailsForThisTask() {
    String taskId = taskWorld.getTaskId();
    SubmitTaskCommand submitTaskCommand = taskWorld.getSubmittedTaskCommand();

    Response response = restGetTask(taskId);

    EvaluationMessage actual = response.as(EvaluationMessage.class);
    String expectedPgn = submitTaskCommand.pgn.replace("*", "").strip();
    assertThat(actual.analysedPgn, containsString(expectedPgn));
    assertThat(actual.analysedFen, not(blankOrNullString()));
    assertThat(actual.analysedPly, is(greaterThan(0)));
    assertThat(actual.lastAlive.isBefore(LocalDateTime.now(ZoneId.systemDefault())), is(true));
    assertThat(actual.status, not(blankOrNullString()));
    assertThat(actual.taskName, is(equalTo(submitTaskCommand.name)));
    assertThat(actual.taskStarted.isBefore(LocalDateTime.now(ZoneId.systemDefault())), is(true));
    assertThat(actual.uciEngineName.toLowerCase(),
        containsString(submitTaskCommand.engineId.toLowerCase()));

    Evaluation evaluation = actual.evaluation;
    assertThat(evaluation.taskId, is(equalTo(taskId)));
    assertThat(evaluation.created.isBefore(LocalDateTime.now(ZoneId.systemDefault())), is(true));

    UciState uciState = evaluation.uciState;
    assertThat(uciState.kiloNodes, is(greaterThan(0L)));
    assertThat(uciState.kiloNodesPerSecond, is(greaterThan(0L)));
    assertThat(uciState.tbHits, is(greaterThanOrEqualTo(0L)));

    List<Variation> variations = evaluation.variations;
    assertThat(variations, hasSize(submitTaskCommand.initialPv));
    variations.forEach(variation -> {
      assertThat(variation.depth, is(greaterThan(0)));
      assertThat(variation.moves, is(not(blankOrNullString())));
      assertThat(variation.pgn, containsString(expectedPgn));
      assertThat(variation.pvId, is(greaterThan(0)));
      assertThat(variation.score, matchesPattern("[+\\-]\\d\\.\\d\\d"));
      assertThat(variation.time, matchesPattern("\\d\\d:\\d\\d:\\d\\d"));
    });

    List<EngineInformation> latestEvents = actual.latestEvents;
    assertThat(latestEvents, hasSize(submitTaskCommand.initialPv));
    latestEvents.forEach(latestEvent -> {
      assertThat(latestEvent.depth, is(greaterThan(0)));
      assertThat(latestEvent.lineSan, is(not(blankOrNullString())));
      assertThat(latestEvent.multiPv, is(greaterThan(0)));
      assertThat(latestEvent.occurredOn.isBefore(LocalDateTime.now(ZoneId.systemDefault())), is(true));
      assertThat(latestEvent.score, is(greaterThan(Integer.MIN_VALUE)));
      assertThat(latestEvent.time, matchesPattern("\\d\\d:\\d\\d:\\d\\d"));
    });
  }

  @Then("the task has the status {string}")
  public void theTaskHasTheState(String expectedState) {
    EvaluationMessage evaluationMessage = responseWorld.get().as(EvaluationMessage.class);
    assertThat(evaluationMessage.status, is(equalTo(expectedState)));
  }

  @When("I stop this task")
  public void stopThisTask() {
    String taskId = taskWorld.getTaskId();
    Response response = stopTask(taskId);
    response.then().assertThat().body("evaluation.taskId", is(taskId));
  }

  @When("I stop all tasks")
  public void stopAllEvaluation() {
    RestAssured.given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpResponseFilter("stopAll"))
        .accept(ContentType.JSON)
        .when()
        .get("/api/tasks/stop")
        .then()
        .statusCode(is(in(new Integer[] {200, 204})));
  }

  private Response restGetTaskList() {
    String path = "/api/tasks";
    return given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpResponseFilter(path))
        .accept(ContentType.JSON)
        .when()
        .get(path)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().response();
  }

  private Response restGetTask(String taskId) {
    String path = "/api/tasks/%s".formatted(taskId);
    return given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpRequestFilter(path))
        .filter(requestResponseDump.dumpResponseFilter(path))
        .accept(ContentType.JSON)
        .when()
        .get(path)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().response();
  }

  private Response startTask(String payload) {
    String path = "/api/tasks";
    return given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpRequestFilter(path))
        .filter(requestResponseDump.dumpResponseFilter(path))
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(HttpStatus.SC_ACCEPTED)
        .and()
        .body("taskId", is(not(blankOrNullString())))
        .extract().response();
  }

  private Response stopTask(String taskId) {
    String path = "/api/tasks/%s/stop".formatted(taskId);
    return given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpResponseFilter(path))
        .accept(ContentType.JSON)
        .when()
        .post(path)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().response();
  }

}

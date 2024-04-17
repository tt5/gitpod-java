package ce.chess.dockfish.adapter.in.web;

import ce.chess.dockfish.adapter.common.dto.EvaluationMessageDto;
import ce.chess.dockfish.adapter.common.dto.SubmitTaskCommand;
import ce.chess.dockfish.adapter.common.mapper.EvaluationMessageDtoMapper;
import ce.chess.dockfish.adapter.common.mapper.SubmitTaskCommandMapper;
import ce.chess.dockfish.domain.model.result.JobStatus;
import ce.chess.dockfish.domain.model.task.AnalysisRun;
import ce.chess.dockfish.domain.model.task.TaskId;
import ce.chess.dockfish.usecase.in.QueryAnalysis;
import ce.chess.dockfish.usecase.in.QueryConfiguration;
import ce.chess.dockfish.usecase.in.QueryEvaluation;
import ce.chess.dockfish.usecase.in.ReceiveAnalysisRequest;
import ce.chess.dockfish.usecase.in.TerminateAnalysis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api")
@ApplicationScoped
public class AnalysisController {
  private static final String TASK_ID_MUST_BE_GIVEN_MSG = "taskId must be given";

  @Context
  UriInfo uriInfo;

  @Inject
  ReceiveAnalysisRequest analysisService;

  @Inject
  QueryAnalysis queryAnalysis;

  @Inject
  TerminateAnalysis terminateAnalysis;

  @Inject
  QueryEvaluation queryEvaluation;

  @Inject
  QueryConfiguration queryConfiguration;

  @Inject
  SubmitTaskCommandMapper submitTaskCommandMapper;

  @Inject
  EvaluationMessageDtoMapper evaluationMessageDtoMapper;

  @GET
  @Path("/engines")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get a list auf available engines", description = "Returns a list of engine names")
  public Response getEngineNames() {
    return Response.ok()
        .entity(
            queryConfiguration.listEngineNames().stream()
                .sorted()
                .toList())
        .build();
  }

  @GET
  @Path("/tasks")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get information about all tasks", description = "Returns a list of Tasks")
  @APIResponse(responseCode = "200", description = "The task list",
      content = @Content(schema = @Schema(type = SchemaType.ARRAY,
          implementation = TaskRepresentation.class)))
  @APIResponse(responseCode = "204", description = "No Content")
  public Response getTaskList() {
    List<TaskId> taskIds = queryEvaluation.getAllTaskIds();
    if (taskIds.isEmpty()) {
      return Response.noContent().entity("Found no task at all").build();
    } else {
      List<TaskRepresentation> elements = taskIds.stream()
          .map(this::createTaskRepresentation)
          .sorted(Comparator.comparing(TaskRepresentation::getSubmitted).reversed())
          .toList();
      return Response.ok().entity(elements).build();
    }
  }

  @GET
  @Path("/tasks/{taskId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get the evaluation of the given TaskId.",
      description = "Returns the evaluation. TaskId may be set to \"current\"")
  @APIResponse(responseCode = "200", description = "The current/last analysis result",
      content = @Content(schema = @Schema(implementation = EvaluationMessageDto.class)))
  @APIResponse(responseCode = "400", description = "Mandatory Request parameter is missing")
  @APIResponse(responseCode = "404", description = "Not Found (unknown TaskId)")
  public Response get(@PathParam("taskId") String rawTaskId) {
    if (rawTaskId.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity(TASK_ID_MUST_BE_GIVEN_MSG).build();
    }
    return evaluationResponse(rawTaskId);
  }

  @POST
  @Path("/tasks")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Post an analysis task",
      description = "Returns the task representation of the posted Task")
  @APIResponse(responseCode = "202", description = "The task representation",
      content = @Content(schema = @Schema(implementation = TaskRepresentation.class)))
  @APIResponse(responseCode = "400", description = "Invalid format")
  @APIResponse(responseCode = "409", description = "The server is already running an analysis")
  public Response submitTask(SubmitTaskCommand command) {
    command.validate();
    Optional<TaskId> newTaskId = analysisService.startAsync(
        submitTaskCommandMapper.toDomainObject(command, LocalDateTime.now(ZoneId.systemDefault())));
    return newTaskId.map(this::taskSubmittedResponse)
        .orElseGet(() -> Response.status(Status.CONFLICT).entity("Already pondering. Post did not succeed.").build());
  }

  @GET
  @Path("/tasks/kill")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Forcibly stop all analysis by killing the engine process",
      description = "Returns the last evaluation")
  @APIResponse(responseCode = "200", description = "The current/last analysis result",
      content = @Content(schema = @Schema(implementation = EvaluationMessageDto.class)))
  @APIResponse(responseCode = "204", description = "No active analysis")
  public Response kill() {
    boolean stopped = terminateAnalysis.kill();
    if (stopped) {
      return queryEvaluation.getLastEvaluationMessage()
          .map(Response::ok)
          .map(Response.ResponseBuilder::build)
          .orElseGet(() -> Response.noContent().build());
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/tasks/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Stop the current analysis",
      description = "Returns the last evaluation")
  @APIResponse(responseCode = "200", description = "The current/last analysis result",
      content = @Content(schema = @Schema(implementation = EvaluationMessageDto.class)))
  @APIResponse(responseCode = "204", description = "No active analysis")
  public Response stop() {
    boolean stopped = terminateAnalysis.stop();
    if (stopped) {
      return queryEvaluation.getLastEvaluationMessage()
          .map(Response::ok)
          .map(Response.ResponseBuilder::build)
          .orElseGet(() -> Response.noContent().build());
    }
    return Response.noContent().build();
  }

  @POST
  @Path("/tasks/{taskId}/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
  @Operation(summary = "Stop the given task",
      description = "Returns the last evaluation")
  @APIResponse(responseCode = "200", description = "The current task has been stopped",
      content = @Content(schema = @Schema(implementation = EvaluationMessageDto.class)))
  @APIResponse(responseCode = "204", description = "Task not found")
  @APIResponse(responseCode = "400", description = "Mandatory Request parameter is missing")
  public Response stop(@PathParam("taskId") String rawTaskId) {
    if (rawTaskId.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity(TASK_ID_MUST_BE_GIVEN_MSG).build();
    }
    terminateAnalysis.stop();
    return evaluationResponse(rawTaskId);
  }

  private Response taskSubmittedResponse(TaskId taskId) {
    TaskRepresentation representation = createTaskRepresentation(taskId);
    URI uri = createGetTaskLink(taskId.getRawId());
    return Response.status(Status.ACCEPTED)
        .location(uri)
        .contentLocation(uri)
        .entity(representation)
        .build();
  }

  private Response evaluationResponse(String taskId) {
    if ("current".equals(taskId)) {
      return queryEvaluation.getLastEvaluationMessage()
          .map(evaluationMessageDtoMapper::toDto)
          .map(Response::ok)
          .orElseGet(Response::noContent)
          .build();
    } else {
      return queryEvaluation.getLastEvaluationMessage(new TaskId(taskId))
          .map(evaluationMessageDtoMapper::toDto)
          .map(Response::ok)
          .orElseGet(Response::noContent)
          .build();
    }
  }

  private TaskRepresentation createTaskRepresentation(TaskId taskId) {
    JobStatus jobStatus = queryAnalysis.getJobStatus(taskId);
    AnalysisRun taskDetails = queryAnalysis.getTaskDetails(taskId);
    URI uri = createGetTaskLink(taskId.getRawId());
    return TaskRepresentation.builder()
        .taskId(taskId.getRawId())
        .taskName(taskDetails.name().orElse(""))
        .reference(taskDetails.reference())
        .submitted(taskDetails.created())
        .startingPosition(taskDetails.startingPosition().getPgn())
        .startingMoveNumber(taskDetails.startingPosition().getLastMovePly())
        .engineProgramName(taskDetails.engineProgramName())
        .hostname(taskDetails.hostname())
        .initialPv(taskDetails.initialPv())
        .maxDepth(taskDetails.maxDepth().orElse(null))
        .maxDuration(taskDetails.maxDuration().orElse(null))
        .useSyzygyPath(taskDetails.useSyzygyPath())
        .estimatedCompletionTime(taskDetails.estimatedCompletionTime().orElse(null))
        .status(jobStatus)
        .link(uri.toString())
        .build();
  }

  private URI createGetTaskLink(String rawTaskId) {
    return uriInfo.getBaseUriBuilder()
        .path(AnalysisController.class).path(AnalysisController.class, "get")
        .build(rawTaskId);
  }

}

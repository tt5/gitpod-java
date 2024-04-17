package ce.chess.dockfish.adapter.in.web;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.log4j.Log4j2;

@Provider
@Log4j2
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {
    if (isIllegalArgument(exception)) {
      log.info("Illegal Argument occurred: {}", exception.toString());
      return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
    } else if (exception instanceof WebApplicationException webApplicationException) {
      log.info("WebApplicationException occurred: {}", exception.toString());
      return webApplicationException.getResponse();
    }
    log.error("Exception occurred.", exception);
    return createInternalServerErrorResponse(exception);
  }

  private boolean isIllegalArgument(Throwable exception) {
    return exception instanceof IllegalArgumentException
        || null != exception.getCause() && isIllegalArgument(exception.getCause());
  }

  private static Response createInternalServerErrorResponse(Exception exception) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(exception.getMessage())
        .build();
  }

}

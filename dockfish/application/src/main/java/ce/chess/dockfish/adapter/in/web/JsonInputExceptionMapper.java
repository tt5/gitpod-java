package ce.chess.dockfish.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.log4j.Log4j2;

// Required because of org.jboss.resteasy.plugins.providers.jackson.JsonProcessingExceptionMapper
@Provider
@Log4j2
public class JsonInputExceptionMapper implements ExceptionMapper<JsonProcessingException> {

  @Override
  public Response toResponse(JsonProcessingException exception) {
    log.warn("JsonProcessingException occurred: {}", exception.toString());
    return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
  }
}

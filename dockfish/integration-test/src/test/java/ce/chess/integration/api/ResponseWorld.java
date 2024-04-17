package ce.chess.integration.api;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class ResponseWorld {
  private Response response;

  public Response get() {
    return response;
  }

  public void set(Response response) {
    this.response = response;
  }

  public Filter set() {
    return new ResponseWorldFilter(this);
  }

  private record ResponseWorldFilter(ResponseWorld responseWorld) implements Filter {
    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
      Response response = ctx.next(requestSpec, responseSpec);
      responseWorld.set(response);
      return response;
    }
  }
}

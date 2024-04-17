package ce.chess.integration;

import io.cucumber.java.Before;
import io.restassured.RestAssured;

public class TestSetup {
  public static String getServiceHost() {
    return System.getProperty("service.host", "localhost");
  }

  public static int getServicePort() {
    return Integer.parseInt(System.getProperty("service.port", "8080"));
  }

  public static String getServiceUrl() {
    return "http://" + getServiceHost();
  }

  @Before
  public void configureRestAssured() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = getServiceUrl();
    RestAssured.port = getServicePort();
  }

}

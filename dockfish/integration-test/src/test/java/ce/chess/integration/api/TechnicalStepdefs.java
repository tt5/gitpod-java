package ce.chess.integration.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import ce.chess.integration.util.JsonPathAssertions;
import ce.chess.integration.util.ResourceUtils;

import com.google.common.util.concurrent.Uninterruptibles;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class TechnicalStepdefs {

  private final ResponseWorld responseWorld;
  private final JsonPathAssertions jsonPathAssertions;
  private final RequestResponseDump requestResponseDump;

  public TechnicalStepdefs(ResponseWorld responseWorld,
                           JsonPathAssertions jsonPathAssertions,
                           RequestResponseDump requestResponseDump) {
    this.responseWorld = responseWorld;
    this.jsonPathAssertions = jsonPathAssertions;
    this.requestResponseDump = requestResponseDump;
  }

  @When("I get {string} from management")
  public void getFromManagement(String path) {
    given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpResponseFilter(path))
        .when()
        .get(path);
  }

  @When("I get {string} from service")
  public void getFromService(String path) {
    given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpResponseFilter(path))
        .when()
        .get(path);
  }

  @When("I post the text {string} to {string}")
  public void postTheText(String text, String apiUrl) {
    given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpRequestFilter(apiUrl))
        .filter(requestResponseDump.dumpResponseFilter(apiUrl))
        .body(text)
        .when()
        .post(apiUrl);
  }

  @When("I post the content from {string} to {string}")
  public void postTheContentFromTo(String resourceName, String apiUrl) {
    String content = ResourceUtils.resourceAsString(resourceName);
    given()
        .filter(responseWorld.set())
        .filter(requestResponseDump.dumpRequestFilter(apiUrl))
        .filter(requestResponseDump.dumpResponseFilter(apiUrl))
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(content)
        .when()
        .post(apiUrl);
  }

  @When("I wait for {int} seconds")
  public void waitForSeconds(int seconds) {
    Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
  }

  @Then("I receive a response with status {int}")
  @Then("the response has status code {int}")
  public void receiveResponseWithStatus(int expectedStats) {
    responseWorld.get().then()
        .statusCode(expectedStats);
  }

  @Then("the response body is equal to {string}")
  public void receiveResponseEqualTo(String expectedText) {
    responseWorld.get().then()
        .body(is(equalTo(expectedText)));
  }

  @Then("the response contains the text {string}")
  public void responseContainsText(String expectedText) {
    responseWorld.get().then()
        .body(containsString(expectedText));
  }

  @Then("the response body has the content of {string}")
  public void theResponseBodyHasTheContentOf(String resourcePath) {
    String expected = ResourceUtils.resourceAsString(resourcePath);
    responseWorld.get().then()
        .body(is(equalToCompressingWhiteSpace(expected)));
  }

  @Then("the response body has the json content of {string}")
  public void theResponseBodyHasTheJsonContentOf(String resourcePath) throws JSONException {
    String expected = ResourceUtils.resourceAsString(resourcePath);
    JSONAssert.assertEquals(expected,
        responseWorld.get().then().extract().asString(),
        JSONCompareMode.LENIENT);
  }

  @Then("the response body has the json content of {string} with regex in fields")
  public void theResponseBodyHasTheJsonContentOf(String resourcePath, List<String> regexFields) throws JSONException {
    String expected = ResourceUtils.resourceAsString(resourcePath);
    List<Customization> customizations = regexFields.stream()
        .map(field -> new Customization(field, new RegularExpressionValueMatcher<>()))
        .toList();
    JSONAssert.assertEquals(expected,
        responseWorld.get().then().extract().asString(),
        new CustomComparator(JSONCompareMode.LENIENT, customizations.toArray(new Customization[]{})));
  }

  @Then("I can dump the response body")
  public void dumpTheResponseBody() {
    responseWorld.get().prettyPrint();
  }

  @Then("the content type is {word}")
  public void theContentTypeIs(String expectedContentType) {
    responseWorld.get().then()
        .contentType(containsString(expectedContentType));
  }

  @Then("the content length is greater than {int} bytes")
  public void theContentIsLonger(int minBytes) {
    byte[] bytes = responseWorld.get().then().extract().asByteArray();
    assertThat(bytes.length, is(greaterThan(minBytes)));
  }

  @Then("the element at {string} is equal to {string}")
  public void theElementAtPathIsEqualTo(String path, String expectedValue) {
    responseWorld.get().then()
        .assertThat()
        .body(path, is(equalTo(expectedValue)));
  }

  @Then("^the json element (.*) is equal to (.*)$")
  public void theElementIsEqualTo(String jsonPath, String value) {
    String responseBody = responseWorld.get().then().extract().asString();

    jsonPathAssertions.checkDocumentToHave(responseBody, jsonPath, value);
  }

  @Then("^the json element (.*) is empty$")
  public void theElementIsEmpty(String jsonPath) {
    String responseBody = responseWorld.get().then().extract().asString();

    jsonPathAssertions.checkDocumentNotToHave(responseBody, jsonPath);
  }

  @Then("^the json element (.*) has an item with (.*)$")
  public void theJsonElementHasAnItemWith(String jsonPath, String value) {
    String responseBody = responseWorld.get().then().extract().asString();

    jsonPathAssertions.checkDocumentToContainListElement(responseBody, jsonPath, value);
  }

  @Then("the response has the json elements")
  public void theResponseHasTheJsonElements(DataTable expectedJsonPathValues) {
    String responseBody = responseWorld.get().then().extract().asString();

    jsonPathAssertions.checkMultipleJsonPaths(responseBody, expectedJsonPathValues.asMap(String.class, String.class));
  }

}

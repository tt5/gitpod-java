package ce.chess.integration.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.List;
import java.util.Map;

public class JsonPathAssertions {
  private final Configuration configuration;
  private static final TypeRef<List<String>> LIST_STRING_TYPE = new TypeRef<>() {
  };

  public JsonPathAssertions() {
    configuration = Configuration.defaultConfiguration()
        .setOptions(Option.REQUIRE_PROPERTIES)
        .jsonProvider(new JacksonJsonProvider())
        .mappingProvider(new JacksonMappingProvider());
  }

  public void checkDocumentToHave(String document, String jsonPath, String expected) {
    if (JsonPath.isPathDefinite(jsonPath)) {
      String actual = parse(document).read(jsonPath, String.class);
      assertRegexOrString(jsonPath, actual, expected);
    } else {
      List<String> matchingStrings = parse(document).read(jsonPath, LIST_STRING_TYPE);
      assertThat(matchingStrings)
          .describedAs("no element found under path " + jsonPath)
          .isNotEmpty()
          .describedAs("not all conditions satisfied for path " + jsonPath)
          .allSatisfy(actual -> assertRegexOrString(jsonPath, actual, expected));
    }
  }

  public void checkMultipleJsonPaths(String document, Map<String, String> expectedAttributes) {
    expectedAttributes.forEach((key, value) -> checkDocumentToHave(document, key, value));
  }

  public void checkDocumentToContainListElement(String document, String jsonPath, String expected) {
    if (JsonPath.isPathDefinite(jsonPath)) {
      throw new IllegalArgumentException("JsonPath will not yield a list");
    }
    List<String> strings = parse(document).read(jsonPath, LIST_STRING_TYPE);
    assertThat(strings).contains(expected);
  }

  public void checkDocumentNotToHave(String document, String jsonPath) {
    if (JsonPath.isPathDefinite(jsonPath)) {
      DocumentContext documentContext = parse(document);
      assertThatExceptionOfType(PathNotFoundException.class)
          .describedAs("Expected element %s to be absent", jsonPath)
          .isThrownBy(() -> documentContext.read(jsonPath));
    } else {
      try {
        assertThat(parse(document).read(jsonPath, LIST_STRING_TYPE))
            .describedAs("Expected element %s to be empty", jsonPath)
            .isEmpty();
      } catch (PathNotFoundException ex) {
        // the test is successful if this happens
      }
    }
  }

  private DocumentContext parse(String document) {
    return JsonPath.using(configuration).parse(document);
  }

  private void assertRegexOrString(String path, String actual, String expected) {
    if (expected.startsWith("/") && expected.endsWith("/")) {
      assertThat(actual)
          .describedAs("path = " + path)
          .matches(expected.substring(1, expected.length() - 1));
    } else {
      assertThat(actual)
          .describedAs("path = " + path)
          .isEqualTo(expected);
    }
  }
}


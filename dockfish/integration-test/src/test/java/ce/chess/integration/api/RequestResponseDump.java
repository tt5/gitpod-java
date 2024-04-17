package ce.chess.integration.api;

import ce.chess.integration.util.ScenarioHolder;

import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RequestResponseDump {

  private final ScenarioHolder scenarioHolder;

  public RequestResponseDump(ScenarioHolder scenarioHolder) {
    this.scenarioHolder = scenarioHolder;
  }

  public Filter dumpRequestFilter(String path) {
    return new RequestLoggingFilter(LogDetail.ALL, requestFileFor(path));
  }

  public Filter dumpResponseFilter(String path) {
    return new ResponseLoggingFilter(LogDetail.BODY, responseFileFor(path));
  }

  private PrintStream responseFileFor(String path) {
    try {
      File outputFile = fileFor("responses", path);
      System.out.println("Response body written to " + outputFile);
      return new PrintStream(outputFile);
    } catch (FileNotFoundException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private PrintStream requestFileFor(String path) {
    try {
      File outputFile = fileFor("requests", path);
      System.out.println("Request body written to " + outputFile);
      return new PrintStream(outputFile);
    } catch (FileNotFoundException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private File fileFor(String subFolder, String path) {
    String filename = (scenarioHolder.getScenario().getName() + "_" + path).replaceAll("[^a-zA-Z_0-9.]", "_");
    return createInTargetFolder(subFolder, filename);
  }

  private File createInTargetFolder(String subFolder, String filename) {
    try {
      URI testClassesFolderUri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
      File targetFolder = new File(testClassesFolderUri).getParentFile();
      File outputDir = new File(targetFolder, subFolder);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
      return new File(outputDir, filename);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

}

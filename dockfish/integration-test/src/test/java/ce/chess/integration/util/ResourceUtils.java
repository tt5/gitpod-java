package ce.chess.integration.util;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ResourceUtils {
  private ResourceUtils() {
  }

  @SuppressWarnings("UnstableApiUsage")
  public static String resourceAsString(String resourceName) {
    try {
      URL url = Resources.getResource(resourceName);
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }
}

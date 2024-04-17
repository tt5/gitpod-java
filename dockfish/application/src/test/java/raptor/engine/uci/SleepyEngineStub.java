package raptor.engine.uci;

import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SleepyEngineStub {
  public static void main(String[] args) {
    System.out.println("> STUB-Sleepy: Started");
    Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
    while (scanner.hasNext()) {
      String line = scanner.next();
      if ("uci".equals(line)) {
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
      } else if ("quit".equals(line)) {
        System.out.println("> STUB-Sleepy: Exit");
        System.exit(42);
      } else {
        System.out.println(line);
      }
    }
    System.exit(11);
  }
}

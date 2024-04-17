package ce.chess.dockfish.archunit;

import jakarta.enterprise.event.Observes;
import java.util.Arrays;

public final class Reflections {
  private Reflections() {
  }

  public static long countObservingMethods(Class<?> clazz, Class<?> event) {
    return Arrays.stream(clazz.getMethods())
        .filter(method -> method.getParameters().length == 1)
        .filter(method -> method.getParameters()[0].getType().equals(event))
        .flatMap(method -> Arrays.stream(method.getParameters()))
        .map(parameter -> parameter.getAnnotation(Observes.class))
        .count();
  }
}

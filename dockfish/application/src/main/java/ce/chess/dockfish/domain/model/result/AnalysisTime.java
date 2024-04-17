package ce.chess.dockfish.domain.model.result;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class AnalysisTime {

  Duration value;

  public static AnalysisTime fromMilliSeconds(long millis) {
    return new AnalysisTime(Duration.ofMillis(millis));
  }

  public static AnalysisTime fromMinutes(int minutes) {
    return new AnalysisTime(Duration.ofMinutes(minutes));
  }

  public String formattedAsTime() {
    return LocalTime.MIDNIGHT.plus(value).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
  }
}

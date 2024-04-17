package raptor.engine.uci;

import java.nio.charset.Charset;
import java.util.Scanner;

public class EngineStub {

  public static void main(String[] args) {
    EngineStub engineStub = new EngineStub();
    engineStub.scanInputLine();
  }

  void scanInputLine() {
    System.out.println("> STUB: Started");
    Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
    scanner.useDelimiter("\n");
    while (scanner.hasNext()) {
      String line = scanner.next();
      if ("uci".equals(line)) {
        System.out.println("id name EngineStub");
        System.out.println("id author CE");
        System.out.println("option name Debug Log File type string default");
        System.out.println("option name Threads type spin default 1 min 1 max 512");
        System.out.println("option name Analysis Contempt type combo default Both var Off var White var Black var Both");
        System.out.println("option name Clear Hash type button");
        System.out.println("option name MultiPv type spin default 1 min 1 max 256");
        System.out.println("option name Ponder type check default false");
        System.out.println("uciok");
      } else if ("isready".equals(line)) {
        System.out.println("ready-waiting");
        System.out.println("readyok");
      } else if ("eval".equals(line)) {
        System.out.println("Eval-Result1\nEval-Result2\nEval-Result3");
      } else if (line.startsWith("go") && line.contains("depth 2")) {
        System.out.println("info string NNUE evaluation using nn-82215d0fd0df.nnue enabled");
        System.out.println("info depth 21 seldepth 28 multipv 1 score cp 35 lowerbound nodes 1000 nps 100 hashfull 858 "
            + "tbhits 1 time 2901 pv e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5 a4 g8f6 cpuload 12");
        System.out.println("info depth 22 currmove d2d4 currmovenumber 2");
        System.out.println("bestmove d2d4 ponder g8f6");
      } else if (line.startsWith("go")) {
        System.out.println("info string long running evaluation started");
        System.out.println("info depth 55 seldepth 28 multipv 1 score cp 35 lowerbound nodes 1000 nps 100 hashfull 858 "
            + "tbhits 1 time 2901 pv e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5 a4 g8f6 cpuload 12");
        System.out.println("info depth 56 currmove d2d4 currmovenumber 2");
      } else if ("quit".equals(line)) {
        System.out.println("> STUB: Quit");
        System.exit(42);
      } else if ("stop".equals(line)) {
        System.out.println("bestmove e2e4 ponder g8f6");
      } else {
        System.err.println("> STUB without action: " + line);
      }
    }
    System.err.println("> STUB: Exit");
    System.exit(11);
  }
}

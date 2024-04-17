/*
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright 2009-2016 RaptorProject (https://github.com/Raptor-Fics-Interface/Raptor)
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * <p>
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package raptor.engine.uci;

import com.google.common.util.concurrent.Uninterruptibles;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import raptor.engine.uci.info.BestLineFoundInfo;
import raptor.engine.uci.info.CPULoadInfo;
import raptor.engine.uci.info.CurrentMoveInfo;
import raptor.engine.uci.info.DepthInfo;
import raptor.engine.uci.info.MultiPV;
import raptor.engine.uci.info.NodesPerSecondInfo;
import raptor.engine.uci.info.NodesSearchedInfo;
import raptor.engine.uci.info.ScoreInfo;
import raptor.engine.uci.info.SelectiveSearchDepthInfo;
import raptor.engine.uci.info.StringInfo;
import raptor.engine.uci.info.TableBaseHitsInfo;
import raptor.engine.uci.info.TimeInfo;
import raptor.engine.uci.options.UCIButton;
import raptor.engine.uci.options.UCICheck;
import raptor.engine.uci.options.UCICombo;
import raptor.engine.uci.options.UCISpinner;
import raptor.engine.uci.options.UCIString;
import raptor.util.RaptorStringTokenizer;

@ApplicationScoped
@Log4j2
public class UCIEngine {
  private static final String STRING = "string";
  private static final String[] SUPPORTED_INFO_TYPES = {"depth", "seldepth", "time", "nodes", "pv", "multipv",
      "score", "currmove", "currentmovenumber", "hashfull", "nps", "tbhits", "cpuload", STRING};
  private static final long CONNECTION_TIMEOUT = 10_000;
  private static final String ENGINE_IS_NOT_CONNECTED = "Engine is not connected.";
  private static final String DEFAULT = "default";

  private Process process;
  private BufferedReader inReader;
  private PrintWriter outWriter;
  private final Map<String, UCIOption> nameToOptions = new ConcurrentHashMap<>();
  @Getter
  @Setter
  private String processPath;
  @Getter
  private String engineName;
  @Getter
  private String engineAuthor;
  private Runnable goRunnable;
  private UCIBestMove lastBestMove;
  private String[] parameters;
  private String goOptions;

  @Getter
  @Setter
  private boolean isSuspended;

  private final long connectionTimeoutMillis;
  private final Object stopSynch = new Object();
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10); // NOPMD

  public UCIEngine() {
    this(CONNECTION_TIMEOUT);
  }

  public UCIEngine(long connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }

  /**
   * Connects to the engine. After this method is invoked the engine name,
   * engine author, and options will be populated in this object.
   *
   * @return true if connection was successful, false otherwise.
   */
  public boolean connect() {
    if (isConnected()) {
      return true;
    }

    resetConnectionState();

    Future<?> connectionTimeoutFuture = executorService.schedule(() -> {
          log.warn("Connection Timeout after {} ms, process.info={}, process.alive={}.",
              connectionTimeoutMillis,
              process == null ? "null" : process.info(),
              process != null && process.isAlive());
          disconnect();
        },
        connectionTimeoutMillis, TimeUnit.MILLISECONDS);

    try {
      long startTime = System.currentTimeMillis();

      if (parameters == null || parameters.length == 0) {
        process = new ProcessBuilder(processPath)
            .directory(new File(new File(processPath).getParent()))
            .start();
      } else {
        String[] args = new String[parameters.length + 1];
        args[0] = processPath;
        System.arraycopy(parameters, 0, args, 1, parameters.length);
        process = new ProcessBuilder(args).redirectErrorStream(true).start();
      }
      inReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8), 10_000);
      outWriter = new PrintWriter(process.getOutputStream(), true, StandardCharsets.UTF_8);
      Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
      log.info("Process info {}", process.info());

      if (log.isDebugEnabled()) {
        log.debug("Before uci: process.info={}, process.alive={}",
            process.info(), process.isAlive());
      }
      uci();

      if (log.isDebugEnabled()) {
        log.debug("Before isReady: process.info={}, process.alive={}",
            process == null ? "null" : process.info(),
            process != null && process.isAlive());
      }
      isReady();

      if (log.isDebugEnabled()) {
        log.debug("engineName={} engineAuthor={} Options:\n{} initialized in {}", engineName, engineAuthor,
            nameToOptions.values(), System.currentTimeMillis() - startTime);
      }

      connectionTimeoutFuture.cancel(true);
      return true;
    } catch (RuntimeException | IOException ex) { // NOPMD
      log.error("Error connecting to UCI Engine {}", this, ex);
      disconnect();
      return false;
    }
  }

  /**
   * Returns the UCIOption with the specified name.
   */
  public UCIOption getOption(String name) {
    return nameToOptions.get(name);
  }

  /**
   * Returns true if the engine has the specified option, false otherwise.
   */
  public boolean hasOption(String name) {
    return nameToOptions.get(name) != null;
  }

  /**
   * Returns an array of all supported option names.
   */
  public String[] getOptionNames() {
    return nameToOptions.keySet().toArray(new String[0]);
  }

  /**
   * go start calculating on the current position set up with the "position"
   * command. There are a number of commands that can follow this command, all
   * will be sent in the same string. If one command is not send its value
   * should be interpreted as it would not influence the search.
   *
   * <pre>
   * searchmoves  ....
   * 		restrict search to this moves only
   * 		Example: After &quot;position startpos&quot; and &quot;go infinite searchmoves e2e4 d2d4&quot;
   * 		the engine should only search the two moves e2e4 and d2d4 in the initial position.
   * ponder
   * 		start searching in pondering mode.
   * 		Do not exit the search in ponder mode, even if it's mate!
   * 		This means that the last move sent in in the position string is the ponder move.
   * 		The engine can do what it wants to do, but after a &quot;ponderhit&quot; command
   * 		it should execute the suggested move to ponder on. This means that the ponder move sent by
   * 		the GUI can be interpreted as a recommendation about which move to ponder. However, if the
   * 		engine decides to ponder on a different move, it should not display any mainlines as they are
   * 		likely to be misinterpreted by the GUI because the GUI expects the engine to ponder
   * 	   on the suggested move.
   * wtime
   * 		white has x msec left on the clock
   * btime
   * 		black has x msec left on the clock
   * winc
   * 		white increment per move in mseconds if x &gt; 0
   * binc
   * 		black increment per move in mseconds if x &gt; 0
   * movestogo
   *       there are x moves to the next time control,
   * 		this will only be sent if x &gt; 0,
   * 		if you don't get this and get the wtime and btime it's sudden death
   * depth
   * 		search x plies only.
   * nodes
   * 	   search x nodes only,
   * mate
   * 		search for a mate in x moves
   * movetime
   * 		search exactly x mseconds
   * infinite
   * 		search until the &quot;stop&quot; command. Do not exit the search without being told so in this mode!
   * </pre>
   */
  public void go(String options, final UCIInfoListener listener) {
    throwIfNotConnected();

    if (isProcessingGo()) {
      log.info("Go is in process. Ignoring go call.");
    } else {
      log.debug("Entering go({})", options);
      this.goOptions = options;

      send("go " + options);

      goRunnable = () -> goAction(listener);
      executorService.execute(goRunnable);
    }
  }

  private void goAction(UCIInfoListener listener) {
    try {
      String line = readLine();
      log.debug("Go received line: {}", line);

      while (line != null) {
        if (line.startsWith("info")) {
          parseInfoLine(line, listener);
        } else if (line.startsWith("bestmove")) {
          if (isSuspended()) {
            log.info("ignoring bestmove because listener is suspended");
          } else {
            lastBestMove = parseBestMove(line);
            listener.engineSentBestMove(lastBestMove);
            break;
          }
        }
        while (isSuspended()) {
          // wait
          Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        line = readLine();
        log.debug("Go received line: {}", line);
      }
      log.info("Go finished: last line {}", line);
      goRunnable = null;
    } catch (Exception tex) { // NOPMD
      log.error("Error occurred executing go ", tex);
    }
  }

  /**
   * Returns true if there is a connection to the UCIEngine, false otherwise.
   */
  public boolean isConnected() {
    if (process == null) {
      return false;
    }
    if (process.isAlive()) {
      return true;
    } else {
      int exitValue = process.exitValue();
      if (exitValue == 0) {
        log.info("Not connected. exitValue=0");
      } else {
        log.warn("Not connected. exitValue={}", exitValue);
      }
      return false;
    }
  }

  /**
   * Returns true if a go command is currently being processed, otherwise
   * false.
   */
  public boolean isProcessingGo() {
    return goRunnable != null;
  }

  private void uci() throws IOException {
    throwIfNotConnected();
    send("uci");

    String currentLine = readLine();
    while (currentLine != null) {
      log.info(currentLine);
      if (currentLine.startsWith("id")) {
        parseIdLine(currentLine);
      } else if (currentLine.startsWith("option ")) {
        parseOptionLine(currentLine);
      } else if (currentLine.startsWith("uciok")) {
        break;
      } else {
        log.info("Unknown response to uci, ignoring: {}", currentLine);
      }
      currentLine = readLine();
    }
  }

  /**
   * Blocks until readyok is received.
   * <p>
   * <p>
   * this is used to synchronize the engine with the GUI. When the GUI has
   * sent a command or multiple commands that can take some time to complete,
   * this command can be used to wait for the engine to be ready again or to
   * ping the engine to find out if it is still alive. E.g. this should be
   * sent after setting the path to the tablebases as this can take some time.
   * This command is also required once before the engine is asked to do any
   * search to wait for the engine to finish initializing. This command must
   * always be answered with "readyok" and can be sent also when the engine is
   * calculating in which case the engine should also immediately answer with
   * "readyok" without stopping the search.
   */
  public void isReady() {
    throwIfNotConnected();
    setSuspended(true);

    send("isready");
    try {
      String reply = readLine();
      while (reply != null && !"readyok".equalsIgnoreCase(reply)) {
        log.info("Waiting for readyok. Got {}", reply);
        reply = readLine();
      }
      log.info("isReady? {}", reply);
    } catch (Exception tex) { // NOPMD
      log.error("Error occurred in isReady. Disconnecting.", tex);
      disconnect();
    } finally {
      setSuspended(false);
    }
  }

  private void throwIfNotConnected() {
    if (!isConnected()) {
      throw new IllegalStateException(ENGINE_IS_NOT_CONNECTED);
    }
  }

  public String eval() {
    throwIfNotConnected();
    if (isProcessingGo()) {
      // Do nothing currently not supported if isProcessingGo.
      log.warn("Engine is processing, Do nothing.");
      return "";
    } else {
      log.debug("Entering eval()");

      send("eval");

      try {
        StringBuilder lBuilder = new StringBuilder();
        String reply = readLine();
        lBuilder.append(reply).append('\n');
        // CE workaround: (dockfish might drain out after sending "info string")
        reply = readLine();
        lBuilder.append(reply).append('\n');
        // CE
        while (reply != null && inReader.ready()) {
          reply = readLine();
          log.debug("Readline: {}", reply);
          lBuilder.append(reply).append('\n');
        }
        return lBuilder.toString();
      } catch (Exception tex) { // NOPMD
        log.error("Error occurred in eval. Disconnecting.", tex);
        disconnect();
        return "";
      }
    }
  }

  /**
   * Quits the program as soon as possible
   */
  public void quit() {
    if (!isConnected()) {
      return;
    }

    log.info("Entering quit()");

    send("quit");
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    disconnect();
  }

  /**
   * setoption name [value ] this is sent to the engine when the user wants to
   * change the internal parameters of the engine. For the "button" type no
   * value is needed. One string will be sent for each parameter and this will
   * only be sent when the engine is waiting. The name of the option in should
   * not be case sensitive and can inludes spaces like also the value. The
   * substrings "value" and "name" should be avoided in and to allow
   * unambiguous parsing, for example do not use = "draw value". Here are some
   * strings for the example below: "setoption name Nullmove value true\n"
   * "setoption name Selectivity value 3\n"
   * "setoption name Style value Risky\n" "setoption name Clear Hash\n"
   * "setoption name NalimovPath value c:\chess\tb\4;c:\chess\tb\5\n"
   */
  public void setOption(UCIOption option) {
    throwIfNotConnected();

    try {
      log.debug("Entering setOption({}", option);

      if (option instanceof UCIButton) {
        send("setoption name " + option.getName());
      } else {
        send("setoption name " + option.getName() + " value " + option.getValue());
      }

      // CE: Fixes an error in raptorchess
      nameToOptions.put(option.getName(), option);

      log.debug("Set UCIOption: {}", option);
    } catch (Exception tex) { // NOPMD
      log.warn("Error occurred setting option: {}", option, tex);
      disconnect();
    }
  }

  public void stopSetOptionGo(UCIOption option) {
    throwIfNotConnected();

    setSuspended(true);
    log.info("Listener is suspended");
    send("stop");
    log.info("Stop was sent");
    Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
    isReady();
    setOption(option);
    log.info("Option was sent");
    send("go " + this.goOptions);
    log.info("Go was sent");
    setSuspended(false);
    log.info("Listener is active");
  }

  public void setParameters(String... parameters) {
    this.parameters = Arrays.copyOf(parameters, parameters.length);
  }

  /**
   * Sets the position to fen and passes in the specified moves
   * <p>
   * position [fen | startpos ] moves .... set up the position described in
   * fenstring on the internal board and play the moves on the internal chess
   * board. if the game was played from the start position the string
   * "startpos" will be sent Note: no "new" command is needed. However, if
   * this position is from a different game than the last position sent to the
   * engine, the GUI should have sent a "ucinewgame" inbetween.
   */
  public void setPosition(String fen, UCIMove... moves) {
    throwIfNotConnected();
    log.debug("Entering setPosition({})", fen);

    if (isProcessingGo()) {
      stop();
    }

    if (moves == null || moves.length == 0) {
      send("position fen " + fen);
    } else {
      StringBuilder movesString = new StringBuilder();
      for (UCIMove move : moves) {
        movesString.append(movesString.toString().isEmpty() ? "" : " ").append(move.getValue());
      }
      send("position fen " + fen + " " + movesString);
    }
  }

  /**
   * Stops a go that is in process. The UCIBestMove is returned.
   */
  public UCIBestMove stop() {
    throwIfNotConnected();

    log.info("Entering stop(...)");

    UCIBestMove result;
    synchronized (stopSynch) {
      if (isProcessingGo()) {
        long totalSleepTime = 0;
        send("stop");
        while (goRunnable != null && totalSleepTime < 2500) {
          try {
            stopSynch.wait(500);
            totalSleepTime += 500;
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }
      result = lastBestMove;
    }
    return result;
  }

  @Override
  public String toString() {
    return engineName == null ? processPath : engineName;
  }

  /**
   * Disconnects from the engine
   */
  protected void disconnect() {
    log.info("Disconnecting");
    try {
      if (isConnected()) {
        process.destroy();
        process = null;

        if (inReader != null) {
          inReader.close();
        }
        if (outWriter != null) {
          outWriter.close();
        }
      }
    } catch (IOException ex) {
      log.error("Error disconnecting from UCIEngine {}", this, ex);
    } finally {
      resetConnectionState();
    }
  }

  public void kill() {
    log.info("Destroy Forcibly");
    if (process != null) {
      process.destroyForcibly();
      process = null;
    } else {
      log.warn("kill engine: process is null");
    }
  }

  protected boolean isSupportedInfoType(String type) {
    boolean result = false;
    for (String currentType : SUPPORTED_INFO_TYPES) {
      if (currentType.equalsIgnoreCase(type)) {
        result = true;
        break;
      }
    }
    return result;
  }

  protected UCIBestMove parseBestMove(String bestMove) {

    log.debug("parseBestMove({})", bestMove);

    RaptorStringTokenizer tok = new RaptorStringTokenizer(bestMove, " ", true);
    tok.nextToken();

    UCIBestMove result = new UCIBestMove();
    result.setBestMove(parseUCIMove(tok.nextToken()));

    if (tok.hasMoreTokens()) {
      String nextToken = tok.nextToken();
      if ("ponder".equalsIgnoreCase(nextToken)) {
        result.setPonderMove(parseUCIMove(tok.nextToken()));
      }
    }

    return result;
  }

  protected void parseIdLine(String idLine) {

    log.debug("parseIdLine({})", idLine);

    RaptorStringTokenizer tok = new RaptorStringTokenizer(idLine, " ", true);
    tok.nextToken();
    String varName = tok.nextToken();
    String varValue = tok.getWhatsLeft().trim();

    if ("name".equalsIgnoreCase(varName)) {
      engineName = varValue;
    } else if ("author".equalsIgnoreCase(varName)) {
      engineAuthor = varValue;
    } else {
      log.info("Unknown id variable name. {} = {} ", varName, varValue);
    }
  }

  /**
   * the engine wants to send infos to the GUI. This should be done whenever
   * one of the info has changed. The engine can send only selected infos and
   * multiple infos can be send with one info command, e.g.
   * "info currmove e2e4 currmovenumber 1" or
   * "info depth 12 nodes 123456 nps 100000". Also all infos belonging to the
   * pv should be sent together e.g.
   * "info depth 2 score cp 214 time 1242 nodes 2124 nps 34928 pv e2e4 e7e5 g1f3"
   * I suggest to start sending "currmove", "currmovenumber", "currline" and
   * "refutation" only after one second to avoid too much traffic.
   *
   * <pre>
   * Additional info:
   * depth
   * 		search depth in plies
   * seldepth
   * 		selective search depth in plies,
   * 		if the engine sends seldepth there must also a &quot;depth&quot; be present in the same string.
   * time
   * 		the time searched in ms, this should be sent together with the pv.
   * nodes
   * 		x nodes searched, the engine should send this info regularly
   * pv  ...
   * 		the best line found
   * multipv
   * 		this for the multi pv mode.
   * 		for the best move/pv add &quot;multipv 1&quot; in the string when you send the pv.
   * 		in k-best mode always send all k variants in k strings together.
   * score
   * cp
   * 			the score from the engine's point of view in centipawns.
   * mate
   * 			mate in y moves, not plies.
   * 			If the engine is getting mated use negativ values for y.
   * lowerbound
   * 	      the score is just a lower bound.
   * upperbound
   * 		   the score is just an upper bound.
   * currmove
   * 		currently searching this move
   * currmovenumber
   * 		currently searching move number x, for the first move x should be 1 not 0.
   * hashfull
   * 		the hash is x permill full, the engine should send this info regularly
   * nps
   * 		x nodes per second searched, the engine should send this info regularly
   * tbhits
   * 		x positions where found in the endgame table bases
   * cpuload
   * 		the cpu usage of the engine is x permill.
   * string
   * 		any string str which will be displayed be the engine,
   * 		if there is a string command the rest of the line will be interpreted as .
   * refutation   ...
   * 	   move  is refuted by the line  ... , i can be any number &gt;= 1.
   * 	   Example: after move d1h5 is searched, the engine can send
   * 	   &quot;info refutation d1h5 g6h5&quot;
   * 	   if g6h5 is the best answer after d1h5 or if g6h5 refutes the move d1h5.
   * 	   if there is norefutation for d1h5 found, the engine should just send
   * 	   &quot;info refutation d1h5&quot;
   * 		The engine should only send this if the option &quot;UCI_ShowRefutations&quot; is set to true.
   * currline   ...
   * 	   this is the current line the engine is calculating.  is the number of the cpu if
   * 	   the engine is running on more than one cpu.  = 1,2,3....
   * 	   if the engine is just using one cpu,  can be omitted.
   * 	   If  is greater than 1, always send all k lines in k strings together.
   * 		The engine should only send this if the option &quot;UCI_ShowCurrLine&quot; is set to true.
   * </pre>
   * <p>
   * Examples:
   *
   * <pre>
   * go infinite
   * info depth 1 seldepth 0 time 34 nodes 0 nps 151466 score cp 1 pv c7c5
   * info nps 151466 nodes 0 cpuload 0 hashfull 0 time 35
   * bestmove c7c5
   * stop
   * </pre>
   */
  @SuppressWarnings({"squid:S3776", "PMD.AvoidInstantiatingObjectsInLoops"})
  protected void parseInfoLine(String info, UCIInfoListener listener) {
    if (!isProcessingGo() || Thread.holdsLock(stopSynch)) {
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("Entering parseInfoLine ({})", info);
    }

    RaptorStringTokenizer tok = new RaptorStringTokenizer(info, " ", true);
    tok.nextToken();

    int currentMoveNumber = 0;

    List<UCIInfo> infos = new ArrayList<>(10);

    String nextType = null;
    while (tok.hasMoreTokens()) {
      String type;
      if (nextType == null) {
        type = tok.nextToken();
      } else {
        type = nextType;
        nextType = null;
      }

      while (!isSupportedInfoType(type) && tok.hasMoreTokens()) { // NOPMD
        type = tok.nextToken();
      }

      if (!isSupportedInfoType(type)) {
        break;
      }

      if ("depth".equalsIgnoreCase(type)) {
        DepthInfo depthInfo = new DepthInfo();
        depthInfo.setSearchDepthPlies(Integer.parseInt(tok.nextToken()));
        infos.add(depthInfo);
      } else if ("seldepth".equalsIgnoreCase(type)) {
        SelectiveSearchDepthInfo ssDepthInfo = new SelectiveSearchDepthInfo();
        ssDepthInfo.setDepthInPlies(Integer.parseInt(tok.nextToken()));
        infos.add(ssDepthInfo);
      } else if ("time".equalsIgnoreCase(type)) {
        TimeInfo timeInfo = new TimeInfo();
        timeInfo.setTimeMillis(Integer.parseInt(tok.nextToken()));
        infos.add(timeInfo);
      } else if ("nodes".equalsIgnoreCase(type)) {
        NodesSearchedInfo nodesSearched = new NodesSearchedInfo();
        nodesSearched.setNodesSearched(Long.parseLong(tok.nextToken()));
        infos.add(nodesSearched);
      } else if ("pv".equalsIgnoreCase(type)) {
        BestLineFoundInfo bestLineFoundInfo = new BestLineFoundInfo();
        String currentMove = tok.nextToken();
        List<UCIMove> currentLine = new ArrayList<>(10);
        currentLine.add(new UCIMove(currentMove));
        while (tok.hasMoreTokens()) {
          currentMove = tok.nextToken();
          if (isSupportedInfoType(currentMove)) {
            nextType = currentMove;
            break;
          } else {
            currentLine.add(new UCIMove(currentMove));
          }
        }
        bestLineFoundInfo.setMoves(currentLine.toArray(new UCIMove[0]));
        infos.add(bestLineFoundInfo);
      } else if ("multipv".equalsIgnoreCase(type)) {
        infos.add(new MultiPV(tok.nextToken()));
      } else if ("score".equalsIgnoreCase(type)) {
        ScoreInfo scoreInfo = new ScoreInfo();

        while (tok.hasMoreTokens()) {
          String nextToken = tok.nextToken();
          if ("cp".equalsIgnoreCase(nextToken)) {
            scoreInfo.setValueInCentipawns(Integer.parseInt(tok.nextToken()));
          } else if ("mate".equalsIgnoreCase(nextToken)) {
            scoreInfo.setMateInMoves(Integer.parseInt(tok.nextToken()));
          } else if ("lowerbound".equalsIgnoreCase(nextToken)) {
            scoreInfo.setLowerBoundScore(true);
          } else if ("upperbound".equalsIgnoreCase(nextToken)) {
            scoreInfo.setUpperBoundScore(true);
          } else {
            nextType = nextToken;
            break;
          }
        }
        infos.add(scoreInfo);
      } else if ("currmove".equalsIgnoreCase(type)) {
        CurrentMoveInfo currentMoveInfo = new CurrentMoveInfo();
        currentMoveInfo.setMove(parseUCIMove(tok.nextToken()));
        currentMoveInfo.setMoveNumber(currentMoveNumber);
        infos.add(currentMoveInfo);
      } else if ("currentmovenumber".equalsIgnoreCase(type)) {
        currentMoveNumber = Integer.parseInt(tok.nextToken());
      } else if ("hashfull".equalsIgnoreCase(type)) {
        tok.nextToken();
      } else if ("nps".equalsIgnoreCase(type)) {
        NodesPerSecondInfo nodesPerSecInfo = new NodesPerSecondInfo();
        nodesPerSecInfo.setNodesPerSecond(Long.parseLong(tok.nextToken()));
        infos.add(nodesPerSecInfo);
      } else if ("tbhits".equalsIgnoreCase(type)) {
        TableBaseHitsInfo tbInfo = new TableBaseHitsInfo();
        tbInfo.setNumberOfHits(Long.parseLong(tok.nextToken()));
        infos.add(tbInfo);
      } else if ("cpuload".equalsIgnoreCase(type)) {
        CPULoadInfo cpuInfo = new CPULoadInfo();
        cpuInfo.setCpuUsage(Integer.parseInt(tok.nextToken()));
        infos.add(cpuInfo);
      } else if (STRING.equalsIgnoreCase(type)) {
        StringInfo stringInfo = new StringInfo();
        stringInfo.setValue(tok.getWhatsLeft().trim());
        infos.add(stringInfo);
      } else {
        log.warn("Unknown type: {}", type);
      }
    }
    listener.engineSentInfo(infos);
  }

  @SuppressWarnings({"squid:S3776", "PMD.AvoidInstantiatingObjectsInLoops"})
  protected void parseOptionLine(String optionLine) {

    log.debug("Parsing option line: {}", optionLine);

    RaptorStringTokenizer tok = new RaptorStringTokenizer(optionLine, " ", true);
    tok.nextToken();
    tok.nextToken();
    String name = parseUntil("type", tok).trim();
    String type = tok.nextToken();
    UCIOption option = null;

    if ("spin".equalsIgnoreCase(type)) {
      String defaultValue = null;
      int minValue = -1;
      int maxValue = -1;

      while (tok.hasMoreTokens()) {
        String nextToken = tok.nextToken();
        if (DEFAULT.equalsIgnoreCase(nextToken)) {
          defaultValue = tok.nextToken();
        } else if ("min".equals(nextToken)) {
          minValue = Integer.parseInt(tok.nextToken());
        } else if ("max".equalsIgnoreCase(nextToken)) {
          maxValue = Integer.parseInt(tok.nextToken());
        }
      }

      if (defaultValue == null) {
        log.warn("Spinner type encountered without a default. Ignoring option. {}", optionLine);
      } else if (minValue == -1) {
        log.warn("Spinner type encountered without a min. Ignoring option. {}", optionLine);
      } else if (maxValue == -1) {
        log.warn("Spinner type encountered without a max. Ignoring option. {}", optionLine);
      }

      UCISpinner spinner = new UCISpinner();
      spinner.setDefaultValue(defaultValue);
      spinner.setName(name);
      spinner.setMaximum(maxValue);
      spinner.setMinimum(minValue);
      option = spinner;

    } else if (STRING.equalsIgnoreCase(type)) {
      String defaultValue = null;
      if (DEFAULT.equalsIgnoreCase(tok.nextToken()) && tok.hasMoreTokens()) {
        defaultValue = tok.getWhatsLeft().trim();
      }
      UCIString string = new UCIString();
      string.setName(name);
      string.setDefaultValue(defaultValue);
      option = string;
    } else if ("check".equalsIgnoreCase(type)) {
      String defaultValue = null;
      if (DEFAULT.equalsIgnoreCase(tok.nextToken()) && tok.hasMoreTokens()) {
        defaultValue = tok.nextToken();
      }
      UCICheck check = new UCICheck();
      check.setName(name);
      check.setDefaultValue(defaultValue == null ? "false" : defaultValue);
      option = check;
    } else if ("combo".equalsIgnoreCase(type)) {
      String nextToken = tok.nextToken();
      List<String> options = new ArrayList<>(10);
      String defaultValue = null;
      if (DEFAULT.equalsIgnoreCase(nextToken)) {
        defaultValue = parseUntil("var", tok);
      }

      while (tok.hasMoreTokens()) {
        options.add(parseUntil("var", tok));
      }

      UCICombo combo = new UCICombo();
      combo.setName(name);
      combo.setDefaultValue(defaultValue);
      combo.setOptions(options.toArray(new String[0]));
      option = combo;
    } else if ("button".equalsIgnoreCase(type)) {
      UCIButton button = new UCIButton();
      button.setName(name);
      option = button;
    } else {
      log.warn("Unknown option type encountered in line {}", optionLine);
    }

    nameToOptions.put(name, option);
  }

  protected UCIMove parseUCIMove(String move) {
    return new UCIMove(move);
  }

  protected String parseUntil(String untilKeyword, RaptorStringTokenizer tok) {
    StringBuilder result = new StringBuilder();
    String token = tok.nextToken();
    while (!token.equalsIgnoreCase(untilKeyword) && tok.hasMoreTokens()) {
      result.append(result.toString().isEmpty() ? "" : " ")
          .append(token);
      token = tok.nextToken();
    }

    if (!token.equalsIgnoreCase(untilKeyword)) {
      result.append(result.toString().isEmpty() ? "" : " ").append(token)
          .append(token);
    }
    return result.toString();
  }

  protected String readLine() throws IOException {
    if (isConnected()) {
      return inReader.readLine();
    } else {
      log.info("readLine: not connected");
      return null;
    }
  }

  protected void resetConnectionState() {
    process = null;
    engineName = null;
    goRunnable = null;
    lastBestMove = null;
  }

  private void send(String command) {
    if (isConnected()) {
      log.info("Sending command: {}", command);
      outWriter.print(command + "\n");
      outWriter.flush();
    }
  }

}

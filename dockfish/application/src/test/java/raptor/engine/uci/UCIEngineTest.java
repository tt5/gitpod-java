package raptor.engine.uci;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class UCIEngineTest {
  // public static final String CLASS_PATH = ".\\target\\classes;.\\target\\test-classes;";
  public static final String CLASS_PATH = System.getProperty("java.class.path");

  @Captor
  private ArgumentCaptor<List<UCIInfo>> uciInfoCaptor;

  @Mock
  private UCIInfoListener uciInfoListener;

  private UCIEngine cut;

  private static LogCaptor logCaptor;

  @BeforeAll
  public static void setupLogCaptor() {
    logCaptor = LogCaptor.forClass(UCIEngine.class);
  }

  @AfterEach
  public void clearLogCaptor() {
    logCaptor.clearLogs();
  }

  @AfterAll
  public static void closeLogCaptor() {
    logCaptor.resetLogLevel();
    logCaptor.close();
  }

  @BeforeEach
  void setUp() {
    cut = new UCIEngine();
  }

  @AfterEach
  void tearDownCut() {
    cut.quit();
  }

  @Nested
  class GivenSleepyEngineStub {
    @BeforeEach
    void setUp() {
      cut = new UCIEngine(1000);
      cut.setProcessPath("java");
      cut.setParameters("-cp", CLASS_PATH, "raptor.engine.uci.SleepyEngineStub");
    }

    @Test
    void whenConnectingThenDoNotConnect() {
      logCaptor.disableLogs();
      boolean result = cut.connect();
      logCaptor.resetLogLevel();

      assertThat(result, is(false));
      assertThat(cut.isConnected(), is(false));
    }
  }

  @Nested
  class GivenEngineStub {
    @BeforeEach
    void setUp() {
      cut.setProcessPath("java");
      cut.setParameters("-cp", CLASS_PATH, "raptor.engine.uci.EngineStub");
    }

    @Nested
    class WhenNotConnected {
      @Test
      void thenGoThrows() {
        assertThrows(IllegalStateException.class, () -> cut.go("any depth", uciInfoListener));
      }
    }

    @Nested
    class WhenConnected {

      private boolean connected;

      @BeforeEach
      void setUp() {
        connected = cut.connect();
        System.out.println("connected = " + connected);
      }

      @Test
      void thenIsConnected() {
        assertThat(connected, is(true));
        assertThat(cut.isConnected(), is(true));
        assertThat(cut.getEngineAuthor(), is("CE"));
        assertThat(cut.getEngineName(), is("EngineStub"));
        assertThat(cut.getOptionNames(), is(arrayContainingInAnyOrder(
            "Debug Log File", "Ponder", "MultiPv", "Threads", "Clear Hash", "Analysis Contempt")));
        Arrays.stream(cut.getOptionNames())
            .forEach(name -> assertThat(cut.hasOption(name), is(true)));

        assertThat(cut.hasOption("Threads"), is(true));
        assertThat(cut.getOption("Threads").getDefaultValue(), is("1"));
        assertThat(cut.getOption("Threads").getValue(), is("1"));

        assertThat(cut.getProcessPath(), is("java"));
      }

      @Test
      void thenOptionsCanBeSet() {
        UCIOption threads = cut.getOption("Threads");
        threads.setValue("5");
        cut.setOption(threads);

        assertThat(cut.getOption("Threads").getValue(), is("5"));
      }

      @Test
      void thenRepeatedConnectDoesReturnTrue() {
        cut.connect();

        assertThat(cut.isConnected(), is(true));
      }

      @Nested
      class WhenProcessingShortGo {

        @BeforeEach
        void setUp() {
          cut.setPosition("fen");
          cut.go("depth 2", uciInfoListener);
        }

        @Test
        void thenEventListenersAreTriggered() {
          verify(uciInfoListener, timeout(1000).times(3)).engineSentInfo(uciInfoCaptor.capture());
          List<List<UCIInfo>> capturedInfos = uciInfoCaptor.getAllValues();

          System.out.println("uciInfoCaptor.getAllValue = " + capturedInfos.toString());
          List<UCIInfo> uciInfosElement0 = capturedInfos.get(0);
          assertThat(((StringInfo) uciInfosElement0.getFirst()).getValue(),
              is(equalTo("NNUE evaluation using nn-82215d0fd0df.nnue enabled")));

          List<UCIInfo> uciInfosElement1 = capturedInfos.get(1);
          assertThat(uciInfosElement1, contains(isA(DepthInfo.class),
              isA(SelectiveSearchDepthInfo.class),
              isA(MultiPV.class),
              isA(ScoreInfo.class),
              isA(NodesSearchedInfo.class),
              isA(NodesPerSecondInfo.class),
              isA(TableBaseHitsInfo.class),
              isA(TimeInfo.class),
              isA(BestLineFoundInfo.class),
              isA(CPULoadInfo.class)
          ));
          BestLineFoundInfo expectedBestLine = new BestLineFoundInfo();
          expectedBestLine.setMoves(new UCIMove("e2e4"), new UCIMove("e7e5"), new UCIMove("g1f3"), new UCIMove("b8c6"),
              new UCIMove("f1b5"), new UCIMove("a7a6"), new UCIMove("b5"), new UCIMove("a4"), new UCIMove("g8f6"));
          BestLineFoundInfo actualBestLine = uciInfosElement1.stream()
              .filter(BestLineFoundInfo.class::isInstance)
              .map(BestLineFoundInfo.class::cast)
              .findFirst()
              .orElseThrow();
          assertThat(actualBestLine.getMoves().stream().map(UCIMove::getValue).toList(),
              contains(expectedBestLine.getMoves().stream().map(UCIMove::getValue).toArray()));

          List<UCIInfo> uciInfosElement2 = capturedInfos.get(2);
          assertThat(uciInfosElement2, contains(isA(DepthInfo.class),
              isA(CurrentMoveInfo.class)
          ));

          verify(uciInfoListener).engineSentBestMove(any());
        }

        @Nested
        class WhenBeingStopped {

          private UCIBestMove uciBestMove;

          @BeforeEach
          void setUp() {
            verify(uciInfoListener, timeout(1000).times(3)).engineSentInfo(any());
            uciBestMove = cut.stop();
          }

          @Test
          void thenItSendsBestMove() {
            assertThat(uciBestMove.getBestMove().getValue(), is("d2d4"));
          }

          @Test
          void thenItIsStopped() {
            assertThat(cut.isProcessingGo(), is(false));
            assertThat(cut.isConnected(), is(true));
          }
        }
      }

      @Nested
      class WhenProcessingLongGo {

        @BeforeEach
        void setUp() {
          cut.setPosition("fen");
          cut.go("depth 50", uciInfoListener);
        }

        @Test
        void thenIsProcessingGo() {
          assertThat(cut.isProcessingGo(), is(true));
        }


        @Test
        void thenRepeatedGoJustLogs() {
          // verify engine is busy
          verify(uciInfoListener, timeout(1000).atLeastOnce()).engineSentInfo(any());

          UCIInfoListener uciInfoListener2 = mock(UCIInfoListener.class);
          cut.go("depth 11", uciInfoListener2);

          assertThat(logCaptor.getInfoLogs(), hasItem(containsString("Go is in process. Ignoring go call.")));
          verifyNoInteractions(uciInfoListener2);
          assertThat(cut.isProcessingGo(), is(true));
        }

        @Test
        void thenCallingEvalWillJustWarn() {
          cut.eval();

          assertThat(logCaptor.getWarnLogs(), hasItem(containsString("Engine is processing, Do nothing")));
          assertThat(cut.isProcessingGo(), is(true));
        }

        @Test
        void thenEventListenersAreTriggered() {
          verify(uciInfoListener, timeout(1000).times(3)).engineSentInfo(any());
          verify(uciInfoListener, times(0)).engineSentBestMove(any());
        }

        @Test
        void thenOptionsCanBeModified() {
          UCIOption multiPv = cut.getOption("MultiPv");
          multiPv.setValue("7");

          cut.stopSetOptionGo(multiPv);
          cut.stop();

          assertThat(cut.getOption("MultiPv").getValue(), is("7"));
        }

        @Nested
        class WhenBeingKilled {
          @BeforeEach
          void setUp() {
            verify(uciInfoListener, timeout(1000).times(3)).engineSentInfo(any());
            assertThat(cut.isConnected(), is(true));
            cut.kill();
          }

          @Test
          void thenItIsStopped() {
            await().atMost(10, TimeUnit.SECONDS).
                untilAsserted(() -> assertThat(cut.isConnected(), is(false)));
          }
        }

        @Nested
        class WhenBeingStopped {

          private UCIBestMove uciBestMove;

          @BeforeEach
          void setUp() {
            verify(uciInfoListener, timeout(1000).times(3)).engineSentInfo(any());
            uciBestMove = cut.stop();
          }

          @Test
          void thenItSendsBestMove() {
            assertThat(uciBestMove.getBestMove().getValue(), is("e2e4"));
          }

          @Test
          void thenItIsStopped() {
            assertThat(cut.isProcessingGo(), is(false));
            assertThat(cut.isConnected(), is(true));
          }
        }
      }

      @Nested
      class WhenProcessingEval {

        private String eval;

        @BeforeEach
        void setUp() {
          eval = cut.eval();
        }

        @Test
        void thenReturnResult() {
          assertThat(eval, is("Eval-Result1\nEval-Result2\nEval-Result3\n"));
        }
      }
    }
  }
}

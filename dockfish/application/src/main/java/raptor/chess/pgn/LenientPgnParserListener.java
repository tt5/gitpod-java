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

package raptor.chess.pgn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import raptor.chess.Game;
import raptor.chess.GameFactory;
import raptor.chess.Move;
import raptor.chess.Result;

/**
 * A Lenient PGN Parser Listener which creates Games from the PGN being handled.
 * <p>
 * Currently sub-lines are disabled.
 */
@Log4j2
public abstract class LenientPgnParserListener implements PgnParserListener {
  protected Game currentGame;

  protected Map<String, String> currentHeaders = new ConcurrentHashMap<>();

  protected Move currentMoveInfo;

  protected boolean isIgnoringCurrentGame;

  protected boolean isParsingGameHeaders;

  protected boolean isParsingGameMoves;

  protected boolean isParsingMove;

  protected boolean isSearchingForGameStart = true;

  protected int lastStartLineNumber;

  protected LenientPgnParserListener() {
  }

  public Game createGameFromDescription() {
    String fen = null;
    Game result;

    if (currentHeaders.get(PgnHeader.FEN.name()) != null) {
      fen = currentHeaders.get(PgnHeader.FEN.name());
    }

    if (fen == null) {
      result = GameFactory.createStartingPosition();
    } else {
      result = GameFactory.createFromFen(fen);
      result.setHeader(PgnHeader.FEN, fen);
    }

    return result;
  }

  public abstract void errorEncountered(PgnParserError error);

  public abstract boolean gameParsed(Game game, int lineNumber);

  @Override
  public void onAnnotation(PgnParser parser, String annotation) {
    if (!isIgnoringCurrentGame && isParsingGameMoves && isParsingMove) {
      MoveAnnotation[] annotations = pgnAnnotationToMoveAnnotations(annotation);
      for (MoveAnnotation moveAnnotation : annotations) {
        currentMoveInfo.addAnnotation(moveAnnotation);
      }
    }
  }

  @Override
  public boolean onGameEnd(PgnParser parser, Result result) {
    boolean returnResult = false;

    if (!isIgnoringCurrentGame) {
      if (isParsingGameMoves) {

        // add the game
        currentGame.setHeader(PgnHeader.Result, result.getDescription());
        currentGame.addState(Game.INACTIVE_STATE);
        returnResult = gameParsed(currentGame, lastStartLineNumber);
      } else {
        errorEncountered(new PgnParserError(PgnParserError.Type.UNEXPECTED_GAME_END,
            PgnParserError.Action.IGNORING_CURRENT_GAME, parser.getLineNumber()));
      }
    }

    setStateToSearchingForNewGame();
    return returnResult;
  }

  @Override
  public void onGameStart(PgnParser parser) {
    if (!isSearchingForGameStart) {
      errorEncountered(new PgnParserError(PgnParserError.Type.UNEXPECTED_GAME_START, PgnParserError.Action.NONE,
          parser.getLineNumber()));
    }
    setStateToSearchingForNewGame();
    isParsingGameHeaders = true;
    lastStartLineNumber = parser.getLineNumber();
  }

  @Override
  public void onHeader(PgnParser parser, String headerName, String headerValue) {
    if (!isIgnoringCurrentGame) {
      if (isParsingGameHeaders) {
        currentHeaders.put(headerName, headerValue);
      } else {
        errorEncountered(new PgnParserError(PgnParserError.Type.UNEXPECTED_HEADER,
            PgnParserError.Action.IGNORING, parser.getLineNumber()));
      }
    }
  }

  @Override
  public void onMoveNumber(PgnParser parser, int moveNumber) {
    if (!isIgnoringCurrentGame && isParsingGameHeaders) {
      createGameFromHeaders(parser);
      isParsingGameHeaders = false;
      isParsingGameMoves = true;
      isParsingMove = true;
    }
  }

  @Override
  public void onMoveWord(PgnParser parser, String word) {
    if (!isIgnoringCurrentGame) {
      if (isParsingGameMoves) {
        // First complete last move if we are in the middle of one.
        try {
          currentMoveInfo = makeGameMoveFromWord(word);
        } catch (IllegalArgumentException ime) {
          log.error("Invalid move encountered", ime);
          errorEncountered(new PgnParserError(PgnParserError.Type.ILLEGAL_MOVE_ENCOUNTERED,
              PgnParserError.Action.IGNORING_CURRENT_GAME, parser.getLineNumber(),
              word));
          isIgnoringCurrentGame = true;
        }
      } else {
        errorEncountered(new PgnParserError(PgnParserError.Type.UNEXPECTED_MOVE_WORD,
            PgnParserError.Action.IGNORING, parser.getLineNumber(), word));
      }
    }
  }


  @Override
  public void onUnknown(PgnParser parser, String unknown) {
    if (!isIgnoringCurrentGame) {
      errorEncountered(new PgnParserError(PgnParserError.Type.UNKNOWN_TEXT_ENCOUNTERED,
          PgnParserError.Action.IGNORING, parser.getLineNumber(), unknown));
    }
  }

  protected void createGameFromHeaders(PgnParser parser) {
    try {
      currentGame = createGameFromDescription();
      currentGame.addState(Game.UPDATING_SAN_STATE);
      currentGame.addState(Game.UPDATING_ECO_HEADERS_STATE);

      // Set all of the headers.
      for (Map.Entry<String, String> header : currentHeaders.entrySet()) {
        setHeader(header);
      }
    } catch (IllegalArgumentException ife) {
      log.warn("error setting up game", ife);
      errorEncountered(new PgnParserError(PgnParserError.Type.UNABLE_TO_PARSE_INITIAL_FEN,
          PgnParserError.Action.IGNORING_CURRENT_GAME, parser.getLineNumber()));
      isIgnoringCurrentGame = true;
    }

  }

  private void setHeader(Map.Entry<String, String> header) {
    try {
      currentGame.setHeader(PgnHeader.valueOf(header.getKey()), header.getValue());
    } catch (IllegalArgumentException iae) {
      log.warn("ignoring error setting up game", iae);
    }
  }

  protected Move makeGameMoveFromWord(String word) {
    return currentGame.makeSanMove(word);
  }

  protected MoveAnnotation[] pgnAnnotationToMoveAnnotations(String pgnAnnotation) {
    List<MoveAnnotation> annotations = new ArrayList<>(3);

    annotations.add(new Comment(pgnAnnotation));

    return annotations.toArray(new MoveAnnotation[0]);
  }

  protected void setStateToSearchingForNewGame() {
    isParsingGameHeaders = true;
    isParsingGameMoves = false;
    isParsingMove = false;
    isIgnoringCurrentGame = false;
    currentHeaders.clear();
    isSearchingForGameStart = true;
  }

}

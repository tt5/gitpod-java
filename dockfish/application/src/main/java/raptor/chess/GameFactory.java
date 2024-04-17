/*
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright 2009-2016 RaptorProject (https://github.com/Raptor-Fics-Interface/Raptor)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.chess;

import java.util.Locale;
import raptor.chess.util.GameUtils;
import raptor.chess.util.ZobristUtils;
import raptor.util.RaptorStringTokenizer;

/**
 * Contains methods to create Games from fen and starting positions.
 */
public final class GameFactory implements GameConstants {
  private GameFactory() {
  }

  /**
   * Creates a game from fen of the specified type.
   *
   * <pre>
   * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
   * </pre>
   *
   * @param fen The FEN (Forsyth Edwards Notation)
   * @return The game.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "squid:S3776"})
  public static Game createFromFen(String fen) {
    Game result = new ClassicGame();

    RaptorStringTokenizer tok = new RaptorStringTokenizer(fen, " ",
        false);
    String boardStr = "";
    String toMoveStr = null;
    String castlingInfoStr = null;
    String epSquareStr = null;
    String fiftyMoveRuleCountStr = null;
    String fullMoveCountStr = null;
    if (tok.hasMoreTokens()) {
      boardStr = tok.nextToken();
    }
    if (tok.hasMoreTokens()) {
      toMoveStr = tok.nextToken();
    }
    if (tok.hasMoreTokens()) {
      castlingInfoStr = tok.nextToken();
    }
    if (tok.hasMoreTokens()) {
      epSquareStr = tok.nextToken();
    }
    if (tok.hasMoreTokens()) {
      fiftyMoveRuleCountStr = tok.nextToken();
    }
    if (tok.hasMoreTokens()) {
      fullMoveCountStr = tok.nextToken();
    }

    int boardIndex = 56;
    for (int i = 0; i < boardStr.length(); i++) {
      char piece = fen.charAt(i);
      if (piece == '/') {
        boardIndex -= 16;
      } else if (Character.isDigit(piece)) {
        boardIndex += Integer.parseInt(String.valueOf(piece));
      } else {
        int pieceColor = Character.isUpperCase(piece) ? WHITE
            : BLACK;
        int pieceInt = PIECE_TO_SAN
            .indexOf(String.valueOf(piece)
                .toUpperCase(Locale.getDefault())
                .charAt(0));
        long pieceSquare = GameUtils.getBitboard(boardIndex);

        result.setPieceCount(pieceColor, pieceInt, result
            .getPieceCount(pieceColor, pieceInt) + 1);
        result.setPiece(boardIndex, pieceInt);
        result.setColorBB(pieceColor, result.getColorBB(pieceColor)
            | pieceSquare);
        result.setOccupiedBB(result.getOccupiedBB() | pieceSquare);
        result.setPieceBB(pieceColor, pieceInt, result.getPieceBB(
            pieceColor, pieceInt)
            | pieceSquare);
        boardIndex++;
      }
    }

    if (toMoveStr == null) {
      result.setColorToMove(WHITE);
    } else {
      if ("w".equals(toMoveStr)) {
        result.setColorToMove(WHITE);
      } else {
        result.setColorToMove(BLACK);
      }
    }

    if (castlingInfoStr == null) {
      result.setCastling(WHITE, CASTLE_NONE);
      result.setCastling(BLACK, CASTLE_NONE);
    } else {
      boolean whiteCastleKSide = castlingInfoStr.indexOf('K') != -1;
      boolean whiteCastleQSide = castlingInfoStr.indexOf('Q') != -1;
      boolean blackCastleKSide = castlingInfoStr.indexOf('k') != -1;
      boolean blackCastleQSide = castlingInfoStr.indexOf('q') != -1;

      if (whiteCastleKSide && whiteCastleQSide) {
        result.setCastling(WHITE, CASTLE_BOTH);
      } else {
        if (whiteCastleKSide) {
          result.setCastling(WHITE, CASTLE_SHORT);
        } else {
          if (whiteCastleQSide) {
            result.setCastling(WHITE, CASTLE_LONG);
          } else {
            result.setCastling(WHITE, CASTLE_NONE);
          }
        }
      }
      if (blackCastleKSide && blackCastleQSide) {
        result.setCastling(BLACK, CASTLE_BOTH);
      } else {
        if (blackCastleKSide) {
          result.setCastling(BLACK, CASTLE_SHORT);
        } else {
          if (blackCastleQSide) {
            result.setCastling(BLACK, CASTLE_LONG);
          } else {
            result.setCastling(BLACK, CASTLE_NONE);
          }
        }
      }
    }

    if (null == epSquareStr || "-".equals(epSquareStr)) {
      result.setEpSquare(EMPTY_SQUARE);
      result.setInitialEpSquare(EMPTY_SQUARE);
    } else {
      result.setEpSquare(GameUtils.getSquare(epSquareStr));
      result.setInitialEpSquare(result.getEpSquare());
    }

    if (null != fiftyMoveRuleCountStr
        && !"-".equals(fiftyMoveRuleCountStr)) {
      result.setFiftyMoveCount(Integer
          .parseInt(fiftyMoveRuleCountStr));
    }

    if (null != fullMoveCountStr && !"-".equals(fullMoveCountStr)) {
      int fullMoveCount = Integer.parseInt(fullMoveCountStr);
      result
          .setHalfMoveCount(result.getColorToMove() == BLACK ? fullMoveCount * 2 - 1
              : fullMoveCount * 2 - 2);
    }

    result.setEmptyBB(~result.getOccupiedBB());

    if (!result.isLegalPosition()) {
      throw new IllegalArgumentException(
          "Resulting position was illegal for FEN: " + fen);
    }

    result.setZobristPositionHash(ZobristUtils.zobristHashPositionOnly(result));

    result.incrementRepCount();

    return result;
  }

  public static Game createStartingPosition() {
    return createFromFen(STARTING_POSITION_FEN);
  }
}

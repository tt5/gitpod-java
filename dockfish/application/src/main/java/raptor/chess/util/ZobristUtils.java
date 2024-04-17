/*
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright 2009-2011 RaptorProject (http://code.google.com/p/raptor-chess-interface/)
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * <p>
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.chess.util;

import static raptor.chess.util.GameUtils.bitscanClear;
import static raptor.chess.util.GameUtils.bitscanForward;

import java.security.SecureRandom;
import raptor.chess.Game;
import raptor.chess.GameConstants;

public final class ZobristUtils implements GameConstants {
  private static final long[][] ZOBRIST_CASTLE = new long[2][4];
  private static final long[] ZOBRIST_EP = new long[65];
  private static final long[][][] ZOBRIST_POSITION = new long[2][7][64];
  private static final long[] ZOBRIST_TO_MOVE = new long[2];

  private static final SecureRandom random = new SecureRandom();

  static {
    initZobrist();
  }

  private ZobristUtils() {
  }

  public static long zobrist(int color, int piece, int square) {
    return ZOBRIST_POSITION[color][piece][square];
  }

  public static long zobrist(int colorToMove, int epSquare,
                             int whiteCastling, int blackCastling) {
    return ZOBRIST_TO_MOVE[colorToMove] ^ ZOBRIST_EP[epSquare]
        ^ ZOBRIST_CASTLE[WHITE][whiteCastling]
        ^ ZOBRIST_CASTLE[BLACK][blackCastling];
  }

  public static long zobristHashPositionOnly(Game game) {
    return zobristPiece(WHITE, PAWN, game)
        ^ zobristPiece(WHITE, BISHOP, game)
        ^ zobristPiece(WHITE, KNIGHT, game)
        ^ zobristPiece(WHITE, ROOK, game)
        ^ zobristPiece(WHITE, QUEEN, game)
        ^ zobristPiece(WHITE, KING, game)
        ^ zobristPiece(BLACK, PAWN, game)
        ^ zobristPiece(BLACK, BISHOP, game)
        ^ zobristPiece(BLACK, KNIGHT, game)
        ^ zobristPiece(BLACK, ROOK, game)
        ^ zobristPiece(BLACK, QUEEN, game)
        ^ zobristPiece(BLACK, KING, game);
  }

  private static void initZobrist() {
    for (int i = 0; i < ZOBRIST_POSITION.length; i++) {
      for (int j = 0; j < ZOBRIST_POSITION[i].length; j++) {
        for (int k = 0; k < ZOBRIST_POSITION[i][j].length; k++) {
          ZOBRIST_POSITION[i][j][k] = random.nextLong();
        }
      }
    }

    for (int i = 0; i < ZOBRIST_TO_MOVE.length; i++) {
      ZOBRIST_TO_MOVE[i] = random.nextLong();
    }

    for (int i = 0; i < ZOBRIST_EP.length; i++) {
      ZOBRIST_EP[i] = random.nextLong();
    }

    for (int i = 0; i < ZOBRIST_CASTLE.length; i++) {
      for (int j = 0; j < ZOBRIST_CASTLE[i].length; j++) {
        ZOBRIST_CASTLE[i][j] = random.nextLong();
      }
    }
  }

  private static long zobristPiece(int color, int piece, Game game) {
    int result = 0;
    long current = game.getPieceBB(color, piece);
    while (current != 0L) {
      result ^= zobrist(color, piece, bitscanForward(current));
      current = bitscanClear(current);
    }
    return result;
  }
}

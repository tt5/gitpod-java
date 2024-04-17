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

import raptor.chess.Game;
import raptor.chess.GameConstants;

//KoggeStone
//http://www.open-aurec.com/wbforum/viewtopic.php?f=4&t=49948&sid=abd6ee7224f34b11a5211aa167f01ac4
public final class GameUtils implements GameConstants {

  private static final long DE_BRUJIN = 0x03f79d71b4cb0a89L;
  private static final int[] DE_BRUJIN_MAGICS_TABLE = {0, 1, 48, 2, 57, 49,
      28, 3, 61, 58, 50, 42, 38, 29, 17, 4, 62, 55, 59, 36, 53, 51, 43,
      22, 45, 39, 33, 30, 24, 18, 12, 5, 63, 47, 56, 27, 60, 41, 37, 16,
      54, 35, 52, 21, 44, 32, 23, 11, 46, 26, 40, 15, 34, 20, 31, 10, 25,
      14, 19, 9, 13, 8, 7, 6,};

  private static final int[] EP_DIR = {SOUTH, NORTH};
  private static final int[] EP_OPP_DIR = {NORTH, SOUTH};

  private static final long[] KING_ATTACKS = new long[64];
  private static final long[] KNIGHT_ATTACKS = new long[64];

  private static final int[] OPPOSITE_COLOR = {BLACK, WHITE};

  static {
    initKingAttacks();
    initKnightAttacks();
  }

  private GameUtils() {
  }

  /**
   * Clears the next one bit in the bitboard.
   */
  public static long bitscanClear(long bitboard) {
    return bitboard & bitboard - 1;
  }

  /**
   * Returns the next 1 bit in the bitboard. Returns 0 if bitboard is 0.
   */
  public static int bitscanForward(final long bitboard) {
    return bitScanForwardDeBruijn64(bitboard);
  }

  public static int bitScanForwardDeBruijn64(final long b) {
    int idx = (int) ((b & -b) * DE_BRUJIN >>> 58);
    return DE_BRUJIN_MAGICS_TABLE[idx];
  }


  public static long diagonalMove(int square, long emptySquares) {
    long seed = getBitboard(square);
    return shiftUpRight(fillUpRightOccluded(seed, emptySquares))
        | shiftUpLeft(fillUpLeftOccluded(seed, emptySquares))
        | shiftDownLeft(fillDownLeftOccluded(seed, emptySquares))
        | shiftDownRight(fillDownRightfccluded(seed, emptySquares));
  }

  public static long fillDownLeftOccluded(long g, long p) {
    p &= 0x7f7f7f7f7f7f7f7fL;
    g |= p & g >>> 9;
    p &= p >>> 9;
    g |= p & g >>> 18;
    p &= p >>> 18;
    return g | p & g >>> 36;
  }

  public static long fillDownOccluded(long g, long p) {
    g |= p & g >>> 8;
    p &= p >>> 8;
    g |= p & g >>> 16;
    p &= p >>> 16;
    return g | p & g >>> 32;
  }

  public static long fillDownRightfccluded(long g, long p) {
    p &= 0xfefefefefefefefeL;
    g |= p & g >>> 7;
    p &= p >>> 7;
    g |= p & g >>> 14;
    p &= p >>> 14;
    return g | p & g >>> 28;
  }

  public static long fillLeftOccluded(long g, long p) {
    p &= 0x7f7f7f7f7f7f7f7fL;
    g |= p & g >>> 1;
    p &= p >>> 1;
    g |= p & g >>> 2;
    p &= p >>> 2;
    return g | p & g >>> 4;
  }

  /**
   * The routine fillUpOccluded() smears the set bits of bitboard g upwards,
   * but only along set bits of p; a reset bit in p is enough to halt a smear.
   * In the above, g = moving piece(s); p = empty squares.
   */
  public static long fillRightOccluded(long g, long p) {
    p &= 0xfefefefefefefefeL;
    g |= p & g << 1;
    p &= p << 1;
    g |= p & g << 2;
    p &= p << 2;
    return g | p & g << 4;
  }

  public static long fillUpLeftOccluded(long g, long p) {
    p &= 0x7f7f7f7f7f7f7f7fL;
    g |= p & g << 7;
    p &= p << 7;
    g |= p & g << 14;
    p &= p << 14;
    return g | p & g << 28;
  }

  public static long fillUpOccluded(long g, long p) {
    g |= p & g << 8;
    p &= p << 8;
    g |= p & g << 16;
    p &= p << 16;
    return g | p & g << 32;
  }

  public static long fillUpRightOccluded(long g, long p) {
    p &= 0xfefefefefefefefeL;
    g |= p & g << 9;
    p &= p << 9;
    g |= p & g << 18;
    p &= p << 18;
    return g | p & g << 36;
  }

  public static long getBitboard(int square) {
    return SQUARE_TO_COORDINATE[square];
  }

  public static int getFile(int square) {
    return square % 8;
  }

  public static int getOppositeColor(int color) {
    return OPPOSITE_COLOR[color];
  }

  public static int getRank(int square) {
    return square / 8;
  }

  /**
   * Returns the SAN,short algebraic notation for the square. If square is a
   * DROP square returns a constant suitable for debugging.
   *
   * @param square The square.
   */
  public static String getSan(int square) {
    if (square == 64) {
      return "-";
    } else if (square < 100) {
      return String.valueOf(FILE_FROM_SAN.charAt(square % 8)) + RANK_FROM_SAN.charAt(square / 8);
    } else {
      return "DROP";
    }
  }

  /**
   * Returns the square given a 0 based rank and file.
   */
  public static int getSquare(int rank, int file) {
    return rank * 8 + file;
  }

  /**
   * Returns the square representing the bit board.
   */
  public static int getSquare(long bitboard) {
    return bitscanForward(bitboard);
  }

  public static int getSquare(String san) {
    return getSquare(RANK_FROM_SAN.indexOf(san.charAt(1)), FILE_FROM_SAN
        .indexOf(san.charAt(0)));
  }

  public static String getString(String[] labels, long... bitBoards) {
    StringBuilder result = new StringBuilder(200 * bitBoards.length);

    for (int i = 0; i < labels.length; i++) {
      result.append(" ");

      if (labels[i].length() > 18) {
        labels[i] = labels[i].substring(0, 18);
      }
      int spaces = 18 - labels[i].length();
      result.append(labels[i]).append(SPACES, 0, spaces);
    }
    result.append("\n");

    for (int i = 7; i > -1; i--) {
      for (long bitBoard : bitBoards) {
        result.append(" ");
        for (int j = 0; j < 8; j++) {
          result.append((bitBoard & SQUARE_TO_COORDINATE[getSquare(
              i, j)]) == 0 ? 0 : 1).append(" ");
        }
        result.append("  ");
      }

      if (i != 0) {
        result.append("\n");
      }
    }

    return result.toString();
  }

  public static boolean isInBounds(int rank, int file) {
    return rank >= 0 && rank <= 7 && file >= 0 && file <= 7;
  }

  public static boolean isWhitePiece(Game game, int square) {
    return (game.getColorBB(WHITE) & getBitboard(square)) != 0;
  }

  public static long kingMove(int square) {
    return KING_ATTACKS[square];
  }

  public static long knightMove(int square) {
    return KNIGHT_ATTACKS[square];
  }

  public static long moveOne(int direction, long bitboard) {
    switch (direction) {
      case NORTH:
        return bitboard << 8;
      case NORTHEAST:
        return bitboard << 9;
      case NORTHWEST:
        return bitboard << 7;
      case SOUTH:
        return bitboard >>> 8;
      case SOUTHEAST:
        return bitboard >>> 7;
      case SOUTHWEST:
        return bitboard >>> 9;
      case EAST:
        return bitboard << 1;
      case WEST:
        return bitboard >>> 1;
      default:
        throw new IllegalArgumentException("Unknown direction: "
            + direction);
    }
  }

  public static long orthogonalMove(int square, long emptySquares) {
    long seed = getBitboard(square);
    return shiftRight(fillRightOccluded(seed, emptySquares))
        | shiftLeft(fillLeftOccluded(seed, emptySquares))
        | shiftUp(fillUpOccluded(seed, emptySquares))
        | shiftDown(fillDownOccluded(seed, emptySquares));
  }

  public static long pawnCapture(int colorToMove, long toMovePawns,
                                 long enemyPieces) {
    return colorToMove == WHITE ? ((toMovePawns & NOT_AFILE) << 7 | (toMovePawns & NOT_HFILE) << 9)
        & enemyPieces
        : ((toMovePawns & NOT_HFILE) >> 7 | (toMovePawns & NOT_AFILE) >>> 9)
        & enemyPieces;
  }

  public static long pawnDoublePush(int colorToMove, long toMovePawns,
                                    long empty) {
    int direction = colorToMove == WHITE ? NORTH : SOUTH;
    long rankBB = colorToMove == WHITE ? RANK4 : RANK5;

    return moveOne(direction, moveOne(direction, toMovePawns) & empty)
        & empty & rankBB;
  }

  public static long pawnEpCapture(int colorToMove, long toMovePawns,
                                   long enemyPawns, long epSquare) {
    long enemyPawnsIdx = enemyPawns;
    enemyPawnsIdx &= moveOne(EP_DIR[colorToMove], epSquare);
    enemyPawnsIdx = moveOne(EP_OPP_DIR[colorToMove], enemyPawnsIdx);
    return pawnCapture(colorToMove, toMovePawns, enemyPawnsIdx);
  }

  public static long pawnSinglePush(int colorToMove, long toMovePawns,
                                    long empty) {
    int direction = colorToMove == WHITE ? NORTH : SOUTH;
    return moveOne(direction, toMovePawns) & empty;
  }

  public static long shiftDown(long b) {
    return b >>> 8;
  }

  public static long shiftDownLeft(long b) {
    return b >>> 9 & 0x7f7f7f7f7f7f7f7fL;
  }

  public static long shiftDownRight(long b) {
    return b >>> 7 & 0xfefefefefefefefeL;
  }

  public static long shiftLeft(long b) {
    return b >>> 1 & 0x7f7f7f7f7f7f7f7fL;
  }

  // KoggeStone algorithm
  public static long shiftRight(long b) {
    return b << 1 & 0xfefefefefefefefeL;
  }

  public static long shiftUp(long b) {
    return b << 8;
  }

  public static long shiftUpLeft(long b) {
    return b << 7 & 0x7f7f7f7f7f7f7f7fL;
  }

  public static long shiftUpRight(long b) {
    return b << 9 & 0xfefefefefefefefeL;
  }

  @SuppressWarnings("squid:S3776")
  private static void initKingAttacks() {
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        long bitMap = 0L;
        if (isInBounds(i, j + 1)) {
          bitMap |= getBitboard(getSquare(i, j + 1));
        }
        if (isInBounds(i, j - 1)) {
          bitMap |= getBitboard(getSquare(i, j - 1));
        }

        if (isInBounds(i + 1, j)) {
          bitMap |= getBitboard(getSquare(i + 1, j));
        }
        if (isInBounds(i + 1, j + 1)) {
          bitMap |= getBitboard(getSquare(i + 1, j + 1));
        }
        if (isInBounds(i + 1, j - 1)) {
          bitMap |= getBitboard(getSquare(i + 1, j - 1));
        }

        if (isInBounds(i - 1, j)) {
          bitMap |= getBitboard(getSquare(i - 1, j));
        }
        if (isInBounds(i - 1, j + 1)) {
          bitMap |= getBitboard(getSquare(i - 1, j + 1));
        }
        if (isInBounds(i - 1, j - 1)) {
          bitMap |= getBitboard(getSquare(i - 1, j - 1));
        }

        KING_ATTACKS[getSquare(i, j)] = bitMap;
      }
    }
  }

  @SuppressWarnings("squid:S3776")
  private static void initKnightAttacks() {
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        long bitMap = 0L;
        if (isInBounds(i + 2, j + 1)) {
          bitMap |= getBitboard(getSquare(i + 2, j + 1));
        }
        if (isInBounds(i + 2, j - 1)) {
          bitMap |= getBitboard(getSquare(i + 2, j - 1));
        }

        if (isInBounds(i - 2, j + 1)) {
          bitMap |= getBitboard(getSquare(i - 2, j + 1));
        }
        if (isInBounds(i - 2, j - 1)) {
          bitMap |= getBitboard(getSquare(i - 2, j - 1));
        }

        if (isInBounds(i + 1, j + 2)) {
          bitMap |= getBitboard(getSquare(i + 1, j + 2));
        }
        if (isInBounds(i + 1, j - 2)) {
          bitMap |= getBitboard(getSquare(i + 1, j - 2));
        }

        if (isInBounds(i - 1, j + 2)) {
          bitMap |= getBitboard(getSquare(i - 1, j + 2));
        }
        if (isInBounds(i - 1, j - 2)) {
          bitMap |= getBitboard(getSquare(i - 1, j - 2));
        }

        KNIGHT_ATTACKS[getSquare(i, j)] = bitMap;
      }
    }
  }

}

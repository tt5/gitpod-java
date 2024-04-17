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

package raptor.chess;

import java.util.ArrayList;
import java.util.List;
import raptor.chess.pgn.Comment;
import raptor.chess.pgn.MoveAnnotation;
import raptor.chess.util.GameUtils;

public class Move implements GameConstants {
  public static final int DOUBLE_PAWN_PUSH_CHARACTERISTIC = 4;
  public static final int EN_PASSANT_CHARACTERISTIC = 16;
  public static final int LONG_CASTLING_CHARACTERISTIC = 2;
  public static final int PROMOTION_CHARACTERISTIC = 8;
  public static final int SHORT_CASTLING_CHARACTERISTIC = 1;

  /**
   * May or may not be used. It is obviously not suitable to use this for a
   * chess engine. That is why it starts out null.
   */
  protected List<MoveAnnotation> annotations;
  protected byte capture;
  protected byte color;
  protected byte epSquare = EMPTY_SQUARE;
  // Bytes are used because they take up less space than ints and there is no
  // need for the extra space.
  protected byte from;
  /**
   * May or may not be used.
   */
  protected int fullMoveCount;
  /**
   * May or may not be used.
   */
  protected byte lastWhiteCastlingState = CASTLE_NONE;
  protected byte lastBlackCastlingState = CASTLE_NONE;
  protected byte moveCharacteristic;
  protected byte piece;

  protected byte piecePromotedTo = EMPTY;

  protected byte previous50MoveCount;

  /**
   * May or may not be used.
   */
  protected String san;

  protected byte to;

  public Move(int from, int to, int piece, int color, int capture) {
    this.piece = (byte) piece;
    this.color = (byte) color;
    this.capture = (byte) capture;
    this.from = (byte) from;
    this.to = (byte) to;
  }

  public Move(int from, int to, int piece, int color, int capture,
              int moveCharacteristic) {
    this.piece = (byte) piece;
    this.color = (byte) color;
    this.capture = (byte) capture;
    this.from = (byte) from;
    this.to = (byte) to;
    this.moveCharacteristic = (byte) moveCharacteristic;
  }

  public Move(int from, int to, int piece, int color, int capture,
              int piecePromotedTo, int epSquare, int moveCharacteristic) {
    this.piece = (byte) piece;
    this.color = (byte) color;
    this.capture = (byte) capture;
    this.from = (byte) from;
    this.to = (byte) to;
    this.piecePromotedTo = (byte) piecePromotedTo;
    this.epSquare = (byte) epSquare;
    this.moveCharacteristic = (byte) moveCharacteristic;
  }

  public void addAnnotation(MoveAnnotation annotation) {
    if (annotations == null) {
      annotations = new ArrayList<>(5);
    }
    annotations.add(annotation);
  }

  /**
   * Returns the capture without the promote mask.
   */
  public int getCapture() {
    return capture & NOT_PROMOTED_MASK;
  }

  public int getCaptureColor() {
    return GameUtils.getOppositeColor(getColor());
  }

  /**
   * Returns the capture with the promote mask.
   */
  public int getCaptureWithPromoteMask() {
    return capture;
  }

  public int getColor() {
    return color;
  }

  public Comment[] getComments() {
    if (annotations == null) {
      return new Comment[0];
    }

    List<Comment> result = new ArrayList<>(3);
    for (MoveAnnotation annotation : annotations) {
      if (annotation instanceof Comment) {
        result.add((Comment) annotation);
      }
    }
    return result.toArray(new Comment[0]);
  }

  public int getEpSquare() {
    return epSquare;
  }

  public int getFrom() {
    return from;
  }

  public int getFullMoveCount() {
    return fullMoveCount;
  }

  public String getLan() {
    if (isCastleShort()) {
      return "O-O";
    } else {
      if (isCastleLong()) {
        return "O-O-O";
      }
      return GameUtils.getSan(getFrom())
          + "-"
          + GameUtils.getSan(getTo())
          + (isPromotion() ? "="
          + PIECE_TO_SAN.charAt(piecePromotedTo
          & NOT_PROMOTED_MASK) : "");
    }
  }

  public int getLastBlackCastlingState() {
    return lastBlackCastlingState;
  }

  public byte getLastWhiteCastlingState() {
    return lastWhiteCastlingState;
  }

  public int getMoveCharacteristic() {
    return moveCharacteristic;
  }

  public int getPiece() {
    return piece & NOT_PROMOTED_MASK;
  }

  public int getPiecePromotedTo() {
    return piecePromotedTo;
  }

  public int getPieceWithPromoteMask() {
    return piece;
  }

  public int getPrevious50MoveCount() {
    return previous50MoveCount;
  }

  public String getSan() {
    return san;
  }

  public int getTo() {
    return to;
  }

  public boolean isCapture() {
    return getCapture() != EMPTY;
  }

  public boolean isCastleLong() {
    return (moveCharacteristic & LONG_CASTLING_CHARACTERISTIC) != 0;
  }

  public boolean isCastleShort() {
    return (moveCharacteristic & SHORT_CASTLING_CHARACTERISTIC) != 0;
  }

  public boolean isPromotion() {
    return piecePromotedTo != EMPTY;
  }

  public boolean isWhitesMove() {
    return color == WHITE;
  }

  public void setFullMoveCount(int fullMoveCount) {
    this.fullMoveCount = fullMoveCount;
  }

  public void setLastBlackCastlingState(int lastBlackCastlingState) {
    this.lastBlackCastlingState = (byte) lastBlackCastlingState;
  }

  public void setLastWhiteCastlingState(int lastWhiteCastlingState) {
    this.lastWhiteCastlingState = (byte) lastWhiteCastlingState;
  }

  public void setPrevious50MoveCount(int previous50MoveCount) {
    this.previous50MoveCount = (byte) previous50MoveCount;
  }

  public void setSan(String san) {
    this.san = san;
  }

  @Override
  public String toString() {
    return san == null ? getLan() : san;
  }
}

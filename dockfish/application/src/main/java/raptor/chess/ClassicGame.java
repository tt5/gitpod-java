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

import static raptor.chess.util.GameUtils.bitscanClear;
import static raptor.chess.util.GameUtils.bitscanForward;
import static raptor.chess.util.GameUtils.diagonalMove;
import static raptor.chess.util.GameUtils.getBitboard;
import static raptor.chess.util.GameUtils.getFile;
import static raptor.chess.util.GameUtils.getOppositeColor;
import static raptor.chess.util.GameUtils.getRank;
import static raptor.chess.util.GameUtils.getSan;
import static raptor.chess.util.GameUtils.getSquare;
import static raptor.chess.util.GameUtils.getString;
import static raptor.chess.util.GameUtils.kingMove;
import static raptor.chess.util.GameUtils.knightMove;
import static raptor.chess.util.GameUtils.moveOne;
import static raptor.chess.util.GameUtils.orthogonalMove;
import static raptor.chess.util.GameUtils.pawnCapture;
import static raptor.chess.util.GameUtils.pawnDoublePush;
import static raptor.chess.util.GameUtils.pawnEpCapture;
import static raptor.chess.util.GameUtils.pawnSinglePush;
import static raptor.chess.util.ZobristUtils.zobrist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import raptor.chess.pgn.PgnHeader;
import raptor.chess.pgn.PgnUtils;
import raptor.chess.util.GameUtils;
import raptor.chess.util.SanUtils;
import raptor.chess.util.SanUtils.SanValidations;

/**
 * Implements Classic game rules and provides protected methods so it can easily
 * be subclassed to override behavior for variants.
 */
public class ClassicGame implements Game {

  protected int[] board = new int[64];
  protected int[] castling = new int[2];
  protected long[] colorBB = new long[2];
  protected int colorToMove;

  protected long emptyBB;
  protected int epSquare = EMPTY_SQUARE;
  protected int fiftyMoveCount;
  protected int halfMoveCount;
  protected int initialEpSquare = EMPTY_SQUARE;
  protected int[] moveRepHash = new int[MOVE_REP_CACHE_SIZE];
  protected MoveList moves = new MoveList();
  protected long occupiedBB;
  protected Map<PgnHeader, String> pgnHeaderMap = new EnumMap<>(PgnHeader.class); // NOPMD
  protected long[][] pieceBB = new long[2][7];
  protected int[][] pieceCounts = new int[2][7];
  protected int state;
  protected long zobristPositionHash;

  public ClassicGame() {
    pgnHeaderMap.put(PgnHeader.Result, Result.ON_GOING.getDescription());
  }

  @Override
  public void addState(int state) {
    setState(this.state | state);
  }

  @Override
  public boolean areBothKingsOnBoard() {
    return getPieceBB(WHITE, KING) != 0L && getPieceBB(BLACK, KING) != 0L;
  }

  @Override
  public void forceMove(Move move) {
    move.setLastWhiteCastlingState(getCastling(WHITE));
    move.setLastBlackCastlingState(getCastling(BLACK));
    setSan(move);
    switch (move.getMoveCharacteristic()) {
      case Move.EN_PASSANT_CHARACTERISTIC -> makeEPMove(move);
      case Move.SHORT_CASTLING_CHARACTERISTIC, Move.LONG_CASTLING_CHARACTERISTIC -> makeCastlingMove(move, false);
      default -> makeNonEpNonCastlingMove(move);
    }

    int oppToMove = getOppositeColor(colorToMove);

    move.setPrevious50MoveCount(fiftyMoveCount);
    if (move.isCapture()) {
      decrementPieceCount(oppToMove, move.getCaptureWithPromoteMask());
      setFiftyMoveCount(0);
    } else if (move.getPiece() == PAWN) {
      setFiftyMoveCount(0);
    } else {
      setFiftyMoveCount(fiftyMoveCount + 1);
    }

    setColorToMove(oppToMove);
    setHalfMoveCount(halfMoveCount + 1);

    moves.append(move);

    incrementRepCount();

    move.setFullMoveCount((halfMoveCount - 1) / 2 + 1);

  }

  @Override
  public int getCastling(int color) {
    return castling[color];
  }

  @Override
  public long getColorBB(int color) {
    return colorBB[color];
  }

  @Override
  public int getColorToMove() {
    return colorToMove;
  }

  @Override
  public int getEpSquare() {
    return epSquare;
  }

  @Override
  @SuppressWarnings("squid:S3358")
  public String getFenCastle() {
    String whiteCastlingFen = getCastling(WHITE) == CASTLE_NONE ? ""
        : getCastling(WHITE) == CASTLE_BOTH ? "KQ"
        : getCastling(WHITE) == CASTLE_SHORT ? "K" : "Q";
    String blackCastlingFen = getCastling(BLACK) == CASTLE_NONE ? ""
        : getCastling(BLACK) == CASTLE_BOTH ? "kq"
        : getCastling(BLACK) == CASTLE_SHORT ? "k" : "q";

    return StringUtils.isBlank(whiteCastlingFen) && StringUtils.isBlank(blackCastlingFen) ? "-"
        : whiteCastlingFen + blackCastlingFen;
  }

  @Override
  public int getFullMoveCount() {
    return halfMoveCount / 2 + 1;
  }

  @Override
  public int getHalfMoveCount() {
    return halfMoveCount;
  }

  @Override
  public String getHeader(PgnHeader header) {
    return pgnHeaderMap.get(header);
  }

  @Override
  public PriorityMoveList getLegalMoves() {
    PriorityMoveList result = getPseudoLegalMoves();

    for (int i = 0; i < result.getHighPrioritySize(); i++) {
      Move move = result.getHighPriority(i);
      forceMove(move);
      if (!isLegalPosition()) {
        result.removeHighPriority(i);
        i--; // NOPMD - remove current element and reduce the index accordingly
      }

      rollback();
    }

    for (int i = 0; i < result.getLowPrioritySize(); i++) {
      Move move = result.getLowPriority(i);
      forceMove(move);

      if (!isLegalPosition()) {
        result.removeLowPriority(i);
        i--; // NOPMD - remove current element and reduce the index accordingly
      }

      rollback();
    }

    return result;
  }

  @Override
  public MoveList getMoveList() {
    return moves;
  }

  @Override
  public long getNotColorToMoveBB() {
    return ~getColorBB(getColorToMove());
  }

  @Override
  public long getOccupiedBB() {
    return occupiedBB;
  }

  @Override
  public int getPiece(int square) {
    return board[square] & NOT_PROMOTED_MASK;
  }

  @Override
  public long getPieceBB(int color, int piece) {
    return pieceBB[color][piece];
  }

  @Override
  public int getPieceCount(int color, int piece) {
    return pieceCounts[color][piece & NOT_PROMOTED_MASK];
  }

  @Override
  public int getPieceWithPromoteMask(int square) {
    return board[square];
  }

  @Override
  public PriorityMoveList getPseudoLegalMoves() {
    PriorityMoveList result = new PriorityMoveList();
    generatePseudoQueenMoves(result);
    generatePseudoKnightMoves(result);
    generatePseudoBishopMoves(result);
    generatePseudoRookMoves(result);
    generatePseudoPawnMoves(result);
    generatePseudoKingMoves(result);
    return result;
  }

  @Override
  public int getRepHash() {
    return (int) (zobristPositionHash & MOVE_REP_CACHE_SIZE_MINUS_1);
  }

  @Override
  public Result getResult() {
    return Result.get(getHeader(PgnHeader.Result));
  }

  @Override
  public void incrementRepCount() {
    moveRepHash[getRepHash()]++;
  }

  @Override
  public boolean isInCheck(int color) {
    return isInCheck(color, getPieceBB(color, KING));
  }

  @Override
  public boolean isInCheck(int color, long pieceBB) {
    int kingSquare = bitscanForward(pieceBB);
    int oppositeColor = getOppositeColor(color);

    return !(pawnCapture(oppositeColor, getPieceBB(oppositeColor, PAWN),
        pieceBB) == 0L
        && (orthogonalMove(kingSquare, emptyBB) & (getPieceBB(
        oppositeColor, ROOK) | getPieceBB(oppositeColor, QUEEN))) == 0L
        && (diagonalMove(kingSquare, emptyBB) & (getPieceBB(
        oppositeColor, BISHOP) | getPieceBB(oppositeColor,
        QUEEN))) == 0L
        && (kingMove(kingSquare) & getPieceBB(oppositeColor, KING)) == 0L && (knightMove(kingSquare) & getPieceBB(
        oppositeColor, KNIGHT)) == 0L);
  }

  @Override
  public boolean isInState(int state) {
    return (this.state & state) != 0;
  }

  @Override
  public boolean isLegalPosition() {
    return areBothKingsOnBoard()
        && !isInCheck(getOppositeColor(colorToMove));
  }

  @Override
  public boolean isSettingEcoHeaders() {
    return isInState(UPDATING_ECO_HEADERS_STATE);
  }

  @Override
  public boolean isSettingMoveSan() {
    return isInState(UPDATING_SAN_STATE);
  }

  @Override
  public boolean isWhitesMove() {
    return colorToMove == WHITE;
  }

  @Override
  public Move makeMove(int startSquare, int endSquare) {
    Move move = null;

    Move[] legals = getLegalMoves().asArray();

    for (int i = 0; move == null && i < legals.length; i++) {
      Move candidate = legals[i];
      if (candidate.getFrom() == startSquare
          && candidate.getTo() == endSquare) {
        move = candidate;
      }
    }

    if (move == null) {
      throw new IllegalArgumentException("Invalid move: "
          + getSan(startSquare) + " " + getSan(endSquare) + " \n"
          + this);
    } else {
      forceMove(move);
    }

    return move;
  }

  @Override
  public Move makeMove(int startSquare, int endSquare, int promotePiece) {
    Move move = null;

    Move[] legals = getLegalMoves().asArray();

    for (int i = 0; move == null && i < legals.length; i++) {
      Move candidate = legals[i];
      if (candidate.getFrom() == startSquare
          && candidate.getTo() == endSquare
          && candidate.getPiecePromotedTo() == promotePiece) {
        move = candidate;
      }
    }

    if (move == null) {
      throw new IllegalArgumentException("Invalid move: "
          + getSan(startSquare) + "-" + getSan(endSquare) + "="
          //+ PIECE_TO_SAN.charAt(promotePiece) + "\n"
          + this);
    } else {
      forceMove(move);
    }

    return move;
  }

  @Override
  @SuppressWarnings("squid:S3776")
  public Move makeSanMove(String shortAlgebraic) {
    SanValidations validations = SanUtils.getValidations(shortAlgebraic);
    // Examples:
    // e4 (a pawn move to e4).
    // e8=Q (a pawn promotion without a capture).
    // de=Q (a pawn promotion from a capture).
    // ed (e pawn captures d pawn).
    // Ne3 (a Knight moving to e3).
    // N5e3 (disambiguity for two knights which can move to e3, the 5th
    // rank
    // knight is the one that should move).
    // Nfe3 (disambiguity for two knights which can move to e3, the
    // knight
    // on the f file is the one that should move).
    // Nf1e3 (disambiguity for three knights which cam move to e3, the
    // f1
    // knight is the one that should move).
    if (!validations.isValidStrict()) {
      throw new IllegalArgumentException("Invalid short algebraic: "
          + shortAlgebraic);
    }

    Move[] pseudoLegals = getPseudoLegalMoves().asArray();
    Move result = null;
    int candidatePromotedPiece = EMPTY;

    if (validations.isCastleKSideStrict()) {
      for (Move move : pseudoLegals) {
        if (move != null
            && (move.getMoveCharacteristic() & Move.SHORT_CASTLING_CHARACTERISTIC) != 0) {
          result = move;
          break;
        }
      }
    } else if (validations.isCastleQSideStrict()) {
      for (Move move : pseudoLegals) {
        if (move != null
            && (move.getMoveCharacteristic() & Move.LONG_CASTLING_CHARACTERISTIC) != 0) {
          result = move;
          break;
        }
      }
    } else {
      MoveList matches = new MoveList(10);
      if (validations.isPromotion()) {
        char pieceChar = validations.getStrictSan().charAt(
            validations.getStrictSan().length() - 1);
        candidatePromotedPiece = SanUtils.sanToPiece(pieceChar);
      }

      if (validations.isPawnMove()) {
        int candidatePieceMoving = PAWN;
        if (validations.isEpOrAmbigPxStrict()
            || validations.isAmbigPxPromotionStrict()) {

          int end = getSquare(RANK_FROM_SAN
                  .indexOf(validations.getStrictSan().charAt(2)),
              FILE_FROM_SAN.indexOf(validations
                  .getStrictSan().charAt(1)));

          int startRank = getRank(end)
              + (colorToMove == WHITE ? -1 : +1);

          if (startRank > 7 || startRank < 0) {
            throw new IllegalArgumentException(
                "Invalid short algebraic: "
                    + shortAlgebraic);
          }

          int start = getSquare(startRank,
              FILE_FROM_SAN.indexOf(validations
                  .getStrictSan().charAt(0)));

          for (Move move : pseudoLegals) {
            if (move != null
                && move.getPiece() == candidatePieceMoving
                && move.isCapture()
                && move.getFrom() == start
                && move.getTo() == end
                && move.getPiecePromotedTo() == candidatePromotedPiece) {
              matches.append(move);
            }
          }
        } else {
          // handle captures
          if (validations.isPxStrict()
              || validations.isPxPPromotionStrict()) {
            int startFile = FILE_FROM_SAN
                .indexOf(validations.getStrictSan().charAt(
                    0));
            int endFile = FILE_FROM_SAN
                .indexOf(validations.getStrictSan().charAt(
                    1));

            for (Move move : pseudoLegals) {
              if (move != null
                  && move.getPiece() == candidatePieceMoving
                  && getFile(move.getFrom()) == startFile
                  && getFile(move.getTo()) == endFile
                  && move.isCapture()
                  && move.getPiecePromotedTo() == candidatePromotedPiece) {
                matches.append(move);
              }
            }
          }
          // handle non captures.
          else {
            int end = getSquare(RANK_FROM_SAN
                .indexOf(validations.getStrictSan().charAt(
                    1)), FILE_FROM_SAN
                .indexOf(validations.getStrictSan().charAt(
                    0)));

            for (Move move : pseudoLegals) {
              if (move != null
                  && move.getPiece() == candidatePieceMoving
                  && !move.isCapture()
                  && move.getTo() == end
                  && move.getPiecePromotedTo() == candidatePromotedPiece) {
                matches.append(move);
              }
            }
          }
        }
      } else {
        int candidatePieceMoving = SanUtils.sanToPiece(validations
            .getStrictSan().charAt(0));
        int end = getSquare(RANK_FROM_SAN
                .indexOf(validations.getStrictSan().charAt(
                    validations.getStrictSan().length() - 1)),
            FILE_FROM_SAN
                .indexOf(validations.getStrictSan()
                    .charAt(
                        validations.getStrictSan()
                            .length() - 2)));

        if (validations.isDisambigPieceRankStrict()) {
          int startRank = RANK_FROM_SAN.indexOf(validations
              .getStrictSan().charAt(1));
          for (Move move : pseudoLegals) {
            if (move != null
                && move.getPiece() == candidatePieceMoving
                && move.getTo() == end
                && getRank(move.getFrom()) == startRank) {
              matches.append(move);
            }
          }
        } else if (validations.isDisambigPieceFileStrict()) {
          int startFile = FILE_FROM_SAN.indexOf(validations
              .getStrictSan().charAt(1));
          for (Move move : pseudoLegals) {
            if (move != null
                && move.getPiece() == candidatePieceMoving
                && move.getTo() == end
                && getFile(move.getFrom()) == startFile) {
              matches.append(move);
            }
          }
        } else if (validations.isDisambigPieceRankFileStrict()) {
          int startSquare = getSquare(RANK_FROM_SAN
                  .indexOf(validations.getStrictSan().charAt(2)),
              FILE_FROM_SAN.indexOf(validations
                  .getStrictSan().charAt(1)));
          for (Move move : pseudoLegals) {
            if (move != null
                && move.getPiece() == candidatePieceMoving
                && move.getTo() == end
                && move.getFrom() == startSquare) {
              matches.append(move);
            }
          }
        } else {
          for (Move move : pseudoLegals) {
            if (move != null
                && move.getPiece() == candidatePieceMoving
                && move.getTo() == end) {
              matches.append(move);
            }
          }
        }
      }
      result = testForSanDisambiguationFromCheck(shortAlgebraic,
          matches);
    }


    if (result == null) {
      throw new IllegalArgumentException("Illegal move " + shortAlgebraic
          + "\n " + this);
    }

    result.setSan(shortAlgebraic);
    if (!move(result)) {
      throw new IllegalArgumentException("Illegal move: " + result);
    }
    return result;
  }

  @Override
  public boolean move(Move move) {
    // first make the move.
    forceMove(move);
    if (!isLegalPosition()) {
      rollback();
      return false;
    }
    return true;
  }


  @Override
  public void removeHeader(PgnHeader headerName) {
    pgnHeaderMap.remove(headerName);
  }

  @Override
  public void rollback() {
    Move move = moves.removeLast();
    decrementRepCount();

    switch (move.getMoveCharacteristic()) {
      case Move.EN_PASSANT_CHARACTERISTIC -> rollbackEpMove(move);
      case Move.SHORT_CASTLING_CHARACTERISTIC, Move.LONG_CASTLING_CHARACTERISTIC -> makeCastlingMove(move, true);
      default -> rollbackNonEpNonCastlingMove(move);
    }

    int oppositeToMove = getOppositeColor(colorToMove);

    if (move.isCapture()) {
      incrementPieceCount(colorToMove, move
          .getCaptureWithPromoteMask());
    }

    setColorToMove(oppositeToMove);
    setHalfMoveCount(halfMoveCount - 1);

    setFiftyMoveCount(move.getPrevious50MoveCount());
    setCastling(WHITE, move.getLastWhiteCastlingState());
    setCastling(BLACK, move.getLastBlackCastlingState());

    rollbackEcoHeaders();
  }

  @Override
  public void setCastling(int color, int castling) {
    this.castling[color] = castling;
  }

  @Override
  public void setColorBB(int color, long bb) {
    colorBB[color] = bb;
  }

  @Override
  public void setColorToMove(int color) {
    colorToMove = color;
  }

  @Override
  public void setEmptyBB(long emptyBB) {
    this.emptyBB = emptyBB;
  }

  @Override
  public void setEpSquare(int epSquare) {
    this.epSquare = epSquare;
  }

  @Override
  public void setFiftyMoveCount(int fiftyMoveCount) {
    this.fiftyMoveCount = fiftyMoveCount;
  }

  @Override
  public void setHalfMoveCount(int halfMoveCount) {
    this.halfMoveCount = halfMoveCount;
  }

  @Override
  public void setHeader(PgnHeader header, String value) {
    pgnHeaderMap.put(header, value);
  }

  @Override
  public void setInitialEpSquare(int initialEpSquare) {
    this.initialEpSquare = initialEpSquare;
  }

  @Override
  public void setOccupiedBB(long occupiedBB) {
    this.occupiedBB = occupiedBB;
  }

  @Override
  public void setPiece(int square, int piece) {
    board[square] = piece;
  }

  @Override
  public void setPieceBB(int color, int piece, long bb) {
    pieceBB[color][piece] = bb;
  }

  @Override
  public void setPieceCount(int color, int piece, int count) {
    pieceCounts[color][piece & NOT_PROMOTED_MASK] = count;
  }

  @Override
  public void setZobristPositionHash(long hash) {
    zobristPositionHash = hash;
  }

  @Override
  public String toFen() {
    return toFenPosition()
        + (colorToMove == WHITE ? " w" : " b")
        + " " + getFenCastle()
        + " " + getSan(epSquare)
        + " " + fiftyMoveCount
        + " " + getFullMoveCount();
  }

  @Override
  @SuppressWarnings("squid:S3776")
  public String toFenPosition() {
    StringBuilder result = new StringBuilder(77);
    for (int j = 7; j > -1; j--) {
      int consecutiveEmpty = 0;
      for (int i = 0; i < 8; i++) {
        int square = getSquare(j, i);
        int piece = getPiece(square);

        if (piece == EMPTY) {
          consecutiveEmpty++;
        } else {
          long squareBB = getBitboard(square);
          int color = (getPieceBB(WHITE, piece) & squareBB) != 0L ? WHITE
              : BLACK;
          if (consecutiveEmpty > 0) {
            result.append(consecutiveEmpty);
            consecutiveEmpty = 0;
          }
          result.append(COLOR_PIECE_TO_CHAR[color].charAt(piece));
        }
      }
      if (j != 0) {
        result.append(consecutiveEmpty != 0 ? consecutiveEmpty : "").append('/');
      } else {
        result.append(consecutiveEmpty != 0 ? consecutiveEmpty : "");
      }
    }
    return result.toString();
  }

  @Override
  public String toPgn() {
    StringBuilder builder = new StringBuilder(2500);

    // Set all of the required headers.
    for (PgnHeader requiredHeader : PgnHeader.getRequiredHeaders()) {
      String headerValue = getHeader(requiredHeader);
      if (StringUtils.isBlank(headerValue)) {
        headerValue = PgnHeader.UNKNOWN_VALUE;
        setHeader(requiredHeader, headerValue);
      }
    }

    List<PgnHeader> pgnHeaders = new ArrayList<>(pgnHeaderMap
        .keySet());
    Collections.sort(pgnHeaders);

    for (PgnHeader header : pgnHeaders) {
      if (!header.equals(PgnHeader.Variant)) {
        PgnUtils.appendHeaderLine(builder, header.name(), getHeader(header));
        builder.append("\n");
      }
    }
    builder.append('\n');

    boolean nextMoveRequiresNumber = true;
    int charsInCurrentLine = 0;

    // TO DO: add breaking up lines in comments.
    for (int i = 0; i < halfMoveCount; i++) {
      int charsBefore = builder.length();
      nextMoveRequiresNumber = PgnUtils.getMove(builder, moves
          .get(i), nextMoveRequiresNumber);
      charsInCurrentLine += builder.length() - charsBefore;

      if (charsInCurrentLine > 75) {
        charsInCurrentLine = 0;
        builder.append('\n');
      } else {
        builder.append(" ");
      }
    }

    builder.append(getResult().getDescription());
    return builder.toString();
  }

  /**
   * Returns a dump of the game class suitable for debugging. Quite a lot of
   * information is produced and its an expensive operation, use with care.
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(1000);

    result.append(getString(new String[] {"emptyBB", "occupiedBB",
            "notColorToMoveBB", "color[WHITE]", "color[BLACK]"},
        emptyBB, occupiedBB, getNotColorToMoveBB(), getColorBB(WHITE), getColorBB(BLACK))).append("\n\n");

    result.append(getString(new String[] {"[WHITE][PAWN]",
                "[WHITE][KNIGHT]", "[WHITE][BISHOP]", "[WHITE][ROOK]",
                "[WHITE][QUEEN]", "[WHITE][KING]"}, getPieceBB(WHITE, PAWN), getPieceBB(WHITE, KNIGHT),
            getPieceBB(WHITE, BISHOP), getPieceBB(WHITE, ROOK), getPieceBB(WHITE, QUEEN), getPieceBB(WHITE, KING)))
        .append("\n\n");

    result.append(getString(new String[] {"[BLACK][PAWN]",
                "[BLACK][KNIGHT]", "[BLACK][BISHOP]", "[BLACK][ROOK]",
                "[BLACK][QUEEN]", "[BLACK][KING]"}, getPieceBB(BLACK, PAWN), getPieceBB(BLACK, KNIGHT),
            getPieceBB(BLACK, BISHOP), getPieceBB(BLACK, ROOK), getPieceBB(BLACK, QUEEN), getPieceBB(BLACK, KING)))
        .append("\n\n");

    for (int i = 7; i > -1; i--) {
      for (int j = 0; j < 8; j++) {
        int square = getSquare(i, j);
        int piece = getPiece(square);
        int color = (getBitboard(square) & getColorBB(colorToMove)) != 0L ? colorToMove
            : getOppositeColor(colorToMove);

        result.append("|").append(COLOR_PIECE_TO_CHAR[color].charAt(piece));
      }
      result.append("|   ");

      switch (i) {
        case 7 -> result.append("To Move: ").append(COLOR_DESCRIPTION[colorToMove]).append(" " + "Last Move: ")
            .append(moves.getSize() == 0 ? "" : moves.getLast());
        case 6 -> result.append(getPieceCountsString());
        case 5 ->
            result.append("Moves: ").append(halfMoveCount).append(" EP: ").append(getSan(epSquare)).append(" Castle: ")
                .append(getFenCastle());
        case 4 -> result.append("FEN: ").append(toFen());
        case 3 -> result.append("State: ").append(state).append(" Variant=").append(getHeader(PgnHeader.Variant))
            .append(" Result=").append(getResult());
        case 2 -> result.append("Event: ").append(getHeader(PgnHeader.Event)).append(" Site=")
            .append(getHeader(PgnHeader.Site)).append(" Date=").append(getHeader(PgnHeader.Date));
        case 1 -> result.append("WhiteName: ").append(getHeader(PgnHeader.White)).append(" BlackName=")
            .append(getHeader(PgnHeader.Black)).append(" WhiteTime=").append(getHeader(PgnHeader.WhiteRemainingMillis))
            .append(" whiteLag=").append(getHeader(PgnHeader.WhiteLagMillis)).append(" blackRemainingTImeMillis = ")
            .append(getHeader(PgnHeader.BlackRemainingMillis)).append(" blackLag=")
            .append(getHeader(PgnHeader.BlackLagMillis));
        default ->
            result.append("initialWhiteClock: ").append(getHeader(PgnHeader.WhiteClock)).append(" initialBlackClocks=")
                .append(getHeader(PgnHeader.BlackClock));
      }

      result.append("\n");
    }

    String legalMovesString = Arrays.toString(getLegalMoves().asArray());
    result.append("\n")
        .append("\nLegals=").append(legalMovesString)
        .append("\nMovelist=").append(moves);

    List<String> squaresWithPromoteMasks = new ArrayList<>();
    for (int i = 0; i < board.length; i++) {
      if ((getPieceWithPromoteMask(i) & PROMOTED_MASK) != 0) {
        squaresWithPromoteMasks.add(getSan(i));
      }
    }
    result.append("\nSquares with promote masks: ").append(squaresWithPromoteMasks);

    return result.toString();
  }

  /**
   * Currently places captures and promotions ahead of non captures.
   */
  protected void addMove(Move move, PriorityMoveList moves) {
    if (move.isCapture() || move.isPromotion()) {
      moves.appendHighPriority(move);
    } else {
      moves.appendLowPriority(move);
    }
  }

  /**
   * Decrements the piece count for the specified piece. This method handles
   * promotion masks as well.
   *
   * @param color WHITE or BLACK.
   * @param piece The un-colored piece constant.
   */
  protected void decrementPieceCount(int color, int piece) {
    int pieceIndex = piece;
    if ((pieceIndex & PROMOTED_MASK) != 0) {
      pieceIndex &= NOT_PROMOTED_MASK;
    }
    pieceCounts[color][pieceIndex]--;
  }

  /**
   * Decrements the current positions repetition count.
   */
  protected void decrementRepCount() {
    moveRepHash[getRepHash()]--;
  }

  /**
   * Generates all of the pseudo legal bishop moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoBishopMoves(PriorityMoveList moves) {
    long fromBB = getPieceBB(colorToMove, BISHOP);

    while (fromBB != 0) {
      int fromSquare = bitscanForward(fromBB);

      long toBB = diagonalMove(fromSquare, emptyBB)
          & getNotColorToMoveBB();

      while (toBB != 0) {
        int toSquare = bitscanForward(toBB);

        int contents = getPieceWithPromoteMask(toSquare);

        addMove(new Move(fromSquare, toSquare,
            getPieceWithPromoteMask(fromSquare), colorToMove,
            contents), moves);
        toBB = bitscanClear(toBB);
      }
      fromBB = bitscanClear(fromBB);
    }
  }

  /**
   * Generates all of the pseudo legal king castling moves in the position and
   * adds them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoKingCastlingMoves(long fromBB,
                                                 PriorityMoveList moves) {
    // The king destination square isnt checked, its checked when legal
    // getMoves() are checked.

    if (colorToMove == WHITE
        && (getCastling(colorToMove) & CASTLE_SHORT) != 0
        && fromBB == E1 && getPiece(SQUARE_G1) == EMPTY
        && GameUtils.isWhitePiece(this, SQUARE_H1)
        && getPiece(SQUARE_H1) == ROOK && getPiece(SQUARE_F1) == EMPTY
        && !isInCheck(WHITE, E1) && !isInCheck(WHITE, F1)) {
      moves
          .appendLowPriority(new Move(SQUARE_E1, SQUARE_G1, KING,
              colorToMove, EMPTY,
              Move.SHORT_CASTLING_CHARACTERISTIC));
    }

    if (colorToMove == WHITE
        && (getCastling(colorToMove) & CASTLE_LONG) != 0
        && fromBB == E1 && GameUtils.isWhitePiece(this, SQUARE_A1)
        && getPiece(SQUARE_A1) == ROOK && getPiece(SQUARE_D1) == EMPTY
        && getPiece(SQUARE_C1) == EMPTY && getPiece(SQUARE_B1) == EMPTY
        && !isInCheck(WHITE, E1) && !isInCheck(WHITE, D1)) {
      moves
          .appendLowPriority(new Move(SQUARE_E1, SQUARE_C1, KING,
              colorToMove, EMPTY,
              Move.LONG_CASTLING_CHARACTERISTIC));
    }

    if (colorToMove == BLACK
        && (getCastling(colorToMove) & CASTLE_SHORT) != 0
        && fromBB == E8 && !GameUtils.isWhitePiece(this, SQUARE_H8)
        && getPiece(SQUARE_H8) == ROOK && getPiece(SQUARE_G8) == EMPTY
        && getPiece(SQUARE_F8) == EMPTY && !isInCheck(BLACK, E8)
        && !isInCheck(BLACK, F8)) {
      moves
          .appendLowPriority(new Move(SQUARE_E8, SQUARE_G8, KING,
              colorToMove, EMPTY,
              Move.SHORT_CASTLING_CHARACTERISTIC));

    }

    if (colorToMove == BLACK
        && (getCastling(colorToMove) & CASTLE_LONG) != 0
        && !GameUtils.isWhitePiece(this, SQUARE_A8)
        && getPiece(SQUARE_A8) == ROOK && fromBB == E8
        && getPiece(SQUARE_D8) == EMPTY && getPiece(SQUARE_C8) == EMPTY
        && getPiece(SQUARE_B8) == EMPTY && !isInCheck(BLACK, E8)
        && !isInCheck(BLACK, D8)) {
      moves
          .appendLowPriority(new Move(SQUARE_E8, SQUARE_C8, KING,
              colorToMove, EMPTY,
              Move.LONG_CASTLING_CHARACTERISTIC));
    }
  }

  /**
   * Generates all of the pseudo legal king moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoKingMoves(PriorityMoveList moves) {
    long fromBB = getPieceBB(colorToMove, KING);
    int fromSquare = bitscanForward(fromBB);
    long toBB = kingMove(fromSquare) & getNotColorToMoveBB();

    generatePseudoKingCastlingMoves(fromBB, moves);

    while (toBB != 0) {
      int toSquare = bitscanForward(toBB);

      int contents = getPieceWithPromoteMask(toSquare);

      addMove(new Move(fromSquare, toSquare, KING, colorToMove,
          contents), moves);
      toBB = bitscanClear(toBB);
    }
  }

  /**
   * Generates all of the pseudo legal knight moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoKnightMoves(PriorityMoveList moves) {

    long fromBB = getPieceBB(colorToMove, KNIGHT);

    while (fromBB != 0) {
      int fromSquare = bitscanForward(fromBB);

      long toBB = knightMove(fromSquare) & getNotColorToMoveBB();

      while (toBB != 0) {
        int toSquare = bitscanForward(toBB);
        int contents = getPieceWithPromoteMask(toSquare);

        addMove(new Move(fromSquare, toSquare,
            getPieceWithPromoteMask(fromSquare), colorToMove,
            contents), moves);

        toBB = bitscanClear(toBB);
      }

      fromBB = bitscanClear(fromBB);
    }
  }

  /**
   * Generates all of the pseudo legal pawn captures in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoPawnCaptures(int fromSquare, long fromBB,
                                            int oppositeColor, PriorityMoveList moves) {

    long toBB = pawnCapture(colorToMove, fromBB,
        getColorBB(oppositeColor));

    while (toBB != 0L) {
      int toSquare = bitscanForward(toBB);
      if ((toBB & RANK8_OR_RANK1) != 0L) {
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            getPieceWithPromoteMask(toSquare), KNIGHT,
            EMPTY_SQUARE, Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            getPieceWithPromoteMask(toSquare), BISHOP,
            EMPTY_SQUARE, Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            getPieceWithPromoteMask(toSquare), QUEEN, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            getPieceWithPromoteMask(toSquare), ROOK, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
      } else {
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            getPieceWithPromoteMask(toSquare)), moves);
      }
      toBB = bitscanClear(toBB);
    }
  }

  /**
   * Generates all of the pseudo legal double pawn pushes in the position and
   * adds them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoPawnDoublePush(int fromSquare, long fromBB,
                                              int epModifier, PriorityMoveList moves) {

    long toBB = pawnDoublePush(colorToMove, fromBB, emptyBB);

    while (toBB != 0) {
      int toSquare = bitscanForward(toBB);
      addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
          EMPTY, EMPTY, toSquare + epModifier,
          Move.DOUBLE_PAWN_PUSH_CHARACTERISTIC), moves);
      toBB = bitscanClear(toBB);
    }

  }

  /**
   * Generates all of the pseudo En-Passant moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoPawnEPCaptures(int fromSquare, long fromBB,
                                              int oppositeColor, PriorityMoveList moves) {
    if (epSquare != EMPTY) {

      long toBB = pawnEpCapture(colorToMove, fromBB, getPieceBB(
          oppositeColor, PAWN), getBitboard(epSquare));

      if (toBB != 0) {
        int toSquare = bitscanForward(toBB);

        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            PAWN, EMPTY, EMPTY_SQUARE,
            Move.EN_PASSANT_CHARACTERISTIC), moves);
      }
    }
  }

  /**
   * Generates all of the pseudo legal pawn moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoPawnMoves(PriorityMoveList moves) {
    long pawnsBB = getPieceBB(colorToMove, PAWN);
    int oppositeColor;
    int epModifier;

    if (colorToMove == WHITE) {
      oppositeColor = BLACK;
      epModifier = -8;
    } else {
      oppositeColor = WHITE;
      epModifier = 8;
    }

    while (pawnsBB != 0) {
      int fromSquare = bitscanForward(pawnsBB);
      long fromBB = getBitboard(fromSquare);

      generatePseudoPawnEPCaptures(fromSquare, fromBB, oppositeColor,
          moves);
      generatePseudoPawnCaptures(fromSquare, fromBB, oppositeColor, moves);
      generatePseudoPawnSinglePush(fromSquare, fromBB,
          moves);
      generatePseudoPawnDoublePush(fromSquare, fromBB,
          epModifier, moves);

      pawnsBB = bitscanClear(pawnsBB);
    }
  }

  /**
   * Generates all of the pseudo legal single push pawn moves in the position
   * and adds them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoPawnSinglePush(int fromSquare, long fromBB,
                                              PriorityMoveList moves) {

    long toBB = pawnSinglePush(colorToMove, fromBB, emptyBB);

    while (toBB != 0) {
      int toSquare = bitscanForward(toBB);

      if ((toBB & RANK8_OR_RANK1) != 0L) {
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            EMPTY, KNIGHT, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            EMPTY, BISHOP, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            EMPTY, QUEEN, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            EMPTY, ROOK, EMPTY_SQUARE,
            Move.PROMOTION_CHARACTERISTIC), moves);
      } else {
        addMove(new Move(fromSquare, toSquare, PAWN, colorToMove,
            EMPTY), moves);
      }

      toBB = bitscanClear(toBB);
    }
  }

  /**
   * Generates all of the pseudo legal queen moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoQueenMoves(PriorityMoveList moves) {
    long fromBB = getPieceBB(colorToMove, QUEEN);

    while (fromBB != 0) {
      int fromSquare = bitscanForward(fromBB);

      long toBB = (orthogonalMove(fromSquare, emptyBB
      ) | diagonalMove(fromSquare, emptyBB
      ))
          & getNotColorToMoveBB();

      while (toBB != 0) {
        int toSquare = bitscanForward(toBB);

        int contents = getPieceWithPromoteMask(toSquare);
        addMove(new Move(fromSquare, toSquare,
            getPieceWithPromoteMask(fromSquare), colorToMove,
            contents), moves);
        toBB = bitscanClear(toBB);
      }

      fromBB = bitscanClear(fromBB);
    }
  }

  /**
   * Generates all of the pseudo legal rook moves in the position and adds
   * them to the specified move list.
   *
   * @param moves A move list.
   */
  protected void generatePseudoRookMoves(PriorityMoveList moves) {
    long fromBB = getPieceBB(colorToMove, ROOK);

    while (fromBB != 0) {
      int fromSquare = bitscanForward(fromBB);

      long toBB = orthogonalMove(fromSquare, emptyBB
      )
          & getNotColorToMoveBB();

      while (toBB != 0) {
        int toSquare = bitscanForward(toBB);

        int contents = getPieceWithPromoteMask(toSquare);
        addMove(new Move(fromSquare, toSquare,
            getPieceWithPromoteMask(fromSquare), colorToMove,
            contents), moves);
        toBB = bitscanClear(toBB);
      }

      fromBB = bitscanClear(fromBB);
    }
  }

  protected String getPieceCountsString() {
    return "Piece counts [WP=" + getPieceCount(WHITE, PAWN) + " WN="
        + getPieceCount(WHITE, KNIGHT) + " WB="
        + getPieceCount(WHITE, BISHOP) + " WR="
        + getPieceCount(WHITE, ROOK) + " WQ="
        + getPieceCount(WHITE, QUEEN) + " WK="
        + getPieceCount(WHITE, KING) + "][BP="
        + getPieceCount(BLACK, PAWN) + " BN= "
        + getPieceCount(BLACK, KNIGHT) + " BB="
        + getPieceCount(BLACK, BISHOP) + " BR="
        + getPieceCount(BLACK, ROOK) + " BQ="
        + getPieceCount(BLACK, QUEEN) + " BK="
        + getPieceCount(BLACK, KING) + "]";
  }

  /**
   * Increments the piece count. This method handles incrementing pieces with
   * a promote mask.
   *
   * @param color WHITE or BLACK
   * @param piece The uncolored piece constant.
   */
  protected void incrementPieceCount(int color, int piece) {
    int pieceIndex = piece;
    if ((pieceIndex & PROMOTED_MASK) != 0) {
      pieceIndex &= NOT_PROMOTED_MASK;
    }
    pieceCounts[color][pieceIndex]++;
  }

  protected void makeCastlingMove(Move move, boolean rollback) {
    long kingFromBB;
    long kingToBB;
    long rookFromBB;
    long rookToBB;

    if (move.getColor() == WHITE) {
      kingFromBB = E1;
      if (move.getMoveCharacteristic() == Move.SHORT_CASTLING_CHARACTERISTIC) {
        kingToBB = G1;
        rookFromBB = H1;
        rookToBB = F1;
        updateZobristPOCastleKsideWhite();
      } else {
        kingToBB = C1;
        rookFromBB = A1;
        rookToBB = D1;
        updateZobristPOCastleQsideWhite();
      }
    } else {
      kingFromBB = E8;
      if (move.getMoveCharacteristic() == Move.SHORT_CASTLING_CHARACTERISTIC) {
        kingToBB = G8;
        rookFromBB = H8;
        rookToBB = F8;
        updateZobristPOCastleKsideBlack();
      } else {
        kingToBB = C8;
        rookFromBB = A8;
        rookToBB = D8;
        updateZobristPOCastleQsideBlack();
      }
    }

    if (rollback) {
      setPiece(bitscanForward(kingFromBB), KING);
      setPiece(bitscanForward(kingToBB), EMPTY);
      setPiece(bitscanForward(rookFromBB), ROOK);
      setPiece(bitscanForward(rookToBB), EMPTY);
    } else {
      setPiece(bitscanForward(kingFromBB), EMPTY);
      setPiece(bitscanForward(kingToBB), KING);
      setPiece(bitscanForward(rookFromBB), EMPTY);
      setPiece(bitscanForward(rookToBB), ROOK);
    }

    long kingFromTo = kingToBB | kingFromBB;
    long rookFromTo = rookToBB | rookFromBB;

    xor(move.getColor(), KING, kingFromTo);
    xor(move.getColor(), kingFromTo);
    setOccupiedBB(occupiedBB ^ kingFromTo);
    setEmptyBB(emptyBB ^ kingFromTo);

    xor(move.getColor(), ROOK, rookFromTo);
    xor(move.getColor(), rookFromTo);
    setOccupiedBB(occupiedBB ^ rookFromTo);
    setEmptyBB(emptyBB ^ rookFromTo);

    if (rollback) {
      setEpSquareFromPreviousMove();
    } else {
      setCastling(colorToMove, CASTLE_NONE);

      setEpSquare(EMPTY_SQUARE);
    }

  }

  protected void makeEPMove(Move move) {
    long fromBB = getBitboard(move.getFrom());
    long toBB = getBitboard(move.getTo());
    long fromToBB = fromBB ^ toBB;
    long captureBB = colorToMove == WHITE ? moveOne(SOUTH, toBB)
        : moveOne(NORTH, toBB);

    int captureSquare = bitscanForward(captureBB);

    xor(move.getColor(), move.getPiece(), fromToBB);
    xor(move.getColor(), fromToBB);
    setOccupiedBB(occupiedBB ^ fromToBB);
    setEmptyBB(emptyBB ^ fromToBB);

    xor(move.getCaptureColor(), move.getPiece(), captureBB);
    xor(move.getCaptureColor(), captureBB);
    setOccupiedBB(occupiedBB ^ captureBB);
    setEmptyBB(emptyBB ^ captureBB);

    setPiece(move.getFrom(), EMPTY);
    setPiece(move.getTo(), PAWN);
    setPiece(captureSquare, EMPTY);

    updateZobristEP(move, captureSquare);
    setEpSquare(EMPTY_SQUARE);
  }

  protected void makeNonEpNonCastlingMove(Move move) {
    long fromBB = getBitboard(move.getFrom());
    long toBB = getBitboard(move.getTo());
    long fromToBB = fromBB ^ toBB;
    int oppositeColor = getOppositeColor(move.getColor());

    prepareNonEpNonCastlingMove(move, oppositeColor, fromBB, toBB, fromToBB);

    if (move.isPromotion()) {
      xor(move.getColor(), move.getPiece(), fromBB);

      xor(move.getColor(), move.getPiecePromotedTo() & NOT_PROMOTED_MASK,
          toBB);

      setPiece(move.getTo(), move.getPiecePromotedTo() | PROMOTED_MASK);
      setPiece(move.getFrom(), EMPTY);

      // capture is handled in forceMove.
      // promoted piece never has a promote mask only captures do.
      // Promotes do not effect drop pieces.
      decrementPieceCount(colorToMove, PAWN);
      incrementPieceCount(colorToMove, move.getPiecePromotedTo());
    } else {
      xor(move.getColor(), move.getPiece(), fromToBB);

      setPiece(move.getTo(), move.getPieceWithPromoteMask());
      setPiece(move.getFrom(), EMPTY);
    }

    updateCastlingRightsForNonEpNonCastlingMove(move);

    setEpSquare(move.getEpSquare());
  }


  protected void rollbackEcoHeaders() {
    if (isSettingEcoHeaders()) {
      removeHeader(PgnHeader.ECO);
      removeHeader(PgnHeader.Opening);
    }
  }

  protected void rollbackEpMove(Move move) {
    int oppositeColor = getOppositeColor(colorToMove);
    long fromBB = getBitboard(move.getFrom());
    long toBB = getBitboard(move.getTo());
    long fromToBB = fromBB ^ toBB;

    long captureBB = oppositeColor == WHITE ? moveOne(SOUTH, toBB)
        : moveOne(NORTH, toBB);
    int captureSquare = bitscanForward(captureBB);

    xor(oppositeColor, move.getPiece(), fromToBB);
    xor(oppositeColor, fromToBB);
    setOccupiedBB(occupiedBB ^ fromToBB);
    setEmptyBB(emptyBB ^ fromToBB);
    setEmptyBB(emptyBB ^ captureBB);
    setOccupiedBB(occupiedBB ^ captureBB);

    xor(colorToMove, move.getCapture(), captureBB);
    xor(colorToMove, captureBB);

    setPiece(move.getTo(), EMPTY);
    setPiece(move.getFrom(), PAWN);
    setPiece(captureSquare, PAWN);

    updateZobristEP(move, captureSquare);
    setEpSquareFromPreviousMove();
  }

  protected void rollbackNonEpNonCastlingMove(Move move) {
    int oppositeColor = getOppositeColor(move.getColor());
    long fromBB = getBitboard(move.getFrom());
    long toBB = getBitboard(move.getTo());
    long fromToBB = fromBB ^ toBB;

    prepareNonEpNonCastlingMove(move, oppositeColor, fromBB, toBB, fromToBB);

    if (move.isPromotion()) {
      xor(move.getColor(), move.getPiece(), fromBB);
      xor(move.getColor(), move.getPiecePromotedTo() & NOT_PROMOTED_MASK,
          toBB);

      // capture is handled in rollback.
      // promoted pieces never have a promote mask.
      // Promotions do not change drop counts.
      incrementPieceCount(move.getColor(), PAWN);
      decrementPieceCount(move.getColor(), move.getPiecePromotedTo());
    } else {
      xor(move.getColor(), move.getPiece(), fromToBB);
    }

    setPiece(move.getFrom(), move.getPieceWithPromoteMask());
    setPiece(move.getTo(), move.getCaptureWithPromoteMask());

    setEpSquareFromPreviousMove();
  }

  private void prepareNonEpNonCastlingMove(Move move, int oppositeColor, long fromBB, long toBB, long fromToBB) {
    xor(move.getColor(), fromToBB);

    if (move.isCapture()) {
      setOccupiedBB(occupiedBB ^ fromBB);
      setEmptyBB(emptyBB ^ fromBB);

      xor(oppositeColor, move.getCapture(), toBB);
      xor(oppositeColor, toBB);

      updateZobristPOCapture(move, oppositeColor);

    } else {
      setOccupiedBB(occupiedBB ^ fromToBB);
      setEmptyBB(emptyBB ^ fromToBB);

      updateZobristPONoCapture(move);
    }
  }

  protected void setEpSquareFromPreviousMove() {
    if (moves.getSize() == 0) {
      setEpSquare(initialEpSquare);
    } else {
      setEpSquare(moves.getLast().getEpSquare());
    }
  }

  /**
   * Should be called before the move is made to update the san field.
   */
  @SuppressWarnings("squid:S3776")
  protected void setSan(Move move) {
    if (isSettingMoveSan() && move.getSan() == null) {
      // TO DO: possible add + or ++ for check/checkmate
      String shortAlgebraic;

      if (move.isCastleShort()) {
        shortAlgebraic = "O-O";
      } else if (move.isCastleLong()) {
        shortAlgebraic = "O-O-O";
      } else if (move.getPiece() == PAWN
          && (move.getMoveCharacteristic() & Move.EN_PASSANT_CHARACTERISTIC) != 0) // e.p.
      // is
      // optional but
      // the x is
      // required.
      // (pawn eps
      // are never
      // unambiguous)
      {
        shortAlgebraic = SanUtils.squareToFileSan(move.getFrom()) + "x"
            + SanUtils.squareToSan(move.getTo());
      } else if (move.getPiece() == PAWN && move.isCapture()) // Possible
      // formats ed
      // ed5 edQ
      // (pawn captures
      // can be
      // ambiguous)
      {
        int oppositeColorToMove = getOppositeColor(colorToMove);
        long fromBB = getPieceBB(colorToMove, PAWN);
        while (fromBB != 0) {
          int fromSquare = bitscanForward(fromBB);

          long allPawnCapturesBB = pawnCapture(colorToMove,
              getBitboard(fromSquare),
              getColorBB(oppositeColorToMove));

          while (allPawnCapturesBB != 0) {
            allPawnCapturesBB = bitscanClear(allPawnCapturesBB);
          }
          fromBB = bitscanClear(fromBB);
        }

        shortAlgebraic = SanUtils.squareToFileSan(move.getFrom())
            + "x"
            + SanUtils.squareToSan(move.getTo())
            + (move.isPromotion() ? "="
            + PIECE_TO_SAN.charAt(move.getPiecePromotedTo()) : "");
      } else if (move.getPiece() == PAWN) // e4 (pawn moves are never ambiguous)
      {
        shortAlgebraic = SanUtils.squareToSan(move.getTo())
            + (move.isPromotion() ? "="
            + PIECE_TO_SAN
            .charAt(move.getPiecePromotedTo()) : "");
      } else {
        long fromBB = getPieceBB(colorToMove, move.getPiece());
        long toBB = getBitboard(move.getTo());

        int sameFilesFound = 0;
        int sameRanksFound = 0;
        int matchesFound = 0;

        if (move.getPiece() != KING) {
          while (fromBB != 0) {
            int fromSquare = bitscanForward(fromBB);
            long resultBB = switch (move.getPiece()) {
              case KNIGHT -> knightMove(fromSquare) & toBB;
              case BISHOP -> diagonalMove(fromSquare, emptyBB)
                  & getNotColorToMoveBB() & toBB;
              case ROOK -> orthogonalMove(fromSquare, emptyBB)
                  & getNotColorToMoveBB() & toBB;
              case QUEEN -> orthogonalMove(fromSquare, emptyBB)
                  & getNotColorToMoveBB()
                  & toBB
                  | diagonalMove(fromSquare, emptyBB)
                  & getNotColorToMoveBB() & toBB;
              default -> throw new IllegalStateException("Unexpected value: " + move.getPiece());
            };

            if (resultBB != 0) {
              int toSquare = bitscanForward(resultBB);

              if (toSquare == move.getTo()) {
                matchesFound++;
                if (getFile(fromSquare) == getFile(move.getFrom())) {
                  sameFilesFound++;
                }
                if (getRank(fromSquare) == getRank(move.getFrom())) {
                  sameRanksFound++;
                }
              }
            }
            fromBB = bitscanClear(fromBB);
          }
        }

        shortAlgebraic = String.valueOf(PIECE_TO_SAN.charAt(move.getPiece()));
        boolean hasHandledAmbiguity = false;
        if (sameRanksFound > 1) {
          shortAlgebraic += SanUtils.squareToFileSan(move.getFrom());
          hasHandledAmbiguity = true;
        }
        if (sameFilesFound > 1) {
          shortAlgebraic += SanUtils.squareToRankSan(move.getFrom());
          hasHandledAmbiguity = true;
        }
        if (matchesFound > 1 && !hasHandledAmbiguity) {
          shortAlgebraic += SanUtils.squareToFileSan(move.getFrom());
        }

        shortAlgebraic += (move.isCapture() ? "x" : "")
            + SanUtils.squareToSan(move.getTo());
      }

      move.setSan(shortAlgebraic);
    }
  }

  protected void setState(int state) {
    this.state = state;
  }

  /**
   * If the match list contains no ambiguity after taking disambiguity by
   * check into consideration the move is returned. Otherwise an
   * IllegalArgumentException is raised
   */
  @SuppressWarnings("squid:S3776")
  protected Move testForSanDisambiguationFromCheck(String shortAlgebraic,
                                                   MoveList matches) {
    Move result = null;
    if (matches.getSize() == 0) {
      throw new IllegalArgumentException("Invalid move " + shortAlgebraic
          + "\n" + this);
    } else if (matches.getSize() == 1) {
      result = matches.get(0);
    } else {
      // now do legality checking on whats left.
      int kingSquare = bitscanForward(getPieceBB(colorToMove, KING));
      int cachedColorToMove = colorToMove;
      int matchesCount = 0;

      if (kingSquare != EMPTY_SQUARE) { // Now trim illegals
        for (int i = 0; i < matches.getSize(); i++) {
          Move current = matches.get(i);

          // Needed for FR.
          if (current.isCastleLong() || current.isCastleShort()) {
            continue;
          }
          synchronized (this) {
            try {
              forceMove(current);
              if (current.getPiece() == KING) {
                int newKingCoordinates = bitscanForward(getPieceBB(
                    cachedColorToMove, KING));
                if (!isInCheck(cachedColorToMove, getBitboard(newKingCoordinates))) {
                  result = current;
                  matchesCount++;
                }
              } else {
                if (!isInCheck(cachedColorToMove, getBitboard(kingSquare))) {
                  result = current;
                  matchesCount++;
                }
              }
              rollback();
            } catch (IllegalArgumentException ie) { // NOPMD
              // ignored?
            }
          }
        }
      }

      if (matchesCount == 0) {
        throw new IllegalArgumentException("Invalid move "
            + shortAlgebraic + "\n" + this);
      } else if (matchesCount > 1) {
        throw new IllegalArgumentException("Ambiguous move "
            + shortAlgebraic + "\n" + this);
      }
    }
    return result;
  }

  /**
   * Provided so it can be easily implemented for Fischer Random type of
   * games.
   */
  @SuppressWarnings("squid:S3776")
  protected void updateCastlingRightsForNonEpNonCastlingMove(Move move) {
    if (move.getPiece() == KING) {
      setCastling(colorToMove, CASTLE_NONE);
    } else {
      if (move.getPiece() == ROOK && move.getFrom() == SQUARE_A1
          && colorToMove == WHITE || move.getCapture() == ROOK
          && move.getTo() == SQUARE_A1 && colorToMove == BLACK) {
        setCastling(WHITE, getCastling(WHITE) & CASTLE_SHORT);
      } else if (move.getPiece() == ROOK && move.getFrom() == SQUARE_H1
          && colorToMove == WHITE || move.getCapture() == ROOK
          && move.getTo() == SQUARE_H1 && colorToMove == BLACK) {
        setCastling(WHITE, getCastling(WHITE) & CASTLE_LONG);
      } else if (move.getPiece() == ROOK && move.getFrom() == SQUARE_A8
          && colorToMove == BLACK || move.getCapture() == ROOK
          && move.getTo() == SQUARE_A8 && colorToMove == WHITE) {
        setCastling(BLACK, getCastling(BLACK) & CASTLE_SHORT);
      } else if (move.getPiece() == ROOK && move.getFrom() == SQUARE_H8
          && colorToMove == BLACK || move.getCapture() == ROOK
          && move.getTo() == SQUARE_H8 && colorToMove == WHITE) {
        setCastling(BLACK, getCastling(BLACK) & CASTLE_LONG);
      }
    }
  }

  protected void updateZobristEP(Move move, int captureSquare) {
    zobristPositionHash ^= zobrist(move.getColor(), PAWN, move.getFrom())
        ^ zobrist(move.getColor(), PAWN, move.getTo())
        ^ zobrist(move.getCaptureColor(), PAWN, captureSquare);
  }

  protected void updateZobristPOCapture(Move move, int oppositeColor) {
    zobristPositionHash ^= zobrist(move.getColor(),
        move.isPromotion() ? move.getPiecePromotedTo()
            & NOT_PROMOTED_MASK : move.getPiece()
            & NOT_PROMOTED_MASK, move.getTo())
        ^ zobrist(oppositeColor, move.getCapture() & NOT_PROMOTED_MASK,
        move.getTo())
        ^ zobrist(move.getColor(), move.getPiece() & NOT_PROMOTED_MASK,
        move.getFrom());
  }

  protected void updateZobristPOCastleKsideBlack() {
    zobristPositionHash ^= zobrist(BLACK, KING, SQUARE_E8)
        ^ zobrist(BLACK, KING, SQUARE_G8)
        ^ zobrist(BLACK, ROOK, SQUARE_H8)
        ^ zobrist(BLACK, ROOK, SQUARE_F8);
  }

  protected void updateZobristPOCastleKsideWhite() {
    zobristPositionHash ^= zobrist(WHITE, KING, SQUARE_E1)
        ^ zobrist(WHITE, KING, SQUARE_G1)
        ^ zobrist(WHITE, ROOK, SQUARE_H1)
        ^ zobrist(WHITE, ROOK, SQUARE_F1);
  }

  protected void updateZobristPOCastleQsideBlack() {
    zobristPositionHash ^= zobrist(BLACK, KING, SQUARE_E8)
        ^ zobrist(BLACK, KING, SQUARE_C8)
        ^ zobrist(BLACK, ROOK, SQUARE_A8)
        ^ zobrist(BLACK, ROOK, SQUARE_D8);
  }

  protected void updateZobristPOCastleQsideWhite() {
    zobristPositionHash ^= zobrist(WHITE, KING, SQUARE_E1)
        ^ zobrist(WHITE, KING, SQUARE_C1)
        ^ zobrist(WHITE, ROOK, SQUARE_A1)
        ^ zobrist(WHITE, ROOK, SQUARE_D1);
  }

  protected void updateZobristPONoCapture(Move move) {
    zobristPositionHash ^= zobrist(move.getColor(),
        move.isPromotion() ? move.getPiecePromotedTo()
            & NOT_PROMOTED_MASK : move.getPiece()
            & NOT_PROMOTED_MASK, move.getTo())
        ^ zobrist(move.getColor(), move.getPiece() & NOT_PROMOTED_MASK,
        move.getFrom());
  }

  /**
   * Exclusive bitwise ors the games piece bitboard with the specified
   * bitboard.
   *
   * @param color WHITE or BLACK
   * @param piece The non-colored piece type.
   * @param bb    The bitmap to XOR.
   */
  protected void xor(int color, int piece, long bb) {
    pieceBB[color][piece] ^= bb;
  }

  /**
   * Exclusive bitwise ors the games color bitboard with the specified
   * bitboard.
   *
   * @param color WHITE or BLACK
   * @param bb    The bitmap to XOR.
   */
  protected void xor(int color, long bb) {
    colorBB[color] ^= bb;
  }

}

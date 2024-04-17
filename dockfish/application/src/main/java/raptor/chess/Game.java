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

import raptor.chess.pgn.PgnHeader;

/**
 * A game class which uses bitboards,Zobrist hash keys, and a reptition hash
 * table.. Games may contain PgnHeaders to store useful data about the game
 * (e.g. whites name, whites remaining time, etc).
 * <p>
 * Games also contain a state. The state is a bitmask of the various *_STATE
 * constants in this class. You can add to or remove a games state flag.
 * <p>
 * PgnHeaders can effect the way a game is displayed. You can set various
 * headers to effect the clocks, if white pieces are on top, the result, etc.
 */
public interface Game extends GameConstants {
  /**
   * The active state bitmask. Set when a game is actively being played.
   */
  int ACTIVE_STATE = 1;

  /**
   * The inactive state bitmask. Set when a game is no longer being played.
   */
  int INACTIVE_STATE = ACTIVE_STATE << 1;

  /**
   * The updating eco headers bitmask. Set when the game is updating eco
   * header information. Should be turned off for engines.
   */
  int UPDATING_ECO_HEADERS_STATE = ACTIVE_STATE << 10;

  /**
   * The setup bitmask. Set when the game is update san on the moves made.
   * Should be turned off for engines.
   */
  int UPDATING_SAN_STATE = ACTIVE_STATE << 11;

  /**
   * Adds the state flag to the games state.
   */
  void addState(int state);

  /**
   * Returns true if a king of each color is on the board.
   */
  boolean areBothKingsOnBoard();

  /**
   * Makes a move with out any legality checking.
   */
  void forceMove(Move move);

  /**
   * Returns the castling constant for the specified color.
   *
   * @param color WHITE or BLACK
   * @return The castling constant.
   */
  int getCastling(int color);

  /**
   * Returns a bitboard with 1s in the squares of the pieces of the specified
   * color.
   *
   * @param color WHITE or BLACK
   */
  long getColorBB(int color);

  /**
   * Returns the color to move, WHITE or BLACK.
   */
  int getColorToMove();

  /**
   * If the last move was a double pawn push, this method returns the square
   * otherwise returns EMPTY.
   */
  int getEpSquare();

  /**
   * Returns the castle part of the fen string.
   */
  String getFenCastle();

  /**
   * Returns the full move count. The next move will have this number.
   */
  int getFullMoveCount();

  /**
   * Returns the games half moves count.
   */
  int getHalfMoveCount();

  /**
   * Returns the value of the PGN header for this game. Returns null if a
   * value is not set.
   */
  String getHeader(PgnHeader header);

  /**
   * Returns a move list of all legal moves in the games current position.
   */
  PriorityMoveList getLegalMoves();

  /**
   * Returns a move list of the moves that have been made in the position.
   */
  MoveList getMoveList();

  /**
   * Returns a bitboard with 1s on all of the squares that do not contain the
   * color to moves pieces.
   */
  long getNotColorToMoveBB();

  /**
   * Returns a bitboard with 1s on all squares that are occupied in the
   * position.
   */
  long getOccupiedBB();

  /**
   * Returns the piece at the specified square with its promotion mask
   * removed.
   */
  int getPiece(int square);

  /**
   * Returns a bitboard with 1s on all squares containing the piece.
   *
   * @param color WHITE or BLACK
   * @param piece The un-colored piece constant.
   */
  long getPieceBB(int color, int piece);

  /**
   * Returns the number of pieces on the board.
   *
   * @param color WHITE or BLACK
   * @param piece The un-colored piece constant.
   */
  int getPieceCount(int color, int piece);

  /**
   * Returns the piece at the specified square with its promotion mask.
   */
  int getPieceWithPromoteMask(int square);

  /**
   * Returns a move list containing all pseudo legal moves.
   */
  PriorityMoveList getPseudoLegalMoves();

  /**
   * Returns a hash that can be used to reference moveRepHash. The hash is
   * created using the zobrist position hash.
   *
   * @return The hash.
   */
  int getRepHash();

  /**
   * Returns the games result constant.
   */
  Result getResult();

  /**
   * Increments the move hash repetition count for the current position.
   */
  void incrementRepCount();

  /**
   * Returns true if the specified color is in check in the specified
   * position.
   *
   * @param color WHITE or BLACK
   * @return true if in check, otherwise false.
   */
  boolean isInCheck(int color);

  /**
   * Returns true if the specified square would be in check if it contained a
   * king.
   *
   * @param color   WHITE or BLACK.
   * @param pieceBB A bitboard representing the square to to check.
   * @return true if in check, false otherwise.
   */
  boolean isInCheck(int color, long pieceBB);

  /**
   * Returns true if one of the state flags is in the specified state.
   */
  boolean isInState(int state);

  /**
   * This is one of the methods that needs to be overridden in subclasses.
   *
   * @return If the position is legal.
   */
  boolean isLegalPosition();

  /**
   * Returns true if the eco game headers are updated on each move.
   */
  boolean isSettingEcoHeaders();

  /**
   * Returns true if SAN, short algebraic notation, is being set on all mvoes
   * generated by this class. This is an expensive operation and should be
   * turned off for engines.
   */
  boolean isSettingMoveSan();

  /**
   * If it is currently white's move in this Game.
   */
  boolean isWhitesMove();

  /**
   * Makes a move using the start/end square.
   *
   * @param startSquare The start square.
   * @param endSquare   The end square.
   * @return The move made.
   */
  Move makeMove(int startSquare, int endSquare);

  /**
   * Makes a move using the start/end square and the specified promotion
   * piece.
   *
   * @param startSquare  The start square.
   * @param endSquare    The end square.
   * @param promotePiece The non colored piece constant representing the promoted
   *                     piece.
   * @return The move made.
   */
  Move makeMove(int startSquare, int endSquare, int promotePiece);

  /**
   * Makes a move given SAN, short algebraic notation.
   *
   * @param shortAlgebraic The move in SAN.
   * @return THe move made.
   */
  Move makeSanMove(String shortAlgebraic);

  /**
   * Makes a move. If the move is illegal false is returned.
   *
   * @param move The move to make.
   * @return true if the move was legal, false otherwise.
   */
  boolean move(Move move);

  /**
   * Removes the specified pgn header from this game.
   */
  void removeHeader(PgnHeader headerName);

  /**
   * Rolls back the last move made.
   */
  void rollback();

  /**
   * Sets the castling state for the specified color.
   *
   * @param color    WHITE or BLACK
   * @param castling The new castling state constant.
   */
  void setCastling(int color, int castling);

  /**
   * Sets the color bitboard for a specified color. This bitboard contains 1s
   * on all of the squares that contain the specified colors pieces.
   *
   * @param color WHITE or BLACK
   * @param bb    The new bitboard.
   */
  void setColorBB(int color, long bb);

  /**
   * Sets the color to move. WHITE or BLACK.
   */
  void setColorToMove(int color);

  /**
   * Sets the empty bitboard. This is the bitboard with 1s in all of the empty
   * squares.
   */
  void setEmptyBB(long emptyBB);

  /**
   * Sets the EP square. This is the move of the last double pawn push. Can be
   * EMPTY.
   */
  void setEpSquare(int epSquare);

  /**
   * Returns the current 50 move count. This is the count since the last
   * non-reversible move. This count is used to determine the 50 move draw
   * rule.
   */
  void setFiftyMoveCount(int fiftyMoveCount);

  /**
   * Sets the number of half moves played.
   */
  void setHalfMoveCount(int halfMoveCount);

  /**
   * Sets the specified pgn header to the specified value for this game.
   */
  void setHeader(PgnHeader header, String value);

  /**
   * Returns the initial ep square used to create this game. This is useful
   * when rolling back the first move. The ep square is the square of the last
   * double pawn push. Can be EMPTY.
   */
  void setInitialEpSquare(int initialEpSquare);

  /**
   * squares set to 1.
   *
   * @param occupiedBB The occupied bitboard.
   */
  void setOccupiedBB(long occupiedBB);

  /**
   * Sets a piece at a specified square. This only updated the board object
   * and does NOT update the bitmaps.
   *
   * @param square The square.
   * @param piece  The un-colored piece constant.
   */
  void setPiece(int square, int piece);

  /**
   * Sets the piece bitboard to the specified bitboard.
   *
   * @param color WHITE or BLACK
   * @param piece The un-colored piece constant.
   * @param bb    The new bitboard.
   */
  void setPieceBB(int color, int piece, long bb);

  /**
   * Sets the piece count for the specified piece. This method handles
   * un-masking the piece so its ok to pass in promotion masked pieces.
   *
   * @param color WHITE or BLACK
   * @param piece The un-colored piece constant.
   * @param count The new piece count.
   */
  void setPieceCount(int color, int piece, int count);

  /**
   * Returns the games Zobrist position hash. The position hash is the Zobrist
   * WITHOUT state info such as color to move, castling, ep info.
   */
  void setZobristPositionHash(long hash);

  /**
   * Returns the FEN, Forsyth Edwards notation, of the game.
   *
   * <pre>
   *  rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
   * </pre>
   *
   * @return The games position in FEN.
   */
  String toFen();

  /**
   * Returns only the position part of the fen.
   *
   * <pre>
   * rnbqkbnr / pppppppp / 8 / 8 / 8 / 8 / PPPPPPPP / RNBQKBNR
   * </pre>
   *
   * @return The board with pieces part only of the FEN message.
   */
  String toFenPosition();

  /**
   * Returns a string containing PGN (Portable Game Notation) for the
   * specified game.
   */
  String toPgn();

}

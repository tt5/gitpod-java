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

@SuppressWarnings("squid:S1214")
public interface GameConstants {
  int MOVE_REP_CACHE_SIZE = 1 << 12;
  int MOVE_REP_CACHE_SIZE_MINUS_1 = MOVE_REP_CACHE_SIZE - 1;

  int MAX_HALF_MOVES_IN_GAME = 600;
  int MAX_LEGAL_MOVES = 600;

  // bitboard coordinate constants
  long A1 = 1L;
  long B1 = A1 << 1;
  long C1 = A1 << 2;
  long D1 = A1 << 3;
  long E1 = A1 << 4;
  long F1 = A1 << 5;
  long G1 = A1 << 6;
  long H1 = A1 << 7;

  long A2 = A1 << 8;
  long B2 = A1 << 9;
  long C2 = A1 << 10;
  long D2 = A1 << 11;
  long E2 = A1 << 12;
  long F2 = A1 << 13;
  long G2 = A1 << 14;
  long H2 = A1 << 15;

  long A3 = A1 << 16;
  long B3 = A1 << 17;
  long C3 = A1 << 18;
  long D3 = A1 << 19;
  long E3 = A1 << 20;
  long F3 = A1 << 21;
  long G3 = A1 << 22;
  long H3 = A1 << 23;

  long A4 = A1 << 24;
  long B4 = A1 << 25;
  long C4 = A1 << 26;
  long D4 = A1 << 27;
  long E4 = A1 << 28;
  long F4 = A1 << 29;
  long G4 = A1 << 30;
  long H4 = A1 << 31;

  long A5 = A1 << 32;
  long B5 = A1 << 33;
  long C5 = A1 << 34;
  long D5 = A1 << 35;
  long E5 = A1 << 36;
  long F5 = A1 << 37;
  long G5 = A1 << 38;
  long H5 = A1 << 39;

  long A6 = A1 << 40;
  long B6 = A1 << 41;
  long C6 = A1 << 42;
  long D6 = A1 << 43;
  long E6 = A1 << 44;
  long F6 = A1 << 45;
  long G6 = A1 << 46;
  long H6 = A1 << 47;

  long A7 = A1 << 48;
  long B7 = A1 << 49;
  long C7 = A1 << 50;
  long D7 = A1 << 51;
  long E7 = A1 << 52;
  long F7 = A1 << 53;
  long G7 = A1 << 54;
  long H7 = A1 << 55;

  long A8 = A1 << 56;
  long B8 = A1 << 57;
  long C8 = A1 << 58;
  long D8 = A1 << 59;
  long E8 = A1 << 60;
  long F8 = A1 << 61;
  long G8 = A1 << 62;
  long H8 = A1 << 63;

  int SQUARE_A1 = 0;
  int SQUARE_B1 = 1;
  int SQUARE_C1 = 2;
  int SQUARE_D1 = 3;
  int SQUARE_E1 = 4;
  int SQUARE_F1 = 5;
  int SQUARE_G1 = 6;
  int SQUARE_H1 = 7;

  int SQUARE_A2 = 8;
  int SQUARE_B2 = 9;
  int SQUARE_C2 = 10;
  int SQUARE_D2 = 11;
  int SQUARE_E2 = 12;
  int SQUARE_F2 = 13;
  int SQUARE_G2 = 14;
  int SQUARE_H2 = 15;

  int SQUARE_A3 = 16;
  int SQUARE_B3 = 17;
  int SQUARE_C3 = 18;
  int SQUARE_D3 = 19;
  int SQUARE_E3 = 20;
  int SQUARE_F3 = 21;
  int SQUARE_G3 = 22;
  int SQUARE_H3 = 23;

  int SQUARE_A4 = 24;
  int SQUARE_B4 = 25;
  int SQUARE_C4 = 26;
  int SQUARE_D4 = 27;
  int SQUARE_E4 = 28;
  int SQUARE_F4 = 29;
  int SQUARE_G4 = 30;
  int SQUARE_H4 = 31;

  int SQUARE_A5 = 32;
  int SQUARE_B5 = 33;
  int SQUARE_C5 = 34;
  int SQUARE_D5 = 35;
  int SQUARE_E5 = 36;
  int SQUARE_F5 = 37;
  int SQUARE_G5 = 38;
  int SQUARE_H5 = 39;

  int SQUARE_A6 = 40;
  int SQUARE_B6 = 41;
  int SQUARE_C6 = 42;
  int SQUARE_D6 = 43;
  int SQUARE_E6 = 44;
  int SQUARE_F6 = 45;
  int SQUARE_G6 = 46;
  int SQUARE_H6 = 47;

  int SQUARE_A7 = 48;
  int SQUARE_B7 = 49;
  int SQUARE_C7 = 50;
  int SQUARE_D7 = 51;
  int SQUARE_E7 = 52;
  int SQUARE_F7 = 53;
  int SQUARE_G7 = 54;
  int SQUARE_H7 = 55;

  int SQUARE_A8 = 56;
  int SQUARE_B8 = 57;
  int SQUARE_C8 = 58;
  int SQUARE_D8 = 59;
  int SQUARE_E8 = 60;
  int SQUARE_F8 = 61;
  int SQUARE_G8 = 62;
  int SQUARE_H8 = 63;

  // Castle state constants.
  int CASTLE_NONE = 0;
  int CASTLE_SHORT = 1;
  int CASTLE_LONG = 2;
  int CASTLE_BOTH = CASTLE_SHORT | CASTLE_LONG;

  // Direction constants.
  int NORTH = 0;
  int SOUTH = 2;
  int EAST = 4;
  int WEST = 8;
  int NORTHEAST = 16;
  int NORTHWEST = 32;
  int SOUTHEAST = 64;
  int SOUTHWEST = 128;

  // Rank bitmaps
  long RANK1 = A1 | B1 | C1 | D1 | E1 | F1 | G1 | H1;
  long RANK2 = A2 | B2 | C2 | D2 | E2 | F2 | G2 | H2;
  long RANK3 = A3 | B3 | C3 | D3 | E3 | F3 | G3 | H3;
  long RANK4 = A4 | B4 | C4 | D4 | E4 | F4 | G4 | H4;
  long RANK5 = A5 | B5 | C5 | D5 | E5 | F5 | G5 | H5;
  long RANK6 = A6 | B6 | C6 | D6 | E6 | F6 | G6 | H6;
  long RANK7 = A7 | B7 | C7 | D7 | E7 | F7 | G7 | H7;
  long RANK8 = A8 | B8 | C8 | D8 | E8 | F8 | G8 | H8;

  long RANK8_OR_RANK1 = RANK1 | RANK8;

  long NOT_RANK1 = ~RANK1;
  long NOT_RANK2 = ~RANK2;
  long NOT_RANK3 = ~RANK3;
  long NOT_RANK4 = ~RANK4;
  long NOT_RANK5 = ~RANK5;
  long NOT_RANK6 = ~RANK6;
  long NOT_RANK7 = ~RANK7;
  long NOT_RANK8 = ~RANK8;

  // File bitmaps
  long AFILE = A1 | A2 | A3 | A4 | A5 | A6 | A7 | A8;
  long BFILE = B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8;
  long CFILE = C1 | C2 | C3 | C4 | C5 | C6 | C7 | C8;
  long DFILE = D1 | D2 | D3 | D4 | D5 | D6 | D7 | D8;
  long EFILE = E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8;
  long FFILE = F1 | F2 | F3 | F4 | F5 | F6 | F7 | F8;
  long GFILE = G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8;
  long HFILE = H1 | H2 | H3 | H4 | H5 | H6 | H7 | H8;

  long NOT_AFILE = ~AFILE;
  long NOT_BFILE = ~BFILE;
  long NOT_CFILE = ~CFILE;
  long NOT_DFILE = ~DFILE;
  long NOT_EFILE = ~EFILE;
  long NOT_FFILE = ~FFILE;
  long NOT_GFILE = ~GFILE;
  long NOT_HFILE = ~HFILE;

  // Piece constants.
  int EMPTY = 0;
  int PAWN = 1;
  int BISHOP = 2;
  int KNIGHT = 3;
  int ROOK = 4;
  int QUEEN = 5;
  int KING = 6;

  int PROMOTED_MASK = 8;
  int NOT_PROMOTED_MASK = 7;

  // Color constants.
  int WHITE = 0;
  int BLACK = 1;

  int EMPTY_SQUARE = 64;

  String SQUARE_TO_FILE_SAN = "abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghx";

  String SQUARE_TO_RANK_SAN = "1111111122222222333333334444444455555555666666667777777788888888x";

  String RANK_FROM_SAN = "12345678";

  String FILE_FROM_SAN = "abcdefgh";

  String[] COLOR_DESCRIPTION = {"White", "Black"};

  String PIECE_TO_SAN = " PBNRQK";

  String[] COLOR_PIECE_TO_CHAR = {"*PBNRQK", "*pbnrqk"};

  String STARTING_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  String SPACES = "                                                                    ";
  long[] SQUARE_TO_COORDINATE = {A1, B1, C1, D1, E1, F1, G1,
      H1, A2, B2, C2, D2, E2, F2, G2, H2, A3, B3, C3, D3, E3, F3, G3, H3,
      A4, B4, C4, D4, E4, F4, G4, H4, A5, B5, C5, D5, E5, F5, G5, H5, A6,
      B6, C6, D6, E6, F6, G6, H6, A7, B7, C7, D7, E7, F7, G7, H7, A8, B8,
      C8, D8, E8, F8, G8, H8, 0L};

}

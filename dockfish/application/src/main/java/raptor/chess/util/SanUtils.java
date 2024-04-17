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

import org.apache.commons.lang3.StringUtils;
import raptor.chess.GameConstants;

/**
 * A class containing validation methods for Short Algebraic Notation (SAN).
 */
public final class SanUtils {
  public static final String FILES = "abcdefgh";

  public static final String PIECES = "BKNQR";

  public static final String PROMOTIONS = "BNQR";

  public static final String RANKS = "12345678";

  private SanUtils() {
  }

  public static SanValidations getValidations(String unstrictShortAlg) {
    SanValidations result = new SanValidations();

    result.strictSan = toStrictSan(unstrictShortAlg);
    result.isCastleKSideStrict = isValidKSideCastle(result.strictSan);
    result.isCastleQSideStrict = isValidQSideCastle(result.strictSan);
    result.isDisambigPieceFileStrict = isValidDisambigFileStrict(result.strictSan);
    result.isDisambigPieceRankFileStrict = isValidDisambigRankFileStrict(result.strictSan);
    result.isDisambigPieceRankStrict = isValidDisambigRankStrict(result.strictSan);
    result.isAmbigPxPromotionStrict = isValidAmbigPxPromotionStrict(result.strictSan);
    result.isEpOrAmbigPxStrict = isValidEpOrAmbigPCaptureStrict(result.strictSan);
    result.isPMoveStrict = isValidPMoveStrict(result.strictSan);
    result.isPPromotionStrict = isValidPPromotionStrict(result.strictSan);
    result.isPxPPromotionStrict = isValidPxPromotionStrict(result.strictSan);
    result.isPxStrict = isValidPxStrict(result.strictSan);
    result.isUnambigPieceStrict = isValidUnambigPieceStrict(result.strictSan);
    result.isValidStrict = result.isEpOrAmbigPxStrict
        || result.isAmbigPxPromotionStrict
        || result.isUnambigPieceStrict
        || result.isDisambigPieceRankStrict
        || result.isDisambigPieceFileStrict
        || result.isDisambigPieceRankFileStrict || result.isPxStrict
        || result.isPMoveStrict || result.isPxPPromotionStrict
        || result.isPPromotionStrict || result.isCastleKSideStrict
        || result.isCastleQSideStrict;

    if (result.isValidStrict) {
      result.isPawnMove = result.isPxStrict
          || result.isPxPPromotionStrict || result.isPMoveStrict
          || result.isPPromotionStrict || result.isEpOrAmbigPxStrict
          || result.isAmbigPxPromotionStrict;
    }

    if (result.isPawnMove) {
      result.isPromotion = result.isPxPPromotionStrict
          || result.isPPromotionStrict
          || result.isAmbigPxPromotionStrict;
    }

    return result;
  }

  /**
   * VALID_EP_OR_AMBIG_P_CAPTURE_PROMOTION_REGEX = "[a-h][a-h][1-8][BNQR]"
   */
  public static boolean isValidAmbigPxPromotionStrict(String san) {
    if (san.length() == 4) {
      return FILES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && RANKS.indexOf(san.charAt(2)) != -1
          && PROMOTIONS.indexOf(san.charAt(3)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_DISAMBIG_FILE_REGEX = "[BKNQR][a-h][a-h][1-8]"
   */
  public static boolean isValidDisambigFileStrict(String san) {
    if (san.length() == 4) {
      return PIECES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && FILES.indexOf(san.charAt(2)) != -1
          && RANKS.indexOf(san.charAt(3)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_DISAMBIG_RANK_FILE_REGEX = "[BKNQR][a-h][1-8][a-h][1-8]";
   */
  public static boolean isValidDisambigRankFileStrict(String san) {
    if (san.length() == 5) {
      return PIECES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && RANKS.indexOf(san.charAt(2)) != -1
          && FILES.indexOf(san.charAt(3)) != -1
          && RANKS.indexOf(san.charAt(4)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_DISAMBIG_RANK_REGEX = "[BKNQR][1-8][a-h][1-8]"
   */
  public static boolean isValidDisambigRankStrict(String san) {
    if (san.length() == 4) {
      return PIECES.indexOf(san.charAt(0)) != -1
          && RANKS.indexOf(san.charAt(1)) != -1
          && FILES.indexOf(san.charAt(2)) != -1
          && RANKS.indexOf(san.charAt(3)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_EP_OR_AMBIG_P_CAPTURE_REGEX = "[a-h][a-h][1-8]"
   */
  public static boolean isValidEpOrAmbigPCaptureStrict(String san) {
    if (san.length() == 3) {
      return FILES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && RANKS.indexOf(san.charAt(2)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_CASTLE_KSIDE_REGEX = [O][-][O]
   */
  public static boolean isValidKSideCastle(String san) {
    return "O-O".equals(san);
  }

  /**
   * VALID_P_MOVE_REGEX = "[a-h][1-8]"
   */
  public static boolean isValidPMoveStrict(String san) {
    if (san.length() == 2) {
      return FILES.indexOf(san.charAt(0)) != -1
          && RANKS.indexOf(san.charAt(1)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_P_PROMOTION_REGEX = [a-h][1-8][BNQR]
   */
  public static boolean isValidPPromotionStrict(String san) {
    if (san.length() == 3) {
      return FILES.indexOf(san.charAt(0)) != -1
          && RANKS.indexOf(san.charAt(1)) != -1
          && PROMOTIONS.indexOf(san.charAt(2)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_PXP_PROMOTION_REGEX = [a-h][a-h][BNQR]
   */
  public static boolean isValidPxPromotionStrict(String san) {
    if (san.length() == 3) {
      return FILES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && PROMOTIONS.indexOf(san.charAt(2)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_P_X_REGEX = "[a-h][a-h]"
   */
  public static boolean isValidPxStrict(String san) {
    if (san.length() == 2) {
      return FILES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1;
    } else {
      return false;
    }
  }

  /**
   * VALID_CASTLE_QSIDE_REGEX = [O][-][O][-][O]
   */
  public static boolean isValidQSideCastle(String san) {
    return "O-O-O".equals(san);
  }

  /**
   * VALID_UNAMBIG_REGEX = "[BKNQR][a-h][1-8]"
   */
  public static boolean isValidUnambigPieceStrict(String san) {
    if (san.length() == 3) {
      return PIECES.indexOf(san.charAt(0)) != -1
          && FILES.indexOf(san.charAt(1)) != -1
          && RANKS.indexOf(san.charAt(2)) != -1;
    } else {
      return false;
    }
  }

  /**
   * Returns the short alg piece representing shortAlgebraic. If isWhite is
   * set the white piece is returned, otherwise the black piece is returned.
   * Null is returned if the piece isnt valid.
   */
  public static int sanToPiece(char shortAlgebraicPiece) {
    switch (shortAlgebraicPiece) {
      case 'P':
      case 'p':
        return GameConstants.PAWN;
      case 'N':
      case 'n':
        return GameConstants.KNIGHT;
      case 'B':
      case 'b':
        return GameConstants.BISHOP;
      case 'R':
      case 'r':
        return GameConstants.ROOK;
      case 'Q':
      case 'q':
        return GameConstants.QUEEN;
      case 'K':
      case 'k':
        return GameConstants.KING;
      default:
        return -1;
    }

  }

  public static char squareToFileSan(int square) {
    return GameConstants.SQUARE_TO_FILE_SAN.charAt(square);

  }

  public static char squareToRankSan(int square) {
    return GameConstants.SQUARE_TO_RANK_SAN.charAt(square);
  }

  public static String squareToSan(int square) {
    return String.valueOf(squareToFileSan(square)) + squareToRankSan(square);
  }

  /**
   * Removes all of the following characters (+,-,=,x,:,"e.p."). Replaces
   * (ACDEFGH) with (acdefgh).
   *
   */
  public static String toStrictSan(String unstrictSan) {
    String result = StringUtils.replaceChars(unstrictSan, ",+#=x:X", null);
    result = StringUtils.remove(result, "e.p.");
    result = StringUtils.replaceChars(result, "ACDEFGH", "acdefgh");

    return result;
  }

  public static class SanValidations {
    boolean isAmbigPxPromotionStrict;

    boolean isCastleKSideStrict;

    boolean isCastleQSideStrict;

    boolean isDisambigPieceFileStrict;

    boolean isDisambigPieceRankFileStrict;

    boolean isDisambigPieceRankStrict;

    boolean isEpOrAmbigPxStrict;

    boolean isPawnMove;

    boolean isPMoveStrict;

    boolean isPPromotionStrict;

    boolean isPromotion;

    boolean isPxPPromotionStrict;

    boolean isPxStrict;

    boolean isUnambigPieceStrict;

    boolean isValidStrict;

    String strictSan;

    public String getStrictSan() {
      return strictSan;
    }

    public boolean isAmbigPxPromotionStrict() {
      return isAmbigPxPromotionStrict;
    }

    public boolean isCastleKSideStrict() {
      return isCastleKSideStrict;
    }

    public boolean isCastleQSideStrict() {
      return isCastleQSideStrict;
    }

    public boolean isDisambigPieceFileStrict() {
      return isDisambigPieceFileStrict;
    }

    public boolean isDisambigPieceRankFileStrict() {
      return isDisambigPieceRankFileStrict;
    }

    public boolean isDisambigPieceRankStrict() {
      return isDisambigPieceRankStrict;
    }

    public boolean isEpOrAmbigPxStrict() {
      return isEpOrAmbigPxStrict;
    }

    public boolean isPawnMove() {
      return isPawnMove;
    }

    public boolean isPromotion() {
      return isPromotion;
    }

    public boolean isPxPPromotionStrict() {
      return isPxPPromotionStrict;
    }

    public boolean isPxStrict() {
      return isPxStrict;
    }

    public boolean isValidStrict() {
      return isValidStrict;
    }

  }
}

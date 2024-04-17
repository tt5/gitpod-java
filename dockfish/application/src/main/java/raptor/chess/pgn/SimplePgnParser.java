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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import raptor.chess.Result;
import raptor.chess.util.RaptorStringUtils;
import raptor.util.RaptorStringTokenizer;

/**
 * The SimplePgnParser.
 */
public class SimplePgnParser extends AbstractPgnParser {

  /**
   * OLD SLOW REGEX private static final String STARTS_WITH_MOVE_NUMBER_REGEX
   * = "((\\d*)(([.])|([.][.][.])))(.*)";
   * <p>
   * private static final String STARTS_WITH_GAME_END_REGEX =
   * "(([1][-][0])|([0][-][1])|([1][////][2][-][1][////][2])|([*]))(.*)";
   * <p>
   * private static final String STARTS_WITH_NAG_REGEX =
   * "([$]\\d\\d?\\d?)(.*)";
   */

  public static final String GAME_START_WORD = "[Event";

  protected int columnNumber;

  protected String currentLine;

  protected int lineNumber;

  protected RaptorStringTokenizer lineTokenizer;


  public SimplePgnParser(String pgn) {
    super();
    if (pgn == null || pgn.isEmpty()) {
      throw new IllegalArgumentException("pgn cant be null or empty.");
    }
    lineTokenizer = new RaptorStringTokenizer(pgn, "\n\r", true);
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  @SuppressWarnings({"PMD.InefficientEmptyStringCheck", "squid:S3776"})
  public void parse() {

    boolean isSearchingForHeaders = true;

    readNextLine();
    outer:
    while (currentLine != null) {

      if (currentLine.trim().isEmpty()) {
        readNextLine();
        continue;
      }

      while (isSearchingForHeaders && !currentLine.isEmpty()) {
        int openBracketIndex = currentLine.indexOf('[');
        int closeBracketIndex = currentLine.indexOf(']');

        if (openBracketIndex != -1 && closeBracketIndex != -1) {
          String suggestedHeader = currentLine.substring(openBracketIndex, closeBracketIndex + 1);

          List<String> headers = parseForHeader(suggestedHeader);

          if (headers.isEmpty()) {
            isSearchingForHeaders = false;
            break;
          }  // We encountered something that was'nt a header.
          // Assume it is part of the game.
          else {
            if (openBracketIndex != 0) {
              fireUnknown(currentLine.substring(0, openBracketIndex));
              columnNumber += openBracketIndex;
            }

            if ("Event".equals(headers.get(0))) {
              fireGameStart();
            }

            fireHeader(headers.get(0), headers.get(1));
            currentLine = currentLine.substring(closeBracketIndex + 1);

          }

        } else // We encountered something that was'nt a header.
        // Assume it is part of the game.
        {
          isSearchingForHeaders = false;
          // CE make sure game is started, in case we initialize from FEN
          fireMoveNumber(0);
          break;
        }

      }

      RaptorStringTokenizer wordTok = new RaptorStringTokenizer(currentLine, " ", true);
      String nextWord = wordTok.nextToken();

      while (true) {
        if (nextWord == null || StringUtils.isBlank(nextWord)) {
          // We have reached the end of the line, read in the next
          // line.
          readNextLine();
          break;
        } else if (")".equals(nextWord)) {
          nextWord = wordTok.nextToken();
        } else if (nextWord.startsWith(")")) {
          nextWord = nextWord.substring(1);
        } else if (nextWord.startsWith("(")) {
          // Determine if its a comment or a sub-line.
          // ( comments can not span multiple lines in PGN.
          int closingParenIndex = nextWord.indexOf(')');
          if (closingParenIndex == -1) {
            if (nextWord.length() > 1) {
              if (nextWord.charAt(1) == '{' || Character.isDigit(nextWord.charAt(1))) {
                // Definitely a subline.
                nextWord = nextWord.substring(1);
              } else {
                // Definitely a comment.
                wordTok.changeDelimiters(")");
                String annotation = nextWord.substring(1) + " " + wordTok.nextToken();
                wordTok.changeDelimiters(" ");
                fireAnnotation(annotation);
                nextWord = wordTok.nextToken();
              }
            } else {
              String nextNextWord = wordTok.peek();
              if (nextNextWord.charAt(0) == '{' || Character.isDigit(nextNextWord.charAt(0))) {
                // Definitely a subline.
                nextWord = wordTok.nextToken();
              } else {
                wordTok.changeDelimiters(")");
                String annotation = nextWord.substring(1) + " " + wordTok.nextToken();
                wordTok.changeDelimiters(" ");
                fireAnnotation(annotation);
                nextWord = wordTok.nextToken();
              }
            }
          } else {
            // Definitely a comment.
            if (nextWord.length() > 2) {
              fireAnnotation(nextWord.substring(1));
            }
          }

        } else if (nextWord.startsWith("{")) {
          int closingBrace = nextWord.indexOf('}');
          if (closingBrace != -1) {
            String annotation = nextWord.substring(1, closingBrace);
            fireAnnotation(annotation);

            if (nextWord.length() > closingBrace + 1) {
              nextWord = nextWord.substring(closingBrace + 1);
            } else {
              nextWord = wordTok.nextToken();
            }
          } else {
            if (wordTok.indexInWhatsLeft('}') != -1) {
              wordTok.changeDelimiters("}");
              String annotation = nextWord.substring(1) + " " + wordTok.nextToken();
              wordTok.changeDelimiters(" ");
              fireAnnotation(annotation);
              nextWord = wordTok.nextToken();
            } else {
              StringBuilder annotation = new StringBuilder()
                  .append(nextWord.substring(1)).append(' ').append(wordTok.getWhatsLeft());

              int closingBraceIndex = -1;
              do {
                readNextLine();

                if (currentLine != null) {
                  closingBraceIndex = currentLine.indexOf('}');
                  if (closingBraceIndex == -1) {
                    annotation.append(' ').append(currentLine);
                  } else {
                    annotation.append(' ').append(currentLine, 0, closingBraceIndex);
                  }
                }
              } while (currentLine != null && closingBraceIndex == -1);

              if (currentLine == null) {
                fireUnknown(annotation.toString());
                // This will actually break all the way out of
                // the
                // main loop since currentLine is null.
                break;
              } else {
                fireAnnotation(annotation.toString());
                wordTok = new RaptorStringTokenizer(currentLine.substring(closingBraceIndex + 1), " \t",
                    true);
                nextWord = wordTok.nextToken();
              }
            }
          }
        } else {
          List<String> moveNumberSplit = splitOutGameMoveNumber(nextWord);
          if (!moveNumberSplit.isEmpty()) {
            int moveNumber = Integer.parseInt(moveNumberSplit.get(0));
            fireMoveNumber(moveNumber);

            if (moveNumberSplit.size() == 1) {
              nextWord = wordTok.nextToken();
            } else {
              nextWord = moveNumberSplit.get(1);
            }
          } else {
            List<String> gameEndSplit = splitOutGameEnd(nextWord);
            if (!gameEndSplit.isEmpty()) {
              fireGameEnd(Result.get(gameEndSplit.get(0)));
              if (isParseCancelled()) {
                break outer;
              }

              String whatsLeft = wordTok.getWhatsLeft();

              if (gameEndSplit.size() != 1) {
                whatsLeft = gameEndSplit.get(1) + whatsLeft;
              }
              isSearchingForHeaders = true;
              currentLine = whatsLeft;
              break;
            } else if (GAME_START_WORD.equals(nextWord)) {
              isSearchingForHeaders = true;
              currentLine = nextWord + " " + wordTok.getWhatsLeft();
              break;
            } else if (nextWord.endsWith(")")) {
              nextWord = nextWord.substring(0, nextWord.length() - 1);
              fireMoveWord(nextWord);
              nextWord = wordTok.nextToken();
            } else {
              fireMoveWord(nextWord);
              nextWord = wordTok.nextToken();
            }
          }
        }
      }
    }

  }

  /**
   * Returns null if string is not a header, otherwise returns a String[2]
   * where index 0 is header name, and index1 is the value. This method is
   * fast and does'nt use REGEX for parsing.
   */
  public List<String> parseForHeader(String string) {
    if (string.length() >= 7 && string.startsWith("[") && string.endsWith("]")) {
      int quoteIndex = string.indexOf('\"');
      if (quoteIndex != -1) {
        int quote2Index = string.lastIndexOf('\"');

        if (quote2Index > quoteIndex && RaptorStringUtils.count(string, '\"') == 2
            && quote2Index + 2 == string.length()) {
          int spaceIndex = string.indexOf(' ');
          if (spaceIndex < quoteIndex && spaceIndex >= 2) {
            return List.of(string.substring(1, spaceIndex),
                string.substring(quoteIndex + 1, quote2Index));
          }
        }
      }
    }
    return List.of();
  }

  protected void readNextLine() {
    currentLine = lineTokenizer.nextToken();
    lineNumber++;
  }

  /**
   * If word token is not a game end indicator null is returned. Else if word
   * token is just a game end indicator then a String[0] is returned with the
   * word token as the game end indicator. Else a String[2] is returned where
   * index 0 is the result indicator, and 1 is the rest of the string.
   */
  private List<String> splitOutGameEnd(String wordToken) {
    if (wordToken.startsWith(Result.BLACK_WON.getDescription())) {
      return splitOutStartString(wordToken, Result.BLACK_WON.getDescription());
    } else if (wordToken.startsWith(Result.WHITE_WON.getDescription())) {
      return splitOutStartString(wordToken, Result.WHITE_WON.getDescription());
    } else if (wordToken.startsWith(Result.DRAW.getDescription())) {
      return splitOutStartString(wordToken, Result.DRAW.getDescription());
    } else if (wordToken.startsWith(Result.ON_GOING.getDescription())) {
      return splitOutStartString(wordToken, Result.ON_GOING.getDescription());
    } else {
      return List.of();
    }
  }

  /**
   * Move numbers are in the format 1. or 1... If wordToken isn't a move
   * number or doesn't start with a move number then null is returned.
   * Otherwise if it contains a move number and nothing else then a String[1]
   * is returned with the move number stripped of all '.'s. Otherwise a
   * String[2] is returned where index 0 is the move number with '.' stripped
   * and index 1 is the rest of the word token.
   */
  @SuppressWarnings("squid:S3776")
  private List<String> splitOutGameMoveNumber(String wordToken) {
    if (Character.isDigit(wordToken.charAt(0))) {
      // Remove the . or ...

      int firstDotIndex = wordToken.indexOf('.');
      if (firstDotIndex != -1) {
        int firstThreeDotIndex = wordToken.indexOf("...");
        if (firstThreeDotIndex != -1) {
          if (wordToken.length() > firstThreeDotIndex + 3) {
            return List.of(wordToken.substring(0, firstThreeDotIndex),
                wordToken.substring(firstThreeDotIndex + 3));
          } else {
            return List.of(wordToken.substring(0, firstThreeDotIndex));
          }
        } else {
          if (wordToken.length() > firstDotIndex + 1) {
            return List.of(wordToken.substring(0, firstDotIndex),
                wordToken.substring(firstDotIndex + 1));
          } else {
            return List.of(wordToken.substring(0, firstDotIndex));
          }
        }
      }
    }
    return List.of();
  }


  private List<String> splitOutStartString(String wordToken, String startString) {
    if (wordToken.length() == startString.length()) {
      return List.of(startString);
    }
    return List.of(startString, wordToken.substring(startString.length()));
  }
}

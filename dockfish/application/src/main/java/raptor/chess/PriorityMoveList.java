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

public final class PriorityMoveList implements GameConstants {
  private final Move[] highPriorityMoves = new Move[MAX_LEGAL_MOVES];
  int highPrioritySize;

  private final Move[] lowPriorityMoves = new Move[MAX_LEGAL_MOVES];
  int lowPrioritySize;

  public void appendHighPriority(Move move) {
    highPriorityMoves[highPrioritySize++] = move;
  }

  public void appendLowPriority(Move move) {
    lowPriorityMoves[lowPrioritySize++] = move;
  }

  public Move[] asArray() {
    Move[] result = new Move[lowPrioritySize + highPrioritySize];

    if (result.length > 0) {
      System.arraycopy(lowPriorityMoves, 0, result, 0, lowPrioritySize);
      System.arraycopy(highPriorityMoves, 0, result, lowPrioritySize,
          highPrioritySize);
    }

    return result;
  }

  public Move getHighPriority(int index) {
    return highPriorityMoves[index];
  }

  public int getHighPrioritySize() {
    return highPrioritySize;
  }

  public Move getLowPriority(int index) {
    return lowPriorityMoves[index];
  }

  public int getLowPrioritySize() {
    return lowPrioritySize;
  }

  public int getSize() {
    return highPrioritySize + lowPrioritySize;
  }

  public void removeHighPriority(int index) {
    int size = highPrioritySize - 1;
    System.arraycopy(highPriorityMoves, index + 1, highPriorityMoves, index, size - index);
    highPrioritySize--;
  }

  public void removeLowPriority(int index) {
    int size = lowPrioritySize - 1;
    System.arraycopy(lowPriorityMoves, index + 1, lowPriorityMoves, index, size - index);
    lowPrioritySize--;
  }

}

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

import java.util.ArrayList;
import java.util.List;
import raptor.chess.Result;

public abstract class AbstractPgnParser implements PgnParser {

  protected boolean parseCancelled;
  private final List<PgnParserListener> listeners = new ArrayList<>(3);

  public boolean isParseCancelled() {
    return parseCancelled;
  }

  @Override
  public void addPgnParserListener(PgnParserListener listener) {
    listeners.add(listener);
  }

  protected void fireAnnotation(String annotation) {
    for (PgnParserListener listener : listeners) {
      listener.onAnnotation(this, annotation);
    }
  }

  protected void fireGameEnd(Result result) {
    for (PgnParserListener listener : listeners) {
      parseCancelled = listener.onGameEnd(this, result);
      if (parseCancelled) {
        break;
      }
    }
  }

  protected void fireGameStart() {
    for (PgnParserListener listener : listeners) {
      listener.onGameStart(this);
    }
  }

  protected void fireHeader(String headerName, String headerValue) {
    for (PgnParserListener listener : listeners) {
      listener.onHeader(this, headerName, headerValue);
    }
  }

  protected void fireMoveNumber(int moveNumber) {
    for (PgnParserListener listener : listeners) {
      listener.onMoveNumber(this, moveNumber);
    }
  }

  protected void fireMoveWord(String moveWord) {
    for (PgnParserListener listener : listeners) {
      listener.onMoveWord(this, moveWord);
    }
  }

  protected void fireUnknown(String unknown) {
    for (PgnParserListener listener : listeners) {
      listener.onUnknown(this, unknown);
    }
  }
}

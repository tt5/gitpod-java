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
package raptor.engine.uci.info;

import raptor.engine.uci.UCIInfo;

public class ScoreInfo implements UCIInfo {
	protected int valueInCentipawns;
	protected int mateInMoves;
	protected boolean lowerBoundScore;
	protected boolean upperBoundScore;

	public int getMateInMoves() {
		return mateInMoves;
	}

	public int getValueInCentipawns() {
		return valueInCentipawns;
	}

	public boolean isLowerBoundScore() {
		return lowerBoundScore;
	}

	public boolean isUpperBoundScore() {
		return upperBoundScore;
	}

	public void setLowerBoundScore(boolean isLowerBoundScore) {
		this.lowerBoundScore = isLowerBoundScore;
	}

	public void setMateInMoves(int mateInMoves) {
		this.mateInMoves = mateInMoves;
	}

	public void setUpperBoundScore(boolean isUpperBoundScore) {
		this.upperBoundScore = isUpperBoundScore;
	}

	public void setValueInCentipawns(int valueInCentipawns) {
		this.valueInCentipawns = valueInCentipawns;
	}

  @Override
  public String toString() {
    return "ScoreInfo [valueInCentipawns=" + valueInCentipawns + ", mateInMoves=" + mateInMoves + ", isLowerBoundScore="
        + lowerBoundScore + ", isUpperBoundScore=" + upperBoundScore + "]";
  }
}

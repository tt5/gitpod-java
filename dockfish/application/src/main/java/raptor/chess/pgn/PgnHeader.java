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

import java.util.Arrays;
import java.util.List;

/**
 * An enum containing frequently used PgnHeaders.
 */
@SuppressWarnings("squid:S115")
public enum PgnHeader {

  /**
   * A required PGN header. The name of the tournament or match.
   */
  Event,
  /**
   * A required PGN header. The location of the event City, Region COUNTRY
   * where COUNTRY is the three letter international olympic committee code.
   */
  Site,
  /**
   * A required PGN header. YYYY.MM.DD format. ?? are used for unknown values.
   */
  Date,
  /**
   * A required PGN header. The playing round ordinal of the game within the
   * event. ? is used for unknown values.
   */
  Round,
  /**
   * A required PGN header. Whites name in last,first name format.
   */
  White,
  /**
   * A required PGN header. Blacks name in last,first name format.
   */
  Black,
  /**
   * A required PGN header. The result of the game. Possible values
   * "1-0","0-1","1/2-1/2","*"(ongoing).
   */
  Result,
  /**
   * A required PGN header. Represents the date the game occured. Can be ? for
   * unknown. Should be in yyyy.mm.dd format (e.g. 2009.10.07).
   */
  EventDate,
  /**
   * The FEN , Forsyth Edwards Notation, of the initial starting position.
   */
  FEN,
  /**
   * A detailed description of the result (e.g. Black wins by white
   * discconection).
   */
  ResultDescription,
  /**
   * Whites rating or elo.
   */
  WhiteElo,
  /**
   * Blacks rating or elo.
   */
  BlackElo,
  /**
   * The number of half moves made in the game.
   */
  PlyCount,
  /**
   * The games ECO code.
   */
  ECO,
  /**
   * A description of the opening (e.g. Sicilian dragon,Yugoslav
   * attack,7...O-O)
   */
  Opening,
  /**
   * Denotes the variant of the game being played.
   * classic,suicide,crazyhouse,losers,atomic,etc.
   */
  Variant,
  /**
   * The terminiation reason (e.g. White had a heart attack).
   */
  Termination,
  /**
   * The time the game took place.
   */
  Time,
  /**
   * The time control of the game in MM+S format, e.g. (60+0). MM = minutes +0
   * == increment in seconds. TimeControl
   */
  TimeControl,
  /**
   * White CLocks starting time in 0:01:00.000 format.
   */
  WhiteClock,
  /**
   * Black CLock's initial time in 0:01:00.000 format.
   */
  BlackClock,
  /**
   * The annotator of the game.
   */
  Annotator,
  /**
   * Total white lag in milliseconds.
   */
  WhiteLagMillis,
  /**
   * Total black lag in milliseconds.
   */
  BlackLagMillis,
  /**
   * The amount of time remaining on whites clock in milliseconds.
   */
  WhiteRemainingMillis,
  /**
   * THe amount of time remaining on blacks clock in milliseconds.
   */
  BlackRemainingMillis,
  /**
   * A header that is set to 1 if white should be placed on the top when
   * viewing the game, 0 if white should be placed on bottom.
   */
  WhiteOnTop;

  public static final transient String UNKNOWN_VALUE = "?";

  private static final transient PgnHeader[] REQUIRED_HEADERS = {Event, Site, Date, Round, White, Black, Result};

  public static List<PgnHeader> getRequiredHeaders() {
    return Arrays.asList(REQUIRED_HEADERS);
  }
}

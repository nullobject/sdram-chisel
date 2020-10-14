/*
 *    __   __     __  __     __         __
 *   /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *   \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *    \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *     \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *    ______     ______       __     ______     ______     ______
 *   /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *   \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *    \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *     \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 *  https://joshbassett.info
 *  https://twitter.com/nullobject
 *  https://github.com/nullobject
 *
 *  Copyright (c) 2020 Josh Bassett
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

import chisel3._
import chisel3.util._

/** Utility functions. */
object Util {
  /**
   * Detects edges of a signal.
   *
   * @param s The signal used to detect edges.
   */
  def edge(s: Bool): Bool = s ^ RegNext(s)

  /**
   * Detects rising edges of a signal.
   *
   * @param s The signal used to detect edges.
   */
  def rising(s: Bool): Bool = s && !RegNext(s)

  /**
   * Detects falling edges of a signal.
   *
   * @param s The signal used to detect edges.
   */
  def falling(s: Bool): Bool = !s && RegNext(s)

  /**
   * Latches a signal.
   *
   * Once latched, the output signal will remain asserted until the latch is reset.
   *
   * @param s The signal value.
   */
  def latch(s: Bool): Bool = {
    val enable = RegInit(false.B)
    when(s) { enable := true.B }
    s || enable
  }

  /** Toggles a bit. */
  def toggle: Bool = {
    val a = RegInit(false.B)
    a := !a
    a
  }

  /**
   * Generates a sync pulse for each rising edge of the target clock.
   *
   * @param targetClock The target clock domain.
   */
  def sync(targetClock: Clock): Bool = {
    val s = withClock(targetClock) { toggle }
    edge(s)
  }
}

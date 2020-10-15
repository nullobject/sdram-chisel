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
import chiseltest._
import chiseltest.experimental.UncheckedClockPoke._
import org.scalatest._

class DataFreezerTest extends FlatSpec with ChiselScalatestTester with Matchers {
  it should "freeze the wait signal" in {
    test(new DataFreezer(addrWidth = 8, dataWidth = 8)) { dut =>
      // Read
      dut.io.in.rd.poke(true.B)

      // Wait
      dut.io.out.ack.poke(true.B)
      dut.io.in.ack.expect(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)
      dut.io.in.ack.expect(true.B)
      dut.clock.step()
      dut.io.in.ack.expect(true.B)
      dut.io.targetClock.high()
      dut.io.targetClock.low()
      dut.clock.step()
      dut.io.in.ack.expect(false.B)
    }
  }

  it should "freeze the valid signal" in {
    test(new DataFreezer(addrWidth = 8, dataWidth = 8)) { dut =>
      // Read
      dut.io.in.rd.poke(true.B)

      // Valid
      dut.io.out.valid.poke(true.B)
      dut.io.in.valid.expect(true.B)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)
      dut.io.in.valid.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.expect(true.B)
      dut.io.targetClock.high()
      dut.io.targetClock.low()
      dut.clock.step()
      dut.io.in.valid.expect(false.B)
    }
  }
}

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
  it should "freeze a read request" in {
    test(new DataFreezer(hold = 2, addrWidth = 8, dataWidth = 8)) { dut =>
      // Read
      dut.io.out.rd.expect(false.B)
      dut.io.in.rd.poke(true.B)
      dut.io.out.rd.expect(true.B)

      // Wait
      dut.io.out.waitReq.poke(true.B)
      dut.io.in.waitReq.expect(true.B)
      dut.io.out.waitReq.poke(false.B)
      dut.io.in.waitReq.expect(false.B)

      // Valid
      dut.io.fastClock.low()
      dut.io.in.valid.expect(false.B)
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(1.U)
      dut.clock.step()
      dut.io.in.valid.expect(true.B)
      dut.io.in.dout.expect(1.U)
      dut.io.fastClock.high()
      dut.io.fastClock.low()
      dut.io.out.dout.poke(0.U)
      dut.io.in.valid.expect(true.B)
      dut.io.in.dout.expect(1.U)
      dut.io.fastClock.high()
      dut.io.fastClock.low()
      dut.clock.step()
      dut.io.in.valid.expect(false.B)
      dut.io.in.dout.expect(0.U)
    }
  }
}

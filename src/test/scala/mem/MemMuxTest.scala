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

package mem

import chisel3._
import chiseltest._
import org.scalatest._

class MemMuxTest extends FlatSpec with ChiselScalatestTester with Matchers {
  private val config = MemMuxConfig(
    addrWidth = 16,
    dataWidth = 8,
    slots = Seq(
      SlotConfig(addrWidth = 8, dataWidth = 8),
      SlotConfig(addrWidth = 8, dataWidth = 8, offset = 0x1000),
      SlotConfig(addrWidth = 9, dataWidth = 4, offset = 0x2000)
    )
  )

  it should "prioritize slot requests" in {
    test(new MemMux(config)) { dut =>
      // Slot requests
      dut.io.in(0).wr.poke(true.B)
      dut.io.in(0).addr.poke(0.U)
      dut.io.in(0).din.poke(1.U)
      dut.io.in(1).rd.poke(true.B)
      dut.io.in(1).addr.poke(1.U)
      dut.io.in(2).rd.poke(true.B)
      dut.io.in(2).addr.poke(2.U)
      dut.clock.step()

      // Line fill (slot 0)
      dut.io.in(0).wr.poke(false.B)
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(0x0000.U)
      dut.clock.step()
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(4.U)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)
      dut.io.in(0).valid.expect(false.B)

      // Line fill (slot 1)
      dut.io.in(1).rd.poke(false.B)
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(0x1001.U)
      dut.clock.step()
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(2.U)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)
      dut.io.in(1).valid.expect(true.B)
      dut.io.in(1).dout.expect(2.U)

      // Line fill (slot 2)
      dut.io.in(2).rd.poke(false.B)
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(0x2001.U)
      dut.clock.step()
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(0x43.U)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)
      dut.io.in(2).valid.expect(true.B)
      dut.io.in(2).dout.expect(3.U)
      dut.clock.step()

      // Done
      dut.io.out.rd.expect(false.B)
    }
  }
}

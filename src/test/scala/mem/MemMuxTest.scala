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

trait MemMuxHelpers {
  protected val config = MemMuxConfig(
    addrWidth = 16,
    dataWidth = 8,
    slots = Seq(
      SlotConfig(addrWidth = 8, dataWidth = 8, depth = 4),
      SlotConfig(addrWidth = 8, dataWidth = 8, depth = 4, offset = 0x1000),
    )
  )
}

class MemMuxTest extends FlatSpec with ChiselScalatestTester with Matchers with MemMuxHelpers {
  it should "prioritize slot requests" in {
    test(new MemMux(config)) { dut =>
      // Wait for cache init
      dut.clock.step(4)

      // Slot requests
      dut.io.in(0).wr.poke(true.B)
      dut.io.in(0).addr.poke(0.U)
      dut.io.in(0).din.poke(1.U)
      dut.io.in(0).ack.expect(true.B)
      dut.io.in(1).rd.poke(true.B)
      dut.io.in(1).addr.poke(1.U)
      dut.io.in(1).ack.expect(true.B)
      dut.clock.step()
      dut.io.in(0).wr.poke(false.B)
      dut.io.in(1).rd.poke(false.B)

      // Line fill 0
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(0x0000.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)

      // Line fill 1
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(0x1001.U)
      dut.io.out.ack.poke(true.B)

      // Done 0
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(1.U)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)

      // Valid 0
      dut.io.out.valid.poke(false.B)
      dut.io.in(0).valid.expect(false.B) // read-only
      dut.io.out.rd.expect(false.B) // all done

      // Done 1
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(2.U)
      dut.clock.step()

      // Valid 1
      dut.io.out.valid.poke(false.B)
      dut.io.in(1).valid.expect(true.B)
      dut.io.in(1).dout.expect(2.U)
    }
  }
}

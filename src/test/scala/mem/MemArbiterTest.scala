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

class MemArbiterTest extends FlatSpec with ChiselScalatestTester with Matchers {
  private def mkMemArbiter = new MemArbiter(2, 8, 8)

  it should "select the highest priority input port" in {
    test(mkMemArbiter) { dut =>
      dut.io.in(0).rd.poke(true.B)
      dut.io.in(0).addr.poke(1.U)
      dut.io.in(1).rd.poke(true.B)
      dut.io.in(1).addr.poke(2.U)

      // Read
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(1.U)
      dut.clock.step()
      dut.io.in(0).rd.poke(false.B)
      dut.io.out.rd.expect(true.B)

      // Valid
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(1.U)
      dut.io.in(0).valid.expect(true.B)
      dut.io.in(1).valid.expect(false.B)
      dut.io.in(0).dout.expect(1.U)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)

      // Read
      dut.io.out.rd.expect(true.B)
      dut.io.out.addr.expect(2.U)
      dut.clock.step()
      dut.io.in(1).rd.poke(false.B)
      dut.io.out.rd.expect(false.B)

      // Valid
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(2.U)
      dut.io.in(0).valid.expect(false.B)
      dut.io.in(1).valid.expect(true.B)
      dut.io.in(1).dout.expect(2.U)
      dut.clock.step()
      dut.io.out.valid.poke(false.B)

      // Done
      dut.io.out.rd.expect(false.B)
    }
  }

  it should "assert the wait signal on the input ports" in {
    test(mkMemArbiter) { dut =>
      dut.io.in(0).addr.poke(1.U)
      dut.io.in(1).addr.poke(2.U)
      dut.io.in(0).waitReq.expect(false.B)
      dut.io.in(1).waitReq.expect(false.B)

      // Read 0
      dut.io.in(0).rd.poke(true.B)
      dut.io.in(1).rd.poke(false.B)

      // Wait
      dut.io.out.waitReq.poke(true.B)
      dut.io.in(0).waitReq.expect(true.B)
      dut.io.in(1).waitReq.expect(false.B)
      dut.io.out.waitReq.poke(false.B)
      dut.io.in(0).waitReq.expect(false.B)
      dut.io.in(1).waitReq.expect(false.B)

      // Read 1
      dut.io.in(0).rd.poke(false.B)
      dut.io.in(1).rd.poke(true.B)

      // Wait
      dut.io.out.waitReq.poke(true.B)
      dut.io.in(0).waitReq.expect(false.B)
      dut.io.in(1).waitReq.expect(true.B)
      dut.io.out.waitReq.poke(false.B)
      dut.io.in(0).waitReq.expect(false.B)
      dut.io.in(1).waitReq.expect(false.B)

      // Read 1+2
      dut.io.in(0).rd.poke(true.B)
      dut.io.in(1).rd.poke(true.B)

      // Wait
      dut.io.out.waitReq.poke(true.B)
      dut.io.in(0).waitReq.expect(true.B)
      dut.io.in(1).waitReq.expect(true.B)
      dut.io.out.waitReq.poke(false.B)
      dut.io.in(0).waitReq.expect(false.B)
      dut.io.in(1).waitReq.expect(true.B)
    }
  }
}

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

trait SDRAMWaitHelpers {
  def waitForInit(dut: SDRAM) =
    while(!dut.io.debug.init.peek().litToBoolean) { dut.clock.step() }

  def waitForMode(dut: SDRAM) =
    while(!dut.io.debug.mode.peek().litToBoolean) { dut.clock.step() }

  def waitForIdle(dut: SDRAM) =
    while(!dut.io.debug.idle.peek().litToBoolean) { dut.clock.step() }

  def waitForActive(dut: SDRAM) =
    while(!dut.io.debug.active.peek().litToBoolean) { dut.clock.step() }

  def waitForRead(dut: SDRAM) =
    while(!dut.io.debug.read.peek().litToBoolean) { dut.clock.step() }

  def waitForWrite(dut: SDRAM) =
    while(!dut.io.debug.write.peek().litToBoolean) { dut.clock.step() }

  def waitForRefresh(dut: SDRAM) =
    while(!dut.io.debug.refresh.peek().litToBoolean) { dut.clock.step() }
}

class SDRAMTest extends FlatSpec with ChiselScalatestTester with Matchers with SDRAMWaitHelpers {
  val config = SDRAMConfig(
    tINIT = 20,
    tMRD = 10,
    tRC = 10,
    tRCD = 10,
    tRP = 10,
    tWR = 10,
    tREFI = 100
  )

  behavior of "FSM"

  it should "move to the mode state after initializing" in {
    test(new SDRAM(config)) { dut =>
      waitForInit(dut)
      dut.clock.step(5)
      dut.io.debug.mode.expect(true.B)
    }
  }

  it should "move to the idle state after setting the mode" in {
    test(new SDRAM(config)) { dut =>
      waitForMode(dut)
      dut.clock.step(2)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "output a NOP during the idle state" in {
    test(new SDRAM(config)) { dut =>
      waitForIdle(dut)
      dut.io.sdram.ras.expect(true.B)
      dut.io.sdram.cas.expect(true.B)
      dut.io.sdram.we.expect(true.B)
    }
  }

  it should "move to the active state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      waitForIdle(dut)
      dut.clock.step()
      dut.io.debug.active.expect(true.B)
    }
  }

  it should "assert the ack signal at the beginning of the active state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      waitForActive(dut)
      dut.io.mem.ack.expect(true.B)
    }
  }

  it should "move to the read state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      waitForActive(dut)
      dut.clock.step(2)
      dut.io.debug.read.expect(true.B)
    }
  }

  it should "move to the write state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      dut.io.mem.wr.poke(true.B)
      waitForActive(dut)
      dut.clock.step(2)
      dut.io.debug.write.expect(true.B)
    }
  }

  it should "return to the idle state from the read state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      waitForRead(dut)
      dut.io.mem.req.poke(false.B)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "return to the idle state from the write state" in {
    test(new SDRAM(config)) { dut =>
      dut.io.mem.req.poke(true.B)
      dut.io.mem.wr.poke(true.B)
      waitForWrite(dut)
      dut.io.mem.req.poke(false.B)
      dut.io.mem.wr.poke(false.B)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "return to the idle state from the refresh state" in {
    test(new SDRAM(config)) { dut =>
      waitForRefresh(dut)
      dut.clock.step()
      dut.io.debug.idle.expect(true.B)
    }
  }

  behavior of "initialize"

  it should "initialize the SDRAM" in {
    test(new SDRAM(config)) { dut =>
      // NOP
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(true.B)
      dut.io.sdram.cas.expect(true.B)
      dut.io.sdram.we.expect(true.B)
      dut.clock.step()

      // Deselect
      dut.io.sdram.cs.expect(true.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(false.B)
      dut.clock.step()

      // Precharge
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(true.B)
      dut.io.sdram.we.expect(false.B)
      dut.io.sdram.addr.expect(1024.U)
      dut.clock.step()

      // Refresh
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(true.B)
      dut.clock.step()

      // Refresh
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(true.B)
      dut.clock.step()

      // Mode
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(false.B)
      dut.io.sdram.addr.expect(32.U)
    }
  }

  behavior of "read"

  it should "read from the SDRAM (burst=1)" in {
    test(new SDRAM(config)) { dut =>
      waitForIdle(dut)

      // Request read
      dut.io.mem.req.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      waitForActive(dut)
      dut.io.mem.req.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForRead(dut)

      // Column
      dut.io.sdram.addr.expect(1025.U)

      // CAS latency
      dut.clock.step(cycles = 2)

      // Data
      dut.io.sdram.dout.poke(1.U)
      dut.clock.step()
      dut.io.mem.dout.expect(1.U)
      dut.io.mem.valid.expect(true.B)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "read from the SDRAM (burst=2)" in {
    test(new SDRAM(config.copy(burstLength = 2))) { dut =>
      waitForIdle(dut)

      // Request read
      dut.io.mem.req.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      waitForActive(dut)
      dut.io.mem.req.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForRead(dut)

      // Column
      dut.io.sdram.addr.expect(1025.U)

      // CAS latency
      dut.clock.step(cycles = 2)

      // Data
      dut.io.sdram.dout.poke(1.U)
      dut.clock.step()
      dut.io.sdram.dout.poke(2.U)
      dut.clock.step()
      dut.io.mem.dout.expect(65538.U)
      dut.io.mem.valid.expect(true.B)
      dut.io.debug.idle.expect(true.B)
    }
  }

  behavior of "write"

  it should "write to the SDRAM (burst=1)" in {
    test(new SDRAM(config)) { dut =>
      waitForIdle(dut)

      // Request write
      dut.io.mem.req.poke(true.B)
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      dut.io.mem.din.poke(1.U)
      waitForActive(dut)
      dut.io.mem.req.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForWrite(dut)

      // Column
      dut.io.sdram.addr.expect(1025.U)

      // Data
      dut.io.sdram.din.expect(1.U)
      dut.clock.step(3)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "write to the SDRAM (burst=2)" in {
    test(new SDRAM(config.copy(burstLength = 2))) { dut =>
      waitForIdle(dut)

      // Request write
      dut.io.mem.req.poke(true.B)
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      dut.io.mem.din.poke(65538.U)
      waitForActive(dut)
      dut.io.mem.req.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForWrite(dut)

      // Column
      dut.io.sdram.addr.expect(1025.U)

      // Data
      dut.io.sdram.din.expect(1.U)
      dut.clock.step()
      dut.io.sdram.din.expect(2.U)
      dut.clock.step(3)
      dut.io.debug.idle.expect(true.B)
    }
  }

  behavior of "refresh"

  it should "refresh the SDRAM after the refresh interval" in {
    test(new SDRAM(config)) { dut =>
      waitForIdle(dut)
      dut.clock.step(10)
      dut.io.debug.refresh.expect(true.B)
      waitForIdle(dut)
      dut.clock.step(10)
      dut.io.debug.refresh.expect(true.B)
    }
  }
}

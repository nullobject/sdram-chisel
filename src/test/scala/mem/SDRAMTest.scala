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

trait SDRAMHelpers {
  protected val sdramConfig = SDRAMConfig(
    clockFreq = 100000000,
    tINIT = 20,
    tMRD = 10,
    tRC = 20,
    tRCD = 10,
    tRP = 10,
    tWR = 10,
    tREFI = 100
  )

  protected def mkSDRAM(config: SDRAMConfig = sdramConfig) = new SDRAM(config)

  protected def waitForInit(dut: SDRAM) =
    while(!dut.io.debug.init.peek().litToBoolean) { dut.clock.step() }

  protected def waitForMode(dut: SDRAM) =
    while(!dut.io.debug.mode.peek().litToBoolean) { dut.clock.step() }

  protected def waitForIdle(dut: SDRAM) =
    while(!dut.io.debug.idle.peek().litToBoolean) { dut.clock.step() }

  protected def waitForActive(dut: SDRAM) =
    while(!dut.io.debug.active.peek().litToBoolean) { dut.clock.step() }

  protected def waitForRead(dut: SDRAM) =
    while(!dut.io.debug.read.peek().litToBoolean) { dut.clock.step() }

  protected def waitForWrite(dut: SDRAM) =
    while(!dut.io.debug.write.peek().litToBoolean) { dut.clock.step() }

  protected def waitForRefresh(dut: SDRAM) =
    while(!dut.io.debug.refresh.peek().litToBoolean) { dut.clock.step() }
}

class SDRAMTest extends FlatSpec with ChiselScalatestTester with Matchers with SDRAMHelpers {
  behavior of "FSM"

  it should "move to the mode state after initializing" in {
    test(mkSDRAM()) { dut =>
      waitForInit(dut)
      dut.clock.step(7)
      dut.io.debug.mode.expect(true.B)
    }
  }

  it should "move to the idle state after setting the mode" in {
    test(mkSDRAM()) { dut =>
      waitForMode(dut)
      dut.clock.step(2)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "move to the active state" in {
    test(mkSDRAM()) { dut =>
      dut.io.mem.rd.poke(true.B)
      waitForIdle(dut)
      dut.clock.step()
      dut.io.debug.active.expect(true.B)
    }
  }

  it should "move to the read state" in {
    test(mkSDRAM()) { dut =>
      dut.io.mem.rd.poke(true.B)
      waitForActive(dut)
      dut.clock.step(2)
      dut.io.debug.read.expect(true.B)
    }
  }

  it should "move to the write state" in {
    test(mkSDRAM()) { dut =>
      dut.io.mem.wr.poke(true.B)
      waitForActive(dut)
      dut.clock.step(2)
      dut.io.debug.write.expect(true.B)
    }
  }

  it should "return to the idle state from the read state" in {
    test(mkSDRAM()) { dut =>
      dut.io.mem.rd.poke(true.B)
      waitForRead(dut)
      dut.io.mem.rd.poke(false.B)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "return to the idle state from the write state" in {
    test(mkSDRAM()) { dut =>
      dut.io.mem.wr.poke(true.B)
      waitForWrite(dut)
      dut.io.mem.wr.poke(false.B)
      dut.clock.step(4)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "return to the idle state from the refresh state" in {
    test(mkSDRAM()) { dut =>
      waitForRefresh(dut)
      dut.clock.step(2)
      dut.io.debug.idle.expect(true.B)
    }
  }

  behavior of "initialize"

  it should "initialize the SDRAM" in {
    test(mkSDRAM()) { dut =>
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
      dut.io.sdram.addr.expect(0x400.U)
      dut.clock.step()

      // Refresh
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(true.B)
      dut.clock.step(2)

      // Refresh
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(true.B)
      dut.clock.step(2)

      // Mode
      dut.io.sdram.cs.expect(false.B)
      dut.io.sdram.ras.expect(false.B)
      dut.io.sdram.cas.expect(false.B)
      dut.io.sdram.we.expect(false.B)
      dut.io.sdram.addr.expect(0x020.U)
    }
  }

  behavior of "idle"

  it should "perform a NOP" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)
      dut.io.sdram.ras.expect(true.B)
      dut.io.sdram.cas.expect(true.B)
      dut.io.sdram.we.expect(true.B)
    }
  }

  behavior of "read"

  it should "read from the SDRAM (burst=1)" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      waitForActive(dut)
      dut.io.mem.rd.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForRead(dut)

      // Column
      dut.io.sdram.addr.expect(0x401.U)

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
    test(mkSDRAM(sdramConfig.copy(burstLength = 2))) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      waitForActive(dut)
      dut.io.mem.rd.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForRead(dut)

      // Column
      dut.io.sdram.addr.expect(0x402.U)

      // CAS latency
      dut.clock.step(cycles = 2)

      // Data
      dut.io.sdram.dout.poke(0x1234.U)
      dut.clock.step()
      dut.io.sdram.dout.poke(0x5678.U)
      dut.clock.step()
      dut.io.mem.dout.expect(0x12345678.U)
      dut.io.mem.valid.expect(true.B)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "read from the SDRAM (burst=4)" in {
    test(mkSDRAM(sdramConfig.copy(burstLength = 4))) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      waitForActive(dut)
      dut.io.mem.rd.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForRead(dut)

      // Column
      dut.io.sdram.addr.expect(0x404.U)

      // CAS latency
      dut.clock.step(cycles = 2)

      // Data
      dut.io.sdram.dout.poke(0x1234.U)
      dut.clock.step()
      dut.io.sdram.dout.poke(0x5678.U)
      dut.clock.step()
      dut.io.sdram.dout.poke(0x1234.U)
      dut.clock.step()
      dut.io.sdram.dout.poke(0x5678.U)
      dut.clock.step()
      dut.io.mem.dout.expect(0x1234567812345678L.U)
      dut.io.mem.valid.expect(true.B)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "assert the wait signal when a read is pending" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.ack.expect(false.B)
      waitForActive(dut)

      // Wait
      dut.io.mem.ack.expect(true.B)
      dut.io.mem.rd.poke(false.B)
      dut.io.mem.ack.expect(false.B)
      waitForRead(dut)

      // Read
      dut.io.mem.ack.expect(false.B)
      dut.clock.step()
      dut.io.mem.ack.expect(false.B)
      dut.clock.step()
      dut.io.mem.ack.expect(false.B)
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.ack.expect(false.B)
    }
  }

  behavior of "write"

  it should "write to the SDRAM (burst=1)" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      dut.io.mem.din.poke(1.U)
      waitForActive(dut)
      dut.io.mem.wr.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForWrite(dut)

      // Column
      dut.io.sdram.addr.expect(0x401.U)

      // Data
      dut.io.sdram.din.expect(1.U)
      dut.clock.step(3)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "write to the SDRAM (burst=2)" in {
    test(mkSDRAM(sdramConfig.copy(burstLength = 2))) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      dut.io.mem.din.poke(0x12345678.U)
      waitForActive(dut)
      dut.io.mem.wr.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForWrite(dut)

      // Column
      dut.io.sdram.addr.expect(0x402.U)

      // Data
      dut.io.sdram.din.expect(0x1234.U)
      dut.clock.step()
      dut.io.sdram.din.expect(0x5678.U)
      dut.clock.step(3)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "write to the SDRAM (burst=4)" in {
    test(mkSDRAM(sdramConfig.copy(burstLength = 4))) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.addr.poke(1.U)
      dut.io.mem.din.poke(0x1234567812345678L.U)
      waitForActive(dut)
      dut.io.mem.wr.poke(false.B)

      // Row
      dut.io.sdram.addr.expect(0.U)
      waitForWrite(dut)

      // Column
      dut.io.sdram.addr.expect(0x404.U)

      // Data
      dut.io.sdram.din.expect(0x1234.U)
      dut.clock.step()
      dut.io.sdram.din.expect(0x5678.U)
      dut.clock.step()
      dut.io.sdram.din.expect(0x1234.U)
      dut.clock.step()
      dut.io.sdram.din.expect(0x5678.U)
      dut.clock.step(3)
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "assert the wait signal when a write is pending" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)

      // Request
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.ack.expect(false.B)
      waitForActive(dut)

      // Wait
      dut.io.mem.ack.expect(true.B)
      dut.io.mem.wr.poke(false.B)
      dut.io.mem.ack.expect(false.B)
      waitForWrite(dut)

      // Write
      dut.io.mem.ack.expect(false.B)
      dut.clock.step()
      dut.io.mem.ack.expect(false.B)
      dut.clock.step()
      dut.io.mem.ack.expect(false.B)
      dut.io.mem.wr.poke(true.B)
      dut.io.mem.ack.expect(false.B)
    }
  }

  behavior of "refresh"

  it should "perform a refresh" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)
      dut.clock.step(10)
      dut.io.debug.refresh.expect(true.B)
      waitForIdle(dut)
      dut.clock.step(10)
      dut.io.debug.refresh.expect(true.B)
    }
  }

  it should "assert the wait signal when a refresh is pending" in {
    test(mkSDRAM()) { dut =>
      waitForIdle(dut)
      dut.clock.step(9)
      dut.io.mem.ack.expect(false.B)
      dut.io.mem.rd.poke(true.B)
      dut.io.mem.ack.expect(true.B)
      dut.clock.step()

      // Refresh
      dut.io.debug.refresh.expect(true.B)
      dut.io.mem.ack.expect(true.B)
      dut.clock.step()
      dut.io.mem.ack.expect(false.B)
    }
  }
}

/*
 *   __   __     __  __     __         __
 *  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *    \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *   ______     ______       __     ______     ______     ______
 *  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 * https://joshbassett.info
 * https://twitter.com/nullobject
 * https://github.com/nullobject
 *
 * Copyright (c) 2020 Josh Bassett
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mem

import chisel3._
import chiseltest._
import org.scalatest._

trait CacheMemHelpers {
  protected val cacheConfig = CacheConfig(
    inAddrWidth = 8,
    inDataWidth = 4,
    outAddrWidth = 16,
    outDataWidth = 8,
    depth = 8,
    offset = 0x1000
  )

  protected def mkCacheMem(config: CacheConfig = cacheConfig) = new CacheMem(config)

  protected def readCache(dut: CacheMem, addr: Int) = {
    dut.io.in.rd.poke(true.B)
    dut.io.in.addr.poke(addr.U)
    dut.clock.step()
    dut.io.in.rd.poke(false.B)
    val result = dut.io.in.dout.peek().litValue()
    dut.clock.step()
    result
  }

  protected def writeCache(dut: CacheMem, addr: Int, data: Int) = {
    dut.io.in.wr.poke(true.B)
    dut.io.in.addr.poke(addr.U)
    dut.io.in.din.poke(data.U)
    dut.clock.step()
    dut.io.in.wr.poke(false.B)
    dut.clock.step()
  }

  protected def writeCacheLine(dut: CacheMem, addr: Int, data: Int) = {
    dut.io.in.rd.poke(true.B)
    dut.io.in.addr.poke(addr.U)
    dut.clock.step()
    dut.io.in.rd.poke(false.B)
    dut.io.out.ack.poke(true.B)
    dut.clock.step()
    dut.io.out.ack.poke(false.B)
    dut.io.out.valid.poke(true.B)
    dut.io.out.dout.poke(data.U)
    dut.clock.step()
    dut.io.out.valid.poke(false.B)
    dut.clock.step()
  }

  protected def waitForIdle(dut: CacheMem) =
    while(!dut.io.debug.idle.peek().litToBoolean) { dut.clock.step() }

  protected def waitForCheck(dut: CacheMem) =
    while(!dut.io.debug.check.peek().litToBoolean) { dut.clock.step() }

  protected def waitForLineFill(dut: CacheMem) =
    while(!dut.io.debug.lineFill.peek().litToBoolean) { dut.clock.step() }

  protected def waitForEvict(dut: CacheMem) =
    while(!dut.io.debug.evict.peek().litToBoolean) { dut.clock.step() }
}

class CacheMemTest extends FlatSpec with ChiselScalatestTester with Matchers with CacheMemHelpers {
  behavior of "FSM"

  it should "assert the ack signal when a request is acknowledged" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.rd.poke(true.B)
      dut.io.in.ack.expect(false.B)
      waitForIdle(dut)
      dut.io.in.ack.expect(true.B)
      waitForCheck(dut)
      dut.io.in.ack.expect(false.B)
    }
  }

  it should "move to the check state after a read request" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.rd.poke(true.B)
      waitForIdle(dut)
      dut.clock.step()
      dut.io.debug.check.expect(true.B)
    }
  }

  it should "move to the check state after a write request" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.wr.poke(true.B)
      waitForIdle(dut)
      dut.clock.step()
      dut.io.debug.check.expect(true.B)
    }
  }

  it should "move to the line fill state after a read miss" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.rd.poke(true.B)
      dut.io.out.ack.poke(true.B)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.lineFill.expect(true.B)
    }
  }

  it should "move to the line fill state after a write miss" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.wr.poke(true.B)
      dut.io.out.ack.poke(true.B)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.lineFill.expect(true.B)
    }
  }

  it should "move to the evict state after a dirty write hit" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      writeCache(dut, 0x01, 3)
      dut.io.in.wr.poke(true.B)
      dut.io.in.addr.poke(16.U)
      dut.io.out.ack.poke(true.B)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.evict.expect(true.B)
    }
  }

  it should "move to the line fill state after an eviction" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      writeCache(dut, 0x01, 3)
      dut.io.in.wr.poke(true.B)
      dut.io.in.addr.poke(16.U)
      dut.io.out.ack.poke(true.B)
      waitForEvict(dut)
      dut.clock.step()
      dut.io.debug.lineFill.expect(true.B)
    }
  }

  it should "return to the check state after a line fill" in {
    test(mkCacheMem()) { dut =>
      dut.io.in.rd.poke(true.B)
      dut.io.out.ack.poke(true.B)
      waitForLineFill(dut)
      dut.io.out.valid.poke(true.B)
      dut.clock.step()
      dut.io.debug.check.expect(true.B)
    }
  }

  it should "return to the idle state after a read hit" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      dut.io.in.rd.poke(true.B)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.idle.expect(true.B)
    }
  }

  it should "return to the idle state after a write hit" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      dut.io.in.wr.poke(true.B)
      dut.io.out.ack.poke(true.B)
      waitForCheck(dut)
      dut.clock.step()
      dut.io.debug.idle.expect(true.B)
    }
  }

  behavior of "read hit"

  it should "read from the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)

      // Read
      dut.io.in.rd.poke(true.B)
      dut.io.in.addr.poke(1.U)
      dut.clock.step()
      dut.io.in.rd.poke(false.B)
      dut.io.in.addr.poke(0.U)

      // No line fill
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)

      // Valid
      dut.io.in.valid.expect(true.B)
      dut.io.in.dout.expect(2.U)
    }
  }

  it should "evict a dirty cache entry before reading from the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      writeCache(dut, 0x01, 3)

      // Read
      dut.io.in.rd.poke(true.B)
      dut.io.in.addr.poke(16.U)
      dut.clock.step()
      dut.io.in.rd.poke(false.B)
      dut.io.in.addr.poke(0.U)

      // Evict
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(true.B)
      dut.io.out.addr.expect(0x1000.U)
      dut.io.out.din.expect(0x31.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)

      // Line fill
      dut.io.out.rd.expect(true.B)
      dut.io.out.wr.expect(false.B)
      dut.io.out.addr.expect(0x1008.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(0x43.U)
      dut.io.in.valid.expect(false.B)
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)
      dut.clock.step()

      // Valid
      dut.io.out.valid.poke(false.B)
      dut.io.in.valid.expect(true.B)
      dut.io.in.dout.expect(3.U)
    }
  }

  behavior of "read miss"

  it should "fill a cache line before reading from the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)

      // Read
      dut.io.in.rd.poke(true.B)
      dut.io.in.addr.poke(0.U)
      dut.clock.step()
      dut.io.in.rd.poke(false.B)

      // Line fill
      dut.io.out.rd.expect(true.B)
      dut.io.out.wr.expect(false.B)
      dut.io.out.addr.expect(0x1000.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(0x21.U)
      dut.io.in.valid.expect(false.B)
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)
      dut.clock.step()

      // Valid
      dut.io.out.valid.poke(false.B)
      dut.io.in.valid.expect(true.B)
      dut.io.in.dout.expect(1.U)
    }
  }

  behavior of "write hit"

  it should "write to the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)

      // Write
      dut.io.in.wr.poke(true.B)
      dut.io.in.addr.poke(1.U)
      dut.io.in.din.poke(3.U)
      dut.clock.step()
      dut.io.in.wr.poke(false.B)
      dut.io.in.addr.poke(0.U)
      dut.io.in.din.poke(0.U)

      // No line fill
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)
      dut.clock.step()

      readCache(dut, 0x00) shouldBe 1
      readCache(dut, 0x01) shouldBe 3
    }
  }

  it should "evict a dirty cache entry before writing to the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)
      writeCacheLine(dut, 0x00, 0x21)
      writeCache(dut, 0x01, 3)

      // Write
      dut.io.in.wr.poke(true.B)
      dut.io.in.addr.poke(16.U)
      dut.io.in.din.poke(4.U)
      dut.clock.step()
      dut.io.in.wr.poke(false.B)
      dut.io.in.addr.poke(0.U)
      dut.io.in.din.poke(0.U)

      // Evict
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(true.B)
      dut.io.out.addr.expect(0x1000.U)
      dut.io.out.din.expect(0x31.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)

      // Line fill
      dut.io.out.rd.expect(true.B)
      dut.io.out.wr.expect(false.B)
      dut.io.out.addr.expect(0x1008.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(0x65.U)
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)
      dut.clock.step()

      // Not valid
      dut.io.out.valid.poke(false.B)
      dut.io.in.valid.expect(false.B)
      dut.clock.step()

      readCache(dut, 0x10) shouldBe 4
      readCache(dut, 0x11) shouldBe 6
    }
  }

  behavior of "write miss"

  it should "fill a cache line before writing to the cache" in {
    test(mkCacheMem()) { dut =>
      waitForIdle(dut)

      // Write
      dut.io.in.wr.poke(true.B)
      dut.io.in.addr.poke(1.U)
      dut.io.in.din.poke(3.U)
      dut.clock.step()
      dut.io.in.wr.poke(false.B)
      dut.io.in.addr.poke(0.U)
      dut.io.in.din.poke(0.U)

      // Line fill
      dut.io.out.rd.expect(true.B)
      dut.io.out.wr.expect(false.B)
      dut.io.out.addr.expect(0x1000.U)
      dut.io.out.ack.poke(true.B)
      dut.clock.step()
      dut.io.out.ack.poke(false.B)
      dut.io.out.valid.poke(true.B)
      dut.io.out.dout.poke(0x21.U)
      dut.io.out.rd.expect(false.B)
      dut.io.out.wr.expect(false.B)
      dut.clock.step()

      // Not valid
      dut.io.out.valid.poke(false.B)
      dut.io.in.valid.expect(false.B)
      dut.clock.step()

      readCache(dut, 0) shouldBe 1
      readCache(dut, 1) shouldBe 3
    }
  }
}

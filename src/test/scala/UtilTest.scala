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

class UtilTest extends FlatSpec with ChiselScalatestTester with Matchers {
  "edge" should "detect edges" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Input(Bool())
        val b = Output(Bool())
      })
      io.b := Util.edge(io.a)
    }) { dut =>
      dut.io.a.poke(false.B)
      dut.io.b.expect(false.B)
      dut.clock.step()
      dut.io.a.poke(true.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.io.a.poke(false.B)
      dut.io.b.expect(true.B)
    }
  }

  "rising" should "detect rising edges" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Input(Bool())
        val b = Output(Bool())
      })
      io.b := Util.rising(io.a)
    }) { dut =>
      dut.io.a.poke(false.B)
      dut.io.b.expect(false.B)
      dut.clock.step()
      dut.io.a.poke(true.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.io.a.poke(false.B)
      dut.io.b.expect(false.B)
    }
  }

  "falling" should "detect falling edges" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Input(Bool())
        val b = Output(Bool())
      })
     io.b := Util.falling(io.a)
    }) { dut =>
      dut.io.a.poke(true.B)
      dut.io.b.expect(false.B)
      dut.clock.step()
      dut.io.a.poke(false.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.io.a.poke(false.B)
      dut.io.b.expect(false.B)
    }
  }

  "latch" should "latch a pulse" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Input(Bool())
        val b = Output(Bool())
      })
      io.b := Util.latch(io.a)
    }) { dut =>
      dut.io.a.poke(true.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.io.a.poke(false.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.reset.poke(true.B)
      dut.io.a.poke(true.B)
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.io.a.poke(false.B)
      dut.io.b.expect(false.B)
    }
  }

  "toggle" should "toggle a bit" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Output(Bool())
      })
      io.a := Util.toggle
    }) { dut =>
      dut.io.a.expect(false.B)
      dut.clock.step()
      dut.io.a.expect(true.B)
      dut.clock.step()
      dut.io.a.expect(false.B)
    }
  }

  "sync" should "generate a sync pulse" in {
    test(new Module {
      val io = IO(new Bundle {
        val a = Input(Clock())
        val b = Output(Bool())
      })
      io.b := Util.sync(io.a)
    }) { dut =>
      dut.io.a.low()
      dut.io.b.expect(false.B)
      dut.clock.step()
      dut.io.a.high()
      dut.io.b.expect(true.B)
      dut.clock.step()
      dut.io.a.low()
      dut.io.b.expect(false.B)
    }
  }
}

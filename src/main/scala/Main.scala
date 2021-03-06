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
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import mem._

/** This is the top-level module */
class Main extends Module {
  val CLOCK_FREQ = 48000000D

  // SDRAM configuration
  val sdramConfig = SDRAMConfig(clockFreq = CLOCK_FREQ, burstLength = 2)

  // Memory multiplexer configuration
  val memMuxConfig = MemMuxConfig(
    addrWidth = sdramConfig.logicalAddrWidth,
    dataWidth = sdramConfig.logicalDataWidth,
    slots = Seq(
      // Write slot
      SlotConfig(8, 8, depth = 1),
      // Read slot
      SlotConfig(8, 8, depth = 4)
    )
  )

  val io = IO(new Bundle {
    val cpuClock = Input(Clock())
    val led = Output(UInt(8.W))
    val sdram = SDRAMIO(sdramConfig)
  })

  // SDRAM
  val sdram = Module(new SDRAM(sdramConfig))
  sdram.io.sdram <> io.sdram

  // Memory multiplexer
  val memMux = Module(new MemMux(memMuxConfig))
  memMux.io.out <> sdram.io.mem

  // Demo
  //
  // The demo module runs in the CPU (slow) clock domain, so the memory slots must be frozen from
  // the system (fast) clock domain.
  val demo = withClock(io.cpuClock) { Module(new Demo) }
  demo.io.write <> DataFreezer.freeze(io.cpuClock) { memMux.io.in(0) }.asAsyncWriteMemIO
  demo.io.read <> DataFreezer.freeze(io.cpuClock) { memMux.io.in(1) }.asAsyncReadMemIO

  // Outputs
  io.led := demo.io.led
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/rtl", "--output-file", "ChiselTop"),
    Seq(ChiselGeneratorAnnotation(() => new Main()))
  )
}

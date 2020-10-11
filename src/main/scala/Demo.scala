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
import chisel3.util._
import mem._

/** This is the top-level module for the demo circuit. */
class Demo extends Module {
  val CLOCK_FREQ = 50000000D

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
      SlotConfig(8, 8, depth = 16)
    )
  )

  val io = IO(new Bundle {
    val led = Output(UInt(8.W))
    val sdram = SDRAMIO(sdramConfig)
  })

  // States
  val writeState :: readState :: Nil = Enum(2)

  // Wires
  val waitCounterEnable = Wire(Bool())
  val addrCounterEnable = Wire(Bool())

  // Registers
  val stateReg = RegInit(writeState)
  val pendingReg = RegInit(true.B)

  // Counters
  val (_, waitCounterWrap) = Counter(0 until (CLOCK_FREQ/10).ceil.toInt, enable = waitCounterEnable)
  val (addrCounterValue, addrCounterWrap) = Counter(0 until 256, enable = addrCounterEnable)

  // Control signals
  val read = stateReg === readState
  val write = stateReg === writeState

  // SDRAM
  val sdram = Module(new SDRAM(sdramConfig))

  // Memory multiplexer
  val memMux = Module(new MemMux(memMuxConfig))
  memMux.io.out <> sdram.io.mem
  memMux.io.in(0).rd := false.B
  memMux.io.in(0).wr := write
  memMux.io.in(0).addr := addrCounterValue
  memMux.io.in(0).din := addrCounterValue
  memMux.io.in(1).rd := read
  memMux.io.in(1).wr := false.B
  memMux.io.in(1).addr := addrCounterValue
  memMux.io.in(1).din := 0.U

  // Toggle counter enable signals
  waitCounterEnable := read && pendingReg
  addrCounterEnable := (write && !memMux.io.in(0).waitReq) || (read && waitCounterWrap)

  // Toggle pending register
  when(waitCounterWrap) { pendingReg := false.B }.elsewhen(read && !memMux.io.in(1).waitReq) { pendingReg := true.B }

  // Set read state
  when(addrCounterWrap) { stateReg := readState }

  // Outputs
  io.led := RegEnable(memMux.io.in(1).dout, 0.U, memMux.io.in(1).valid)
  io.sdram <> sdram.io.sdram
}

object Demo extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/rtl", "--output-file", "ChiselTop"),
    Seq(ChiselGeneratorAnnotation(() => new Demo()))
  )
}

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
  val CLOCK_FREQ: Double = 50000000

  // SDRAM configuration
  val config = SDRAMConfig(clockFreq = CLOCK_FREQ)

  val io = IO(new Bundle {
    val led = Output(UInt(8.W))
    val sdram = SDRAMIO(config.bankWidth, config.addrWidth, config.dataWidth)
  })

  // States
  val write :: read :: Nil = Enum(2)

  // Registers
  val stateReg = RegInit(write)
  val waitEnable = RegInit(false.B)

  // Counters
  val (_, waitCounterWrap) = Counter(0 until (CLOCK_FREQ/100).ceil.toInt, enable = waitEnable)
  val (addrCounterValue, addrCounterWrap) = Counter(0 until 256, enable = waitCounterWrap)

  // SDRAM
  val sdram = Module(new SDRAM(config))
  sdram.io.mem.rd := stateReg === read && !waitEnable
  sdram.io.mem.wr := stateReg === write && !waitEnable
  sdram.io.mem.addr := addrCounterValue
  sdram.io.mem.din := addrCounterValue

  // Toggle the wait register when the request is acknowledged and when the wait counter wraps
  when(sdram.io.mem.ack) {
    waitEnable := true.B
  }.elsewhen(waitCounterWrap) {
    waitEnable := false.B
  }

  // Move to the write state after the address counter wraps
  when(addrCounterWrap) { stateReg := read }

  // Outputs
  io.led := RegEnable(sdram.io.mem.dout, 0.U, sdram.io.mem.valid)
  io.sdram <> sdram.io.sdram
}

object Demo extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/rtl", "--output-file", "ChiselTop"),
    Seq(ChiselGeneratorAnnotation(() => new Demo()))
  )
}

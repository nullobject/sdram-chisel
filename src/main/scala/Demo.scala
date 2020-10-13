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
import chisel3.util._
import mem._

class Demo extends Module {
  val io = IO(new Bundle {
    val write = AsyncWriteMemIO(8, 8)
    val read = AsyncReadMemIO(8, 8)
    val led = Output(UInt(8.W))
  })

  // States
  val writeState :: readState :: Nil = Enum(2)

  // Wires
  val writeCounterEnable = Wire(Bool())
  val readCounterEnable = Wire(Bool())

  // Registers
  val stateReg = RegInit(writeState)

  // Counters
  //
  // The write counter overshoots the address range to ensure the write cache line is flushed to
  // memory. This could be avoided if we were able to manually flush the cache.
  val (writeAddrCounterValue, writeCounterWrap) = Counter(0 until 20, enable = writeCounterEnable)
  val (readAddrCounterValue, _) = Counter(0 until 16, enable = readCounterEnable)

  // Control signals
  val readEnable = stateReg === readState
  val writeEnable = stateReg === writeState
  writeCounterEnable := writeEnable && !io.write.waitReq
  readCounterEnable := readEnable && !io.read.waitReq

  // Set read state
  when(writeCounterWrap) { stateReg := readState }

  // Outputs
  io.write.wr := writeEnable
  io.write.addr := writeAddrCounterValue
  io.write.din := writeAddrCounterValue
  io.read.rd := readEnable
  io.read.addr := readAddrCounterValue
  io.led := RegEnable(io.read.dout, 0.U, io.read.valid)
}

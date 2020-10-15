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
import mem._

/**
 * Transfers asynchronous memory IO control signals between clock domains.
 *
 * The data freezer requires the system clock domain frequency to be an integer multiple of the
 * target clock domain frequency. For example, a system clock of 48MHz can be frozen to a target
 * clock of 6MHz.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class DataFreezer(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    /** The target clock domain */
    val targetClock = Input(Clock())
    /** Input port */
    val in = Flipped(AsyncReadWriteMemIO(addrWidth, dataWidth))
    /** Output port */
    val out = AsyncReadWriteMemIO(addrWidth, dataWidth)
  })

  // Connect input/output ports
  io.in <> io.out

  // Registers
  val pendingReg = RegInit(false.B)

  // Hold control pulses for long enough to be latched in the target clock domain (i.e. until a
  // rising edge of the target clock)
  val clear = Util.sync(io.targetClock)
  val ack = withReset(clear) { Util.latch(io.out.ack) }
  val valid = withReset(clear) { Util.latch(io.out.valid) }

  // Assert request signal when there is a pending read/write request
  val request = io.in.rd || io.in.wr

  // Set the pending register when a request is acknowledged. It is cleared by a rising edge of the
  // target clock.
  when(clear && ack) { pendingReg := false.B }.elsewhen(request && ack) { pendingReg := true.B }

  // Outputs
  io.out.rd := io.in.rd && !pendingReg
  io.out.wr := io.in.wr && !pendingReg
  io.in.ack := ack
  io.in.valid := valid
}

object DataFreezer {
  /**
   * Wraps the given memory interface with a data freezer.
   *
   * The control signals for the memory interface will be transferred to the target clock domain.
   *
   * @param targetClock The target clock domain.
   * @param mem The memory interface.
   */
  def freeze(targetClock: Clock)(mem: AsyncReadWriteMemIO): AsyncReadWriteMemIO = {
    val freezer = Module(new DataFreezer(mem.addrWidth, mem.dataWidth))
    freezer.io.targetClock := targetClock
    freezer.io.out <> mem
    freezer.io.in
  }
}

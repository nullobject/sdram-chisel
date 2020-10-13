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

/**
 * Transfers asynchronous memory IO signals between clock domains.
 *
 * @param hold The number of clock cycles to hold signals when transferring them from the fast clock
 *             domain to the slow clock domain.
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class DataFreezer(hold: Int, addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    /** Input port */
    val in = Flipped(AsyncReadWriteMemIO(addrWidth, dataWidth))
    /** Output port (fast clock domain) */
    val out = AsyncReadWriteMemIO(addrWidth, dataWidth)
  })

  // Connect input/output ports
  io.in <> io.out

  val (_, counterWrap) = Counter(true.B, hold)

  // Outputs
  io.out.rd := io.in.rd
  io.out.wr := io.in.wr
  io.in.valid := Util.stretch(io.out.valid, counterWrap)
  io.in.waitReq := !Util.stretch(!io.out.waitReq, counterWrap)
}

object DataFreezer {
  /**
   * Wraps the given memory interface with a data freezer.
   *
   * The returned memory interface will operate in the fast clock domain.
   *
   * @param hold The number of clock cycles to hold the signal.
   * @param mem The memory interface.
   */
  def freeze(hold: Int, mem: AsyncReadWriteMemIO): AsyncReadWriteMemIO = {
    val freezer = Module(new DataFreezer(hold, mem.addrWidth, mem.dataWidth))
    freezer.io.out <> mem
    freezer.io.in
  }
}
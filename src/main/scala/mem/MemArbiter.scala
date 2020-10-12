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

/**
 * Coordinates memory access from multiple input ports to a single output memory port.
 *
 * @param n Then number of inputs.
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class MemArbiter(n: Int, addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    /** Input port */
    val in = Flipped(Vec(n, AsyncReadWriteMemIO(addrWidth, dataWidth)))
    /** Output port */
    val out = AsyncReadWriteMemIO(addrWidth, dataWidth)
  })

  // Wires
  val index = WireInit(0.U)

  // Registers
  val pendingReg = RegInit(0.U)

  // Input/output port aliases
  val in = io.in(index)
  val out = io.out

  // Assert request signal when there is a pending request
  val request = in.rd || in.wr

  // Set pending register when there is a request
  when(request && !out.waitReq) { pendingReg := index }

  // Default outputs
  out.rd := false.B
  out.wr := false.B
  out.addr := 0.U
  out.din := 0.U

  for ((in, i) <- io.in.zipWithIndex.reverse) {
    // Assert request signal when there is a pending request
    val request = in.rd || in.wr

    // Route highest priority input port with a pending request to the output port
    when(request) {
      out <> in
      index := i.U
    }

    // Route wait signal to the selected input port. The wait signal is asserted for pending
    // requests on all other ports.
    in.waitReq := (i.U === index && out.waitReq) || (i.U =/= index && request)

    // Route valid signal to the pending input port
    in.valid := i.U === pendingReg && out.valid

    // Route output port data to the selected input port
    in.dout := out.dout
  }

  printf(p"MemArbiter(pending: $pendingReg, index: $index)\n")
}

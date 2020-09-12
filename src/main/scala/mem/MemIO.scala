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
 * A simple flow control interface for reading from synchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class ReadMemIO private (addrWidth: Int, dataWidth: Int) extends Bundle {
  /** Read enable */
  val rd = Output(Bool())
  /** Address bus */
  val addr = Output(UInt(addrWidth.W))
  /** Data bus */
  val dout = Input(UInt(dataWidth.W))

  override def cloneType: this.type = new ReadMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object ReadMemIO {
  def apply(addrWidth: Int, dataWidth: Int): ReadMemIO = new ReadMemIO(addrWidth, dataWidth)
}

/**
 * A simple flow control interface for writing to synchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class WriteMemIO private (addrWidth: Int, dataWidth: Int) extends Bundle {
  /** Read enable */
  val wr = Output(Bool())
  /** Address bus */
  val addr = Output(UInt(addrWidth.W))
  /** Data bus */
  val din = Output(UInt(dataWidth.W))

  override def cloneType: this.type = new WriteMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object WriteMemIO {
  def apply(addrWidth: Int, dataWidth: Int): WriteMemIO = new WriteMemIO(addrWidth, dataWidth)
}

/**
 * A simple flow control interface for reading and writing to synchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class ReadWriteMemIO private (addrWidth: Int, dataWidth: Int) extends Bundle {
  /** Read enable */
  val rd = Output(Bool())
  /** Write enable */
  val wr = Output(Bool())
  /** Address bus */
  val addr = Output(UInt(addrWidth.W))
  /** Data input bus */
  val din = Output(Bits(dataWidth.W))
  /** Data output bus */
  val dout = Input(Bits(dataWidth.W))

  override def cloneType: this.type = new ReadWriteMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object ReadWriteMemIO {
  def apply(addrWidth: Int, dataWidth: Int): ReadWriteMemIO = new ReadWriteMemIO(addrWidth, dataWidth)
}

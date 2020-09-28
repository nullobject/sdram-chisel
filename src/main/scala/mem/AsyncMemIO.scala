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
 * A simple flow control interface for reading from asynchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class AsyncReadMemIO private[mem] (addrWidth: Int, dataWidth: Int) extends ReadMemIO(addrWidth, dataWidth) {
  /** Flag to indicate when the read request has been acknowledged */
  val ack = Input(Bool())
  /** Flag to indicate when the output data is valid */
  val valid = Input(Bool())

  override def cloneType: this.type = new AsyncReadMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object AsyncReadMemIO {
  def apply(addrWidth: Int, dataWidth: Int) = new AsyncReadMemIO(addrWidth, dataWidth)
}

/**
 * A simple flow control interface for writing to asynchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class AsyncWriteMemIO private[mem] (addrWidth: Int, dataWidth: Int) extends WriteMemIO(addrWidth, dataWidth) {
  /** Flag to indicate when the write request has been acknowledged */
  val ack = Input(Bool())

  override def cloneType: this.type = new AsyncWriteMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object AsyncWriteMemIO {
  def apply(addrWidth: Int, dataWidth: Int) = new AsyncWriteMemIO(addrWidth, dataWidth)
}

/**
 * A simple flow control interface for reading and writing to asynchronous memory.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class AsyncReadWriteMemIO private[mem] (addrWidth: Int, dataWidth: Int) extends ReadWriteMemIO(addrWidth, dataWidth) {
  /** Flag to indicate when the read/write request has been acknowledged */
  val ack = Input(Bool())
  /** Flag to indicate when the output data is valid */
  val valid = Input(Bool())

  override def cloneType: this.type = new AsyncReadWriteMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object AsyncReadWriteMemIO {
  def apply(addrWidth: Int, dataWidth: Int) = new AsyncReadWriteMemIO(addrWidth, dataWidth)
}
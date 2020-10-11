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
 * Represents a configuration for a multiplexed memory slot.
 *
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 * @param depth The number of entries in the cache.
 * @param offset The offset of the output address.
 */
case class SlotConfig(addrWidth: Int, dataWidth: Int, depth: Int = 16, offset: Int = 0)

/**
 * Represents the memory multiplexer configuration.
 *
 * @param addrWidth The address bus width.
 * @param dataWidth The data bus width.
 * @param slots The slots to be multiplexed.
 */
case class MemMuxConfig(addrWidth: Int, dataWidth: Int, slots: Seq[SlotConfig])

/**
 * Multiplexes multiple memory ports to a single memory port.
 *
 * @param config The memory multiplexer configuration.
 */
class MemMux(config: MemMuxConfig) extends Module {
  val io = IO(new Bundle {
    /** Input port */
    val in = Flipped(Vec(config.slots.size, AsyncReadWriteMemIO(config.addrWidth, config.dataWidth)))
    /** Output port */
    val out = AsyncReadWriteMemIO(config.addrWidth, config.dataWidth)
  })

  // Arbiter
  val arbiter = Module(new MemArbiter(config.slots.size, config.addrWidth, config.dataWidth))
  arbiter.io.out <> io.out

  // Slots
  for ((slotConfig, i) <- config.slots.zipWithIndex) {
    val slot = Module(new CacheMem(CacheConfig(
      inAddrWidth = slotConfig.addrWidth,
      inDataWidth = slotConfig.dataWidth,
      outAddrWidth = config.addrWidth,
      outDataWidth = config.dataWidth,
      depth = slotConfig.depth,
      offset = slotConfig.offset
    )))
    slot.io.in <> io.in(i)
    slot.io.out <> arbiter.io.in(i)
  }
}

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
import chisel3.util._

/**
 * Represents the cache configuration.
 *
 * @param inAddrWidth The width of the input address bus.
 * @param inDataWidth The width of the input data bus.
 * @param outAddrWidth The width of the output address bus.
 * @param outDataWidth The width of the output data bus.
 * @param depth The number of entries in the cache.
 * @param offset The offset of the output address.
 */
case class CacheConfig(inAddrWidth: Int,
                       inDataWidth: Int,
                       outAddrWidth: Int,
                       outDataWidth: Int,
                       depth: Int,
                       offset: Int = 0) {
  /** The width of the words in a cache line */
  val wordWidth = inDataWidth
  /** The number of words in a cache line */
  val numWords = outDataWidth / inDataWidth
  /** The width of a cache address index */
  val indexWidth = log2Ceil(depth)
  /** The width of a cache address offset */
  val offsetWidth = log2Ceil(numWords)
  /** The width of a cache tag */
  val tagWidth = outAddrWidth - indexWidth - offsetWidth
  /** The width of a cache line */
  val lineWidth = wordWidth * numWords
}

/** Represents the data stored in each cache entry. */
class CacheLine(private val config: CacheConfig) extends Bundle {
  /** The words in the cache line */
  val words = Vec(config.numWords, Bits(config.wordWidth.W))
}

/** Represents the location of a word stored within the cache. */
class CacheAddress(private val config: CacheConfig) extends Bundle {
  /** The most significant bits of the address */
  val tag = UInt(config.tagWidth.W)
  /** The index of the cache entry within the cache */
  val index = UInt(config.indexWidth.W)
  /** The offset of the word within the cache line */
  val offset = UInt(config.offsetWidth.W)
}

/** Represents an entry stored in the cache. */
class CacheEntry(private val config: CacheConfig) extends Bundle {
  /** Flag to indicate whether the cache entry is valid */
  val valid = Bool()
  /** Flag to indicate whether the cache entry is dirty */
  val dirty = Bool()
  /** The most significant bits of the address */
  val tag = UInt(config.tagWidth.W)
  /** The cache line */
  val line = new CacheLine(config)
}

/**
 * A direct-mapped cache memory.
 *
 * A cache can be used to speed up access to a high latency memory device (e.g. SDRAM), by keeping a
 * copy of frequently accessed data. It supports both read and write operations.
 *
 * Cache entries are stored in BRAM, which means that reading/writing to a memory address that is
 * already stored in the cache (cache hit) only takes one clock cycle.
 *
 * Reading/writing to memory address that isn't stored in the cache (cache miss) is slower, as the
 * data must first be fetched from the high latency memory device (line fill). Although, subsequent
 * reads/writes to the same address will be much faster.
 *
 * @param config The cache configuration.
 */
class CacheMem(config: CacheConfig) extends Module {
  val io = IO(new Bundle {
    /** Input port */
    val in = Flipped(AsyncReadWriteMemIO(config.inAddrWidth, config.inDataWidth))
    /** Output port */
    val out = AsyncReadWriteMemIO(config.outAddrWidth, config.outDataWidth)
    /** Debug port */
    val debug = Output(new Bundle {
      val idle = Bool()
      val check = Bool()
      val lineFill = Bool()
      val evict = Bool()
    })
  })

  // Input/output port aliases
  val in = io.in
  val out = io.out
  val inAddr = io.in.addr.asTypeOf(new CacheAddress(config))

  // States
  val initState :: idleState :: checkState :: lineFillState :: evictState :: Nil = Enum(5)

  // Wires
  val entry = Wire(new CacheEntry(config))

  // Registers
  val stateReg = RegInit(initState)
  val writeReg = RegEnable(in.wr, stateReg === idleState)
  val addrReg = RegEnable(inAddr, stateReg === idleState)
  val dataReg = RegEnable(in.din, stateReg === idleState)

  // Counters
  val (counterValue, counterWrap) = Counter(stateReg === initState, config.depth)

  // Cache entry memory
  val mem = SyncReadMem(config.depth, new CacheEntry(config), SyncReadMem.WriteFirst)

  // Control signals
  val dirty = entry.dirty && entry.tag =/= addrReg.tag
  val hit = entry.valid && entry.tag === addrReg.tag
  val miss = !hit
  val waitReq = (dirty || miss) && out.waitReq
  val request = in.rd || in.wr

  // Set output address to cache entry address during an eviction, otherwise use the cache address
  val outAddr = Mux(stateReg === checkState && dirty, entry.tag, addrReg.tag) ## addrReg.index

  // Read cache entry during the idle state, so it's valid in time for the check state
  entry := mem.read(inAddr.index, stateReg === idleState)

  // Initialize cache entries
  when(stateReg === initState) {
    mem.write(counterValue, 0.U.asTypeOf(new CacheEntry(config)))
  }

  // Fill cache line from memory
  when(stateReg === lineFillState && out.valid) {
    entry.valid := true.B
    entry.dirty := false.B
    entry.tag := addrReg.tag
    entry.line := out.dout.asTypeOf(new CacheLine(config))
    mem.write(addrReg.index, entry)
  }

  // Update word in the cache line
  when(stateReg === checkState && hit && writeReg) {
    entry.dirty := true.B
    entry.line.words(addrReg.offset) := dataReg
    mem.write(addrReg.index, entry)
  }

  // FSM
  switch(stateReg) {
    // Initialize cache
    is(initState) {
      when(counterWrap) { stateReg := idleState }
    }

    // Wait for a request
    is(idleState) {
      when(request) { stateReg := checkState }
    }

    // Check cache entry
    is(checkState) {
      when(waitReq) {
        // Wait for request
      }.elsewhen(dirty) {
        stateReg := evictState
      }.elsewhen(miss) {
        stateReg := lineFillState
      }.otherwise {
        stateReg := idleState
      }
    }

    // Fill cache line
    is(lineFillState) {
      when(out.valid) { stateReg := checkState }
    }

    // Evict dirty cache entry
    is(evictState) {
      when(!out.waitReq) { stateReg := lineFillState }
    }
  }

  // Outputs
  in.valid := stateReg === checkState && hit && !writeReg
  in.waitReq := stateReg =/= idleState && request
  in.dout := entry.line.words(addrReg.offset)
  out.rd := (stateReg === checkState && !dirty && miss) || stateReg === evictState
  out.wr := stateReg === checkState && dirty
  out.addr := outAddr + config.offset.U
  out.din := entry.line.asUInt
  io.debug.idle := stateReg === idleState
  io.debug.check := stateReg === checkState
  io.debug.lineFill := stateReg === lineFillState
  io.debug.evict := stateReg === evictState

  printf(p"CacheMem(state: $stateReg, write: $writeReg, addr: $addrReg, data: $dataReg, entry: $entry, hit: $hit, dirty: $dirty)\n")
}

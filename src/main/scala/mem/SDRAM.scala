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
 * An interface for reading and writing to SDRAM.
 *
 * @param config The SDRAM configuration.
 */
class SDRAMIO private (config: SDRAMConfig) extends Bundle {
  /** Clock enable */
  val cke = Output(Bool())
  /** Chip select */
  val cs = Output(Bool())
  /** Row address strobe */
  val ras = Output(Bool())
  /** Column address strobe */
  val cas = Output(Bool())
  /** Write enable */
  val we = Output(Bool())
  /** Output enable */
  val oe = Output(Bool())
  /** Bank bus */
  val bank = Output(UInt(config.bankWidth.W))
  /** Address bus */
  val addr = Output(UInt(config.addrWidth.W))
  /** Data input bus */
  val din = Output(Bits(config.dataWidth.W))
  /** Data output bus */
  val dout = Input(Bits(config.dataWidth.W))
}

object SDRAMIO {
  def apply(config: SDRAMConfig) = new SDRAMIO(config)
}

/**
 * Represents the SDRAM configuration.
 *
 * @param clockFreq The SDRAM clock frequency (Hz).
 * @param addrWidth The address bus width.
 * @param dataWidth The data bus width.
 * @param bankWidth The bank width.
 * @param rowWidth The row width.
 * @param colWidth The column width.
 * @param burstLength The number of words to be transferred during a read/write.
 * @param burstType The burst type (0=sequential, 1=interleaved).
 * @param casLatency The delay in clock cycles, between the start of a read command and the
 *                   availability of the output data.
 * @param writeBurstMode The write burst mode (0=burst, 1=single).
 * @param tINIT The initialization delay (ns). Typically around 200us.
 * @param tMRD The mode register cycle time (ns).
 * @param tRC The row cycle time (ns).
 * @param tRCD The RAS to CAS delay (ns).
 * @param tRP The precharge to activate delay (ns).
 * @param tWR The write recovery time (ns).
 * @param tREFI The average refresh interval (ns).
 */
case class SDRAMConfig(clockFreq: Double,
                       addrWidth: Int = 13,
                       dataWidth: Int = 16,
                       bankWidth: Int = 2,
                       rowWidth: Int = 13,
                       colWidth: Int = 9,
                       burstLength: Int = 1,
                       burstType: Int = 0,
                       casLatency: Int = 2,
                       writeBurstMode: Int = 0,
                       tINIT: Double = 200000,
                       tMRD: Double = 12,
                       tRC: Double = 60,
                       tRCD: Double = 18,
                       tRP: Double = 18,
                       tWR: Double = 12,
                       tREFI: Double = 7800) {
  /** The logical address bus width (i.e. the total width of the address space). */
  val logicalAddrWidth = bankWidth+rowWidth+colWidth

  /** The logical data bus width (i.e. the total width of all words bursted from SDRAM). */
  val logicalDataWidth = dataWidth*burstLength

  /** The SDRAM clock period (ns). */
  val clockPeriod = 1/clockFreq*1000000000

  /** The number of clock cycles to wait before selecting the device. */
  val deselectWait = (tINIT/clockPeriod).ceil.toLong

  /** The number of clock cycles to wait for a PRECHARGE command. */
  val prechargeWait = (tRP/clockPeriod).ceil.toLong

  /** The number of clock cycles to wait for a REFRESH command. */
  val refreshWait = (tRC/clockPeriod).ceil.toLong

  /** The number of clock cycles to wait for a MODE command. */
  val modeWait = (tMRD/clockPeriod).ceil.toLong

  /** The number of clock cycles to wait for an ACTIVE command. */
  val activeWait = (tRCD/clockPeriod).ceil.toLong

  /** The number of clock cycles to wait for a READ command. */
  val readWait = casLatency+burstLength

  /** The number of clock cycles to wait for a WRITE command. */
  val writeWait = burstLength+((tWR+tRP)/clockPeriod).ceil.toLong

  /** The number of clock cycles between REFRESH commands. */
  val refreshInterval = (tREFI/clockPeriod).floor.toLong

  /** The maximum value of the wait counter. */
  val waitCounterMax = 1 << log2Ceil(deselectWait+prechargeWait+refreshWait+refreshWait)

  /** The maximum value of the refresh counter. */
  val refreshCounterMax = 1 << log2Ceil(refreshInterval)
}

/**
 * Handles reading/writing data to a SDRAM memory device.
 *
 * @param config The SDRAM configuration.
 */
class SDRAM(config: SDRAMConfig) extends Module {
  val io = IO(new Bundle {
    /** Memory port */
    val mem = Flipped(AsyncReadWriteMemIO(config.logicalAddrWidth, config.logicalDataWidth))
    /** SDRAM port */
    val sdram = SDRAMIO(config)
    /** Debug port */
    val debug = Output(new Bundle {
      val init = Bool()
      val mode = Bool()
      val idle = Bool()
      val active = Bool()
      val read = Bool()
      val write = Bool()
      val refresh = Bool()
    })
  })

  // States
  val initState :: modeState :: idleState :: activeState :: readState :: writeState :: refreshState :: Nil = Enum(7)

  // Commands
  val modeCommand :: refreshCommand :: prechargeCommand :: activeCommand :: writeCommand :: readCommand :: stopCommand :: nopCommand :: deselectCommand :: Nil = Enum(9)

  // Convert input address from the a memory address to the a SDRAM address
  val addr = io.mem.addr << log2Ceil(config.burstLength)

  // Wires
  val nextState = Wire(UInt())
  val nextCommand = Wire(UInt())
  val latchRequest = WireInit(false.B)
  val latchData = WireInit(false.B)

  // Registers
  val stateReg = RegNext(nextState, initState)
  val commandReg = RegNext(nextCommand, nopCommand)
  val writeReg = RegEnable(io.mem.wr, latchRequest)
  val addrReg = RegEnable(addr, latchRequest)
  val dataReg = Reg(Vec(config.burstLength, Bits(config.dataWidth.W)))

  // Set mode opcode
  val mode =
    0.U(3.W) ## // unused
    config.writeBurstMode.U(1.W) ##
    0.U(2.W) ## // unused
    config.casLatency.U(3.W) ##
    config.burstType.U(1.W) ##
    log2Ceil(config.burstLength).U(3.W)

  // Extract address components
  val bank = addrReg(config.colWidth+config.rowWidth+config.bankWidth-1, config.colWidth+config.rowWidth)
  val row = addrReg(config.colWidth+config.rowWidth-1, config.colWidth)
  val col = addrReg(config.colWidth-1, 0)

  // Counters
  val (waitCounterValue, _) = Counter(0 until config.waitCounterMax,
    reset = nextState =/= stateReg
  )
  val (refreshCounterValue, _) = Counter(0 until config.refreshCounterMax,
    enable = stateReg =/= initState && stateReg =/= modeState,
    reset = stateReg === refreshState && waitCounterValue === 0.U
  )

  // Control signals
  val modeDone = waitCounterValue === (config.modeWait-1).U
  val activeDone = waitCounterValue === (config.activeWait-1).U
  val readDone = waitCounterValue === (config.readWait-1).U
  val writeDone = waitCounterValue === (config.writeWait-1).U
  val refreshDone = waitCounterValue === (config.refreshWait-1).U
  val refresh = refreshCounterValue >= (config.refreshInterval-1).U
  val request = io.mem.rd || io.mem.wr

  // Latch request during IDLE, READ, WRITE, and REFRESH commands
  latchRequest :=
    stateReg === idleState ||
    stateReg === readState && readDone ||
    stateReg === writeState && writeDone ||
    stateReg === refreshState && refreshDone

  // Latch data from the SDRAM port. The data is shifted into the data register.
  when(stateReg === readState || stateReg === writeState) {
    dataReg := io.sdram.dout +: dataReg.init
  }

  // Latch input from the memory port. The data is split into words and assigned to the data
  // register.
  when(latchRequest && io.mem.wr) {
    dataReg := Seq.tabulate(config.burstLength) { n =>
      io.mem.din(((n+1)*config.dataWidth)-1, n*config.dataWidth)
    }
  }

  // Default to the previous state
  nextState := stateReg

  // Default to a NOP
  nextCommand := nopCommand

  // FSM
  switch(stateReg) {
    // Execute initialization sequence
    is(initState) {
      when(waitCounterValue === 0.U) {
        nextCommand := deselectCommand
      }.elsewhen(waitCounterValue === (config.deselectWait-1).U) {
        nextCommand := prechargeCommand
      }.elsewhen(waitCounterValue === (config.deselectWait+config.prechargeWait-1).U) {
        nextCommand := refreshCommand
      }.elsewhen(waitCounterValue === (config.deselectWait+config.prechargeWait+config.refreshWait-1).U) {
        nextCommand := refreshCommand
      }.elsewhen(waitCounterValue === (config.deselectWait+config.prechargeWait+config.refreshWait+config.refreshWait-1).U) {
        nextCommand := modeCommand
        nextState := modeState
      }
    }

    // Set mode register
    is(modeState) {
      when(modeDone) { nextState := idleState }
    }

    // Wait for request
    is(idleState) {
      when(refresh) {
        nextCommand := refreshCommand
        nextState := refreshState
      }.elsewhen(request) {
        nextCommand := activeCommand
        nextState := activeState
      }
    }

    // Activate row
    is(activeState) {
      when(activeDone) {
        when(writeReg) {
          nextCommand := writeCommand
          nextState := writeState
        }.otherwise {
          nextCommand := readCommand
          nextState := readState
        }
      }
    }

    // Execute read command
    is(readState) {
      when(readDone) {
        when(refresh) {
          nextCommand := refreshCommand
          nextState := refreshState
        }.elsewhen(request) {
          nextCommand := activeCommand
          nextState := activeState
        }.otherwise {
          nextState := idleState
        }
      }
    }

    // Execute write command
    is(writeState) {
      when(writeDone) {
        when(refresh) {
          nextCommand := refreshCommand
          nextState := refreshState
        }.elsewhen(request) {
          nextCommand := activeCommand
          nextState := activeState
        }.otherwise {
          nextState := idleState
        }
      }
    }

    // Execute refresh command
    is(refreshState) {
      when(refreshDone) {
        when(request) {
          nextCommand := activeCommand
          nextState := activeState
        }.otherwise {
          nextState := idleState
        }
      }
    }
  }

  // Outputs
  io.mem.waitReq := nextState =/= activeState && request
  io.mem.dout := dataReg.asUInt
  io.mem.valid := RegNext(stateReg === readState && readDone)
  io.sdram.cke := !(stateReg === initState && waitCounterValue === 0.U)
  io.sdram.cs := commandReg(3)
  io.sdram.ras := commandReg(2)
  io.sdram.cas := commandReg(1)
  io.sdram.we := commandReg(0)
  io.sdram.oe := stateReg === writeState
  io.sdram.bank := Mux(stateReg === activeState || stateReg === readState || stateReg === writeState, bank, 0.U)
  io.sdram.addr := MuxLookup(stateReg, 0.U, Seq(
    initState -> "b0010000000000".U,
    modeState -> mode,
    activeState -> row,
    readState -> "b0010".U ## col,
    writeState -> "b0010".U ## col
  ))
  io.sdram.din := dataReg.last
  io.debug.init := stateReg === initState
  io.debug.mode := stateReg === modeState
  io.debug.idle := stateReg === idleState
  io.debug.active := stateReg === activeState
  io.debug.read := stateReg === readState
  io.debug.write := stateReg === writeState
  io.debug.refresh := stateReg === refreshState

  printf(p"SDRAM(state: $stateReg, nextState: $nextState, command: $commandReg, nextCommand: $nextCommand, counter: $waitCounterValue, dout: $dataReg, valid: ${io.mem.valid})\n")
}

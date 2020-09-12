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
 * @param bankWidth The width of the bank bus.
 * @param addrWidth The width of the address bus.
 * @param dataWidth The width of the data bus.
 */
class SDRAMIO(bankWidth: Int, addrWidth: Int, dataWidth: Int) extends Bundle {
  /** Clock enable */
  val cen = Output(Bool())
  /** Row address strobe */
  val ras = Output(Bool())
  /** Column address strobe */
  val cas = Output(Bool())
  /** Write enable */
  val we = Output(Bool())
  /** Bank bus */
  val bank = Output(UInt(bankWidth.W))
  /** Address bus */
  val addr = Output(UInt(addrWidth.W))
  /** Data input bus */
  val din = Output(Bits(dataWidth.W))
  /** Data output bus */
  val dout = Input(Bits(dataWidth.W))

  override def cloneType: this.type = new SDRAMIO(bankWidth, addrWidth, dataWidth).asInstanceOf[this.type]
}

/**
 * Represents the SDRAM configuration.
 *
 * @param clockFreq The SDRAM clock frequency (MHz).
 * @param burstLength The number of words to be transferred during a read/write.
 * @param burstType The burst type (0=sequential, 1=interleaved).
 * @param casLatency The delay in clock cycles, between the start of a read
 *                   command and the availability of the output data.
 * @param writeBurstMode The write burst mode (0=burst, 1=single).
 * @param tINIT The initialization delay (ns). Typically around 200us.
 * @param tMRD The mode register cycle time (ns).
 * @param tRC The row cycle time (ns).
 * @param tRCD The RAS to CAS delay (ns).
 * @param tRP The precharge to activate delay (ns).
 * @param tWR The write recovery time (ns).
 * @param tREFI The average refresh interval (ns).
 */
case class SDRAMConfig(clockFreq: Double = 100,
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
  /** The SDRAM clock period (ns). */
  val clockPeriod = 1/clockFreq*1000

  /** The number of clock cycles to wait before initializing the device. */
  val initWait = (tINIT/clockPeriod).ceil.toLong

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
}

// TODO: Merge into ReadWriteMemIO
class AsyncReadWriteMemIO private (addrWidth: Int, dataWidth: Int) extends Bundle {
  /** Request */
  val req = Output(Bool())
  /** Acknowledge */
  val ack = Input(Bool())
  /** Write enable */
  val we = Output(Bool())
  /** Address bus */
  val addr = Output(UInt(addrWidth.W))
  /** Data input bus */
  val din = Output(Bits(dataWidth.W))
  /** Data output bus */
  val dout = Input(Bits(dataWidth.W))
  /** Valid */
  val valid = Input(Bool())

  override def cloneType: this.type = new AsyncReadWriteMemIO(addrWidth, dataWidth).asInstanceOf[this.type]
}

object AsyncReadWriteMemIO {
  def apply(addrWidth: Int, dataWidth: Int): AsyncReadWriteMemIO = new AsyncReadWriteMemIO(addrWidth, dataWidth)
}

/**
 * Represents a SDRAM controller.
 *
 * @param config The SDRAM configuration.
 */
class SDRAM(config: SDRAMConfig) extends Module {
  val SDRAM_ADDR_WIDTH = 13
  val SDRAM_DATA_WIDTH = 16
  val BANK_WIDTH = 2
  val ROW_WIDTH = 13
  val COL_WIDTH = 9
  val ADDR_WIDTH = BANK_WIDTH+ROW_WIDTH+COL_WIDTH
  val DATA_WIDTH = SDRAM_DATA_WIDTH*config.burstLength

  val io = IO(new Bundle {
    /** Memory port */
    val mem = Flipped(AsyncReadWriteMemIO(ADDR_WIDTH, DATA_WIDTH))
    /** SDRAM port */
    val sdram = new SDRAMIO(BANK_WIDTH, SDRAM_ADDR_WIDTH, SDRAM_DATA_WIDTH)
    /** Debug port */
    val debug = new Bundle {
      val init = Output(Bool())
      val mode = Output(Bool())
      val idle = Output(Bool())
      val active = Output(Bool())
      val read = Output(Bool())
      val write = Output(Bool())
      val refresh = Output(Bool())
    }
  })

  // States
  val stInit :: stMode :: stIdle :: stActive :: stRead :: stWrite :: stRefresh :: Nil = Enum(7)

  // Commands
  val cmdMode :: cmdRefresh :: cmdPrecharge :: cmdActive :: cmdWrite :: cmdRead :: cmdStop :: cmdNop :: Nil = Enum(8)

  // Wires
  val nextState = WireInit(stInit)
  val nextCmd = WireInit(cmdNop)
  val latchRequest = WireInit(false.B)
  val latchData = WireInit(false.B)

  // Registers
  val stateReg = RegNext(nextState, stInit)
  val cmdReg = RegNext(nextCmd, cmdNop)
  val weReg = RegEnable(io.mem.we, latchRequest)
  val addrReg = RegEnable(io.mem.addr, 0.U(ADDR_WIDTH.W), latchRequest)
  val dataReg = Reg(Vec(config.burstLength, UInt(SDRAM_DATA_WIDTH.W)))

  // Set mode opcode
  val mode =
    "b000".U ## // unused
    config.writeBurstMode.U(1.W) ##
    "b00".U ## // unused
    config.casLatency.U(3.W) ##
    config.burstType.U(1.W) ##
    log2Up(config.burstLength).U(3.W)

  // Extract the address components
  val bank = addrReg(COL_WIDTH+ROW_WIDTH+BANK_WIDTH-1, COL_WIDTH+ROW_WIDTH)
  val row = addrReg(COL_WIDTH+ROW_WIDTH-1, COL_WIDTH)
  val col = addrReg(COL_WIDTH-1, 0)

  // Counters
  val (waitCounterValue, _) = Counter(0 until 32768, reset = nextState =/= stateReg)
  val (refreshCounterValue, _) = Counter(0 until 1024, reset = stateReg === stRefresh && waitCounterValue === 0.U)

  // Flags
  val modeDone = waitCounterValue === (config.modeWait-1).U
  val activeDone = waitCounterValue === (config.activeWait-1).U
  val readDone = waitCounterValue === (config.readWait-1).U
  val writeDone = waitCounterValue === (config.writeWait-1).U
  val refreshDone = waitCounterValue === (config.refreshWait-1).U
  val shouldRefresh = refreshCounterValue === (config.refreshInterval-1).U

  // Latch request during IDLE, READ, WRITE, and REFRESH commands
  latchRequest :=
    stateReg === stIdle ||
    stateReg === stRead && readDone ||
    stateReg === stWrite && writeDone ||
    stateReg === stRefresh && refreshDone

  // Latch output data from the SDRAM port. The output data is shifted into the
  // data register.
  when(stateReg === stRead || stateReg === stWrite) {
    dataReg := io.sdram.dout +: dataReg.init
  }

  // Latch input data from the memory port. The input data is split into words
  // and assigned to the data register.
  when(latchRequest && io.mem.we) {
    dataReg := Seq.tabulate(config.burstLength) { n =>
      io.mem.din(((n+1)*SDRAM_DATA_WIDTH)-1, n*SDRAM_DATA_WIDTH)
    }
  }

  // Default to the previous state
  nextState := stateReg

  // Default to a NOP
  nextCmd := cmdNop

  // FSM
  switch(stateReg) {
    // Execute the initialization sequence
    is(stInit) {
      when(waitCounterValue === (config.initWait-1).U) {
        nextCmd := cmdPrecharge
      }.elsewhen(waitCounterValue === (config.initWait+config.prechargeWait-1).U) {
        nextCmd := cmdRefresh
      }.elsewhen(waitCounterValue === (config.initWait+config.prechargeWait+config.refreshWait-1).U) {
        nextCmd := cmdRefresh
      }.elsewhen(waitCounterValue === (config.initWait+config.prechargeWait+config.refreshWait+config.refreshWait-1).U) {
        nextCmd := cmdMode
        nextState := stMode
      }
    }

    // Write the mode register
    is(stMode) {
      when(modeDone) {
        nextState := stIdle
      }
    }

    // Wait for read/write request
    is(stIdle) {
      when(shouldRefresh) {
        nextCmd := cmdRefresh
        nextState := stRefresh
      }.elsewhen(io.mem.req) {
        nextCmd := cmdActive
        nextState := stActive
      }
    }

    // Activate the row
    is(stActive) {
      when(activeDone) {
        when(weReg) {
          nextCmd := cmdWrite
          nextState := stWrite
        }.otherwise {
          nextCmd := cmdRead
          nextState := stRead
        }
      }
    }

    // Execute a read command
    is(stRead) {
      when(readDone) {
        when(shouldRefresh) {
          nextCmd := cmdRefresh
          nextState := stRefresh
        }.elsewhen(io.mem.req) {
          nextCmd := cmdActive
          nextState := stActive
        }.otherwise {
          nextState := stIdle
        }
      }
    }

    // Execute a write command
    is(stWrite) {
      when(writeDone) {
        when(shouldRefresh) {
          nextCmd := cmdRefresh
          nextState := stRefresh
        }.elsewhen(io.mem.req) {
          nextCmd := cmdActive
          nextState := stActive
        }.otherwise {
          nextState := stIdle
        }
      }
    }

    // Execute a refresh command
    is(stRefresh) {
      when(refreshDone) {
        when(io.mem.req || io.mem.we) {
          nextCmd := cmdActive
          nextState := stActive
        }.otherwise {
          nextState := stIdle
        }
      }
    }
  }

  // Outputs
  io.mem.ack := stateReg === stActive && waitCounterValue === 0.U
  io.mem.dout := dataReg.asUInt
  io.mem.valid := RegNext(stateReg === stRead && readDone)
  io.sdram.cen := stateReg === stInit && waitCounterValue === 0.U
  io.sdram.ras := cmdReg(2)
  io.sdram.cas := cmdReg(1)
  io.sdram.we := cmdReg(0)
  io.sdram.bank := Mux(stateReg === stActive || stateReg === stRead || stateReg === stWrite, bank, 0.U)
  io.sdram.addr := MuxLookup(stateReg, 0.U, Seq(
    stInit -> "b0010000000000".U,
    stMode -> mode,
    stActive -> row,
    stRead -> "b0010".U ## col,
    stWrite -> "b0010".U ## col
  ))
  io.sdram.din := dataReg.last
  io.debug.init := stateReg === stInit
  io.debug.mode := stateReg === stMode
  io.debug.idle := stateReg === stIdle
  io.debug.active := stateReg === stActive
  io.debug.read := stateReg === stRead
  io.debug.write := stateReg === stWrite
  io.debug.refresh := stateReg === stRefresh

  printf(p"SDRAM(state: $stateReg, nextState: $nextState, counter: $waitCounterValue, dout: $dataReg, valid: ${io.mem.valid})\n")
}

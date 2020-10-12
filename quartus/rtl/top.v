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

module top (
  input clk,
  input [1:0] key,
  output [7:0] led,
  output sdram_clk,
  output sdram_cs_n,
  output sdram_cke,
  output sdram_ras_n,
  output sdram_cas_n,
  output sdram_we_n,
  output sdram_dqml,
  output sdram_dqmh,
  output [1:0] sdram_ba,
  output [12:0] sdram_a,
  inout [15:0] sdram_dq

);
  wire sdram_oe;
  wire [15:0] sdram_din;
  wire [15:0] sdram_dout;
  wire reset = !key[0];

  assign sdram_clk = clk;
  assign sdram_dq = sdram_oe ? sdram_din : 16'bZ;
  assign sdram_dout = sdram_dq;

  Demo demo (
    .clock(sys_clk),
    .reset(reset),
    .io_led(led),
    .io_sdram_cke(sdram_cke),
    .io_sdram_cs(sdram_cs_n),
    .io_sdram_ras(sdram_ras_n),
    .io_sdram_cas(sdram_cas_n),
    .io_sdram_we(sdram_we_n),
    .io_sdram_oe(sdram_oe),
    .io_sdram_bank(sdram_ba),
    .io_sdram_addr(sdram_a),
    .io_sdram_din(sdram_din),
    .io_sdram_dout(sdram_dout)
  );
endmodule
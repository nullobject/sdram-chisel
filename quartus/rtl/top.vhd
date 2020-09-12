--   __   __     __  __     __         __
--  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
--  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
--   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
--    \/_/ \/_/   \/_____/   \/_____/   \/_____/
--   ______     ______       __     ______     ______     ______
--  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
--  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
--   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
--    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
--
-- https://joshbassett.info
-- https://twitter.com/nullobject
-- https://github.com/nullobject
--
-- Copyright (c) 2020 Josh Bassett
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library pll;

entity top is
  port (
    clk : in std_logic;

    -- Keys
    key : in std_logic_vector(1 downto 0);

    -- LEDs
    led : out std_logic_vector(7 downto 0);

    -- SDRAM interface
    sdram_clk   : out std_logic;
    sdram_cs_n  : out std_logic;
    sdram_cke   : out std_logic;
    sdram_ras_n : out std_logic;
    sdram_cas_n : out std_logic;
    sdram_we_n  : out std_logic;
    sdram_dqml  : out std_logic;
    sdram_dqmh  : out std_logic;
    sdram_ba    : out unsigned(1 downto 0);
    sdram_a     : out unsigned(12 downto 0);
    sdram_dq    : inout std_logic_vector(15 downto 0)
  );
end top;

architecture arch of top is
  signal sdram_ras : std_logic;
  signal sdram_cas : std_logic;
  signal sdram_we  : std_logic;

  component Demo is
    port (
      clock          : in std_logic;
      reset          : in std_logic;
      io_led         : out std_logic_vector(7 downto 0);
      io_sdram_cen   : out std_logic;
      io_sdram_ras   : out std_logic;
      io_sdram_cas   : out std_logic;
      io_sdram_we    : out std_logic;
      io_sdram_bank  : out unsigned(1 downto 0);
      io_sdram_addr  : out unsigned(12 downto 0);
      io_sdram_din   : out std_logic_vector(15 downto 0);
      io_sdram_dout  : in std_logic_vector(15 downto 0)
    );
  end component Demo;
begin
  demo_inst : component Demo
  port map (
    clock          => clk,
    reset          => not key(0),
    io_led         => led,
    io_sdram_cen   => sdram_cke,
    io_sdram_ras   => sdram_ras,
    io_sdram_cas   => sdram_cas,
    io_sdram_we    => sdram_we,
    io_sdram_bank  => sdram_ba,
    io_sdram_addr  => sdram_a,
    io_sdram_din   => sdram_dq,
    io_sdram_dout  => sdram_dq
  );

  sdram_clk <= clk;
  sdram_cs_n <= '0';
  sdram_ras_n <= not sdram_ras;
  sdram_cas_n <= not sdram_cas;
  sdram_we_n <= not sdram_we;
  sdram_dqml <= '0';
  sdram_dqmh <= '0';
end arch;

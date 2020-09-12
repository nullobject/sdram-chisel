#!/bin/bash

cd quartus
quartus_sh --flow compile sdram && quartus_pgm -m jtag -c 1 -o "p;output_files/sdram.sof@1"

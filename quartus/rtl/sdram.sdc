set sys_clk "pll:pll|altpll:altpll_component|pll_altpll:auto_generated|wire_pll1_clk[0]"
set cpu_clk "pll:pll|altpll:altpll_component|pll_altpll:auto_generated|wire_pll1_clk[1]"

create_clock -name clk -period 20 [get_ports clk]

derive_pll_clocks -use_net_name
derive_clock_uncertainty

# This is tAC in the data sheet
set_input_delay -clock $sys_clk -max [expr 6.0 + 0.5] [get_ports sdram_dq[*]]

# This is tOH in the data sheet
set_input_delay -clock $sys_clk -min 2.5 [get_ports sdram_dq[*]]

# This is tIS in the data sheet (setup time)
set_output_delay -clock $sys_clk -max 1.5 [get_ports sdram_*]

# This is tIH in the data sheet (hold time)
set_output_delay -clock $sys_clk -min 0.8 [get_ports sdram_*]

# Constrain I/O ports
set_false_path -from * -to [get_ports {key*}]
set_false_path -from * -to [get_ports {led*}]

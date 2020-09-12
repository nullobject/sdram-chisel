.PHONY: test build program clean

test:
	sbt test

build:
	sbt compile run
	cd quartus; quartus_sh --flow compile sdram

program:
	cd quartus; quartus_pgm -m jtag -c 1 -o "p;output_files/sdram.sof@2"

clean:
	rm -rf project/target rtl/ChiselTop.v target test_run_dir quartus/db quartus/incremental_db quartus/output_files

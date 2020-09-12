# YMZ280B Chisel


## ADPCM Files

To generate MIF files:

    srec_cat u6.bin -binary -crop 0x000 0x2000 -o pcm.mif -mif 8

To play the DoDonPachi sound ROM using Sox:

    play -t vox -r 22k u6.bin 

## License

This project is licensed under the MIT license. See the LICENSE file for more details.

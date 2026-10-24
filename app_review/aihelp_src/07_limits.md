## 7. Limits (computed from the app's own validator; **the game crashes/closes when a "fatal" one is exceeded**)

{{LIMITS}}

Other facts: ANOs referenced by links are 12-bit (max 4095); HDAT 4 values are 16-bit (max 32767); reaction table columns are 1 byte (ANO −128..127, voice 1..127 effective, durations 0..255, speeds −128..127); sprite index must exist in the CNV or the frame draws nothing; reaction slots of a character: 20 (HDAT 14..33), and the `$HDAT_14+n` block must already exist in `000.bin.txt` to be written by the app (it creates the missing `$REACAO_DANO_n` inside it).

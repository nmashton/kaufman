# kaufman-justeson

A script to process Kaufman & Justeson's 2003
[A Preliminary Mayan Etymological Dictionary](http://www.famsi.org/reports/01051/index.html)
into a form more suitable for further computational manipulation.

Includes plaintext copy of the dictionary, scraped from the source PDF
by means of [pdfminer](http://www.unixuser.org/~euske/python/pdfminer/index.html).
This text file has been trivially cleaned to remove some lines that
cause the script problems.

## Usage

1. Download the repository and run `lein deps` in its root directory to download dependencies.

2. Load the kaufman-justeson.core namespace in a REPL.

3. Do something cool with the value of `processed`.

## License

Copyright (C) 2012-14 Neil M. Ashton.

Distributed under the Eclipse Public License, the same as Clojure.

# kaufman-justeson

A script to process Kaufman & Justeson's 2003
[A Preliminary Mayan Etymological Dictionary](http://www.famsi.org/reports/01051/index.html)
into a form more suitable for further computational manipulation.

Includes plaintext copy of the dictionary, scraped from the source PDF
by means of [pdfminer](http://www.unixuser.org/~euske/python/pdfminer/index.html).
This text file has been trivially cleaned to remove some lines that
cause the script problems.

## Usage

Download the repository and run `lein deps` in its root directory to download dependencies.

Run `lein run` to generate a CSV `kaufman-justeson.csv` in
the project root directory. 

## License

Copyright (C) 2012-14 Neil M. Ashton.

Distributed under the Eclipse Public License, the same as Clojure.

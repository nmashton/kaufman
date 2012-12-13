# kaufman

A script to process Kaufman & Justeson's 2003
[A Preliminary Mayan Etymological Dictionary](http://www.famsi.org/reports/01051/index.html)
into a form more suitable for further computational manipulation.

kaufman.core contains two versions of the data: `processed`, a
nested map which mirrors the hierarchical structure of the source document;
and `relational`, a flattened sequence of maps in which the hierarchical
structure of the source is encoded in the values of attributes. The
latter form is intended to be easy to transform into a relational database.

Includes plaintext copy of the dictionary, scraped from the source PDF
by means of [pdfminer](http://www.unixuser.org/~euske/python/pdfminer/index.html).
This text file has been trivially cleaned to remove some lines that
cause the script problems.

## Usage

1. Download the repository and run `lein deps` in its root directory to download dependencies.

2. Load the kaufman.core namespace in a REPL. Wait for the values of
`processed` and `relational` to finish computing, which takes a while.

3. Do whatever you want with those values.

## License

Copyright (C) 2012 Neil M. Ashton.

Distributed under the Eclipse Public License, the same as Clojure.

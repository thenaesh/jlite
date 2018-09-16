The makefile structure from the original template project has been preserved.

To run, just type `make`.

This will generate the parser and then run it on all 5 provided sample files:
* sample.1.pl
* sample.2.pl
* sample.3.pl
* sample.4.pl
* sample.5.pl

The output for each file is a parse tree in a JSON-like format.
This output is written directly to STDOUT.

This output is the result of running the `toString()` method on the AST classes
defined in AST.java. These classes are generated and built up during parsing.
Each `AST` object has a `kind` (what it is) and some `operands` (subtrees of
the parse tree).

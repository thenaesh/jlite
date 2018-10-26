Sample Programs:
    sample1.pl and sample2.pl fails the typecheck
    the remaining samples all pass the typecheck

Running make prints out, for each file:
    1) generated IR3 code
    2) AST (which is valid JSON) containing type information

The AST classes are augmented with 3 functions:
    1) void distinctNamesCheck() throws DistinctNamesCheckingException
    2) LocalEnvironment typeCheck(ClassDescriptors, LocalEnvironment) throws TypeCheckingException
    3) ArrayList<IR3> genIR

Typechecking is performed in 2 passes: the first to build the ClassDescriptors object and the second to actually perform the typechecking.
IR3 generation is performed by a postorder traversal of the AST nodes, aided by some IR3 classes.
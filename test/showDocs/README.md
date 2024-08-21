# APIDiff `showDocs`

`showDocs` is a utility to generate HTML files that show the parts of 
API documentation files that are extracted by the `APIReader` component of APIDiff.

The utility can be built by the rules in `showDocs.gmk`, to generate `showDocs.jar`.

## show-demo-docs

The utility can be run on the classes in `demo-src` by the rules in `showDocs.gmk`.
The rules require that the makefile variable `JDK`_N_`HOME` variable must be set 
for any JDK for which the output is to be generated.

`show-demo-docs.sh` is a demonstration script to show how the `JDK`_N_`HOME`
variables can be set up and the makefile rules invoked.

## show-jdk-docs

`show-jdk-docs.sh` is a script to run `showDocs` on JDK API documentation
for specified versions of JDK.  The documentation must be provided in an
enclosing directory, containing subdirectories which each contain the 
documentation for a version of JDK.
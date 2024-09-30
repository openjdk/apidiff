# Building _apidiff_

The fundamental way to build _apidiff_ is with GNU `make`, although there is
a convenient wrapper script `make/build.sh` to help download the necessary
dependencies before invoking `make`. Once the dependencies have been downloaded,
you can also configure an IDE to build the tool and run the tests.

_apidiff_ has various external dependencies:

* _JDK_: must be at least JDK 17
* _Java Diff Utils_
* _HtmlCleaner_
* _Daisy Diff_
* _JUnit_ (for testing only)

## Using `make/build.sh`

`make/build.sh` is a script that can download the necessary dependencies
for _apidiff_ and then run `make`.  You can configure all values used
by the script by setting environment variables; you can also configure
some commonly used options with command-line argume nts for the script.

The `make/build.sh` script reads the details of the dependencies from
a file, which defaults to `make/version-numbers`, although an alternate
file can be specified, depending on the build environment.

The script supports the following build scenarios:

* Download dependencies from standard default locations such as Maven Central
  and Google Code Archive. This is the default.

* Download dependencies from other available locations, such as an instance of
  Artifactory. The details can be specified in an alternate `version-numbers`
  file.

* Use local copies of the dependencies on the same machine.
  The details can be specified in an alternate `version-numbers` file,
  or you can bypass the script entirely and invoke `make` directly.

For more details, see the comments in `make/build.sh` and use the `--help`
option when running the script.

The makefile provides the following targets:

* `build`: build _apidiff_

   Requires the following to be set:
   `JDKHOME`, `JAVA_DIFF_UTILS_JAR`, `JAVA_DIFF_UTILS_LICENSE`,
   `DAISYDIFF_JAR`, `DAISYDIFF_LICENSE`,
   `HTMLCLEANER_JAR`, `HTMLCLEANER_LICENSE`.

* `test`: run tests

    Requires `JUNIT_JAR` to be set.

* `clean`: delete the `build` directory and its contents

* `images`: create bundles for uploading to an available server

* `sanity`: show the settings of the standard variables.

### Examples:

    $ JDKHOME=/opt/jdk/17 sh build.sh

    $ JDKHOME=/opt/jdk/17 sh build.sh build test images



## File Locations

| Files                | GNU Make                 | IntelliJ          |
|----------------------|--------------------------|-------------------|
| Default Dependencies | build/deps               | build/deps        |
| Main Classes         | build/classes            | out/production    |
| Test Classes         | build/JUnitTests/classes | out/test          |
| Test Work            | build/JUnitTests/work    | build/test/work   |
| Test Report          | build/JUnitTests/report  |                   |
| Image                | build/images/apidiff     |                   |
| Bundle               | build/images/apidiff.zip |                   |

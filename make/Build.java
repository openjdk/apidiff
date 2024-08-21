/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
# This program will download/build the dependencies for apidiff and then
# build apidiff. Downloaded files are verified against known/specified
# checksums.
#
# The program can be executed directly as a single source-file program
# by the Java launcher, using JDK 12 or later.
#
#     $ /path/to/jdk  make/Build.java  options
#
# For help on command-line options, use the --help option.
# Note: apidiff itself requires JDK 17 or later.

# As a side effect, the program writes a file build/make.sh which
# can subsequently be used directly to build apidiff, bypassing
# the need to rerun this program if all the dependencies are still
# available.

# The default version to use when building apidiff can be found in the
# make/version-numbers file, where the default versions and
# corresponding known checksums for the dependencies are also
# specified. Almost all the defaults can be overridden by setting
# the properties on the command line, or in a properties file,
# or as environment variables.

# For each of the dependency the following steps are applied and the
# first successful one is used:
#
# 1. Check if the dependency is available locally
# 2. Download a prebuilt version of the dependency
# 3. Build the dependency from source, downloading the source archive
#    first
#
# In particular, when not found locally the dependencies will be
# handled as follows:
#
# * JUnit, Java Diff Utils, and HtmlCleaner are by default downloaded from Maven Central.
# * Daisy Diff is by default built from source.
#

# Some noteworthy control variables:
#
# MAVEN_REPO_URL_BASE (e.g. "https://repo1.maven.org/maven2")
#     The base URL for the maven central repository.
#
# APIDIFF_VERSION         (e.g. "1.0")
# APIDIFF_VERSION_STRING  (e.g. "apidiff-1.0+8"
# APIDIFF_BUILD_NUMBER    (e.g. "8")
# APIDIFF_BUILD_MILESTONE (e.g. "dev")
#     The version information to use for when building apidiff.
#     Additional arguments to pass to make when building apidiff.
#
# RM, TAR, UNZIP
#     Paths to standard POSIX commands.

# The control variables for dependencies are on the following general
# form (not all of them are relevant for all dependencies):
#
# <dependency>_URL (e.g. DAISYDIFF_BIN_ARCHIVE_URL)
#     The full URL for the dependency.
#
# <dependency>_URL_BASE (e.g. DAISYDIFF_BIN_ARCHIVE_URL_BASE)
#     The base URL for the dependency. Requires additional dependency
#     specific variables to be specified.
#
# <dependency>_CHECKSUM (e.g. DAISYDIFF_BIN_ARCHIVE_CHECKSUM)
#     The expected checksum of the download file.
#

# The below outlines the details of how the dependencies are
# handled. For each dependency the steps are tried in order and the
# first successful one will be used.
#
# JDK
#     Checksum variables:
#         JDK_ARCHIVE_CHECKSUM: checksum of binary archive
#
#     1. JAVA_HOME
#         The path to the JDK.
#     2a. JDK_ARCHIVE_URL
#         The full URL for the archive.
#     2b. JDK_ARCHIVE_URL_BASE + JDK_VERSION + JDK_BUILD_NUMBER + JDK_FILE
#         The individual URL components used to construct the full URL.
#
# Java Diff Utils
#     Checksum variables:
#         JAVADIFFUTILS_JAR_CHECKSUM: checksum of jar
#         JAVADIFFUTILS_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. JAVADIFFUTILS_JAR + JAVADIFFUTILS_LICENSE
#         The path to java-diff-utils.jar and LICENSE.txt respectively.
#     2a. JAVADIFFUTILS_JAR_URL
#         The full URL for the jar.
#     2b. JAVADIFFUTILS_JAR_URL_BASE + JAVADIFFUTILS_VERSION + JAVADIFFUTILS_FILE
#         The individual URL components used to construct the full URL.
#
# Daisy Diff
#     Checksum variables:
#         DAISYDIFF_BIN_ARCHIVE_CHECKSUM: checksum of binary archive
#         DAISYDIFF_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. DAISYDIFF_JAR + DAISYDIFF_LICENSE
#         The path to daisydiff.jar and LICENSE.txt respectively.
#     2a. DAISYDIFF_JAR_URL
#         The full URL for the jar.
#     2b. DAISYDIFF_JAR_URL_BASE + DAISYDIFF_BIN_VERSION + DAISYDIFF_FILE
#         The individual URL components used to construct the full URL.
#
# Html Cleaner
#     Checksum variables:
#         HTMLCLEANER_JAR_CHECKSUM: checksum of jar
#         HTMLCLEANER_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. HTMLCLEANER_JAR + HTMLCLEANER_LICENSE
#         The path to htmlcleaner.jar and licence.txt respectively.
#     2a. HTMLCLEANER_JAR_URL
#         The full URL for the jar.
#     2b. HTMLCLEANER_JAR_URL_BASE + HTMLCLEANER_VERSION + HTMLCLEANER_FILE
#         The individual URL components used to construct the full URL.
#
# JUnit, for running self-tests
#     Checksum variables:
#         JUNIT_JAR_CHECKSUM: checksum of binary archive
#
#     1. JUNIT_JAR + JUNIT_LICENSE
#         The path to junit.jar and LICENSE respectively.
#     2a. JUNIT_JAR_URL
#         The full URL for the jar.
#     2b. JUNIT_JAR_URL_BASE + JUNIT_VERSION + JUNIT_FILE
#         The individual URL components used to construct the full URL.
#
# Some control variables can be overridden by command-line options.
# Use the  --help option for details.
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility to download the dependencies needed to build APIDiff,
 * based on command-line parameters, configuration info in
 * make/build-support/version-numbers, and environment variables.
 *
 * <p>The class can be executed directly by the Java source code launcher,
 * using JDK 17 or later.
 */
public class Build {
    public enum Exit {
        OK, BAD_OPTION, ERROR
    }

    /**
     * Execute the main program.
     *
     * @param args command-line arguments
     */
    public static void main(String... args) {
        try {
            PrintWriter outWriter = new PrintWriter(System.out);
            PrintWriter errWriter = new PrintWriter(System.err, true);
            try {
                try {
                    new Build().run(outWriter, errWriter, args);
                } finally {
                    outWriter.flush();
                }
            } finally {
                errWriter.flush();
            }
            System.exit(Exit.OK.ordinal());
        } catch (BadOption e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(Exit.BAD_OPTION.ordinal());
        } catch (Fault e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(Exit.ERROR.ordinal());
        }
    }

    /**
     * The root directory for the repo containing this class.
     */
    private final Path rootDir;

    /**
     * The minimum version of JDK required to build apidiff.
     */
    private static final int requiredJDKVersion = 17;

    /**
     * Creates an instance of the utility.
     *
     * @throws Fault if an unrecoverable error occurs while determining the root directory
     */
    Build() throws Fault {
        rootDir = getRootDir();
    }

    /**
     * The main worker method for the utility.
     *
     * @param out the stream to which to write any requested output
     * @param err the stream to which to write any logging or error output
     * @param args any command-line arguments
     * @throws BadOption if there is an error in any of the command-line arguments
     * @throws Fault if there is an unrecoverable error
     */
    public void run(PrintWriter out, PrintWriter err, String... args) throws BadOption, Fault {

        // The collection of values specified by the command-line options.
        var options = Options.handle(rootDir, List.of(args));

        // The collection of values derived from command-line options,
        // the make/build-support/version-numbers file, and default values.
        var config = new Config(rootDir, options, out, err);

        var done = false;

        if (options.help) {
            options.showCommandHelp(config.out);
            done = true;
        }

        if (options.showDefaultVersions) {
            showProperties(config.properties, config.out);
            done = true;
        }

        if (options.showConfigDetails) {
            if (config.properties.isEmpty()) {
                config.out.println("no custom configuration values");
            } else {
                showProperties(config.properties, config.out);
            }
            done = true;
        }

        if (done) {
            return;
        }

        DaisyDiff dd;
        var dependencies = List.of(
                new BuildInfo(config),
                dd = new DaisyDiff(config),
                new Equinox(config, dd),
                new HtmlCleaner(config),
                new JavaDiffUtils(config),
                new JUnit(config)
        );

        for (var d : dependencies) {
            d.setup();
        }

        for (var d : dependencies) {
            d.verify();
        }

        var makeScript = config.buildDir.resolve("make.sh");
        new MakeScript(config).writeFile(makeScript, dependencies);

        if (!options.skipMake) {
            config.log("Building");
            config.out.flush();
            config.err.flush();
            execScript(makeScript, config.options.makeArgs);
        }
    }

    /**
     * Writes a set of properties to a given output stream.
     *
     * @param p the properties
     * @param out the output stream
     */
    private static void showProperties(Properties p, PrintWriter out) {
        p.stringPropertyNames().stream()
                .sorted()
                .forEach(k -> out.println(k + "=" + p.getProperty(k)));
    }

    /**
     * Executes a shell script.
     *
     * @param script the path for the script
     * @param args the arguments, if any, for the script
     * @throws Fault if an error occurs while executing the script
     */
    private static void execScript(Path script, List<String> args) throws Fault {
        try {
            Process p = new ProcessBuilder(join("sh", join(script.toString(), args)))
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
            p.waitFor();
            int rc = p.exitValue();
            if (rc != 0) {
                throw new Fault("Error while running " + script + ": rc=" + rc);
            }
        } catch (IOException | InterruptedException e) {
            throw new Fault("error running " + script + ": " + e);
        }
    }

    /**
     * Forms a single list from a string and a list of strings.
     *
     * @param cmd the string
     * @param args the list of strings
     * @return a list formed from the string and list of strings
     */
    private static List<String> join(String cmd, List<String> args) {
        if (args.isEmpty()) {
            return List.of(cmd);
        }
        var list = new ArrayList<String>();
        list.add(cmd);
        list.addAll(args);
        return list;
    }

    /**
     * Returns the root directory for the repo containing this class,
     * as determined by checking enclosing directories for the marker
     * file make/Makefile.
     *
     * @return the root directory
     * @throws Fault if the root directory cannot be determined
     */
    private static Path getRootDir() throws Fault {
        Path dir = getThisClass().getParent();
        Path marker = Path.of("make").resolve("Makefile");
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve(marker))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new Fault("cannot determine root directory");
    }

    /**
     * Returns the path for this class, determined from the location in
     * the class' protection domain.
     *
     * @return the path
     * @throws Fault if an error occurs
     */
    private static Path getThisClass() throws Fault {
        try {
            return Path.of(Build.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new Fault("cannot determine location of this class");
        }
    }

    /**
     * Exception used to report a bad command-line option.
     */
    static class BadOption extends Exception {
        BadOption(String message) {
            super(message);
        }
        BadOption(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception used to report an unrecoverable error.
     */
    static class Fault extends Exception {
        Fault(String message) {
            super(message);
        }
        Fault(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * The set of allowable command-line options.
     */
    enum Option {
        @Description("Show this message")
        HELP("--help -h -help -?", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.help = true;
            }
        },

        @Description("Path to JDK; must be JDK " + requiredJDKVersion + " or higher")
        JDK("--jdk", "<jdk>") {
            @Override
            void process(String opt, String arg, Options options) throws BadOption {
                options.jdk = asExistingPath(arg);
            }
        },

        @Description("Reduce the logging output")
        QUIET("--quiet -q", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.quiet = true;
            }
        },

        @Description("Show default versions of external components")
        SHOW_DEFAULT_VERSIONS("--show-default-versions", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.showDefaultVersions = true;
            }
        },

        @Description("Show configuration details")
        SHOW_CONFIG_DETAILS("--show-config-details", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.showConfigDetails = true;
            }
        },

        @Description("Skip checksum check")
        SKIP_CHECKSUM_CHECK("--skip-checksum-check", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.skipChecksumCheck = true;
            }
        },

        @Description("Skip downloads if file available locally")
        SKIP_DOWNLOAD("--skip-download", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.skipDownloads = true;
            }
        },

        @Description("Skip running 'make' (just download dependencies if needed)")
        SKIP_MAKE("--skip-make", null) {
            @Override
            void process(String opt, String arg, Options options) {
                options.skipMake = true;
            }
        },

        @Description("Provide an alternate file containing dependency version information")
        VERSION_NUMBERS("--version-numbers", "<file>") {
            @Override
            void process(String opt, String arg, Options options) throws BadOption {
                options.versionNumbers = asExistingPath(arg);
            }
        },

        @Description("Provide an alternate file containing configuration details")
        CONFIG_FILE("--config", "<file>") {
            @Override
            void process(String opt, String arg, Options options) throws BadOption, Fault {
                var p = asExistingPath(arg);
                try (BufferedReader r = Files.newBufferedReader(p)) {
                    options.configProperties.load(r);
                } catch (IOException e) {
                    throw new Fault("error reading " + p + ": " + e, e);
                }
            }
        },

        @Description("Override a specific configuration value")
        CONFIG_VALUE("NAME=VALUE", null),

        @Description("Subsequent arguments are for 'make'")
        MAKE_ARGS("--", null);

        @Retention(RetentionPolicy.RUNTIME)
        @interface Description {
            String value();
        }

        final List<String> names;
        final String arg;

        Option(String names, String arg) {
            this.names = Arrays.asList(names.split("\\s+"));
            this.arg = arg;
        }

        void process(String opt, String arg, Options options) throws BadOption, Fault {
            throw new Error("internal error");
        }

        static Path asPath(String p) throws BadOption {
            try {
                return Path.of(p);
            } catch (InvalidPathException e) {
                throw new BadOption("File not found: " + p, e);
            }
        }

        static Path asExistingPath(String p) throws BadOption {
            var path = asPath(p);
            if (!Files.exists(path)) {
                throw new BadOption("File not found: " + p);
            }
            return path;
        }
    }

    /**
     * The set of values given by the command-line options.
     */
    static class Options {
        boolean help;
        Path jdk;
        boolean quiet;
        boolean showDefaultVersions;
        boolean showConfigDetails;
        boolean skipChecksumCheck;
        boolean skipDownloads;
        boolean skipMake;
        private Path versionNumbers;
        private List<String> makeArgs = List.of();

        final private Properties configProperties;

        Options(Path rootDir) {
            var dir = rootDir.resolve("make").resolve("build-support");
            versionNumbers = dir.resolve("version-numbers");
            configProperties = new Properties();
        }

        static Options handle(Path rootDir, List<String> args) throws BadOption, Fault {
            Options options = new Options(rootDir);

            Map<String, Option> map = new HashMap<>();
            for (Option o : Option.values()) {
                o.names.forEach(n -> map.put(n, o));
            }

            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                // currently no support for positional args
                String optName, optValue;
                int eq = arg.indexOf("=");
                if (eq == -1) {
                    optName = arg;
                    optValue = null;
                } else {
                    optName = arg.substring(0, eq);
                    optValue = arg.substring(eq + 1);
                }
                if (optName.isEmpty()) {
                    throw new BadOption("bad argument: " + arg);
                } else {
                    Option opt = map.get(optName);
                    if (opt == null) {
                        if (optName.matches("[A-Z_]+")) {
                            options.configProperties.setProperty(optName, optValue);
                        } else {
                            throw new BadOption("unknown option: " + optName);
                        }
                    } else {
                        if (opt == Option.MAKE_ARGS) {
                            options.makeArgs = args.subList(i + 1, args.size());
                            i = args.size();
                        } else if (opt.arg == null) {
                            // no value for option required
                            if (optValue != null) {
                                throw new BadOption("unexpected value for " + optName + " option: " + optValue);
                            } else {
                                opt.process(optName, null, options);
                            }
                        } else {
                            // value for option required; use next arg if not found after '='
                            if (optValue == null) {
                                if (i + 1 < args.size()) {
                                    optValue = args.get(++i);
                                } else {
                                    throw new BadOption("no value for " + optName + " option");
                                }
                            }
                            opt.process(optName, optValue, options);
                        }
                    }
                }
            }

            return options;
        }

        void showCommandHelp(PrintWriter out) {
            out.println("Usage: java " + Build.class.getSimpleName() + ".java "
                    + "<options> [ -- <make options and target>]" );
            out.println("Options:");
            for (var o : Option.values()) {
                out.println(o.names.stream()
                        .map(n -> n + (o.arg == null ? "" : " " + o.arg))
                        .collect(Collectors.joining(", ", "  ", "")));
                try {
                    Field f = Option.class.getDeclaredField(o.name());
                    Option.Description d = f.getAnnotation(Option.Description.class);
                    out.println("      " + d.value());
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }
        }
    }

    /**
     * The set of configuration values determined from command-line options,
     * the make/build-support/version-numbers file, and any defaults.
     */
    static class Config {
        final Path rootDir;
        final Options options;
        final PrintWriter out;
        final PrintWriter err;
        private final Path buildDir;
        private final Properties properties;
        private final Path jdk;
        private final Map<String, String>sysEnv;

        Config(Path rootDir, Options options, PrintWriter out, PrintWriter err) throws Fault {
            this.rootDir = rootDir;
            this.options = options;
            this.out = out;
            this.err = err;

            this.buildDir = rootDir.resolve("build");

            var versionNumbers = readProperties(options.versionNumbers);
            properties = new Properties(versionNumbers);
            properties.putAll(options.configProperties);

            sysEnv = System.getenv();

            var jdk = options.jdk;
            if (jdk == null) {
                jdk = getPath("JAVA_HOME");
            }
            if (jdk == null) {
                jdk = Path.of(System.getProperty("java.home"));
            }
            this.jdk = jdk;
        }

        void log(String line) {
            if (!options.quiet) {
                err.println(line);
            }
        }

        void error(String lines) {
            lines.lines().forEach(err::println);
        }

        private String getString(String key) {
            var v = properties.getProperty(key);
            if (v == null) {
                if (key.endsWith("_VERSION")
                        || key.endsWith("_CHECKSUM")
                        || key.endsWith("_SRC_TAG")
                        || key.contains("_LICENSE_")) {
                    v = properties.getProperty("DEFAULT_" + key);
                }

                if (v == null) {
                    v = sysEnv.get(key);
                }
            }
            return v;
        }

        private String getRequiredString(String key) throws Fault {
            var v = getString(key);
            if (v == null) {
                throw new Fault("no configuration value for " + key);
            }
            return v;
        }

        public Path getPath(String key) throws Fault {
            String v = getString(key);
            try {
                return v == null ? null : Path.of(v);
            } catch (InvalidPathException e) {
                throw new Fault("bad path: " + v + ": " + e);
            }
        }

        public Path getCommandPath(String name) throws Fault {
            String n = name.toUpperCase(Locale.ROOT);
            Path p = getPath(n);
            if (p == null) {
                p = which(name);
                if (p != null) {
                    properties.put(n, p.toString());
                }
            }
            return p;
        }

        public URL getURL(String key) {
            var v = getString(key);
            try {
                return v == null ? null : new URL(v);
            } catch (MalformedURLException e) {
                throw new Error("Bad URL for " + key + ": " + v + ": " + e);
            }
        }

        private Properties readProperties(Path file) throws Fault {
            Properties p = new Properties();
            if (file != null) {
                try (Reader r = Files.newBufferedReader(file)) {
                    p.load(r);
                } catch (IOException e) {
                    throw new Fault("error reading " + file + ": " + e, e);
                }
            }
            return p;
        }

        Path which(String cmd) throws Fault {
            try {
                Process p = new ProcessBuilder(List.of("which", cmd))
                        .redirectErrorStream(true)
                        .start();
                try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String out = r.lines().collect(Collectors.joining());
                    p.waitFor();
                    int rc = p.exitValue();
                    if (rc != 0) {
                        throw new Fault("error running '" + cmd + "': rc=" + rc);
                    }
                    return out.isEmpty() ? null : Path.of(out);
                }
            } catch (InvalidPathException e) {
                throw new Fault("Unexpected output from 'which " + cmd + "': " + e, e);
            } catch (IOException | InterruptedException e) {
                throw new Fault("error running '" + cmd +"': " + e);
            }
        }
    }
    /**
     * Base class for a dependency to be made available for the build.
     */
    static abstract class Dependency {
        protected final String name;
        protected final Path depsDir;
        protected final Config config;

        private static final String DEFAULT_MAVEN_URL = "https://repo1.maven.org/maven2";

        Dependency(String name, Config config) {
            this.name = name;
            this.config = config;
            this.depsDir = config.rootDir.resolve("build").resolve("deps").resolve(name);
        }

        public abstract void setup() throws Fault;

        public abstract void verify() throws Fault;

        public Map<String, String> getMakeArgs() {
            return Collections.emptyMap();
        }

        protected void createDepsDir() throws Fault {
            try {
                Files.createDirectories(depsDir);
            } catch (IOException e) {
                throw new Fault("Failed to create " + depsDir + ": " + e, e);
            }
        }

        protected Path download(URL url, Path file, String checksum) throws Fault {
            if (Files.isDirectory(file)) {
                file = file.resolve(baseName(url));
            }

            if (Files.isReadable(file) && config.options.skipDownloads) {
                return file;
            }

            config.log("Downloading " + url);
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                throw new Fault("Error creating directory for " + file + ": " + e);
            }

            try (var in = url.openStream()) {
                var md = MessageDigest.getInstance("SHA-1");
                try (var in2 = new DigestInputStream(in, md)) {
                    Files.copy(in2, file, StandardCopyOption.REPLACE_EXISTING);
                }
                var digest = toString(md.digest());
                if ((!config.options.skipChecksumCheck && !checksum.equals("--"))
                        && !checksum.equals(digest)) {
                    config.error("Checksum error for " + url + "\n"
                            + "  expect: " + checksum + "\n"
                            + "  actual: " + digest);
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new Fault("Error downloading " + url + ": " + e, e);
            }

            return file;
        }

        protected Path downloadStandardJar(BiFunction<URL, String, String> makeDefaultURL) throws Fault {
            createDepsDir();
            var prefix = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z_]+", "");
            var jarURL = config.getURL(prefix + "_JAR_URL");
            if (jarURL == null) {
                var jarURLBase = config.getURL(prefix + "_JAR_URL_BASE");
                if (jarURLBase == null) {
                    jarURLBase = config.getURL("MAVEN_REPO_URL_BASE");
                    if (jarURLBase == null) {
                        jarURLBase = newURL(DEFAULT_MAVEN_URL);
                    }
                }
                var version = config.getString(prefix + "_VERSION");
                jarURL = newURL(makeDefaultURL.apply(jarURLBase, version));
            }
            var checksum = config.getString(prefix + "_JAR_CHECKSUM");
            return download(jarURL, depsDir, checksum);
        }

        protected Path unpack(Path archive, Path dir) throws Fault {
            try (var ds = Files.newDirectoryStream(depsDir, Files::isDirectory)) {
                for (var d : ds) {
                    exec(config.getCommandPath("rm"), List.of("-rf", d.toString()));
                }
            } catch (IOException e) {
                throw new Fault("error listing " + depsDir +": " + e, e);
            }

            String s = archive.getFileName().toString();
            if (s.endsWith(".tar.gz")) {
                exec(config.getCommandPath("tar"),
                        List.of("-xzf", archive.toString(), "-C", dir.toString()));
            } else if (s.endsWith(".zip")) {
                // cannot extract files with permissions using standard ZipFile API
                // so resort to the unzip command
                exec(config.getCommandPath("unzip"),
                        List.of("-q", archive.toString(), "-d", dir.toString()));
            } else {
                throw new Fault("unrecognized archive type for file " + archive);
            }

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, Files::isDirectory)) {
                Path bestSoFar = null;
                FileTime bestSoFarTime = null;
                for (var p : ds) {
                    var pTime = Files.getLastModifiedTime(p);
                    if (bestSoFar == null || pTime.compareTo(bestSoFarTime) > 0) {
                        bestSoFar = p;
                    }
                    bestSoFarTime = pTime;
                }
                return bestSoFar;
            } catch (IOException e) {
                throw new Fault("Error listing contents of " + dir + ": " + e, e);
            }
        }

        protected void checkFile(Path file) throws Fault {
            config.log("Checking " + file);
            if (!(Files.isRegularFile(file) && Files.isReadable(file))) {
                throw new Fault(file + " is not a readable file");
            }
        }

        protected void checkDirectory(Path dir) throws Fault {
            config.log("Checking " + dir);
            if (!Files.isDirectory(dir)) {
                throw new Fault(dir + " is not a directory");
            }
        }

        private String toString(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (var b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        protected URL newURL(String u) throws Fault {
            try {
                return new URL(u);
            } catch (MalformedURLException e) {
                throw new Fault("Error creating URL " + u + ": " + e);
            }
        }

        protected String baseName(URL url) {
            var p = url.getPath();
            var lastSep = p.lastIndexOf("/");
            return lastSep == -1 ? p : p.substring(lastSep+ 1);
        }

        protected void exec(Path cmd, List<String> args) throws Fault {
            config.out.flush();
            config.err.flush();
//            System.err.println("exec: " + cmd + " " + args);
            try {
                Process p = new ProcessBuilder(join(cmd.toString(), args))
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .start();
                p.waitFor();
                int rc = p.exitValue();
                if (rc != 0) {
                    throw new Fault("error running '" + cmd + "': rc=" + rc);
                }
            } catch (IOException | InterruptedException e) {
                throw new Fault("error running '" + cmd + "': " + e);
            }
        }
    }

    /**
     * A pseudo-dependency to provide build version details.
     */
    static class BuildInfo extends Dependency {
        String version;
        String buildMileStone;
        String buildNumber;
        String versionString;

        BuildInfo(Config config) {
            super("apidiff", config);
        }

        @Override
        public void setup() throws Fault {
            var prefix = name.toUpperCase(Locale.ROOT);
            version = config.getRequiredString(prefix + "_VERSION");

            buildMileStone = config.getString(prefix + "_BUILD_MILESTONE");
            if (buildMileStone == null) {
                buildMileStone = "dev";
            }

            buildNumber = config.getString(prefix + "_BUILD_NUMBER");
            if (buildNumber == null) {
                buildNumber = "0";
            }

            versionString = config.getString(prefix + "_VERSION_STRING");
            if (versionString == null) {
                versionString = version
                        + (buildMileStone.isEmpty() ? "" : "-" + buildMileStone)
                        + "+" + buildNumber;
            }
        }

        @Override
        public void verify() throws Fault {
            int version;
            if (config.jdk.equals(Path.of(System.getProperty("java.home")))) {
                version = Runtime.version().feature();
            } else {
                var javaCmd = config.jdk.resolve("bin").resolve("java");
                try {
                    Process p = new ProcessBuilder(List.of(javaCmd.toString(), "-version"))
                            .redirectErrorStream(true)
                            .start();
                    try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String out = r.lines()
                                .filter(l -> l.matches(".*(java|openjdk).*"))
                                .findFirst()
                                .orElse("");
                        var m = Pattern.compile("\"(1.)?(?<v>[0-9]+)[^ \"]*\"").matcher(out);
                        if (m.find()) {
                            version = Integer.parseInt(m.group("v"));
                        } else {
                            throw new Fault("version info not found in output from '" + javaCmd + " -version'");
                        }
                    }
                } catch (IOException e) {
                    throw new Fault("Error running '" + javaCmd + " -version': " + e, e);
                }
            }

            if (version < requiredJDKVersion) {
                throw new Fault("JDK " + requiredJDKVersion + " or newer is required to build apidiff");
            }
        }

        @Override
        public Map<String, String> getMakeArgs() {
            return Map.of(
                    "BUILDDIR", config.buildDir.toString(),
                    "JDKHOME", config.jdk.toString(),
                    "BUILD_VERSION", version,
                    "BUILD_MILESTONE", buildMileStone,
                    "BUILD_NUMBER", buildNumber,
                    "BUILD_VERSION_STRING", versionString
            );
        }
    }

    /**
     * DaisyDiff, providing the ability to compare HTML files.
     *
     * @see <a href="https://github.com/guyvdbroeck/daisydiff-1">DaisyDiff</a>
     */
    static class DaisyDiff extends Dependency {
        private Path jar;
        private Path src;
        private Path license;

        static final String DEFAULT_REPO_URL = "https://github.com/guyvdbroeck/daisydiff-1";

        DaisyDiff(Config config) {
            super("daisydiff", config);
        }

        @Override
        public void setup() throws Fault {
            jar = config.getPath("DAISYDIFF_JAR");
            if (jar == null) {
                createDepsDir();
                src = config.getPath("DAISYDIFF_SRC");
                if (src == null) {
                    var srcArchiveURL = config.getURL("DAISYDIFF_SRC_ARCHIVE_URL");
                    if (srcArchiveURL == null) {
                        // build URL from base and version number
                        var srcArchiveURLBase = config.getURL("DAISYDIFF_SRC_ARCHIVE_URL_BASE");
                        if (srcArchiveURLBase == null) {
                            var repoURLBase = config.getURL("DAISYDIFF_REPO_URL_BASE");
                            if (repoURLBase == null) {
                                repoURLBase = newURL(DEFAULT_REPO_URL);
                            }
                            srcArchiveURLBase = repoURLBase;
                        }
                        var srcVersion = config.getString("DAISYDIFF_SRC_VERSION");
                        srcArchiveURL = newURL(srcArchiveURLBase
                                + "/archive/refs/tags/release-"
                                + srcVersion
                                + ".tar.gz");
                    }
                    var checksum = config.getString("DAISYDIFF_SRC_ARCHIVE_CHECKSUM");
                    var srcArchive = download(srcArchiveURL, depsDir, checksum);
                    src = unpack(srcArchive, depsDir).resolve("src");
                }
            }

            license = config.getPath("DAISYDIFF_LICENSE");
            if (license == null) {
                var version = config.getString("DAISYDIFF_LICENSE_VERSION");
                var licenseURL = newURL("https://raw.githubusercontent.com/DaisyDiff/DaisyDiff/"
                        + version
                        + "/LICENSE.txt");
                var licenseChecksum = config.getString("DAISYDIFF_LICENSE_CHECKSUM");
                license = download(licenseURL, depsDir, licenseChecksum);
            }
        }

        @Override
        public void verify() throws Fault {
            if (jar == null && src == null) {
                throw new Fault("jar file or source directory not found for DaisyDiff");
            }
            if (jar != null) {
                checkFile(jar);
            }
            if (src != null) {
                checkDirectory(src);
            }
            checkFile(license);
        }

        @Override
        public Map<String, String> getMakeArgs() {
            var args = new HashMap<String, String>();
            if (jar != null) {
                args.put("DAISYDIFF_JAR", jar.toString());
            }
            if (src != null) {
                args.put("DAISYDIFF_SRC", src.toString());
            }
            args.put("DAISYDIFF_LICENSE", license.toString());
            return args;
        }
    }

    /**
     * Eclipse Equinox, required when building DaisyDiff from source.
     *
     * @see <a href="https://eclipse.dev/equinox/">Common Eclipse Runtime</a>
     */
    static class Equinox extends Dependency {
        Path jar;
        Path license;
        DaisyDiff daisyDiff;

        private static final String DEFAULT_LICENSE_URL = "https://www.eclipse.org/org/documents/epl-v10.html";

        Equinox(Config config, DaisyDiff daisyDiff) {
            super("equinox", config);
            this.daisyDiff = daisyDiff;
        }

        @Override
        public void setup() throws Fault {
            // Only need equinox when building daisydiff from source
            if (daisyDiff.src == null) {
                return;
            }

            jar = config.getPath("EQUINOX_JAR");
            if (jar == null) {
                jar = downloadStandardJar((urlBase, version) ->
                        urlBase
                        + "/org/eclipse/equinox/org.eclipse.equinox.common/"
                        + version
                        + "/org.eclipse.equinox.common-" + version + ".jar"
                );
            }

            license = config.getPath("EQUINOX_LICENSE");
            if (license == null) {
                var licenseURL = newURL(DEFAULT_LICENSE_URL);
                var licenseChecksum = config.getString("EQUINOX_LICENSE_CHECKSUM");
                license = download(licenseURL, depsDir, licenseChecksum);
            }
        }

        @Override
        public void verify() throws Fault {
            checkFile(jar);
            checkFile(license);
        }

        @Override
        public Map<String, String> getMakeArgs() {
            return daisyDiff.src == null
                    ? Collections.emptyMap()
                    : Map.of(
                    "EQUINOX_JAR", jar.toString(),
                    "EQUINOX_LICENSE", license.toString());
        }
    }

    /**
     * HtmlCleaner, to transform dirty HTML to well-formed XML.
     *
     * @see <a href="https://htmlcleaner.sourceforge.net">HtmlCleaner</a>
     */
    static class HtmlCleaner extends Dependency {
        private Path jar;
        private Path license;

        HtmlCleaner(Config config) {
            super("htmlcleaner", config);
        }

        @Override
        public void setup() throws Fault {
            jar = config.getPath("HTMLCLEANER_JAR");
            if (jar == null) {
                jar = downloadStandardJar((urlBase, version) ->
                        urlBase
                        + "/net/sourceforge/htmlcleaner/htmlcleaner/"
                        + version
                        + "/htmlcleaner-" + version + ".jar");
            }

            license = config.getPath("HTMLCLEANER_LICENSE");
            if (license == null) {
                var version = config.getString("HTMLCLEANER_VERSION");
                var licenseURL = newURL("https://sourceforge.net/p/htmlcleaner/code/HEAD/tree/tags/"
                        + "htmlcleaner-" + version
                        + "/licence.txt?format=raw");
                var licenseChecksum = config.getString("HTMLCLEANER_LICENSE_CHECKSUM");
                license = download(licenseURL, depsDir, licenseChecksum);
            }
        }

        @Override
        public void verify() throws Fault {
            checkFile(jar);
            checkFile(license);
        }

        @Override
        public Map<String, String> getMakeArgs() {
            return Map.of(
                    "HTMLCLEANER_JAR", jar.toString(),
                    "HTMLCLEANER_LICENSE", license.toString());
        }
    }

    /**
     * Java Diff Utilities, to compare text files.
     *
     * @see <a href="https://github.com/java-diff-utils/java-diff-utils">Java Diff Utilities</a>
     */
    static class JavaDiffUtils extends Dependency {
        private Path jar;
        private Path license;

        JavaDiffUtils(Config config) {
            super("java-diff-utils", config);
        }

        @Override
        public void setup() throws Fault {
            jar = config.getPath("JAVADIFFUTILS_JAR");
            if (jar == null) {
                jar = downloadStandardJar((urlBase, version) ->
                        urlBase
                        + "/io/github/java-diff-utils/java-diff-utils/"
                        + version
                        + "/java-diff-utils-" + version + ".jar");
            }

            license = config.getPath("JAVADIFFUTILS_LICENSE");
            if (license == null) {
                var version = config.getString("JAVADIFFUTILS_LICENSE_VERSION");
                var licenseURL = newURL("https://raw.githubusercontent.com/java-diff-utils/java-diff-utils/"
                        + "java-diff-utils-" + version
                        + "/LICENSE");
                var licenseChecksum = config.getString("JAVADIFFUTILS_LICENSE_CHECKSUM");
                license = download(licenseURL, depsDir, licenseChecksum);
            }
        }

        @Override
        public void verify() throws Fault {
            checkFile(jar);
        }

        @Override
        public Map<String, String> getMakeArgs() {
            return Map.of(
                    "JAVADIFFUTILS_JAR", jar.toString(),
                    "JAVADIFFUTILS_LICENSE", license.toString());
        }
    }

    /**
     * JUnit, to run tests for APIDiff.
     *
     * @see <a href="https://junit.org/junit5/">JUnit</a>
     */
    static class JUnit extends Dependency {
        private Path jar;

        JUnit(Config config) {
            super("junit", config);
        }

        @Override
        public void setup() throws Fault {
            jar = config.getPath("JUNIT_JAR");
            if (jar == null) {
                jar = downloadStandardJar((urlBase, version) ->
                        urlBase
                        + "/org/junit/platform/junit-platform-console-standalone/"
                        + version
                        + "/junit-platform-console-standalone-" + version + ".jar");
            }
        }

        @Override
        public void verify() throws Fault {
            checkFile(jar);
        }

        @Override
        public Map<String, String> getMakeArgs() {
            return Map.of("JUNIT_JAR", jar.toString());
        }
    }

    /**
     * Generates a script to run "make", based on the set of dependencies.
     */
    static class MakeScript {
        private final Config config;
        MakeScript(Config config) {
            this.config = config;
        }

        void writeFile(Path file, List<? extends Dependency> deps) throws Fault {
            var allMakeArgs = new TreeMap<String, String>();
            deps.forEach(d -> allMakeArgs.putAll(d.getMakeArgs()));

            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
                out.println("#!/bin/sh");
                out.println();
                out.println("cd \"" + config.rootDir.resolve("make") + "\"");
                out.println("make \\");
                allMakeArgs.forEach((name, value) ->
                        out.printf("    %s=\"%s\" \\%n", name, value));
                out.println("    \"$@\"");
            } catch (IOException e) {
                throw new Fault("Error writing make command script: " + file + ": " + e);
            }
        }
    }

}

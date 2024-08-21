/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package apitest.lib;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.StackWalker.StackFrame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.codetools.apidiff.Main;
import jdk.codetools.apidiff.Main.Result;
import toolbox.ToolBox;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class providing utility methods for test classes.
 */
public class APITester {
    private static final String lineSeparator = System.lineSeparator();
    Path base = Paths.get("build/test"); // TODO: better if injected

    /** A toolbox. */
    protected final ToolBox tb = new ToolBox();

    /** A stream for logging messages. */
    protected final PrintStream log = System.out;

    /** The kind of output. */
    public enum OutputKind {
        /** Output written to the standard output stream. */
        OUT,
        /** Output written to the standard error stream. */
        ERR
    }

    public Map<OutputKind, String> run(List<String> options) throws Error {
        return run(options, EnumSet.of(Result.OK, Result.DIFFS));
    }

    /**
     * Executes a new instance of <i>apidiff</i> with  a given set of options,
     * returning the output that is written to the standard output and error
     * stream.
     *
     * @param options the options
     * @return the output
     */
    public Map<OutputKind, String> run(List<String> options, Set<Result> expectResult) {
        Main.Result r;
        StringWriter outSW = new StringWriter();
        StringWriter errSW = new StringWriter();
        try (PrintWriter out = new PrintWriter(outSW); PrintWriter err = new PrintWriter(errSW)) {
            Main m = new Main(out, err);
            r = m.run(options);
        }
        Map<OutputKind, String> map = new EnumMap<>(OutputKind.class);

        String out = outSW.toString();
        log.println("stdout:");
        log.println(out);
        map.put(OutputKind.OUT, out);

        String err = errSW.toString();
        log.println("stderr:");
        log.println(err);
        map.put(OutputKind.ERR, err);

        // defer this check until stdout and stderr have been written out
        if (!expectResult.contains(r)) {
            throw new AssertionError("unexpected result: " + r + "; expected: " + expectResult);
        }

        return map;
    }

    /**
     * Returns an empty scratch directory based on the name of the class and method
     * calling this method.
     *
     * @return the path of a clean scratch directory
     * @throws IOException if there is a problem creating the directory
     */
    protected Path getScratchDir() throws IOException {
        StackFrame caller = StackWalker.getInstance()
                .walk(s -> s.filter(f -> !f.getClassName().equals(APITester.class.getName()))
                        .findFirst()
                        .orElseThrow());

        Path dir = Files.createDirectories(base
                .resolve("work")
                .resolve(caller.getClassName().replace(".", File.separator))
                .resolve(caller.getMethodName()));
        tb.cleanDirectory(dir);
        return dir;
    }

    /**
     * Returns an empty scratch directory based on the name of the class and method
     * calling this method, and a given subdirectory name.
     *
     * @param subDir the name of the subdirectory
     *
     * @return the path of a clean scratch directory
     * @throws IOException if there is a problem creating the directory
     */
    protected Path getScratchDir(String subDir) throws IOException {
        StackFrame caller = StackWalker.getInstance()
                .walk(s -> s.filter(f -> !f.getClassName().equals(APITester.class.getName()))
                        .findFirst()
                        .orElseThrow());

        Path dir = Files.createDirectories(base
                .resolve("work")
                .resolve(caller.getClassName().replace(".", File.separator))
                .resolve(caller.getMethodName())
                .resolve(subDir));
        tb.cleanDirectory(dir);
        return dir;
    }

    protected String getClassMethodName() {
        StackFrame caller = StackWalker.getInstance()
                .walk(s -> s.filter(f -> !f.getClassName().equals(APITester.class.getName()))
                        .findFirst()
                        .orElseThrow());
        return caller.getClassName().replaceAll("^.*\\.", "") + "." + caller.getMethodName();
    }

    public void checkOutput(Path p, String... expect) throws IOException {
        String s = Files.readString(p);
        for (String e : expect) {
            assertTrue(s.contains(e), "expected content not found: " + e);
        }
    }
}

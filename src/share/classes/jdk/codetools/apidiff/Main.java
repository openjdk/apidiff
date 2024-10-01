/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jdk.codetools.apidiff;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.codetools.apidiff.Options.VerboseKind;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIComparator;
import jdk.codetools.apidiff.model.AccessKind;
import jdk.codetools.apidiff.model.Selector;
import jdk.codetools.apidiff.report.LogReporter;
import jdk.codetools.apidiff.report.MultiplexReporter;
import jdk.codetools.apidiff.report.Reporter;
import jdk.codetools.apidiff.report.html.HtmlReporter;

/**
 * Main entry point for the "apidiff" utility.
 * The code can be invoked from the command line,
 * or by equivalent API methods.
 */
public class Main {
    /**
     * An encapsulation of the exit code from running the tool.
     */
    public enum Result {
        OK(0),
        DIFFS(1),
        BAD_ARGS(2),
        FAULT(3);
        final int exitCode;

        Result(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    /**
     * Executes the tool, configured with the given arguments.
     *
     * This is the main entry point when invoked from the command-line.
     * It uses the standard output and error stream.
     *
     * @param args the arguments to configure the tool
     */
    public static void main(String... args) {
        Result r = new Main().run(args);
        if (r != Result.OK) {
            System.exit(r.exitCode);
        }
    }

    private final PrintWriter out;
    private final PrintWriter err;

    /**
     * Creates an instance of the class that uses
     * the standard output and error streams.
     */
    public Main() {
        out = new PrintWriter(System.out);
        err = new PrintWriter(System.err, true);
    }

    /**
     * Creates an instance of the class that uses the given stream.
     *
     * @param out the stream for standard output
     * @param err the stream for error messages and other diagnostics.
     */
    public Main(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Executes the tool, configured with the given arguments.
     *
     * @param args the arguments to configure the tool
     *
     * @return a value indicating the outcome of the comparison
     */
    public Result run(String... args) {
        return run(List.of(args));
    }

    /**
     * Executes the tool, configured with the given arguments.
     *
     * @param args the arguments to configure the tool
     *
     * @return a value indicating the outcome of the comparison
     */
    public Result run(List<String> args) {
        Log log = new Log(out, err);
        try {
            return run(args, log);
        } finally {
            log.flush();
        }
    }

    private Result run(List<String> args, Log log) {
        try {
            args = CommandLine.parse(args);
        } catch (IOException e) {
            log.error("main.err.bad-@file", e.getMessage());
            return Result.BAD_ARGS;
        }

        Options options = new Options(log, args);
        if (log.errorCount() > 0) {
            return Result.BAD_ARGS;
        }

        if (options.version) {
            Version.getCurrent().show(log.out);
        }

        if (options.help) {
            options.showHelp();
            log.flush();
        }

        if ((options.version || options.help) && options.allAPIOptions.isEmpty()) {
            return Result.OK;
        }

        options.validate();
        if (log.errorCount() > 0) {
            return Result.BAD_ARGS;
        }

        Instant start = Instant.now();

        Selector s = new Selector(options.includes, options.excludes);
        AccessKind ak = options.getAccessKind();

        boolean verboseOptions = options.isVerbose(VerboseKind.OPTIONS);
        if (verboseOptions) {
            options.allAPIOptions.values().forEach(a -> a.showVerboseSummary(log));
        }

        // TODO: when APIDiff moves to JDK 21, thia can trivially become SequencedSet,
        //       which would be useful in varoius places, such as PageReporter.getResultGlyph
        Set<API> apis = options.allAPIOptions.values().stream()
                .map(a -> API.of(a, s, ak, log, verboseOptions))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Reporter> rList = new ArrayList<>();

        rList.add(new LogReporter(log, options));
        if (options.getHiddenOption("trace-reporter") != null) {
            rList.add(createTraceReporter(log));
        }

        if (options.getOutDir() != null) {
            try {
                Files.createDirectories((options.getOutDir()));
            } catch (IOException e) {
                log.error("main.err.cant-create-output-directory", options.getOutDir());
                return Result.FAULT;
            }
            Notes notes = null;
            if (options.notes != null) {
                try {
                    notes = Notes.read(options.notes, log);
                } catch (IOException e) {
                    log.error("main.err.cant-read-notes", options.notes, e);
                    return Result.FAULT;
                }
            }
            rList.add(new HtmlReporter(apis, options, notes, log));
        }

        Reporter r = (rList.size() == 1) ? rList.get(0) : new MultiplexReporter(rList);

        boolean equal;
        try {
            APIComparator ac = new APIComparator(apis, options, r, log);
            equal = ac.compare();
        } catch (Abort ex) {
            // processing aborted
            equal = false;
        }

        if (options.isVerbose((VerboseKind.TIME))) {
            Instant now = Instant.now();
            Duration d = Duration.between(start, now);
            long hours = d.toHours();
            int minutes = d.toMinutesPart();
            int seconds = d.toSecondsPart();
            log.report("main.elapsed", hours, minutes, seconds);
        }

        log.reportCounts();
        log.flush();

        return (log.errorCount() > 0) ? Result.FAULT : equal ? Result.OK : Result.DIFFS;
    }

    private Reporter createTraceReporter(Log log) {
        return (Reporter) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ Reporter.class },
                (proxy, method, args) -> {
                    log.err.println("!! " + method.getName() + ": " + Arrays.toString(args));
                    return null;
                });
    }
}

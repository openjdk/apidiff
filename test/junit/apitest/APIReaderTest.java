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

package apitest;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.model.APIDocs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

/**
 * A test for the ability to extract documentation from API documentation
 * generated by <i>javadoc</i>.
 */
// TODO: either here or in a JUnit-free class, we could have a main program
//       that reads a file, and prints out the resulting APIDocs in a stylized
//       HTML format, that makes it easy to see the text that is extracted.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class APIReaderTest extends APITester {
    private Log log;
    private Path api;

    /**
     * Generates sample API documentation from sample API.
     *
     * @throws IOException if an IO exception occurs
     */
    @BeforeAll
    public void generateAPIDocs() throws IOException {
        Path base = getScratchDir();
        super.log.println(base);

        Path src = base.resolve("src");
        generateSampleAPI(src);

        // Run javadoc on sample API
        api = Files.createDirectories(base.resolve("api"));
        Task.Result r = new JavadocTask(tb)
                .sourcepath(src.resolve("mA"))
                .outdir(api)
                .options("-noindex", "-quiet", "--module", "mA")
                .run();
        r.writeAll();

        PrintWriter out = new PrintWriter(System.out) {
            @Override
            public void close() {
                flush();
            }
        };
        PrintWriter err = new PrintWriter(System.err, true){
            @Override
            public void close() {
                flush();
            }
        };

        log = new Log(out, err);
    }

    /**
     * Flushes any output that has been written to the log streams.
     */
    @AfterEach
    public void flushLog() {
        log.out.flush();
        log.err.flush();
    }

    void generateSampleAPI(Path dir) throws IOException {
        new ModuleBuilder(tb, "mA")
                .comment("This is module mA. This is more text for mA.\n@see \"See Text\"")
                .exports("p")
                .classes("/** This is package p. This is more text for p.\n@see \"See Text\" */ package p;")
                .classes("""
                        package p;
                        /**
                         * This is anno-type A. This is more text for A.
                         * @see "See Text"
                         */
                        public @interface A {
                            /**
                             * This is required element r1. This is more text for r1.
                             * @return dummy.
                             * @see "See Text"
                             */
                            int r1();
                            /**
                             * This is required element r2. This is more text for r2.
                             * @return dummy.
                             * @see "See Text"
                             */
                            int r2();
                            /**
                             * This is optional element o1. This is more text for o1.
                             * @return dummy.
                             * @see "See Text"
                             */
                            int o1() default 0;
                            /**
                             * This is optional element o2. This is more text for o2.
                             * @return dummy.
                             * @see "See Text"
                             */
                            int o2() default 0;
                        }""")
                .classes("""
                        package p;
                        /**
                         * This is enum E; This is more text for E.
                         * @see "See Text"
                         */
                        public enum E {
                            /**
                             * This is enum constant E1. This is more text for E1.
                             * @see "See Text"
                             */
                            E1,
                            /**
                             * This is enum constant E2. This is more text for E2.
                             * @see "See Text"
                             */
                            E2;
                            /**
                             * This is field f1. This is more text for f1.
                             * @see "See Text"
                             */
                            public int f1;
                            /**
                             * This is field f2. This is more text for f2.
                             * @see "See Text"
                             */
                            public int f2;
                            /**
                             * This is method m1. This is more text for m1.
                             * @see "See Text"
                             */
                            public void m1() { }
                            /**
                             * This is method m2. This is more text for m2.
                             * @see "See Text"
                             */
                            public void m2() { }
                            /**
                             * This is nested class N. This is more test for N.
                             * @see "See Text"
                             */
                            public class N {
                                /**
                                 * This is field f1 in nested class N.
                                 */
                                public int f1;
                            }
                        }""")
                .classes("""
                        package p;
                        /**
                         * This is class C; This is more text for C.
                         * @see "See Text"
                         */
                        public class C {
                            /**
                             * This is field f1. This is more text for f1.
                             * @see "See Text"
                             */
                            public int f1;
                            /**
                             * This is field f2. This is more text for f2.
                             * @see "See Text"
                             */
                            public int f2;
                            /**
                             * This is the no-args constructor. This is more text for the no-args constructor.
                             * @see "See Text"
                             */
                            public C() { }
                            /**
                             * This is the 1-arg constructor. This is more text for the 1-arg constructor.
                             * @param i the arg
                             * @see "See Text"
                             */
                            public C(int i) { }
                            /**
                             * This is method m1. This is more text for m1.
                             * @see "See Text"
                             */
                            public void m1() { }
                            /**
                             * This is method m2. This is more text for m2.
                             * @see "See Text"
                             */
                            public void m2() { }
                            /**
                             * This is nested class N. This is more test for N.
                             * @see "See Text"
                             */
                            public class N {
                                /**
                                 * This is field f1 in nested class N.
                                */
                                public int f1;
                            }
                        }""")
                .classes("""
                        package p;
                        /**
                         * This is interface I; This is more text for I.
                         * @see "See Text"
                         */
                        public interface I {
                            /**
                             * This is field f1. This is more text for f1.
                             * @see "See Text"
                             */
                            static final int f1 = 0;
                            /**
                             * This is field f2. This is more text for f2.
                             * @see "See Text"
                             */
                            static final int f2 = 0;
                            /**
                             * This is method m1. This is more text for m1.
                             * @see "See Text"
                             */
                            void m1();
                            /**
                             * This is method m2. This is more text for m2.
                             * @see "See Text"
                             */
                            void m2();
                            /**
                             * This is nested class N. This is more test for N.
                             * @see "See Text"
                             */
                            public class N {
                                /**
                                 * This is field f1 in nested class N.
                                 */
                                 public int f1;
                            }
                        }""")
                .write(dir);
    }

    /**
     * Find all the files that are in the <i>api</i> directory.
     *
     * @return the files
     * @see #checkFile(Path)
     */
    public Stream<Arguments> findFiles() {
        List<Object[]> files = new ArrayList<>();
        try {
            walkFileTree(api, log, (log, file) -> files.add(new Object[] { file }));
        } catch (IOException e) {
            Assertions.fail("problem finding files", e);
        }
        return files.stream().map(Arguments::of);
    }

    private static void walkFileTree(Path dir, Log log, BiConsumer<Log, Path> f) throws IOException {
        Pattern p = Pattern.compile("(module-summary|package-summary|[A-Z].*)\\.html");
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (p.matcher(file.getFileName().toString()).matches()) {
                    System.err.println("file: " + file);
                    f.accept(log, file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.err.println("dir: " + dir);
                return switch (dir.getFileName().toString()) {
                    case "jquery", "resources" -> FileVisitResult.SKIP_SUBTREE;
                    default -> FileVisitResult.CONTINUE;
                };
            }
        });
    }

    /**
     * Tests the content of a file.
     *
     * @param file the file
     */
    @ParameterizedTest
    @MethodSource("findFiles")
    public void checkFile(Path file) {
        // TestNG oddity: the test works as expecting when using the following call,
        // but not if the next 5 source lines are removed, causing the body of the
        // method to be executed directly; in that case, the test is only executed
        // once, for the first file, with no indication of why other data values
        // are not used.
        checkFile(log, file);
    }

    // TODO: add more checks for content of members
    private void checkFile(Log log, Path file) {
        APIDocs docs = APIDocs.read(log, file);
        showDocs(log, file, docs);

        switch (file.getFileName().toString()) {
            case "module-summary.html" -> {
                checkDescription(docs.getDescription(), "module-description", null, "This is module m[A-Z]. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions());
            }

            case "package-summary.html" -> {
                checkDescription(docs.getDescription(), "package-description", null, "This is package p. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions());
            }

            case "A.html" -> {
                checkDescription(docs.getDescription(), null, null, "This is anno-type A. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions(), "r1()", "r2()", "o1()", "o2()");
            }

            case "C.html" -> {
                checkDescription(docs.getDescription(), null, null, "This is class C. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions(), "<init>()", "<init>(int)", "f1", "f2", "m1()", "m2()");
                checkDescription(docs.getDescription("<init>()"),
                        "<init>()", "C", "This is the no-args constructor. This is more");
            }

            case "C.N.html",
                 "E.N.html",
                 "I.N.html" -> {
                checkDescription(docs.getDescription(), null, null, "This is nested class N. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions(), "<init>()", "f1");
            }

            case "E.html" -> {
                checkDescription(docs.getDescription(), null, null, "This is enum E. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions(), "E1", "E2", "f1", "f2", "m1()", "m2()", "values()", "valueOf(java.lang.String)");
            }

            case "I.html" -> {
                checkDescription(docs.getDescription(), null, null, "This is interface I. This is more");
                checkMemberDescriptions(docs.getMemberDescriptions(), "f1", "f2", "m1()", "m2()");
            }

            default -> Assertions.fail(file.toString());
        }
    }

    private void checkDescription(String desc, String id, String heading, String body) {
        if (id != null && !desc.contains("id=\"" + escape(id) + "\"")) {
            Assertions.fail("expected id not found: " + id);
        }
        if (heading != null && !Pattern.compile("<h[34][^>]*>\\Q" + heading + "\\E</h[34]").matcher(desc).find()) {
            Assertions.fail("expected heading not found: " + heading);
        }

        if (!Pattern.compile(body).matcher(desc).find()) {
            Assertions.fail("expected body not found: " + body);
        }
    }

    private void checkMemberDescriptions(Map<String, String> descriptions, String... ids) {
        Assertions.assertEquals(Set.of(ids), descriptions.keySet());
    }

    void showFile(Log log, Path file) {
        APIDocs docs = APIDocs.read(log, file);
        log.out.println("File: " + file);
        log.out.println("Description: ");
        printDescription(log, docs.getDescription());
        new TreeMap<>(docs.getMemberDescriptions()).forEach((id, d) -> {
            log.out.println("Member: " + id);
            printDescription(log, docs.getDescription());

        });
    }

    void showDocs(Log log, Path file, APIDocs docs) {
        log.out.println("File: " + file);
        log.out.println("Description: ");
        printDescription(log, docs.getDescription());
        new TreeMap<>(docs.getMemberDescriptions()).forEach((id, d) -> {
            log.out.println("Member: " + id);
            printDescription(log, docs.getDescription(id));
        });
    }

    private void printDescription(Log log, String s) {
        if (s == null) {
            log.out.println("<null>");
        } else {
            s.lines().forEach(l -> log.out.println("| " + l));
        }
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

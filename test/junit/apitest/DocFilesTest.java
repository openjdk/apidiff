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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jdk.codetools.apidiff.Main.Result;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.Options.Mode;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

public class DocFilesTest extends APITester {
    interface DocFilesWriter {
        void write(Path src, int i) throws IOException;
    }

    public static Stream<Arguments> getModes() {
        return Stream.of(
                Arguments.of(Mode.PACKAGE, Mode.PACKAGE),
                Arguments.of(Mode.MODULE, Mode.PACKAGE),
                // module doc-files are not supported before JDK 13, and only partially supported in JDK 13.
                // In JDK 13, non-HTML files are OK, HTML files cause a crash.
                // The tests will automatically skip as needed.
                Arguments.of(Mode.MODULE, Mode.MODULE)
        );
    }

    private void requireVersion(int v, String msg) {
        Assumptions.assumeTrue(Runtime.version().feature() >= v, msg);
    }

    @ParameterizedTest
    @MethodSource("getModes")
    public void testAddHtml(Mode sourceMode, Mode docFileKind) throws IOException {
        if (sourceMode == Mode.MODULE && docFileKind == Mode.MODULE) {
            requireVersion(14, "HTML module doc files not supported in this version of JDK");
        }

        Path base = getScratchDir(sourceMode + "-" + docFileKind);
        log.println(base);

        testDocFiles(base, sourceMode, docFileKind, (dir, i) -> {
            if (i > 0) {
                tb.writeFile(dir.resolve("added.html"),
                        """
                                <!DOCTYPE html>
                                <html>
                                <head><title>info</title></head>
                                <body>
                                First line.<br>
                                Last line.<br>
                                </body>
                                </html>
                                """);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getModes")
    public void testChangeHtml(Mode sourceMode, Mode docFileKind) throws IOException {
        if (sourceMode == Mode.MODULE && docFileKind == Mode.MODULE) {
            requireVersion(14, "HTML module doc files not supported in this version of JDK");
        }
        Path base = getScratchDir(sourceMode + "-" + docFileKind);
        log.println(base);

        testDocFiles(base, sourceMode, docFileKind, (dir, i) ->
            tb.writeFile(dir.resolve("changed.html"),
                    "<!DOCTYPE html>\n"
                            + "<html>\n"
                            + "<head><title>info</title></head>\n"
                            + "<body>\n"
                            + "First line.<br>\n"
                            + "Before the change " + (i == 0 ? "old" : "new") + " after the change.<br>\n"
                            + "Last line.<br>\n"
                            + "</body>\n"
                            + "</html>\n")
        );
    }

    @ParameterizedTest
    @MethodSource("getModes")
    public void testAddText(Mode sourceMode, Mode docFileKind) throws IOException {
        if (sourceMode == Mode.MODULE && docFileKind == Mode.MODULE) {
            requireVersion(13, "Module doc files not supported in this version of JDK");
        }
        Path base = getScratchDir(sourceMode + "-" + docFileKind);
        log.println(base);

        testDocFiles(base, sourceMode, docFileKind, (dir, i) -> {
            if (i > 0) {
                tb.writeFile(dir.resolve("added.txt"),
                        """
                                First line.
                                Last line.
                                """);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getModes")
    public void testChangeText(Mode sourceMode, Mode docFileKind) throws IOException {
        if (sourceMode == Mode.MODULE && docFileKind == Mode.MODULE) {
            requireVersion(13, "Module doc files not supported in this version of JDK");
        }
        Path base = getScratchDir(sourceMode + "-" + docFileKind);
        log.println(base);

        testDocFiles(base, sourceMode, docFileKind, (dir, i) ->
            tb.writeFile(dir.resolve("changed.txt"),
                    "First line.\n"
                            + "Before the change " + (i == 0 ? "old" : "new") + " after the change.\n"
                            + "Last line.\n")
        );
    }

    @ParameterizedTest
    @MethodSource("getModes")
    public void testMulti(Mode sourceMode, Mode docFileKind) throws IOException {
        if (sourceMode == Mode.MODULE && docFileKind == Mode.MODULE) {
            requireVersion(14, "HTML module doc files not supported in this version of JDK");
        }
        Path base = getScratchDir(sourceMode + "-" + docFileKind);
        log.println(base);

        testDocFiles(base, sourceMode, docFileKind, (dir, i) -> {
            tb.writeFile(dir.resolve("changed.html"),
                    "<!DOCTYPE html>\n"
                            + "<html>\n"
                            + "<head><title>info</title></head>\n"
                            + "<body>\n"
                            + "First line.<br>\n"
                            + "Before the change " + (i == 0 ? "old" : "new") + " after the change.<br>\n"
                            + "Last line.<br>\n"
                            + "</body>\n"
                            + "</html>\n");

            tb.writeFile(dir.resolve("changed.txt"),
                    "First line.\n"
                            + "Before the change " + (i == 0 ? "old" : "new") + " after the change.\n"
                            + "Last line.\n");

            tb.writeFile(dir.resolve("same.html"),
                    """
                            <!DOCTYPE html>
                            <html>
                            <head><title>info</title></head>
                            <body>
                            First line.<br>
                            Last line.<br>
                            </body>
                            </html>
                            """);

            tb.writeFile(dir.resolve("same.txt"),
                    """
                            First line.
                            Last line.
                            """);

            if (i > 0) {
                tb.writeFile(dir.resolve("added.html"),
                        """
                                <!DOCTYPE html>
                                <html>
                                <head><title>info</title></head>
                                <body>
                                First line.<br>
                                Last line.<br>
                                </body>
                                </html>
                                """);

                tb.writeFile(dir.resolve("added.txt"),
                        """
                                First line.
                                Last line.
                                """);
            }
        });
    }

    void testDocFiles(Path base, Options.Mode sourceMode, Mode docFileKind, DocFilesWriter docFilesWriter) throws IOException {
        List<String> options = new ArrayList<>();

        String apidiffIncludes;
        String apidiffSourcePathOption;
        switch (sourceMode) {
            case MODULE -> {
                apidiffIncludes = "m/**";
                apidiffSourcePathOption = "--module-source-path";
            }

            case PACKAGE -> {
                apidiffIncludes = "p.**";
                apidiffSourcePathOption = "--source-path";
            }

            default -> throw new Error();
        }

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path src = base.resolve(apiName).resolve("src");
            Path pkgSrc;
            List<String> javadocOptions = new ArrayList<>();
            javadocOptions.addAll(List.of("-noindex", "-quiet"));
            switch (sourceMode) {
                case MODULE -> {
                    new ModuleBuilder(tb, "m")
                            .exports("p")
                            .write(src);
                    pkgSrc = src.resolve("m");
                    javadocOptions.addAll(List.of("--module", "m"));
                }

                case PACKAGE -> {
                    pkgSrc = src;
                    javadocOptions.add("p");
                }

                default -> throw new Error();
            }
            tb.writeJavaFiles(pkgSrc, "package p; public class C { }\n");
            Path dfDir = ((docFileKind == Mode.MODULE) ? pkgSrc : pkgSrc.resolve("p")).resolve("doc-files");
            docFilesWriter.write(dfDir, i);

            Path api = base.resolve(apiName).resolve("api");
            Files.createDirectories(api);
            Task.Result r = new JavadocTask(tb)
                    .sourcepath(pkgSrc)
                    .outdir(api)
                    .options(javadocOptions)
                    .run();
            r.writeAll();

            options.addAll(List.of(
                    "--api", apiName,
                    apidiffSourcePathOption, src.toString(),
                    "--api-directory", api.toString()));
        }

        options.addAll(List.of(
                "--include", apidiffIncludes,
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);
        Map<OutputKind,String> outMap = run(options, EnumSet.of(Result.DIFFS));
    }
}

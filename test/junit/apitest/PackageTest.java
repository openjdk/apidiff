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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

/**
 * Tests for the ability to compare packages.
 */
public class PackageTest extends APITester {
    /**
     * Tests handling of missing packages.
     * Three APIs are generated.
     * <ul>
     * <li>all APIs contain equal definitions of package p1
     * <li>two APIs contain equal definitions of package p2
     * <li>only one API contains a definition of package p3
     * </ul>
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testMissingPackages() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 3;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int p = 0; p <= a; p++) {
                tb.writeJavaFiles(apiDir,
                        "package p.p%p%; public class C%p% { }\n".replace("%p%", String.valueOf(p)));
            }

            options.addAll(List.of(
                    "--api", apiName,
                    "--source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "-XDshow-debug-summary",
                "-XDtrace-reporter",
                "--include", "p.**",
                "-d", base.resolve("out").toString(),
                "--verbose", "missing"));

        log.println("Options: " + options);
        // TODO: Main needs resource for bad option
        // TODO: Main needs to check for output dir
        // TODO: handle compilation errors in source
        Map<OutputKind,String> outMap = run(options);
        long notFound = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found"))
                .count();
        Assertions.assertEquals(3, notFound);
    }

    /**
     * Tests handling of missing packages.
     * Three APIs are generated, all containing module mA
     * <ul>
     * <li>all APIs contain equal definitions of package p1
     * <li>two APIs contain equal definitions of package p2
     * <li>only one API contains a definition of package p3
     * </ul>
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testMissingPackagesInModules() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 3;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA");
            for (int p = 0; p <= a; p++) {
                mb.classes("package p%p%; public class C%p% { }\n".replace("%p%", String.valueOf(p)));
            }
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString(),
                    "--access", "private"));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString(),
                "--verbose", "missing"));

        log.println("Options: " + options);
        // TODO: Main needs resource for bad option
        // TODO: Main needs to check for output dir
        // TODO: handle compilation errors in source
        Map<OutputKind,String> outMap = run(options);
        long notFound = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found"))
                .count();
        Assertions.assertEquals(3, notFound);
    }

    /**
     * Tests handling of different doc comments.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testRawDocComments() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            String cs = "/**\n * This is 'p.same'.\n * Unchanged.\n * More.\n **/\n";
            String ci = "/**\n * This is 'p.insert'.\n" + (a == 1 ? " * Inserted.\n" : "") + " * More.\n **/\n";
            String cr = "/**\n * This is 'p.remove'.\n" + (a == 0 ? " * Removed.\n" : "") + " * More.\n **/\n";
            String cc = "/**\n * This is 'p.change'.\n * API " + a + "\n * More.\n **/\n";

            ModuleBuilder mb = new ModuleBuilder(tb, "mA");
            mb.classes(cs + "package p.same;\n",
                    ci + "package p.insert;\n",
                    cr + "package p.remove;\n",
                    cc + "package p.change;\n");
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

    }

    @Test
    public void testPackageHtml() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            String cs = "This is 'p.same'.\nUnchanged.\nMore.\n";
            String ci = "This is 'p.insert'.\n" + (a == 1 ? "Inserted.\n" : "") + "More.\n";
            String cr = "This is 'p.remove'.\n" + (a == 0 ? " Removed.\n" : "") + "More.\n";
            String cc = "This is 'p.change'.\nAPI " + a + "\nMore.\n";

            new ModuleBuilder(tb, "m")
                    .exports("p.same")
                    .exports("p.insert")
                    .exports("p.remove")
                    .exports("p.change")
                    .classes("package p.same; public class Same { }\n",
                            "package p.insert; public class Insert { }\n",
                            "package p.remove; public class Remove { }\n",
                            "package p.change; public class Change { }\n")
                    .write(apiDir);
            writePackageHtml(apiDir, "m", "p.same",   cs);
            writePackageHtml(apiDir, "m", "p.insert", ci);
            writePackageHtml(apiDir, "m", "p.remove", cr);
            writePackageHtml(apiDir, "m", "p.change", cc);

            Path api = base.resolve(apiName).resolve("api");
            Files.createDirectories(api);
            List<String> javadocOptions = List.of(
                    "-noindex", "-quiet",
                    "--module", "m");
            Task.Result r = new JavadocTask(tb)
                    .sourcepath(apiDir.resolve("m"))
                    .outdir(api)
                    .options(javadocOptions)
                    .run();
            r.writeAll();

            options.addAll(List.of(
                    "--api", apiName,
                    "--api-directory", api.toString(),
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "m/**",
                "--compare-doc-comments", "yes",
                "--compare-api-descriptions", "yes",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    private void writePackageHtml(Path dir, String mName, String pName, String body)
            throws IOException {
        tb.writeFile(dir.resolve(mName)
                .resolve(pName.replace(".", File.separator))
                .resolve("package.html"),
                "<!DOCTYPE html>\n"
                        + "<html>\n"
                        + "<title>pName</title>\n"
                        + "<body>\n"
                        + body
                        + "\n</body>\n"
                        + "</html>\n");
    }
}

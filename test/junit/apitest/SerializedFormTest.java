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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

public class SerializedFormTest extends APITester {

    @Test()
    public void testAddSuperclass() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String c = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C" + (api == 0 ? "" : " implements Serializable") + " {\n"
                    + "}\n";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(c)
                    .write(src);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test()
    public void testClassExclude() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String c = "package p;\n"
                    + "import java.io.*;\n"
                    + (api == 0 ? "" : "/** Sentence.\n * @serial exclude\n */\n")
                    + "public class C implements Serializable {\n"
                    + "}\n";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(c)
                    .write(src);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test()
    public void testPackageExclude() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String p =
                    (api == 0 ? "" : "/** Sentence.\n * @serial exclude\n */\n")
                    + "package p;";
            String c = """
                    package p;
                    import java.io.*;
                    public class C implements Serializable {
                    }
                    """;
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(p, c)
                    .write(src);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test()
    public void testPackageExcludeClassInclude() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String p =
                    (api == 0 ? "" : "/** Sentence.\n * @serial exclude\n */\n")
                            + "package p;";
            String c = "package p;\n"
                    + "import java.io.*;\n"
                    + (api == 0 ? "" : "/** Sentence.\n * @serial include\n */\n")
                    + "public class C implements Serializable {\n"
                    + "}\n";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(p, c)
                    .write(src);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }
}

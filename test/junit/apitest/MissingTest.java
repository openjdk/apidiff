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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

/**
 * Tests for the ability to detect missing items.
 */
public class MissingTest extends APITester {
    @Test
    public void testMissingModule() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        for (int a = 0; a < 3; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int i = 0; i < 3; i++) {
                if (a != 1 || i != 1) {
                    new ModuleBuilder(tb, "m." + (char)('A' + i))
                            .exports("p" + i)
                            .classes("package p" + i + "; public class C { }")
                    .write(apiDir);
                }
            }

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        options.addAll(List.of(
                "-XDshow-debug-summary",
                "-XDtrace-reporter",
                "--include", "m.*/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testMissingPackage() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        for (int a = 0; a < 3; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "m");
            for (int i = 0; i < 3; i++) {
                if (a != 1 || i != 1) {
                    mb.exports("p" + i).classes("package p" + i + "; public class C { }");
                }
            }
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        options.addAll(List.of(
                "-XDshow-debug-summary",
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testMissingType() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        for (int a = 0; a < 3; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "m");
            mb.exports("p");
            for (int i = 0; i < 3; i++) {
                if (a != 1 || i != 1) {
                    mb.classes("package p; public class C" + i + " { }");
                }
            }
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        options.addAll(List.of(
                "-XDshow-debug-summary",
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testMissingField() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        for (int a = 0; a < 3; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "m");
            mb.exports("p");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (a != 1 || i != 1) {
                    sb.append("public int f" + i + "; ");
                }
            }
            mb.classes("package p; public class C { " + sb + "}");
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        options.addAll(List.of(
                "-XDshow-debug-summary",
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testMissingMethod() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        for (int a = 0; a < 3; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "m");
            mb.exports("p");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (a != 1 || i != 1) {
                    sb.append("public void m" + i + "(); ");
                }
            }
            mb.classes("package p; public class C { " + sb + "}");
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        options.addAll(List.of(
                "-XDshow-debug-summary",
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }
}

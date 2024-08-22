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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

/**
 * Tests for the ability to compare modules.
 */
public class ModuleTest extends APITester {
    /**
     * Tests handling of missing modules.
     * Three APIs are generated.
     * <ul>
     * <li>all APIs contain equal definitions of module mA
     * <li>two APIs contain equal definitions of mB
     * <li>only one API contains a definition of mC
     * </ul>
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testMissingModules() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 3;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m <= a; m++) {
                new ModuleBuilder(tb, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "-d", base.resolve("out").toString(),
                "--verbose", "missing"));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
        long notFound = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found"))
                .count();
        Assertions.assertEquals(3, notFound);

    }

    /**
     * Tests handling of modules with different modifiers.
     * Two APIs are generated, containing four modules.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDifferentModifiers() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();
        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m < 4; m++) {
                boolean open = (m & (1 << a)) != 0;
                new ModuleBuilder(tb, open, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "--include", "mD/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

    }

    /**
     * Tests handling of modules with different doc comments.
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

            String cs = "/**\n * This is 'm.same'.\n * Unchanged.\n * More.\n **/\n";
            String ci = "/**\n * This is 'm.insert'.\n" + (a == 1 ? " * Inserted.\n" : "") + " * More.\n **/\n";
            String cr = "/**\n * This is 'm.remove'.\n" + (a == 0 ? " * Removed.\n" : "") + " * More.\n **/\n";
            String cc = "/**\n * This is 'm.change'.\n * API " + a + "\n * More.\n **/\n";

            new ModuleBuilder(tb, "m.same")
                    .comment(cs)
                    .classes("package p; public class C { }")
                    .write(apiDir);

            new ModuleBuilder(tb, "m.insert")
                    .comment(ci)
                    .classes("package p; public class C { }")
                    .write(apiDir);

            new ModuleBuilder(tb, "m.remove")
                    .comment(cr)
                    .classes("package p; public class C { }")
                    .write(apiDir);

            new ModuleBuilder(tb, "m.change")
                    .comment(cc)
                    .classes("package p; public class C { }")
                    .write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "m.same/**",
                "--include", "m.insert/**",
                "--include", "m.remove/**",
                "--include", "m.change/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

    }

    @Test
    public void testDifferentRequiresModule() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "mA")
                    .requires(a == 0 ? "java.compiler" : "jdk.compiler")
                    .exports("p")
                    .classes("package p; public class C { }\n")
                    .write(apiDir);

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
    public void testDifferentRequiresStatic() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            boolean isStatic = (a != 0);
            new ModuleBuilder(tb, false, "mA")
                    .requires("java.compiler", isStatic, false)
                    .exports("p")
                    .classes("package p; public class C { }\n")
                    .write(apiDir);

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
    public void testDifferentRequiresTransitive() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            boolean isTransitive = (a != 0);
            new ModuleBuilder(tb, false, "mA")
                    .requires("java.compiler", false, isTransitive)
                    .exports("p")
                    .classes("package p; public class C { }\n")
                    .write(apiDir);

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
    public void testDifferentExportTargets() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "m")
                    .exports("p1")
                    .exportsTo("p2",
                            "mX", "m%a%".replace("%a%", String.valueOf((char) ('A' + a))), "mZ")
                    .classes("package p1; public class C { }\n")
                    .classes("package p2; public class C { }\n")
                    .write(apiDir);

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
    public void testDifferentOpenTargets() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "m")
                    .opens("p1")
                    .opensTo("p2",
                            "mX", "m%a%".replace("%a%", String.valueOf((char) ('A' + a))), "mZ")
                    .classes("package p1; public class C { }\n")
                    .classes("package p2; public class C { }\n")
                    .write(apiDir);

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
    public void testDifferentProvides() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "m")
                    .exports("p")
                    .provides("p.S",
                            "p.I0", "p.I%a%".replace("%a%", String.valueOf(a + 1)), "p.I3")
                    .classes("package p; public class S { }\n")
                    .classes("package p; public class I0 extends S { }\n")
                    .classes("package p; public class I1 extends S { }\n")
                    .classes("package p; public class I2 extends S { }\n")
                    .classes("package p; public class I3 extends S { }\n")
                    .write(apiDir);

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
    public void testDifferentUses() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            new ModuleBuilder(tb, false, "m")
                    .exports("p")
                    .uses("p.S0")
                    .uses("p.S%a%".replace("%a%", String.valueOf(a + 1)))
                    .uses("p.S3")
                    .classes("package p; public class S0 { }\n")
                    .classes("package p; public class S1 { }\n")
                    .classes("package p; public class S2 { }\n")
                    .classes("package p; public class S3 { }\n")
                    .write(apiDir);

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

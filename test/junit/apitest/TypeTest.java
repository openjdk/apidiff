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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.JavacTask;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;

/**
 * Tests the ability to compare types.
 */
public class TypeTest extends APITester {
    /**
     * Tests handling of missing types.
     * Three APIs are generated, all containing module mA, package p
     * <ul>
     * <li>all APIs contain equal definitions of class p.C1
     * <li>two APIs contain equal definitions of class p.C2
     * <li>only one API contains a definition of class p.C3
     * </ul>
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testMissingTypes() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 3;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            for (int c = 0; c <= a; c++) {
                mb.classes("package p; public class C%c% { }\n".replace("%c%", String.valueOf(c)));
            }
            mb.write(apiDir);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
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
     * Tests handling of different kinds of types.
     * Four APIs are generated, all containing module mA, package p.
     * Each API declares a class T of a different kind.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDifferentKinds() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 4;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String t = switch (a) {
                case 0 -> "@interface T { }";
                case 1 -> "class T { }";
                case 2 -> "enum T { V }";
                case 3 -> "interface T { }";
                default -> throw new IllegalStateException(String.valueOf(a));
            };
            mb.classes("package p; public " + t + "\n");
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
    /**
     * Tests handling of different kinds of types with supertypes.
     * Two APIs are generated, both containing module mA, package p.
     * One API contains class T which implements Runnable,
     * the other contains interface T which extends Runnable.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDifferentKindsWithSupertype() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String t = switch (a) {
                case 0 -> "abstract class T implements Runnable { }";
                case 1 -> "interface T extends Runnable { }";
                default -> throw new IllegalStateException(String.valueOf(a));
            };
            mb.classes("package p; public " + t + "\n");
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

    /**
     * Tests handling of different kinds of types.
     * Two APIs are generated, all containing module mA, package p.
     * Each API declares a class C with a different superclass,
     * and a class D which does or does not have an explicit superclass.
     *
     * Note that only CLASS kinds can have an explicit superclass, and
     * all classes except {@code java.lang.Object} will have a superclass.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDifferentSuperclasses() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String scc = "extends " + ((char) ('A' + a));
            String scd = (a == 0) ? "" : "extends A";
            mb.classes(
                    "package p; public class A { }\n",
                    "package p; public class B { }\n",
                    "package p; public class C " + scc + " { }\n",
                    "package p; public class D " + scd + " { }\n");
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

    /**
     * Tests handling of different kinds of types.
     * Two APIs are generated, all containing module mA, package p.
     * Each API declares a class C with a different superclass,
     * and a class D which does or does not have an explicit superclass.
     *
     * Note that only CLASS kinds can have an explicit superclass, and
     * all classes except {@code java.lang.Object} will have a superclass.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDifferentSuperinterfaces() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String scc = "implements " + ((char) ('A' + a));
            String scd = (a == 0) ? "" : "implements A";
            String sce = "implements java.util.List<" + ((char) ('A' + a)) + ">";
            mb.classes(
                    "package p; public interface A { }\n",
                    "package p; public interface B { }\n",
                    "package p; public class C " + scc + " { }\n",
                    "package p; public class D " + scd + " { }\n",
                    "package p; public class E " + sce + " { }\n");
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

    /**
     * Tests handling of type parameters.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testTypeParameters() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String tpeC = a == 0 ? "" : "<T>";
            String tpeD = a == 0 ? "<T>" : "<U>";
            String tpeE = a == 0 ? "<T>" : "<T,U>";
            mb.classes(
                    "package p; public interface A<T> { }\n",
                    "package p; public interface B<T,U> { }\n",
                    "package p; public interface C" + tpeC + " { }\n",
                    "package p; public interface D" + tpeD + " { }\n",
                    "package p; public interface E" + tpeE+ " { }\n");
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
    public void testDifferentRawDocComments() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String cs = "/**\n * This is Same.\n * Unchanged.\n * More.\n **/";
            String ci = "/**\n * This is Insert.\n" + (a == 1 ? " * Inserted.\n" : "") + " * More.\n **/";
            String cr = "/**\n * This is Remove.\n" + (a == 0 ? " * Removed.\n" : "") + " * More.\n **/";
            String cc = "/**\n * This is Change.\n * API " + apiName + "\n * More.\n **/";
            mb.classes(
                    "package p; " + cs + "public class Same { }\n",
                    "package p; " + ci + "public class Insert { }\n",
                    "package p; " + cr + "public class Remove { }\n",
                    "package p; " + cc + "public class Change { }\n"
                    );
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
    public void testDifferentAPIDescriptions() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path srcDir = base.resolve(apiName).resolve("src");

            ModuleBuilder mb = new ModuleBuilder(tb, "mA").exports("p");
            String cs = "/**\n * This is Same.\n * Unchanged.\n * More.\n **/";
            String ci = "/**\n * This is Insert.\n" + (a == 1 ? " * Inserted.\n" : "") + " * More.\n **/";
            String cr = "/**\n * This is Remove.\n" + (a == 0 ? " * Removed.\n" : "") + " * More.\n **/";
            String cc = "/**\n * This is Change.\n * API " + apiName + "\n * More.\n **/";
            mb.classes(
                    "package p; " + cs + "public class Same { }\n",
                    "package p; " + ci + "public class Insert { }\n",
                    "package p; " + cr + "public class Remove { }\n",
                    "package p; " + cc + "public class Change { }\n"
            );
            mb.write(srcDir);

            Path modulesDir = Files.createDirectories(base.resolve(apiName).resolve("modules"));
            new JavacTask(tb)
                    .outdir(modulesDir)
                    .options("--module-source-path", srcDir.toString())
                    .files(tb.findJavaFiles(srcDir))
                    .run();

            Path apiDir = Files.createDirectories(base.resolve(apiName).resolve("api"));
            new JavadocTask(tb)
                    .outdir(apiDir)
                    .options("--module-source-path", srcDir.toString(),
                            "--module", "mA")
                    .run();

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-path", modulesDir.toString(),
                    "--api-directory", apiDir.toString()));
        }
        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }
}

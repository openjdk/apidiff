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
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

/**
 * Tests for the ability to compare methods.
 */
public class MethodTest extends APITester {

    /**
     * Tests methods with different modifiers.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testModifiers() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> {
            String mods = (i == 0) ? "public" : "protected";
            return mods + " void m() { }";
        });
    }

    /**
     * Tests methods with different return types.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testReturnType() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> (i == 0)
                ? "public void m() { }"
                : "public int m() { return 0; }"

        );
    }

    /**
     * Tests methods with different receiver types.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testReceiverType() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> (i == 0)
                ? "public void m(int m) { }"
                : "public void m(@Anno C this, int m) { }"

        );
    }

    /**
     * Tests methods with different annotations on parameters.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testParameterAnnos() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> (i == 0)
                ? "public void m(int m) { }"
                : "public void m(@Anno int m) { }"

        );
    }

    /**
     * Tests methods with different modifiers on annotations.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testParameterModifiers() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> (i == 0)
                ? "public void m(int m) { }"
                : "public void m(final int m) { }"

        );
    }

    /**
     * Tests methods with different names of parameters.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testParameterNames() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i -> (i == 0)
                ? "public void m(int m0) { }"
                : "public void m(int m1) { }"

        );
    }

    /**
     * Tests methods with different exceptions declared to be thrown.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testThrows() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i  -> new StringBuilder()
                .append("public void m1()")
                .append(i == 0 ? "" : " throws Exception")
                .append(" { }")
                .append("public void m2() throws ")
                .append(i == 0 ? "Exception" : "Error")
                .append(" { }")
                .toString());
    }

    /**
     * Tests methods with different doc comments.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testRawDocComments() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testMethods(base, i  -> {
            String cs1 = "/**\n * This is 'same()'.\n * Unchanged.\n * More.\n **/\n";
            String ci1 = "/**\n * This is 'insert1()'.\n" + (i == 1 ? " * Inserted.\n" : "") + " * More.\n **/\n";
            String ci2 = "/**\n * This is 'insert2()'."
                    + (i == 1 ? " Inserted 1\n * Inserted 2\n * Inserted 3" : "") + " rest of line\n * More.\n **/\n";
            String ci3 = "/**\n * This is 'insert3()'."
                    + (i == 1 ? " Inserted." : "") + " rest of line\n * More.\n **/\n";
            String cr1 = "/**\n * This is 'remove1()'.\n" + (i == 0 ? " * Removed.\n" : "") + " * More.\n **/\n";
            String cr2 = "/**\n * This is 'remove2()'."
                    + (i == 0 ? " Removed 1\n * Removed 2\n * Removed 3" : "") + " rest of line\n * More.\n **/\n";
            String cr3 = "/**\n * This is 'remove3()'."
                    + (i == 0 ? " Removed." : "") + " rest of line\n * More.\n **/\n";
            String cc1 = "/**\n * This is 'change()'.\n * API " + i + "\n * More.\n **/\n";
            return    cs1 + "public void same() { }\n"
                    + ci1 + "public void insert1() { }\n"
                    + ci2 + "public void insert2() { }\n"
                    + ci3 + "public void insert3() { }\n"
                    + cr1 + "public void remove1() { }\n"
                    + cr2 + "public void remove2() { }\n"
                    + cr3 + "public void remove3() { }\n"
                    + cc1 + "public void change() { }\n";
        });
    }

    private void testMethods(Path base, Function<Integer,String> f) throws IOException {
        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path apiDir = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            new ModuleBuilder(tb, "mA")
                    .exports("p")
                    .classes("package p; import java.lang.annotation.*; "
                            + "@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE}) "
                            + "@interface Anno { }")
                    .classes("package p; public class C {\n" + f.apply(i) + "\n}\n")
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
}

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
 * Tests for the ability to compare fields.
 */
public class FieldTest extends APITester {

    /**
     * Tests equal fields.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testSame() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i -> "public int f;" );
    }

    /**
     * Tests fields with different modifiers.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testModifiers() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i -> {
            String mods = (i == 0) ? "public" : "protected";
            return mods + " int f;";
        });
    }

    /**
     * Tests fields with different types.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testType() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i -> (i == 0)
                ? "public int f;"
                : "public float f;"
        );
    }

    /**
     * Tests fields with different constant values.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    void testValues() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i ->
                "public final int i = " + i + ";\n"
                + "public final char c = '" + ((char)('0' + i)) + "';\n"
                + "public final String s = \"" + i + "\";\n"
                + "public final boolean b = " + (i == 0 ? "false" : "true") + ";");
    }

    /**
     * Tests fields with annotations with different values.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    void testSimpleAnnotations() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i ->
                "@SuppressWarnings(\"none\")\n"
                + "@Deprecated(since=\"" + i + "\")\n"
                + "public final int i;\n");
    }

    /**
     * Tests characters with "special" values.
     * It is not so much the whether differences are detected as whether values are written correctly,
     * and with no exception occurring on output.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    void testSpecialCharValues() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i ->
                "public static final char c0 = 0;"
                + "public static final char cn = '\\n';"
                + "public static final char c31 = (char) 0x1f;"
                + "public static final char cs = ' ';"
                + "public static final char cq = '\\'';"
                + "public static final char cMinLS = Character.MIN_LOW_SURROGATE;"
                + "public static final char cMaxLS = Character.MAX_LOW_SURROGATE;"
                + "public static final char cMinHS = Character.MIN_HIGH_SURROGATE;"
                + "public static final char cMaxHS = Character.MAX_HIGH_SURROGATE;");
    }

    /**
     * Tests fields with different doc comments.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testRawDocComments() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i  -> {
            String cs = "/**\n * This is 'same'.\n * Unchanged.\n * More.\n **/\n";
            String ci = "/**\n * This is 'insert'.\n" + (i == 1 ? " * Inserted.\n" : "") + " * More.\n **/\n";
            String cr = "/**\n * This is 'remove'.\n" + (i == 0 ? " * Removed.\n" : "") + " * More.\n **/\n";
            String cc = "/**\n * This is 'change'.\n * API " + i + "\n * More.\n **/\n";
            return cs + "public int same;\n" +
                    ci + "public int insert;\n" +
                    cr + "public int remove;\n" +
                    cc + "public int change;\n";
        });

    }

    private void testFields(Path base, Function<Integer,String> f) throws IOException {
        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path apiDir = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            Path p = new ModuleBuilder(tb, "mA")
                    .exports("p")
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

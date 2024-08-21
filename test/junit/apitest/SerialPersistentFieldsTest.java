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
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

public class SerialPersistentFieldsTest extends APITester {

    public static Stream<Arguments> provideSingleFields() {
        return Stream.of(
                Arguments.of("boolean"),
                Arguments.of("byte"),
                Arguments.of("char"),
                Arguments.of("double"),
                Arguments.of("float"),
                Arguments.of("int"),
                Arguments.of("long"),
                Arguments.of("short"),
                Arguments.of("java.lang.Object"),
                Arguments.of("java.util.Hashtable"),
                Arguments.of("Object"),
                Arguments.of("String"),
                Arguments.of("Hashtable"),
                Arguments.of("int[]"),
                Arguments.of("String[]"),
                Arguments.of("UNKNOWN")
        );
    }

    @ParameterizedTest
    @MethodSource("provideSingleFields")
    public void testSingleFields(String type) throws IOException {
        String name = type.toLowerCase().replaceAll("[^A-Za-z0-9]+", "_");
        log.printf("Test %s: %s%n", name, type);
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "import java.util.*;\n"
                    + "public class C implements Serializable {\n"
                    + "    /**\n"
                    + "     * A field.\n"
                    + "     * @serialField f " + type + " a description\n"
                    + "     */\n"
                    + "    private static final ObjectStreamField[] serialPersistentFields = { };\n"
                    + "}\n";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(s)
                    .write(src);

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString()));
        }

        options.addAll(List.of(
                "--info-text", "header=" + name,
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

    }
}

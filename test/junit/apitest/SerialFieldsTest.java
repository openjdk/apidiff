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

public class SerialFieldsTest extends APITester {

    public static Stream<Arguments> provideSimpleFields() {
        return Stream.of(
                Arguments.of("none", "", ""),
                Arguments.of("add",  "", "int i;"),
                Arguments.of("remove", "int i;", ""),
                Arguments.of("changeName", "int i;", "int j;"), // effectively, delete and add
                Arguments.of("changeType", "int i;", "long i;"),
                Arguments.of("changeMods1", "private int i;", "public int i;"),
                Arguments.of("changeMods2", "int i;", "static int i;") // effectively, delete
        );
    }

    @ParameterizedTest
    @MethodSource("provideSimpleFields")
    public void testSimpleFields(String name, String api0, String api1) throws IOException {
        log.printf("Test %s: %s | %s%n", name, api0, api1);
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements Serializable {\n"
                    + "    " + (api == 0 ? api0 : api1) + "\n"
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
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    public static Stream<Arguments> provideDocComments() {
        return Stream.of(
                Arguments.of("changeMain",
                        "This is a comment",
                        "This is a different comment"),
                Arguments.of("changeSerial",
                        "This is a comment.\n@serial This is a description",
                        "This is a comment.\n@serial This is a different description"),
                Arguments.of("changeBoth",
                        "This is a comment.\n@serial This is a description",
                        "This is a different comment.\n@serial This is a different description")
        );
    }

    @ParameterizedTest
    @MethodSource("provideDocComments")
    public void testDocComments(String name, String c0, String c1) throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements Serializable {\n"
                    + "    " + (api == 0 ? toComment(c0) : toComment(c1)) + "\n"
                    + "    int i;\n"
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
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    String toComment(String s) {
        return "/**\n *" + s.replace("\n", "\n *") + " */";
    }
}

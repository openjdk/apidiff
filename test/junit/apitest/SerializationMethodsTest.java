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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.codetools.apidiff.Main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

public class SerializationMethodsTest extends APITester {

    public static Stream<Arguments> getMethods() {
        return Stream.of(
                Arguments.of("none", "Serializable", List.<String>of()),
                Arguments.of("serSingle", "Serializable",
                        List.of("private void readObject(ObjectInputStream in) { }")),
                Arguments.of("serMulti", "Serializable",
                        List.of("private void readObject(ObjectInputStream in) { }",
                                "private Object readResolve() { return null; }")),
                Arguments.of("extMin", "Externalizable",
                        List.of("public void readExternal(ObjectInput in) { }",
                                "public void writeExternal(ObjectOutput out) { }")),
                Arguments.of("extOpt", "Externalizable",
                        List.of("public void readExternal(ObjectInput in) { }",
                                "public void writeExternal(ObjectOutput out) { }",
                                "private Object readResolve() { return null; }"))
                );
    }

    @ParameterizedTest
    @MethodSource("getMethods")
    public void testEqualMethods(String name, String intf, List<String> methods) throws IOException {
        log.printf("Test %s: %s %s%n", name, intf, methods);
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements " + intf + " {\n"
                    + methods.stream().map(m -> "    " + m + "\n").collect((Collectors.joining()))
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

        Map<OutputKind,String> outMap = run(options, EnumSet.of(Main.Result.OK));

    }

    @ParameterizedTest
    @MethodSource("getMethods")
    public void testAddMethod(String name, String intf, List<String> methods) throws IOException {
        log.printf("Test %s: %s %s%n", name, intf, methods);
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements " + intf + " {\n"
                    + methods.stream().map(m -> "    " + m + "\n").collect((Collectors.joining()))
                    + (api == 0 ? "" : "    private Object writeReplace() { return this; }\n")
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

        Map<OutputKind,String> outMap = run(options, EnumSet.of(Main.Result.DIFFS));

    }

    @Test
    public void testAddThrows() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        String m = "private void readObject(ObjectInputStream in) { };";

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            String s = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements Serializable {\n"
                    + "    " + ((api == 0) ? m : m.replace(") {", ") throws IOException {")) + "\n"
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
                "--info-text", "header=testAddThrows",
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        options.add("-XDshow-debug-summary");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options, EnumSet.of(Main.Result.DIFFS));

    }
}

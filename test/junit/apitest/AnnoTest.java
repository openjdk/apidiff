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
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

/**
 * Tests for the ability to compare annotations.
 */
public class AnnoTest extends APITester {

    /**
     * Tests different default values.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDefaultValues() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testAnnos(base, i -> {
            String dv = (i == 0) ? "" : " default 1";
            return "int v()" + dv + ";";
        });
    }

    private void testAnnos(Path base, Function<Integer,String> f) throws IOException {
        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path apiDir = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            Path p = new ModuleBuilder(tb, "mA")
                    .exports("p")
                    .classes("package p; public @interface Anno {\n" + f.apply(i) + "\n}\n")
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

    public static Stream<Arguments> provideOptions() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("public", false),
                Arguments.of("protected", false),
                Arguments.of("package", true),
                Arguments.of("private", true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideOptions")
    public void testDocumentedMix(String accessKind, boolean expectNotDoc) throws IOException {
        String name = (accessKind == null) ? "none" : accessKind;
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path apiDir = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            Path p = new ModuleBuilder(tb, "mA")
                    .exports("p")
                    .classes("package p; import java.lang.annotation.*; public @Documented @interface Doc    { }\n")
                    .classes("package p; public @interface NotDoc { }\n")
                    .classes("package p; public @Doc @NotDoc class  C { }\n")
                    .write(apiDir);
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }

        if (accessKind != null) {
            options.addAll(List.of("--access", accessKind));
        }

        options.addAll(List.of(
                "--include", "mA/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

        String c_html = Files.readString(base.resolve("out").resolve("mA").resolve("p").resolve("C.html"));
        checkContains(c_html, "<span class=\"annotation\">@Doc</span>", true);
        checkContains(c_html, "<span class=\"annotation\">@NotDoc</span>", expectNotDoc);
    }

    private void checkContains(String full, String s, boolean expect) {
        if (full.contains(s)) {
            if (!expect) {
                throw new AssertionError("string found unexpectedly");
            }
        } else {
            if (expect) {
                throw new AssertionError("expected string not found");
            }
        }
    }
}

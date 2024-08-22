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

public class SerialVersionUIDTest extends APITester {
    enum Kind {
        IMPLICIT(null),
        DEFAULT(-6240593132596067277L),  // from serialver
        EXPLICIT(123L);
        final Long value;
        Kind(Long value) {
            this.value = value;
        }
    }

    public static Stream<Arguments> provideSUIDs() {
        List<Arguments> list = new ArrayList<>();
        for (Kind k1 : Kind.values()) {
            for (Kind k2 : Kind.values()) {
                list.add(Arguments.of(k1.name().toLowerCase() + "-" + k2.name().toLowerCase(), k1.value, k2.value));
            }
        }
        return list.stream();
    }

    @ParameterizedTest
    @MethodSource("provideSUIDs")
    public void testSUIDs(String name, Long api0, Long api1) throws IOException {
        log.printf("Test %s: %s | %s%n", name, api0, api1);
        Path base = getScratchDir(name);
        log.println(base);

        List<String> options = new ArrayList<>();

        for (int api = 0; api < 2; api++) {
            String apiName = "api" + api;
            Path src = Files.createDirectories(base.resolve(apiName) .resolve("src"));
            Long v = (api == 0) ? api0 : api1;
            String f = v == null ? "" : "    private static final long serialVersionUID = " + v + "L;\n";
            String c = "package p;\n"
                    + "import java.io.*;\n"
                    + "public class C implements Serializable {\n"
                    + f
                    + "    int i;"
                    + "}\n";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes(c)
                    .write(src);
            tb.writeJavaFiles(src, "module m { exports p; }", c);

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

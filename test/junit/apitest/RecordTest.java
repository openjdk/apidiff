/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

public class RecordTest extends APITester {
    final Runtime.Version version;
    final boolean enablePreview;

    RecordTest() {
        version = Runtime.version();
        Assumptions.assumeTrue(version.feature() >= 14, "records not supported in JDK " + version);

        enablePreview = switch (version.feature()) {
            case 14, 15 -> true;
            default -> false;
        };
    }

    @Test
    public void changeKind() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p;
                public class C {
                    private final int c;
                    public C(int c) { this.c = c; }
                    public int c() { return c; }
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p;
                public record C(int c) {
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB);
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different"))
                .count();
        Assertions.assertEquals(3, differences);

    }

    @Test
    public void changeNames() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p;
                public record C(int c1) {
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p;
                public record C(int c2) {
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB);
        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found") | l.contains("Different"))
                .sorted()
                .collect(Collectors.toList());

        List<String> expect = List.of(
                "Different names for record p.C, record component 0",
                "Item not found in API 'A': method p.C#c2()",
                "Item not found in API 'B': method p.C#c1()"
        );

        tb.checkEqual(expect, found);
    }

    @Test
    public void changeType() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p;
                public record C(int c) {
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p;
                public record C(long c) {
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB);
        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found") || l.contains("Different"))
                .sorted()
                .collect(Collectors.toList());

        List<String> expect = List.of(
                "Different types for method p.C#c() return type",
                "Different types for record p.C, record component 0",
                "Item not found in API 'A': constructor p.C#C(long)",
                "Item not found in API 'B': constructor p.C#C(int)"
        );

        tb.checkEqual(expect, found);
    }

    @Test
    public void addComponent() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p;
                public record C(int c1) {
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p;
                public record C(int c1, int c2) {
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB);
        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found"))
                .sorted()
                .collect(Collectors.toList());

        List<String> expect = List.of(
                "Item not found in API 'A': constructor p.C#C(int,int)",
                "Item not found in API 'A': method p.C#c2()",
                "Item not found in API 'A': record p.C, record component 1",
                "Item not found in API 'B': constructor p.C#C(int)"
        );

        tb.checkEqual(expect, found);
    }

    private Map<OutputKind,String> run(Path base, Path srcA, Path srcB) {

        List<String> options = new ArrayList<>();
        for (String api : List.of("A", "B")) {
            options.addAll(List.of(
                    "--api", api,
                    "--source-path", (api.equals("A") ? srcA : srcB).toString()));
            if (enablePreview) {
                options.add("--enable-preview");
                options.addAll(List.of("--source", String.valueOf(version.feature())));
            }
        }

        options.addAll(List.of(
                "--include", "p.*",
                "-d",  base.resolve("out").toString(),
                "--verbose", "differences,missing"));

        log.println("Options: " + options);

        return run(options);
    }

}

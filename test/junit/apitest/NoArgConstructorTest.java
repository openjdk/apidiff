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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

public class NoArgConstructorTest extends APITester {
    @Test
    public void addNoArgConstructor() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA,
                "package p;\n"
                + "public class C { }");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB,
                """
                        package p;
                        public class C {
                          /** Explicit no-args constructor. */
                          public C() { }
                        }""");

        List<String> options = List.of(
                "--api", "A",
                "--source-path", srcA.toString(),
                "--api", "B",
                "--source-path", srcB.toString(),
                "--include", "p.*",
                "-d",  base.resolve("out").toString(),
                "--verbose", "differences");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different raw doc comments for constructor p.C#C()"))
                .count();
        Assertions.assertEquals(1, differences);
    }

    @Test
    public void addPrivateNoArgConstructor() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p;
                public class C { }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB,"""
                package p;
                public class C {
                  /** Private no-args constructor. */
                  private C() { }
                }""");

        List<String> options = List.of(
                "--api", "A",
                "--source-path", srcA.toString(),
                "--api", "B",
                "--source-path", srcB.toString(),
                "--include", "p.*",
                "-d",  base.resolve("out").toString(),
                "--verbose", "missing");

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
        long notFound = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found in API 'B': constructor p.C#C()"))
                .count();
        Assertions.assertEquals(1, notFound);

    }
}

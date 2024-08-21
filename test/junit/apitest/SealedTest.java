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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

public class SealedTest extends APITester {
    final Runtime.Version version;
    final boolean enablePreview;

    SealedTest() {
        version = Runtime.version();
        Assumptions.assumeTrue(version.feature() >= 15, "sealed classes not supported in JDK " + version);

        enablePreview = switch (version.feature()) {
            case 15, 16 -> true;
            default -> false;
        };
    }

    @Test
    public void addSealedModifier() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p; public class C {
                    public class C1 { }
                    public final class C2 extends C1 { }
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p; public class C {
                    public sealed class C1 { }
                    public final class C2 extends C1 { }
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB, getClassMethodName());
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different"))
                .count();
        Assertions.assertEquals(3, differences);

    }

    @Test
    public void addNonSealedModifier() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p; public class C {
                    public sealed class C1 { }
                    public final class C2 extends C1 { }
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p; public class C {
                    public sealed class C1 { }
                    public non-sealed class C2 extends C1 { }
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB, getClassMethodName());
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different"))
                .count();
        Assertions.assertEquals(1, differences);

    }

    @Test
    public void addPermits() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p; public class C {
                    public sealed class C1 { }
                    public final class C2 extends C1 { }
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p; public class C {
                    public sealed class C1 permits C2 { }
                    public final class C2 extends C1 { }
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB, getClassMethodName());
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different"))
                .count();
        Assertions.assertEquals(0, differences);

    }

    @Test
    public void changePermits() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = Files.createDirectories(base.resolve("srcA"));
        tb.writeJavaFiles(srcA, """
                package p; public class C {
                    public sealed class C1 permits C2 { }
                    public final class C2 extends C1 { }
                    public final class C3 { }
                }""");

        Path srcB = Files.createDirectories(base.resolve("srcB"));
        tb.writeJavaFiles(srcB, """
                package p; public class C {
                    public sealed class C1 permits C3 { }
                    public final class C2 { }
                    public final class C3 extends C1 { }
                }""");

        Map<OutputKind,String> outMap = run(base, srcA, srcB, getClassMethodName());
        long differences = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Different"))
                .count();
        Assertions.assertEquals(5, differences);

    }

    private Map<OutputKind,String> run(Path base, Path srcA, Path srcB, String description) {

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
                "--description", description,
                "--include", "p.*",
                "-d",  base.resolve("out").toString(),
                "--verbose", "differences,missing"));

        log.println("Options: " + options);

        return run(options);
    }

}

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

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

public class AddRemoveElementTest extends APITester {

    @Test
    public void testAddDecls() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        Path src = base.resolve("src");
        List<String> options = new ArrayList<>();
        int apiCount = 3;
        generateAPI(src, apiCount);

        for (int a = 0; a < apiCount; a++) {
            options.addAll(List.of(
                    "--api", "api" + a,
                    "--module-source-path", src.resolve("api" + a).toString()));
        }
        for (int a = 0; a < apiCount; a++) {
            String moduleName = "m" + a;
            options.addAll(List.of("--include", moduleName + "/**"));
        }
        options.addAll(List.of("-d", base.resolve("out").toString()));
        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
        outMap.forEach((k, s) -> {
            log.println("[" + k + "]");
            log.println(s);
        });
    }

    void generateAPI(Path base, int count) throws IOException {
        for (int a = 0; a < count ; a++) {
            Path apiDir = Files.createDirectories(base.resolve("api" + a));
            for (int m = a; m < count; m++) {
                String moduleName = "m" + m;
                Path moduleDir = Files.createDirectories(apiDir.resolve(moduleName));
                StringBuilder moduleSrc = new StringBuilder();
                moduleSrc.append("module m").append(m).append(" {\n");
                for (int p = m; p < count; p++) {
                    moduleSrc.append("  exports p").append(p).append(";\n");
                    Path packageDir = Files.createDirectories(moduleDir.resolve("p" + p));
                    for (int t = p; t < count; t++) {
                        StringBuilder typeSrc = new StringBuilder();
                        typeSrc.append("package p").append(p).append(";\n")
                                .append("public class C" + t + " {\n");
                        for (int f = t; f < count; f++) {
                            typeSrc.append("    public int f" + f + ";\n");
                        }
                        typeSrc.append("}\n");
                        Files.writeString(packageDir.resolve("C" + t +".java"), typeSrc);
                    }
                }
                moduleSrc.append("}\n");
                Files.writeString(moduleDir.resolve("module-info.java"), moduleSrc);
            }
        }
    }
}

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
 * Tests for the ability to compare {@code enum}s.
 */
public class EnumTest extends APITester {

    /**
     * Tests field modifiers.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testFieldMods() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i -> {
            String mods = (i == 0) ? "public" : "protected";
            return mods + " int f;";
        });
    }

    /**
     * Tests field types.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testFieldTypes() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        testFields(base, i -> (i == 0)
                ? "public int f;"
                : "public float f;"

        );
    }

    private void testFields(Path base, Function<Integer,String> f) throws IOException {
        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path apiDir = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            Path p = new ModuleBuilder(tb, "mA")
                    .exports("p")
                    .classes("package p; public enum E {\n  A,  B,  C;\n" + f.apply(i) + "\n}\n")
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

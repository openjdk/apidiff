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

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

public class SerialCommentsTest  extends APITester {
    @Test
    public void testSerialVersionUID() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, i ->
                "private static final long serialVersionUID = " + ((i == 0) ? "123L" : "456L") + ";\n");
    }

    @Test
    public void testOverview() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, i ->
                "/**\n"
                + " * This is " + ((i == 0) ? "a" : "an updated") + " overview.\n"
                + " */\n"
                + "private static final ObjectStreamField[] serialPersistentFields = null;\n");
    }

    @Test
    public void testDefaultField() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, i ->
                "/**\n"
                + " * This is " + ((i == 0) ? "a" : "an updated") + " field.\n"
                + "*/\n"
                + "private int i;\n");
    }

    @Test
    public void testSerialPersistentField() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, i ->
                "/**\n"
                + " * @serialField i int This is " + ((i == 0) ? "a" : "an updated") + " field.\n"
                + " */\n"
                + "private static final ObjectStreamField[] serialPersistentFields = null;\n");
    }

    @Test
    public void testMethod() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, i ->
                "/**\n"
                + " * This is " + ((i == 0) ? "a" : "an updated") + " method.\n"
                + " * @param in the input stream"
                + "*/\n"
                + "private void readObject(ObjectInputStream in) { }\n");

    }

    private void test(Path base, Function<Integer, String> f) throws IOException {
        List<String> options = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            String apiName = "api" + i;
            Path src = base.resolve(apiName).resolve("src");

            String mods = (i == 0) ? "public" : "protected";
            new ModuleBuilder(tb, "m")
                    .exports("p")
                    .classes("package p;\n"
                            + "import java.io.*;\n"
                            + "public class C implements Serializable {\n"
                            + f.apply(i)
                            + "}\n")
                    .write(src);

            Path api = base.resolve(apiName).resolve("api");
            Files.createDirectories(api);
            Task.Result r = new JavadocTask(tb)
                    .sourcepath(src.resolve("m"))
                    .outdir(api)
                    .options("-noindex", "-quiet", "--module", "m")
                    .run();
            r.writeAll();

            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", src.toString(),
                    "--api-directory", api.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);

    }
}

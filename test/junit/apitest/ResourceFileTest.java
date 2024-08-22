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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.JavadocTask;

public class ResourceFileTest extends APITester {

    @Test
    public void testSingleFile() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = setup(base, "A", "", "test-resource-files/test.txt");
        Path srcB = setup(base, "B", "public void m() { }");

        Path out = base.resolve("out");

        Map<OutputKind,String> outMap = run(base,
                srcA, base.resolve("apiA"),
                srcB, base.resolve("apiB"),
                out,
                "--resource-files", "test-resource-files/test.txt");

        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found") | l.contains("Different"))
                .sorted().toList();

        checkResourceFiles(out, "test-resource-files/test.txt");
    }

    @Test
    public void testDirectory() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = setup(base, "A", "",
                "test-resource-files/test1.txt",
                "test-resource-files/test2.txt");
        Path srcB = setup(base, "B", "public void m() { }");

        Path out = base.resolve("out");

        Map<OutputKind,String> outMap = run(base,
                srcA, base.resolve("apiA"),
                srcB, base.resolve("apiB"),
                out,
                "--resource-files", "test-resource-files");

        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found") | l.contains("Different"))
                .sorted().toList();

        checkResourceFiles(out,
                "test-resource-files/test1.txt",
                "test-resource-files/test2.txt");
    }

    @Test
    public void testStandard() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path srcA = setup(base, "A", "",
                "test.svg",
                "testA.svg",
                "resource-files/test.txt",
                "resource-files/testA.txt");
        Path srcB = setup(base, "B", "public void m() { }",
                "test.svg",
                "testB.svg",
                "resource-files/test.txt",
                "resource-files/testB.txt");

        Path out = base.resolve("out");

        Map<OutputKind,String> outMap = run(base,
                srcA, base.resolve("apiA"),
                srcB, base.resolve("apiB"),
                out);

        List<String> found = outMap.get(OutputKind.ERR).lines()
                .filter(l -> l.contains("Item not found") | l.contains("Different"))
                .sorted().toList();

        String FS = File.separator;
        Predicate<String> apiA = s -> s.contains(FS + "apiA" + FS);
        Predicate<String> apiB = s -> s.contains(FS + "apiB" + FS);

        checkResourceFiles(out,
                new RFInfo("test.svg", apiB),
                new RFInfo("testA.svg", apiA),
                new RFInfo("testB.svg", apiB),
                new RFInfo("resource-files/test.txt", apiB),
                new RFInfo("resource-files/testA.txt", apiA),
                new RFInfo("resource-files/testB.txt", apiB));

    }

    Path setup(Path base, String id, String s, String... resFiles) throws IOException {
        Path src = base.resolve("src" + id);
        tb.writeJavaFiles(src,
                "package p;\n"
                        + "public class C{\n"
                        + s + "\n"
                        + "}");
        Path api = base.resolve("api" + id);
        javadoc(src, api);
        for (var resFile : resFiles) {
            addResourceFile(api, Path.of(resFile));
        }
        return src;
    }

    private void javadoc(Path src, Path out) throws IOException {
        Files.createDirectories(out);
        JavadocTask t = new JavadocTask(tb);
        t.sourcepath(src)
                .outdir(out)
                .options("-quiet", "p")
                .run()
                .writeAll();
    }

    private void addResourceFile(Path api, Path resFile) throws IOException {
        var p = api.resolve(resFile);
        Files.createDirectories(p.getParent());
        Files.writeString(p, "dummy resource: " + p);
    }

    private void checkResourceFiles(Path out, String... files) {
        for (String s : files) {
            if (Files.exists(out.resolve(s))) {
                log.println("found " + s);
            } else {
                Assertions.fail("resource file not found: " + s);
            }
        }
    }

    private record RFInfo(String file, Predicate<String> test) { }
    private void checkResourceFiles(Path out, RFInfo... infos) {
        for (var info : infos) {
            if (Files.exists(out.resolve(info.file))) {
                try {
                    String s = Files.readString(out.resolve(info.file));
                    Assertions.assertTrue(info.test.test(s), "found " + info.file + " but failed check");
                } catch (IOException e) {
                    Assertions.fail("exception " + e);
                }
            } else {
                Assertions.fail("resource file not found: " + info.file);
            }
        }
    }

    private Map<OutputKind,String> run(Path base, Path srcA, Path apiA, Path srcB, Path apiB, Path out, String...opts) {

        List<String> options = new ArrayList<>();
        for (String api : List.of("A", "B")) {
            options.addAll(List.of(
                    "--api", api,
                    "--source-path", (api.equals("A") ? srcA : srcB).toString(),
                    "--api-directory", (api.equals("A") ? apiA : apiB).toString()));
        }

        options.addAll(List.of(
                "--include", "p.*",
                "-d",  out.toString()));
        options.addAll(List.of(opts));

        log.println("Options: " + options);

        return run(options);
    }
}

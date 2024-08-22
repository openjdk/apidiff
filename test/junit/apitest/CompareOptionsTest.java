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
import java.util.Locale;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.Task;

public class CompareOptionsTest extends APITester {
    @Test
    public void testCompareAPIDescriptions() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path outDir = run(base, "--compare-api-descriptions=true");
        checkOutput(outDir.resolve("p/C.html"),
                "abc. <span class=\"diff-html-removed\">BEFORE</span><span class=\"diff-html-added\">AFTER</span>. def.");
    }

    @Test
    public void testCompareAPIDescriptionsAsText() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path outDir = run(base, "--compare-api-descriptions-as-text=true");
        checkOutput(outDir.resolve("p/C.html"),
                """
                      7 <span class="sdiffs-lines-changed"> abc. </span><span class="sdiffs-chars-changed">BEFORE</span><span class="sdiffs-lines-changed">. def.</span>
                    """,
                """
                      7 <span class="sdiffs-lines-changed"> abc. </span><span class="sdiffs-chars-changed">AFTER</span><span class="sdiffs-lines-changed">. def.</span>
                    """);
    }

    @Test
    public void testCompareDocComments() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path outDir = run(base, "--compare-doc-comments=true");
        checkOutput(outDir.resolve("p/C.html"),
                """
                      2 <span class="sdiffs-lines-changed"> abc. </span><span class="sdiffs-chars-changed">BEFORE</span><span class="sdiffs-lines-changed">. def.</span>
                    """,
                """
                      2 <span class="sdiffs-lines-changed"> abc. </span><span class="sdiffs-chars-changed">AFTER</span><span class="sdiffs-lines-changed">. def.</span>
                    """);
    }

    Path run(Path base, String... compareOpts) throws IOException {
        var options = new ArrayList<String>();
        for (var phase : List.of("before", "after")) {
            Path src = base.resolve(phase).resolve("src");
            tb.writeJavaFiles(src,
                    """
                        package p;
                        /**
                         * First sentence.
                         * abc. #PHASE#. def.
                         * Tail.
                         */
                        public class C { }
                        """.replace("#PHASE#", phase.toUpperCase(Locale.ROOT)));

            Path api = base.resolve(phase).resolve("api");
            Files.createDirectories(api);
            Task.Result r = new JavadocTask(tb)
                    .sourcepath(src)
                    .outdir(api)
                    .options("p")
                    .run();
            r.writeAll();

            options.addAll(List.of(
                    "--api", phase,
                    "--source-path", src.toString(),
                    "--api-directory", api.toString()));
        }

        Path outDir = base.resolve("out");
        options.addAll(List.of("--output-directory", outDir.toString()));
        options.addAll(List.of(compareOpts));
        options.addAll(List.of("--include", "p.*"));

        log.println(options);

        var outMap = run(options);

        return outDir;

    }
}

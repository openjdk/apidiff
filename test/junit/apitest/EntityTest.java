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

public class EntityTest extends APITester {
    @Test
    public void testEntities() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        Path outDir = run(base);
        checkOutput(outDir.resolve("p/C.html"),
                "entities aacute '\u00e1' agrave '\u00e0' nbsp '\u00A0' @-dec '@' at-hex '@' Tail.");
    }

    Path run(Path base, String... extraOpts) throws IOException {
        var options = new ArrayList<String>();
        for (var phase : List.of("before", "after")) {
            Path src = base.resolve(phase).resolve("src");
            tb.writeJavaFiles(src,
                    """
                        package p;
                        /**
                         * First sentence.
                         * abc. #PHASE#. def.
                         * entities aacute '&aacute;' agrave '&agrave;' nbsp '&nbsp;' @-dec '&#64;' at-hex '&#x40;'
                         * Tail.
                         */
                        public class C { }
                        """.replace("#PHASE#", phase.toUpperCase(Locale.ROOT)));

            Path api = base.resolve(phase).resolve("api");
            Files.createDirectories(api);
            Task.Result r = new JavadocTask(tb)
                    .sourcepath(src)
                    .outdir(api)
                    .options("-quiet", "p")
                    .run();
            r.writeAll();

            options.addAll(List.of(
                    "--api", phase,
                    "--source-path", src.toString(),
                    "--api-directory", api.toString()));
        }

        Path outDir = base.resolve("out");
        options.addAll(List.of("--output-directory", outDir.toString()));
        options.addAll(List.of(extraOpts));
        options.addAll(List.of("--include", "p.*"));

        log.println(options);

        var outMap = run(options);

        return outDir;

    }
}

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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.model.SerializedFormDocs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import apitest.lib.APITester;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SerializedFormReaderTest extends APITester {
    private Log log;
    private Path api;

    /**
     * Generates sample API documentation from sample API.
     *
     * @throws IOException if an IO exception occurs
     */
    @BeforeAll
    public void generateAPIDocs() throws IOException {
        Path base = getScratchDir();
        super.log.println(base);

        Path src = base.resolve("src");
        generateSampleAPI(src);

        // Run javadoc on sample API
        api = Files.createDirectories(base.resolve("api"));
        Task.Result r = new JavadocTask(tb)
                .sourcepath(src.resolve("m"))
                .outdir(api)
                .options("-noindex", "-quiet", "--module", "m")
                .run();
        r.writeAll();

        PrintWriter out = new PrintWriter(System.out) {
            @Override
            public void close() {
                flush();
            }
        };
        PrintWriter err = new PrintWriter(System.err, true){
            @Override
            public void close() {
                flush();
            }
        };

        log = new Log(out, err);
    }

    void generateSampleAPI(Path dir) throws IOException {
        new ModuleBuilder(tb, "m")
                .exports("p")
                .classes("package p; import java.io.*; public class NoFields implements Serializable { }")
                .classes("package p; import java.io.*; public class OneDefaultFieldNoComments  implements Serializable { int i; }")
                .classes("package p; import java.io.*; public class TwoDefaultFieldsNoComments implements Serializable { int i; int j; }")
                .classes("""
                        package p;
                        import java.io.*;
                        public class TwoDefaultFieldsWithComments implements Serializable {
                            /**
                             * This is the main description for {@code i}. This is more description for {@code i}.
                             * @serial This is the serial description for {@code i}.
                             */
                            int i;
                            /**
                             * This is the main description for {@code j}. This is more description for {@code j}.
                             * @serial This is the serial description for {@code j}.
                             */
                            int j;
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class OnePersistentField implements Serializable {
                            /**
                             * @serialField i int This is {@code i}.
                             */
                            private static final ObjectStreamField[] serialPersistentFields = null;
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class TwoPersistentFields implements Serializable {
                            /**
                             * @serialField i int This is {@code i}.
                             * @serialField j int This is {@code j}.
                             */
                            private static final ObjectStreamField[] serialPersistentFields = null;
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class SVUID implements Serializable {
                            private static final long serialVersionUID = 123L;
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class Overview implements Serializable {
                            /**
                             * This is the serialization overview.
                             */
                            private static final ObjectStreamField[] serialPersistentFields = null;
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class ReadObject implements Serializable {
                            /**
                             * This is {@code readObject}. This is more about {@code readObject}.
                             * @param in the input stream
                             */
                            private void readObject(ObjectInputStream in) { }
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class WriteObject implements Serializable {
                            /**
                             * This is {@code writeObject}. This is more about {@code writeObject}.
                             * @param out the output stream
                             * @serialData This is the serial data description.
                             */
                            private void writeObject(ObjectOutputStream out) { }
                        }
                        """)
                .classes("""
                        package p;
                        import java.io.*;
                        public class ReadWriteObject implements Serializable {
                            /**
                             * This is {@code readObject}. This is more about {@code readObject}.
                             * @param in the input stream
                             */
                            private void readObject(ObjectInputStream in) { }
                            /**
                             * This is {@code writeObject}. This is more about {@code writeObject}.
                             * @param out the output stream
                             * @serialData This is the serial data description.
                             */
                            private void writeObject(ObjectOutputStream out) { }
                        }
                        """)
                .write(dir);
    }

    @Test
    public void checkAPI() {
        Map<String, SerializedFormDocs> serializedFormDocs = SerializedFormDocs.read(log, api.resolve("serialized-form.html"));

        serializedFormDocs.forEach((name, docs) -> {
            log.flush();
            super.log.println("Type " + name);
            if (docs.getSerialVersionUID() != null) {
                super.log.println("  serialVersionUID: " + docs.getSerialVersionUID());
            }
            if (docs.getOverview() != null) {
                super.log.println("  overview: " + docs.getOverview());
            }
            docs.getFieldDescriptions().forEach((f, d) -> {
                super.log.println("  " + f + ": " + d);
            });
            docs.getMethodDescriptions().forEach((m, d) -> {
                super.log.println("  " + m + ": " + d);
            });
            super.log.println();
        });
    }
}

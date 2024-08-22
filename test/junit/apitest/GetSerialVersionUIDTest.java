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

import java.io.ObjectStreamClass;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;

import jdk.codetools.apidiff.model.SerializedForm;
import jdk.codetools.apidiff.model.SerializedFormFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import apitest.lib.APITester;

public class GetSerialVersionUIDTest extends APITester {

    ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow(() -> new Error("can't find javac"));

    public static Stream<Arguments> provideLocalClasses() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("", "int i;"),
                Arguments.of("", "Object o;"),
                Arguments.of("", "byte[] ba;"),
                Arguments.of("", "String[] sa;"),
                Arguments.of("", "int i; Object o;"),
                Arguments.of("", "int i; static Object o;"),
                Arguments.of("", "int i; transient Object o;"),
                Arguments.of("", "byte b; static int i;"),
                Arguments.of("", "byte b; static int i0 = 42;"),
                Arguments.of("", "byte b; static final int i1 = 42;"),
                Arguments.of("", "byte b; static int i2 = Integer.parseInt(\"1\");"),
                Arguments.of("", "byte b; static Object o1 = null;"),
                Arguments.of("", "byte b; static final Object o2 = null;"),
                Arguments.of("", "byte b1; static { System.out.println(\"HW\"); }"),
                Arguments.of("", """
                        int i;
                        /** @serialField i int an int field */
                        private static final ObjectStreamField[] serialPersistentFields = {
                            new ObjectStreamField("i", int.class)
                        };"""),
                Arguments.of("", "int i; static final long serialVersionUID = 123;"),
                Arguments.of("Runnable", "String[] sa; public void run() { }"),
                Arguments.of("Runnable, java.util.concurrent.Callable",
                        "String[] sa; public void run() { } public Object call() { return null; }")
        );
    }

    @ParameterizedTest
    @MethodSource("provideLocalClasses")
    public void testLocal(String interfaces, String members) throws Exception {
        log.printf("interfaces: %s; members: %s%n", interfaces, members);
        Path base = getScratchDir().resolve(getDirectory(interfaces, members));
        log.println(base);

        Path src = Files.createDirectories(base.resolve("src"));

        StringBuilder sb = new StringBuilder();
        sb.append("package p;\n")
                .append("import java.io.*;\n")
                .append("public class C implements Serializable");
        if (!interfaces.isEmpty()) {
            sb.append(", ").append(interfaces);
        }
        sb.append(" {\n");
        if (!members.isEmpty()) {
            Stream.of(members.split(";\\s*")).forEach(s -> sb.append("    ").append(s).append(";\n"));
        }
        sb.append("}\n");
        tb.writeJavaFiles(src, sb.toString());

        Path classes = base.resolve("classes");
        List<String> options = new ArrayList<>();
        options.addAll(List.of("-d", classes.toString()));
        Arrays.stream(tb.findJavaFiles(src)).map(Object::toString).forEach(options::add);
        javac.run(log, log, options.toArray(new String[0]));

        long platformSerialVersionUID = getPlatformSerialVersionUID(classes, "p.C");
        log.println("platform " + platformSerialVersionUID);

        long sourceSerialVersionUID = getSourceSerialVersionUID(src, "p.C");
        log.println("source   " + sourceSerialVersionUID);

        long classSerialVersionUID = getClassSerialVersionUID(classes, "p.C");
        log.println("class    " + classSerialVersionUID);

        Assertions.assertEquals(platformSerialVersionUID, sourceSerialVersionUID, "serialVersionUID from source");
        Assertions.assertEquals(platformSerialVersionUID, classSerialVersionUID, "serialVersionUID from class");
    }

    public static Stream<Arguments> provideSystemClasses() {
        return Stream.of(
                Arguments.of("java.lang.Exception"),
                Arguments.of("java.io.IOException"),
                Arguments.of("java.awt.Component")
        );
    }

    @ParameterizedTest
    @MethodSource("provideSystemClasses")
    public void testSystemClass(String name) throws Exception {
        log.println(name);

        Path base = getScratchDir();
        Path classes = Files.createDirectories(base.resolve("classes"));

        long platformSerialVersionUID = getPlatformSerialVersionUID(classes, name);
        log.println("platform " + platformSerialVersionUID);

        long classSerialVersionUID = getClassSerialVersionUID(classes, name);
        log.println("class    " + classSerialVersionUID);

        Assertions.assertEquals(platformSerialVersionUID, classSerialVersionUID, "serialVersionUID from class");
    }

    Path getDirectory(String interfaces, String members) {
        String i = Stream.of(interfaces.split("\\s+"))
                .map(s -> s.substring(s.lastIndexOf(".") + 1))
                .collect(Collectors.joining("-"));
        String m = Stream.of(members.split(";\\s*"))
                .map(s -> s.replaceAll("=.*", ""))
                .map(s -> s.substring(s.lastIndexOf(" ") + 1))
                .collect(Collectors.joining("-"));
        String sep = i.isEmpty() || m.isEmpty() ? "" : "-";
        return Path.of(i + sep + m);
    }

    long getPlatformSerialVersionUID(Path classes, String name) throws Exception {
        URLClassLoader cl = new URLClassLoader(new URL[] { classes.toUri().toURL()});
        Class<?> c = cl.loadClass(name);
        ObjectStreamClass osc = ObjectStreamClass.lookup(c);
        return osc.getSerialVersionUID();
    }

    long getSourceSerialVersionUID(Path src, String name) throws Exception {
        JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        List<String> options = List.of("-proc:only", "--source-path", src.toString());
        List<String> classes = List.of(name);
        List<JavaFileObject> files = List.of();
        PrintWriter out = new PrintWriter(log, true);
        JavacTask task = (JavacTask) javac.getTask(out, null, null, options, classes, files);
        task.analyze();
        SerializedFormFactory sff = getSerializedFormFactory(task);
        TypeElement te = task.getElements().getTypeElement(name);
        SerializedForm sf = sff.get(te);
        return sf.getSerialVersionUID();
    }

    long getClassSerialVersionUID(Path classes, String name) throws Exception {
        JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        List<String> options = List.of("-proc:only", "--class-path", classes.toString());
        List<String> classes2 = List.of(name);
        List<JavaFileObject> files = List.of();
        PrintWriter out = new PrintWriter(log, true);
        JavacTask task = (JavacTask) javac.getTask(out, null, null, options, classes2, files);
        task.analyze();
        SerializedFormFactory sff = getSerializedFormFactory(task);
        TypeElement te = task.getElements().getTypeElement(name);
        SerializedForm sf = sff.get(te);
        return sf.getSerialVersionUID();
    }

    private SerializedFormFactory getSerializedFormFactory(JavacTask task) {
        return new SerializedFormFactory(task.getElements(), task.getTypes(), DocTrees.instance(task));
    }
}

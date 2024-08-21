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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Notes;
import jdk.codetools.apidiff.Notes.Entry;
import jdk.codetools.apidiff.model.ElementKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;
import toolbox.ModuleBuilder;

public class NotesTest extends APITester {
    @Test
    public void testGetEntries() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        String text ="""
                # this is a comment

                http://openjdk.java.net/jeps/0 JEP 0
                  mA
                  mA/*
                  mA/p
                  mA/p.*
                  mA/p.C.*
                  mA/p.C
                  mA/p.C#f1
                  mA/p.C#m1(int)
                """;
        readNotes(base, text);

        Path src = base.resolve("src");
        for (int i = 0; i < 2; i++){
            ModuleBuilder mb = new ModuleBuilder(tb, "m" + (char)('A' + i));
            mb.classes("""
                    package p; public class C {
                      public static class Nested { }
                      public int f1;
                      public int f2;
                      public void m1(String s) { }
                      public void m1(int i) { }
                      public void m2(String s) { }
                      public void m2(int i) { }
                    }""",
                    "package p;")
                .write(src);
        }

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = c.getStandardFileManager(null, null, null);
        fm.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, List.of(src));
        List<String> options = List.of("-proc:only");
        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(tb.findFiles(".java", src));
        JavacTask t = (JavacTask) c.getTask(new PrintWriter(log, true), fm, null, options, null, files);
        t.analyze();

        Elements elements = t.getElements();
        ModuleElement mA = elements.getModuleElement("mA");
        checkEntries(mA,
                "Entry[name=mA,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=false]",
                "Entry[name=mA,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]");

        PackageElement p = elements.getPackageElement(mA, "p");
        checkEntries(p,
                "Entry[name=mA,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=false]",
                "Entry[name=mA/p,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]");

        TypeElement pC = elements.getTypeElement(mA, "p.C");
        checkEntries(pC,
                "Entry[name=mA,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p.C,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p.C,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=false]");

        TypeElement pCN = elements.getTypeElement(mA, "p.C.Nested");
        checkEntries(pCN,
                "Entry[name=mA,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]",
                "Entry[name=mA/p.C,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=true]");

        VariableElement f1 = getField(pC, "f1");
        checkEntries(f1,
                "Entry[name=mA/p.C#f1,uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=false]");
        VariableElement f2 = getField(pC, "f2");
        checkEntries(f2);

        ExecutableElement m1_int = getMethod(pC, "m1", "int");
        checkEntries(m1_int,
                "Entry[name=mA/p.C#m1(int),uri=http://openjdk.java.net/jeps/0,description=JEP 0,recursive=false]");

        ExecutableElement m1_String = getMethod(pC, "m1", "java.lang.String");
        checkEntries(m1_String);
        checkEntries(f2);

        ExecutableElement m2_int = getMethod(pC, "m2", "int");
        checkEntries(m2_int);
    }

    @Test
    public void testBadLine() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        String text = """
                # this is a comment

                http:jeps/0 JEP 0
                  m/p/C stuff
                """;
        readNotes(base, text);
        checkError("notes.txt:4: bad line:   m/p/C stuff");
    }

    @Test
    public void testBadSignature() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        String text = """
                # this is a comment

                http:jeps/0 JEP 0
                  m/p/C
                """;
        readNotes(base, text);
        checkError("notes.txt:4: bad signature: m/p/C");
    }

    @Test
    public void testBadURI() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        String text = """
                # this is a comment

                http: JEP 0
                  m/p.C
                """;
        readNotes(base, text);
        checkError("notes.txt:3: bad uri: http:");
    }

    @Test
    public void testNoCurrentURI() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        String text = """
                # this is a comment

                  m/p.C
                """;
        readNotes(base, text);
        checkError("notes.txt:3: no current URI and description");
    }

    @Test
    public void testModule() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes,"""
                http://example.com/module example-module-A
                  mA
                http://example.com/module example-module-B
                  mB
                """);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m <= a; m++) {
                new ModuleBuilder(tb, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--notes", notes.toString(),
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testPackage() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes, """
                http://example.com/module example-module-A
                  mA/*
                http://example.com/module example-module-A-p0
                  mA/p0
                http://example.com/module example-module-B
                  mB/*
                """);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m <= a; m++) {
                new ModuleBuilder(tb, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--notes", notes.toString(),
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testMethod() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes, """
                http://example.com/module example-module-A
                  mA/*
                http://example.com/module example-module-A-p0
                  mA/p0
                http://example.com/module example-module-A-p0-C0-m
                  mA/p0.C0#m()
                http://example.com/module example-module-B
                  mB/*
                """);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m <= a; m++) {
                new ModuleBuilder(tb, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { public void m() { } }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--notes", notes.toString(),
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    @Test
    public void testConstructor() throws IOException {
        Path base = getScratchDir();
        log.println(base);
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes, """
                http://example.com/module example-module-A
                  mA/*
                http://example.com/module example-module-A-p0
                  mA/p0
                http://example.com/module example-module-A-p0-C0-init
                  mA/p0.C0#<init>()
                http://example.com/module example-module-B
                  mB/*
                """);

        List<String> options = new ArrayList<>();

        int APIS = 2;
        for (int a = 0; a < APIS; a++) {
            String apiName = "api" + a;
            Path apiDir = base.resolve(apiName).resolve("src");

            for (int m = 0; m <= a; m++) {
                new ModuleBuilder(tb, "m%m%".replace("%m%", String.valueOf((char) ('A' + m))))
                        .exports("p%m%".replace("%m%", String.valueOf(m)))
                        .classes("package p%m%; public class C%m% { public C%m%() { } }\n".replace("%m%", String.valueOf(m)))
                        .write(apiDir);
            }
            options.addAll(List.of(
                    "--api", apiName,
                    "--module-source-path", apiDir.toString()));
        }
        options.addAll(List.of(
                "--notes", notes.toString(),
                "--include", "mA/**",
                "--include", "mB/**",
                "--include", "mC/**",
                "-d", base.resolve("out").toString()));

        log.println("Options: " + options);

        Map<OutputKind,String> outMap = run(options);
    }

    private String notesErr;
    private Notes notes;

    private void readNotes(Path base, String text) throws IOException {
        Path file = writeNotes(base.resolve("notes.txt"), text);
        StringWriter notesOutSW = new StringWriter();
        StringWriter notesErrSW = new StringWriter();
        Log log = new Log(new PrintWriter(notesOutSW), new PrintWriter(notesErrSW));
        try {
            notes = Notes.read(file, log);
        } finally {
            String notesOut = notesOutSW.toString();
            if (!notesOut.isEmpty()) {
                NotesTest.this.log.println("out:\n" + notesOut);
            }
            notesErr = notesErrSW.toString();
            if (!notesErr.isEmpty()) {
                NotesTest.this.log.println("err:\n" + notesErr);
            }
        }
    }


    private void checkEntries(Element e, String... expect) {
        log.println("Check entries for " + e);
        Objects.requireNonNull(e);
        ElementKey eKey = ElementKey.of(e);
        Set<Entry> entries = notes.getEntries(eKey).keySet();
        Set<String> found = entries.stream().map(Notes.Entry::toString).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(expect), found);
    }

    private void checkError(String msg) {
        if (!notesErr.contains(msg)) {
            Assertions.fail("expected message not found: " + msg);
        }
    }

    private Path writeNotes(Path file, String text) throws IOException {
        Files.createDirectories((file.getParent()));
        Files.writeString(file, text);
        return file;
    }

    VariableElement getField(TypeElement te, String name) {
        return te.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name))
                .map(e -> (VariableElement) e)
                .findFirst()
                .orElse(null);
    }

    ExecutableElement getMethod(TypeElement te, String name, String... paramTypes) {
        return te.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD
                        && e.getSimpleName().contentEquals(name))
                .map(e -> (ExecutableElement) e)
                .filter(e -> hasParams(e, paramTypes))
                .findFirst()
                .orElse(null);
    }

    private boolean hasParams(ExecutableElement e, String... paramTypes) {
        List<String> et = e.getParameters().stream()
                .map(p -> p.asType().toString())
                .collect(Collectors.toList());
        return Objects.equals(List.of(paramTypes), et);
    }
}

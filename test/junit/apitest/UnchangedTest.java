/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import apitest.lib.APITester;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import javax.tools.FileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import toolbox.ModuleBuilder;

/**
 * Tests the output for unchanged elements.
 */
public class UnchangedTest extends APITester {

    @Test
    public void testAllUnchanged() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        Path originalDir = base.resolve("original").resolve("src");
        Path updatedDir = base.resolve("updated").resolve("src");

        for (Path target : new Path[] {originalDir, updatedDir}) {
            ModuleBuilder m =
                    new ModuleBuilder(tb, "m")
                            .exports("p").exports("p2")
                            .opens("p").opens("p2")
                            .provides("java.lang.Runnable", "p.C1", "p.C2")
                            .provides("java.lang.FunctionalInterface", "p2.C")
                            .uses("java.lang.Runnable")
                            .uses("java.lang.FunctionalInterface")
                            .requiresTransitive("java.compiler")
                            .requiresTransitive("jdk.compiler");

            m.classes("""
                       package p;
                       /** class documentation */
                       public class C1 implements Runnable {
                           /** test field documentation1 */
                           public final int F1;
                           /** test field documentation2 */
                           public final int F2;
                           /** test constructor documentation1 */
                           public C1() {}
                           /** test constructor documentation2 */
                           public C1(int i) {}
                           /** test method documentation */
                           public void test() { }
                           /** run method documentation */
                           public void run() { }
                       }
                       """);

            m.classes("""
                       package p;
                       /** class documentation */
                       public class C2 implements Runnable {
                           /** test method documentation */
                           public void test() { }
                           /** run method documentation */
                           public void run() { }
                       }
                       """);

            m.classes("""
                       package p2;
                       /** class documentation */
                       public class C implements FunctionalInterface {
                           /** test method documentation */
                           public void test() { }
                           /** run method documentation */
                           public void run() { }
                       }
                       """);

            m.write(target);

            new ModuleBuilder(tb, "m2").write(target);

            options.addAll(List.of(
                    "--api", target.getParent().getFileName().toString(),
                    "--module-source-path", target.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "--include", "m2/**",
                "-d", base.resolve("out").toString(),
                "--verbose", "missing"));

        log.println("Options: " + options);

        run(options);

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/index.html");
            List<String> expectedUnchangedTextsModule =
                    List.of("""
                            Modules
                             m m2
                            """);
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/module-summary.html");
            List<String> expectedUnchangedTextsModule =
                    List.of("""
                            Exports
                             exports p exports p2
                            """,
                            """
                            Opens
                             opens p opens p2
                            """,
                            """
                            Requires
                             requires transitive java.compiler requires transitive jdk.compiler
                            """,
                            """
                            Packages
                             p p2
                            """);
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/p/package-summary.html");
            List<String> expectedUnchangedTextsModule =
                    List.of("""
                            Types
                             C1 C2
                            """);
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/p/C1.html");
            List<String> expectedUnchangedTextsModule =
                    List.of("""
                            Fields
                            public final int F1
                            public final int F2
                            """,
                            """
                            Constructors
                            public C1()
                            public C1(int i)
                            """,
                            """
                            Methods
                            public void run()
                            public void test()
                            """);
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }
    }

    @Test
    public void testSomeUnchanged() throws IOException {
        //one method in C1 with a changed javadoc, and module info changed:
        Path base = getScratchDir();
        log.println(base);

        List<String> options = new ArrayList<>();

        Path originalDir = base.resolve("original").resolve("src");
        Path updatedDir = base.resolve("updated").resolve("src");

        for (Path target : new Path[] {originalDir, updatedDir}) {
            ModuleBuilder m;
            if (target == originalDir) {
                m =
                    new ModuleBuilder(tb, "m")
                            .exports("p").exports("p2").exports("p3")
                            .opens("p").opens("p2").opens("p3")
                            .provides("java.lang.Runnable", "p.C1", "p.C2")
                            .provides("java.lang.FunctionalInterface", "p2.C")
                            .uses("java.lang.Runnable")
                            .uses("java.lang.FunctionalInterface")
                            .requiresTransitive("java.compiler")
                            .requiresTransitive("jdk.compiler");
            } else {
                m =
                    new ModuleBuilder(tb, "m")
                            .exports("p").exports("p3").exports("p4")
                            .opens("p").opens("p3").opens("p4")
                            .provides("java.lang.Runnable", "p.C1", "p4.C")
                            .provides("java.lang.FunctionalInterface", "p2.C")
                            .provides("java.io.Serializable", "p4.C")
                            .uses("java.io.Serializable")
                            .uses("java.lang.FunctionalInterface")
                            .requiresTransitive("java.compiler")
                            .requiresTransitive("java.desktop");
            }

            if (target == originalDir) {
                m.classes("""
                           package p;
                           /** class documentation */
                           public class C1 implements Runnable {
                               /** test field documentation1 */
                               public final int F1;
                               /** test field documentation2 */
                               public final int F2;
                               /** test constructor documentation1 */
                               public C1() {}
                               /** test constructor documentation2 */
                               public C1(int i) {}
                               /** test method documentation */
                               public void test() { }
                               /** run method documentation */
                               public void run() { }
                           }
                           """);
            } else {
                m.classes("""
                           package p;
                           /** class documentation */
                           public class C1 implements Runnable {
                               /** test field documentation1 - updated */
                               public final int F1;
                               /** test field documentation2 */
                               public final int F2;
                               /** test constructor documentation1 - updated */
                               public C1() {}
                               /** test constructor documentation2 */
                               public C1(int i) {}
                               /** test method documentation - updated */
                               public void test() { }
                               /** run method documentation */
                               public void run() { }
                           }
                           """);
            }

            m.classes("""
                       package p;
                       /** class documentation */
                       public class C2 implements Runnable {
                           /** test method documentation */
                           public void test() { }
                           /** run method documentation */
                           public void run() { }
                       }
                       """);

            m.classes("""
                       package p2;
                       /** class documentation */
                       public class C implements FunctionalInterface {
                           /** test method documentation */
                           public void test() { }
                           /** run method documentation */
                           public void run() { }
                       }
                       """);

            m.classes("""
                       package p3;
                       /** class documentation */
                       public class C {
                       }
                       """);

            if (target == updatedDir) {
                m.classes("""
                           package p4;
                           /** class documentation */
                           public class C implements java.io.Serializable, Runnable {
                               /** test method documentation */
                               public void test() { }
                               /** run method documentation */
                               public void run() { }
                           }
                           """);
            }

            m.write(target);

            new ModuleBuilder(tb, "m2").write(target);

            options.addAll(List.of(
                    "--api", target.getParent().getFileName().toString(),
                    "--module-source-path", target.toString()));
        }

        options.addAll(List.of(
                "--include", "m/**",
                "--include", "m2/**",
                "-d", base.resolve("out").toString(),
                "--verbose", "missing"));

        log.println("Options: " + options);

        run(options);

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/index.html");
            List<String> expectedUnchangedTextsModule =
                    List.of(" m2");
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/module-summary.html");
            List<String> expectedUnchangedTextsModule =
                    List.of(" exports p", " exports p3",
                            " opens p", " opens p3",
                            " requires transitive java.compiler",
                            " p3");
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/p/package-summary.html");
            List<String> expectedUnchangedTextsModule =
                    List.of(" C2");
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }

        {
            List<String> unchangedTexts =
                    gatherUnchangedTexts(base, "out/m/p/C1.html");
            List<String> expectedUnchangedTextsModule =
                    List.of("public final int F2\n",
                            "public C1(int i)\n",
                            "public void run()\n");
            Assertions.assertEquals(expectedUnchangedTextsModule, unchangedTexts);
        }
    }

    private List<String> gatherUnchangedTexts(Path base, String path) throws IOException {
        DocCommentTree html = parseHTML(base, path);
        List<String> unchangedTexts = new ArrayList<>();

        new DocTreeScanner<>() {
            //HTML elements which may hold the unchanged class:
            private static final Set<String> ELEMENTS_WITH_UNCHANGED_CLASS =
                    Set.of("span", "div", "li");
            private final Stack<ElementDesc> nestedElements = new Stack<>();
            private boolean unchanged;
            private final StringBuilder unchangedText = new StringBuilder();

            @Override
            public Object visitStartElement(StartElementTree node, Object p) {
                String name = node.getName().toString();
                
                if (ELEMENTS_WITH_UNCHANGED_CLASS.contains(name)) {
                    nestedElements.push(new ElementDesc(name, unchanged));

                    for (DocTree t : node.getAttributes()) {
                        String treeText = t.toString();
                        if (treeText.contains("class=") && treeText.contains("unchanged")) {
                            unchanged = true;
                        }
                    }
                }
                return null;
            }

            @Override
            public Object visitEndElement(EndElementTree node, Object p) {
                String name = node.getName().toString();
                
                if (ELEMENTS_WITH_UNCHANGED_CLASS.contains(name)) {
                    ElementDesc removed = nestedElements.pop();

                    if (!removed.name().equals(name)) {
                        throw new IllegalStateException("Unexpected name!");
                    }

                    boolean wasUnchanged = unchanged;

                    unchanged = removed.previousUnchanged();

                    if (wasUnchanged && !unchanged) {
                        String text = unchangedText.toString();

                        unchangedTexts.add(text.replaceAll("\n+", "\n"));
                        unchangedText.delete(0, unchangedText.length());
                    }
                }
                return null;
            }

            @Override
            public Object visitText(TextTree node, Object p) {
                if (unchanged) {
                    unchangedText.append(node.getBody());
                }
                return null;
            }
            
            record ElementDesc(String name, boolean previousUnchanged) {}
        }.scan(html, null);

        return unchangedTexts;
    }

    private DocCommentTree parseHTML(Path base, String path) throws IOException {
        String[] pathElements = path.split("/");
        Path fileToTest = base;
        for (String el : pathElements) {
            fileToTest = fileToTest.resolve(el);
        }
        String content = Files.readString(fileToTest);
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, null, null, null, null);
        DocTrees trees = DocTrees.instance(task);
        DocCommentTree html = trees.getDocCommentTree(new FileObject() {
            @Override
            public URI toUri() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                return "test.html";
            }

            @Override
            public InputStream openInputStream() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream openOutputStream() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return content;
            }

            @Override
            public Writer openWriter() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getLastModified() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean delete() {
                throw new UnsupportedOperationException();
            }
            
        });
        return html;
    }
}

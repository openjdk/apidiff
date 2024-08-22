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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.report.html.TextDiffBuilder;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

/**
 * Unit tests for the {@code TextDiffBuilder} class.
 */
public class TextDiffBuilderTest extends APITester {
    /**
     * Tests the behavior when the two sets of input are equal.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testEqual() throws IOException {
        List<String> list1 = lines(10);
        List<String> list2 = new ArrayList<>(list1);
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior when a line is inserted into the "modified" set.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testSimpleInsert() throws IOException {
        List<String> list1 = lines(10);
        List<String> list2 = new ArrayList<>(list1);
        list2.add(5, "inserted line");
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior when a line is removed from the "modified" set.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testSimpleDelete() throws IOException {
        List<String> list1 = lines(10);
        List<String> list2 = new ArrayList<>(list1);
        list2.remove(5);
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior when a line is changed in the "modified" set.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testSimpleChange() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list2.set(5, "changed line");
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior when multiple changes are made in the "modified" set.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testMultiple() throws IOException {
        List<String> list1 = lines(20, 32);
        List<String> list2 = new ArrayList<>(list1);
        list2.add(3, "inserted line");
        list2.set(10, "changed line");
        list2.remove(15);
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior when changes are made in different parts of the modified set,
     * such that they are presented as disjoint differences.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testDisjoint() throws IOException {
        List<String> list1 = IntStream.range(1, 50)
                .mapToObj(i -> ("line:" + i))
                .collect(Collectors.toList());
        List<String> list2 = new ArrayList<>(list1);
        list2.add(10, "insert");
        list2.set(20, "change");
        list2.remove(30);
        test(getScratchDir(), list1, list2);
    }

    void test(Path dir, List<String> list1, List<String> list2) throws IOException {
        try (PrintWriter out = wrap(System.out); PrintWriter err = wrap(System.err)) {
            Log log = new Log(out, err);
            TextDiffBuilder.SDiffs sd = new TextDiffBuilder.SDiffs();
            Content c = sd
                    .setReference("Reference Text", list1)
                    .setModified("Modified Text", list2)
                    .setContextSize(3)
                    .setShowLineNumbers(true)
                    .build(log);

            try (Writer w = Files.newBufferedWriter(dir.resolve("out.html"))) {
                HtmlTree head = HtmlTree.HEAD("utf-8", "test")
                        .add(new HtmlTree(TagName.STYLE, new Text(style)));
                HtmlTree body = HtmlTree.BODY(List.of(c));
                HtmlTree html = new HtmlTree(TagName.HTML, head, body);
                html.write(w);
            }
        }
    }

    PrintWriter wrap(PrintStream out) {
        return new PrintWriter(out) {
            @Override
            public void close() {
                flush();
            }
        };
    }

    List<String> lines(int size) {
        return lines(size, 64);
    }

    List<String> lines(int lineCount, int lineLength) {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Pattern ws = Pattern.compile("\\s");
        Matcher m = ws.matcher(lorem_ipsum);
        int start = 0;
        while (m.find()) {
            if (sb.length() + (m.start() - start) > lineLength || m.group().equals("\n")) {
                lines.add(sb.toString());
                if (lines.size() > lineCount) {
                    return lines;
                }
                sb = new StringBuilder();
            }
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lorem_ipsum, start, m.start());
            start = m.end();
        }
        sb.append(lorem_ipsum.substring(start));
        lines.add(sb.toString());
        System.err.println(lines);
        return lines;
    }

    private static final String style = """
            div.sdiffs {
                display: grid;
                grid-template-columns: auto auto;
                grid-column-gap: 10px;
                margin: 2px 10px;
                padding: 2px 2px;
                border: 1px solid grey;
            }
            .sdiffs div.sdiffs-ref { grid-column: 1; overflow-x: auto }
            .sdiffs div.sdiffs-mod { grid-column: 2; overflow-x: auto }
            .sdiffs span.sdiffs-title { margin-left:2em; text-weight: bold }
            .sdiffs span.sdiffs-changed { color: blue }
            """;

    private static final String lorem_ipsum = LoremIpsum.text;
}

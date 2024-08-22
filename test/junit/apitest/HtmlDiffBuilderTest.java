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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.report.html.HtmlDiffBuilder;
import jdk.codetools.apidiff.report.html.ResultTable.CountKind;

import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

/**
 * Unit tests for the {@code TextDiffBuilder} class.
 */
public class HtmlDiffBuilderTest extends APITester {
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

    /**
     * Tests the behavior if a style is added.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testStyleAdded() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list2.set(5, list2.get(5).replaceAll("^(\\S+\\s+)(\\S+)(.*)", "$1<i>$2</i>$3"));
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior if the change is just to the HTML style.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testStyleChange() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list1.set(5, list1.get(5).replaceAll("^(\\S+\\s+)(\\S+)(.*)", "$1<b>$2</b>$3"));
        list2.set(5, list2.get(5).replaceAll("^(\\S+\\s+)(\\S+)(.*)", "$1<i>$2</i>$3"));
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior if a style is removed.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testStyleRemoved() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list1.set(5, list1.get(5).replaceAll("^(\\S+\\s+)(\\S+)(.*)", "$1<i>$2</i>$3"));
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior if a block element, like a heading, is changed.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testHeadingChanged() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list1.set(3, "<h3>" + list1.get(3) + "</h3>");
        list2.set(3, "<h4>" + list2.get(3) + "</h4>");
        test(getScratchDir(), list1, list2);
    }

    /**
     * Tests the behavior if a link is changed.
     *
     * @throws IOException if an IO exception occurs
     */
    @Test
    public void testLinkChanged() throws IOException {
        List<String> list1 = lines(20);
        List<String> list2 = new ArrayList<>(list1);
        list1.set(3, "<a href=\"old.html\">" + list1.get(3) + "</a>");
        list2.set(3, "<a href=\"new.html\">" + list2.get(3) + "</a>");
        test(getScratchDir(), list1, list2);
    }

    void test(Path dir, List<String> list1, List<String> list2) throws IOException {
        test(dir, String.join("<br>\n", list1), String.join("<br>\n", list2));
    }

    void test(Path dir, String html1, String html2) throws IOException {
        try (PrintWriter out = wrap(System.out); PrintWriter err = wrap(System.err)) {
            APIMap<String> apiMap = APIMap.of();
            apiMap.put(new TestAPI("api1"), html1);
            apiMap.put(new TestAPI("api2"), html1);
            Log log = new Log(out, err);
            Messages msgs = Messages.instance("jdk.codetools.apidiff.report.html.resources.report");
            HtmlDiffBuilder b = new HtmlDiffBuilder(apiMap.keySet(), log, msgs);
            Map<CountKind, Integer> counts = new EnumMap<>(CountKind.class);
            List<Content> c = b.build(apiMap, ck -> counts.put(ck, counts.computeIfAbsent(ck, ck_ -> 0) + 1));
            try (Writer w = Files.newBufferedWriter(dir.resolve("out.html"))) {
                HtmlTree head = HtmlTree.HEAD("utf-8", "test")
                        .add(new HtmlTree(TagName.STYLE, new Text(style)));
                HtmlTree body = HtmlTree.BODY(c);
                HtmlTree html = new HtmlTree(TagName.HTML, head, body);
                html.write(w);
            }
            log.out.println("Counts: " + counts);
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
            // wrap long lines
            if (sb.length() + (m.start() - start) > lineLength) {
                lines.add(sb.toString());
                if (lines.size() > lineCount) {
                    return lines;
                }
                sb = new StringBuilder();
            }

            // append word
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lorem_ipsum, start, m.start());

            // handle explicit newline
            if (m.group().equals("\n")) {
                lines.add(sb.toString());
                if (lines.size() > lineCount) {
                    return lines;
                }
                sb = new StringBuilder();
            }

            start = m.end();
        }
        sb.append(lorem_ipsum.substring(start));
        lines.add(sb.toString());
        System.err.println(lines);
        return lines;
    }

    // TODO: consider using apidiff.css either by linking to it
    //       or copying it inline.
    private static final String style = """
            div.hdiffs {
                margin: 2px 10px;
                padding: 2px 2px;
                border: 1px solid grey;
            }

            div.hdiffs-title {
                padding-left: 2em;
                text-weight: bold;
                background-color: #eee;
                border-bottom: 1px solid grey;
                margin-bottom: 5px;}
            .hdiffs span.diff-html-added { background-color: #bfb }
            .hdiffs span.diff-html-changed { background-color: #ffb }
            .hdiffs span.diff-html-removed { background-color: #fbb; }
            """;

    private static final String lorem_ipsum = LoremIpsum.text;
}

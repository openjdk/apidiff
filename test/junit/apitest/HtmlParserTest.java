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
import java.util.function.Supplier;

import jdk.codetools.apidiff.model.HtmlParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import apitest.lib.APITester;

public class HtmlParserTest extends APITester {
    @Test
    public void testSimple() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, "<tag>content</tag>",
                "Start: tag {} false",
                "Content: content",
                "End: tag");
    }

    @Test
    public void testAttr() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, "<tag a1=\"value\" a2='value' a3=value>",
                "Start: tag {a1=value, a2=value, a3=value} false");
    }

    @Test
    public void testMultilineContent() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, "<tag>line 1\nline 2</tag>",
                "Start: tag {} false",
                "Content: line 1\\n",
                "Content: line 2",
                "End: tag");
    }

    @Test
    public void testComment() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base, "<tag>before<!--comment-->after</tag>",
                "Start: tag {} false",
                "Content: before",
                "Comment: comment",
                "Content: after",
                "End: tag");
    }

    @Test
    public void testDocType() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base,"<!doctype html><head>",
                "DocType: doctype html",
                "Start: head {} false");
    }

    @Test
    public void testSample() throws IOException {
        Path base = getScratchDir();
        log.println(base);

        test(base,"<html><head><title>This is the title</title></head><body>This is content<a href=\"#foo\">link</a></body></html>",
                "Start: html {} false",
                "Start: head {} false",
                "Start: title {} false",
                "Content: This is the title",
                "End: title",
                "End: head",
                "Start: body {} false",
                "Content: This is content",
                "Start: a {href=#foo} false",
                "Content: link",
                "End: a",
                "End: body",
                "End: html");
    }

    private void test(Path base, String html, String... expect) throws IOException {
        // avoid using .html extension, to avoid 'tidy' errors
        Path file = base.resolve("test.htmlx");
        Files.writeString(file, html);
        List<String> list = new ArrayList<>();
        HtmlParser p = new HtmlParser() {
            @Override
            public void startElement(String name, Map<String, String> attrs, boolean selfClosing) {
                record("Start: " + name + " " + attrs + " " + selfClosing);
            }

            @Override
            public void endElement(String name) {
                record("End: " + name);

            }

            @Override
            public void content(Supplier<String> content) {
                record("Content: " + content.get().replace("\n", "\\n"));
            }

            @Override
            public void comment(Supplier<String> comment) {
                record("Comment: " + comment.get().replace("\n", "\\n"));
            }

            @Override
            public void doctype(Supplier<String> doctype) {
                record("DocType: " + doctype.get().replace("\n", "\\n"));
            }

            @Override
            protected void error(Path file, int lineNumber, String message) {
                System.err.println("Error: " + file + ":" + lineNumber + ":" + message);
            }

            @Override
            protected void error(Path file, int lineNumber, Throwable t) {
                System.err.println("Error: " + file + ":" + lineNumber + ":" + t);

            }

            private void record(String msg) {
                System.out.println(msg);
                list.add(msg);
            }
        };

        p.read(file);
        Assertions.assertEquals(List.of(expect), list);
    }
}

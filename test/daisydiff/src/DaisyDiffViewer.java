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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlAttr;
import jdk.codetools.apidiff.html.HtmlTag;
import jdk.codetools.apidiff.html.HtmlTree;
import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class DaisyDiffViewer {
    public static void main(String... args) {
        new DaisyDiffViewer().run(args);
    }

    public void run(String... args) {

        JTextArea leftArea = new JTextArea(10, 40);
        leftArea.setLineWrap(true);

        JTextArea rightArea = new JTextArea(10, 40);
        rightArea.setLineWrap(true);

        JSplitPane leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(leftArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                new JScrollPane(rightArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

        JTextArea diffArea = new JTextArea();
        diffArea.setLineWrap(true);

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int prefWidth = (int) (screenSize.width * .8);
        int prefHeight = screenSize.height / 2;
        diffArea.setPreferredSize(new Dimension(prefWidth, prefHeight));

        JSplitPane topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                leftRight,
                new JScrollPane(diffArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
                );

        JPanel body = new JPanel(new BorderLayout());
        body.add(topBottom, BorderLayout.CENTER);

        JButton goBtn = new JButton("Go");
        goBtn.addActionListener(ev -> go(leftArea, rightArea, diffArea));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnPanel.add(goBtn);
        body.add(btnPanel, BorderLayout.SOUTH);

        JFrame frame = new JFrame();
        frame.getContentPane().add(body);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    void go(JTextArea leftArea, JTextArea rightArea, JTextComponent result) {
        System.err.println("go");
        String leftText = leftArea.getText();
        String rightText = rightArea.getText();
        try {
            String diff = diffHtml(leftText, rightText);
            result.setText(diff);
        } catch (Exception e) {
            result.setText(e.toString());
        }
    }


    String diffHtml(String oldText, String newText) {
        try {
            Reader oldStream = new StringReader(oldText);
            Reader newStream = new StringReader(newText);
            Handler handler = new Handler();
            diffHtml(oldStream, newStream, handler);
            StringWriter sw = new StringWriter();
            handler.doc.write(sw);
            return sw.toString();
        } catch (IOException | SAXException e) {
            return "Exception: " + e;
        }
    }

    // This method is an extract from the DaisyDiff main program, lines 120-157,
    // the body of "if (htmlDiff) { ... }". It runs the HtmlCleaner on the input
    // text prior to calling HtmlDiffer.
    void diffHtml(Reader oldStream, Reader newStream, ContentHandler postProcess)
            throws IOException, SAXException {

        Locale locale = Locale.getDefault();
        String prefix = "diff";

        HtmlCleaner cleaner = new HtmlCleaner();

        InputSource oldSource = new InputSource(oldStream);
        InputSource newSource = new InputSource(newStream);

        DomTreeBuilder oldHandler = new DomTreeBuilder();
        cleaner.cleanAndParse(oldSource, oldHandler);
        System.out.print(".");
        TextNodeComparator leftComparator = new TextNodeComparator(
                oldHandler, locale);

        DomTreeBuilder newHandler = new DomTreeBuilder();
        cleaner.cleanAndParse(newSource, newHandler);
        System.out.print(".");
        TextNodeComparator rightComparator = new TextNodeComparator(
                newHandler, locale);

        postProcess.startDocument();
        postProcess.startElement("", "diffreport", "diffreport",
                new AttributesImpl());
//        doCSS(css, postProcess);
        postProcess.startElement("", "diff", "diff",
                new AttributesImpl());
        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess,
                prefix);

        HTMLDiffer differ = new HTMLDiffer(output);
        differ.diff(leftComparator, rightComparator);
        System.out.print(".");
        postProcess.endElement("", "diff", "diff");
        postProcess.endElement("", "diffreport", "diffreport");
        postProcess.endDocument();
    }

    class Handler implements ContentHandler {
        Stack<HtmlTree> stack;
        StringBuilder text;
        HtmlTree doc;

        Handler() {
            stack = new Stack<>();
            text = new StringBuilder();
        }

        Content toContent() {
            return stack.peek();
        }


        @Override
        public void setDocumentLocator(Locator locator) {
            // should not happen
        }

        @Override
        public void startDocument() {
            stack.push(new HtmlTree(HtmlTag.DIV));
        }

        @Override
        public void endDocument() {
            doc = stack.pop();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            // should not happen
        }

        @Override
        public void endPrefixMapping(String prefix) {
            // should not happen
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            flushText();
            switch (localName) {
                // ignore possibility of <html>, <head>, <body> etc for now
                case "diffreport":
                case "diff":
                    return;
            }
            try {
                HtmlTag tag = HtmlTag.valueOf(localName.toUpperCase(Locale.US));
                HtmlTree tree = new HtmlTree(tag);
                for (int i = 0; i < atts.getLength(); i++) {
                    String name = atts.getLocalName(i);
                    String value = atts.getValue(i);
                    if (tag == HtmlTag.SPAN) {
                        // if changeId is found, we might want to ignore id as well,
                        // to avoid duplicate ids across different diff blocks
                        switch (name) {
                            case "changes":
                            case "changeId":
                            case "next":
                            case "previous":
                                System.err.println("!! " + name + " '" + value + "'");
                                continue;
                        }
                    }
                    try {
                        HtmlAttr a = HtmlAttr.valueOf(name.toUpperCase(Locale.US));
                        tree.set(a, value);
                    } catch (IllegalArgumentException e) {
                        System.err.println("unknown attribute name: " + localName + "; " + e);
                    }
                }
                stack.peek().add(tree);
                stack.push(tree);
            } catch (IllegalArgumentException e) {
                System.err.println("unknown element name: " + localName + "; " + e);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            flushText();
            switch (localName) {
                // ignore possibility of <html>, <head>, <body> etc for now
                case "diffreport":
                case "diff":
                    return;
            }
            HtmlTree tree = stack.pop();
            if (!tree.tag.name().equals(localName.toUpperCase(Locale.US))) {
                System.err.println("popping unbalanced tree node: expect: " + localName + ", found " + tree.tag);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) {
            // should not happen
        }

        @Override
        public void skippedEntity(String name) {
            // should not happen
            // Note:
            // known entities are translated into the equivalent character; e.g. &lt; to <
            // unknown entities are handled as literal strings; e.g. &foo; remains as &foo;
        }

        private void flushText() {
            if (text.length() > 0) {
                stack.peek().add(text.toString());
                text.setLength(0);
            }
        }
    }

    class Handler2 implements ContentHandler {
        List<String> info = new ArrayList<>();

        Handler2() { }

        java.util.List<String> toList() {
            return info;
        }


        @Override
        public void setDocumentLocator(Locator locator) {
            info.add("setDocumentLocator: " + locator);
        }

        @Override
        public void startDocument() {
            info.add("startDocument");
        }

        @Override
        public void endDocument() {
            info.add("startDocument");
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            info.add("startPrefixMapping " + prefix + " " + uri);

        }

        @Override
        public void endPrefixMapping(String prefix) {
            info.add("endPrefixMapping " + prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            Map<String,String> map = new TreeMap<>();
            for (int i = 0; i < atts.getLength(); i++) {
                String name = atts.getLocalName(i);
                String value = atts.getValue(i);
                map.put(name, value);
            }
            info.add("startElement " + uri + " "  + localName + " " + qName + " " + map);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            info.add("endElement " + uri + " " + localName + " " + qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            String s = new String(ch, start, length);
            if (s.length() > 30) {
                s = s.substring(0, 10) + "..." + s.substring(s.length() - 10);
            }
            info.add("characters " + s);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            String s = new String(ch, start, length);
            if (s.length() > 30) {
                s = s.substring(0, 10) + "..." + s.substring(s.length() - 10);
            }
            info.add("ignorableWhitespace '" + s + "' (" + length + ")");
        }

        @Override
        public void processingInstruction(String target, String data) {
            info.add("processingInstruction " + target + " " + data);
        }

        @Override
        public void skippedEntity(String name) {
            info.add("skippedEntity " + name);
        }
    }

}

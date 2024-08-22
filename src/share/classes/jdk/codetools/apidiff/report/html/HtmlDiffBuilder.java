/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.codetools.apidiff.report.html;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlAttr;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.RawHtml;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.report.html.ResultTable.CountKind;

import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CommentNode;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SpecialEntities;
import org.htmlcleaner.SpecialEntity;
import org.htmlcleaner.TagNode;

import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTree;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A class to build HTML to display the differences between strings containing HTML,
 * such as may be found in the documentation for corresponding elements in different
 * instances of an API.
 */
public class HtmlDiffBuilder extends PairwiseDiffBuilder<String> {

    /**
     * Creates an instance of a {@code HtmlDiffBuilder}.
     *
     * @param pageReporter the reporter for the parent page
     */
    public HtmlDiffBuilder(PageReporter<?> pageReporter) {
        super(pageReporter.parent.apis, pageReporter.log, pageReporter.msgs);
    }

    /**
     * Creates an instance of a {@code HtmlDiffBuilder}.
     *
     * @param log the log to which to report any problems while using this builder
     */
    public HtmlDiffBuilder(Set<API> apis, Log log, Messages msgs) {
        super(apis, log, msgs);
    }

    @Override
    protected Content build(List<API> refAPIs, String refItem,
                            List<API> focusAPIs, String focusItem,
                            Consumer<ResultTable.CountKind> counter) {
        String refNames = getNameList(refAPIs);
        String focusNames = getNameList(focusAPIs);
        if (refItem != null && focusItem != null) {
            // item in both groups: display the comparison
            Content title = Text.of(msgs.getString("htmldiffs.comparing", refNames, focusNames));
            return build(title, refItem, focusItem, counter);
        } else if (refItem == null) {
            Content title = Text.of(msgs.getString("htmldiffs.not-in-only-in", refNames, focusNames));
            counter.accept(CountKind.DESCRIPTION_ADDED);
            return build(title, focusItem);
        } else {
            Content title = Text.of(msgs.getString("htmldiffs.only-in-not-in", refNames, focusNames));
            counter.accept(CountKind.DESCRIPTION_REMOVED);
            return build(title, refItem);
        }
    }

    @Override
    protected String getKeyString(String item) {
        return item;
    }

    public Content build(Content title, String refItem, String modItem,
                                           Consumer<ResultTable.CountKind> counter) {
        try {
            Reader oldStream = new StringReader(refItem);
            Reader newStream = new StringReader(modItem);
            Handler handler = new Handler(log);
            diffHtml(oldStream, newStream, handler);

            // TODO: consider styling the titles (will need custom formatter)
            // msgs.getString("htmldiffs.comparing", refTitle, modTitle)
            HtmlTree title2 = HtmlTree.DIV(title)
                    .setClass("hdiffs-title");
            HtmlTree doc = handler.doc;
            count(doc, counter);
            return HtmlTree.DIV(title2, doc).setClass("hdiffs");
        } catch (IOException | SAXException e) {
            log.error("htmldiffs.err.exception-in-diff", e);
            return Content.empty;
        }

    }

    private Content build(Content title, String item) {
        HtmlTree title2 = HtmlTree.DIV(title)
                .setClass("hdiffs-title");

        Content html = new RawHtml(item);
        List<Content> contents = List.of(title2, html);
        return new HtmlTree(TagName.DIV, contents).setClass("hdiffs");
    }

    // This method is a minimally edited extract from the DaisyDiff main program,
    // lines 120-157, the body of "if (htmlDiff) { ... }".
    // It runs the HtmlCleaner on the input text prior to calling HtmlDiffer,
    // and uses the provided content handler to process the result.
    void diffHtml(Reader oldStream, Reader newStream, ContentHandler postProcess)
            throws IOException, SAXException {

        Locale locale = Locale.getDefault();
        String prefix = "diff";

//        HtmlCleaner cleaner = new HtmlCleaner();

//        InputSource oldSource = new InputSource(oldStream);
//        InputSource newSource = new InputSource(newStream);

        DomTreeBuilder oldHandler = new DomTreeBuilder();
//        cleaner.cleanAndParse(oldSource, oldHandler);
        cleanAndParse(oldStream, oldHandler);
//        System.out.print(".");
        TextNodeComparator leftComparator = new TextNodeComparator(
                oldHandler, locale);

        DomTreeBuilder newHandler = new DomTreeBuilder();
//        cleaner.cleanAndParse(newSource, newHandler);
        cleanAndParse(newStream, newHandler);
//        System.out.print(".");
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
//        System.out.print(".");
        postProcess.endElement("", "diff", "diff");
        postProcess.endElement("", "diffreport", "diffreport");
        postProcess.endDocument();
    }

    private void cleanAndParse(Reader in, DomTreeBuilder builder) throws IOException, SAXException {
        HtmlCleaner cleaner = new HtmlCleaner();
        builder.startDocument();
        convert(cleaner.clean(in), builder);
        builder.endDocument();
    }

    private void convert(BaseToken node, DomTreeBuilder builder) throws SAXException {
        if (node instanceof TagNode t) {
            String name = t.getName();
            AttributesImpl attrs = new AttributesImpl();
            t.getAttributes().forEach((k, v) -> attrs.addAttribute("", k, k, "CDATA", v));
            builder.startElement("", name, name, attrs);
            for (var c : t.getAllChildren()) {
                convert(c, builder);
            }
            builder.endElement("", name, name);
        } else if (node instanceof ContentNode c) {
            var s = handleEntities(c.getContent());
            var chars = new char[s.length()];
            s.getChars(0, s.length(), chars, 0);
            builder.characters(chars, 0, chars.length);
        } else if (node instanceof CommentNode c) {
            // ignore, at least for now: it's just a comment
        } else {
            throw new IllegalArgumentException(node.getClass().toString());
        }
    }

    private static final Pattern entity = Pattern.compile("(?i)&(?:(?<name>[a-z][a-z0-9]*)|#(?<dec>[0-9]+)|#x(?<hex>[0-9a-f]+))(;)?");
    private String handleEntities(String s) {
        StringBuilder sb = null;

        var m = entity.matcher(s);
        while (m.find()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            String g;
            if ((g = m.group("name")) != null) {
                SpecialEntity e = SpecialEntities.INSTANCE.getSpecialEntity(g);
                if (e != null) {
                    m.appendReplacement(sb, String.valueOf(e.charValue()));
                } else {
                    m.appendReplacement(sb, m.group(0));
                }
            } else if ((g = m.group("dec")) != null) {
                m.appendReplacement(sb, escapeReplacementCharacter((char) Integer.parseInt(g)));
            } else if ((g = m.group("hex")) != null) {
                m.appendReplacement(sb, escapeReplacementCharacter((char) Integer.parseInt(g, 16)));
            } else {
                // should not happen, but if it does ...
                m.appendReplacement(sb, m.group(0));
            }
        }

        if (sb != null) {
            m.appendTail(sb);
            return sb.toString();
        } else {
            return s;
        }
    }

    private String escapeReplacementCharacter(char ch) {
        return switch (ch) {
            case '\\' -> "\\\\";
            case '$' -> "\\$";
            default -> String.valueOf(ch);
        };
    }

    private void show(DomTree tree, PrintStream out) {
        show(tree.getBodyNode(), out,0);
    }

    private void show(org.outerj.daisy.diff.html.dom.Node node, PrintStream out, int depth) {
        var indent = "    ".repeat(depth);
        if (node instanceof org.outerj.daisy.diff.html.dom.TagNode tagNode) {
            out.println(indent + tagNode.getOpeningTag());
            for (var c : tagNode) {
                show(c, out, depth + 1);
            }
        } else if (node instanceof org.outerj.daisy.diff.html.dom.TextNode textNode) {
            out.println(indent + textNode.getText());
        } else {
            out.println(indent + node.getClass().getSimpleName());
        }
    }

    private void count(HtmlTree tree, Consumer<CountKind> counter) {
        for (int i = 0; i < tree.contents().size(); i++) {
            HtmlTree t = getChildAsTree(tree.contents(), i);
            if (t == null) {
                continue;
            }
            if (isDiffSpan(t)) {
                Set<String> set = new HashSet<>();
                while (t != null && isDiffSpan(t)) {
                    set.add(t.get(HtmlAttr.CLASS));
                    t = getChildAsTree(tree.contents(), ++i);
                }
                if (set.contains("diff-html-changed")
                        || (set.contains("diff-html-added")
                            && set.contains("diff-html-removed"))) {
                    counter.accept(CountKind.DESCRIPTION_CHANGED);
                } else if (set.contains("diff-html-added")) {
                    counter.accept(CountKind.DESCRIPTION_ADDED);
                } else {
                    counter.accept(CountKind.DESCRIPTION_REMOVED);
                }
            } else {
                count(t, counter);
            }
        }
    }

    private HtmlTree getChildAsTree(List<Content> contents, int i) {
        if (i < contents.size()) {
            Content c = contents.get(i);
            if (c instanceof HtmlTree) {
                return (HtmlTree) c;
            }
        }
        return null;
    }

    private boolean isDiffSpan(HtmlTree tree) {
        if (tree.hasTag(TagName.SPAN)) {
            String classAttr = tree.get(HtmlAttr.CLASS);
            if (classAttr != null) {
                switch (classAttr) {
                    case "diff-html-added":
                    case "diff-html-changed":
                    case "diff-html-removed":
                        return true;
                }
            }
        }
        return false;
    }

    static class Handler implements ContentHandler {
        /** The log, for reporting any errors. */
        private final Log log;
        /** The stack of {@code HtmlTree} nodes being constructed. */
        private final Stack<HtmlTree> stack;
        /** A buffer for sequences of characters. */
        private final StringBuilder text;

        HtmlTree doc;

        Handler(Log log) {
            this.log = log;
            stack = new Stack<>();
            text = new StringBuilder();
        }

        /**
         * Returns the generated tree, after {@code endDocument} has been called.
         *
         * @return the generated tree.
         */
        public HtmlTree getDoc() {
            return doc;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            // should not happen
        }

        @Override
        public void startDocument() {
            stack.push(new HtmlTree(TagName.DIV));
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

            HtmlTree tree;
            try {
                tree = new HtmlTree(TagName.of(localName));
            } catch (IllegalArgumentException e) {
                log.warning("htmldiffs.warn.unknown-tag-name", localName);
                tree = new HtmlTree(localName);
            }

            for (int i = 0; i < atts.getLength(); i++) {
                String name = atts.getLocalName(i);
                String value = atts.getValue(i);
                if (tree.hasTag(TagName.SPAN)) {
                    switch (name) {
                        case "changes":
                            // The value is an HTML fragment that describes the change.
                            // It may contain simple phrasing elements, as well as simple or nested lists.
                            // Convert it to a child node to display as a rich-text tooltip with CSS.
                            tree.add(getChangeTooltip(value));
                            continue;

                        case "changeId":
                        case "next":
                        case "previous":
                            continue;

                        case "id":
                            // if changeId is present, skip id to avoid duplicates;
                            // an alternative would be to make the names unique
                            if (atts.getIndex("changeId") != -1) {
                                continue;
                            }
                    }
                }

                try {
                    HtmlAttr a = HtmlAttr.of(name);
                    tree.set(a, value);
                } catch (IllegalArgumentException e) {
                    log.warning("htmldiffs.warn.unknown-attribute-name", localName, name);
                    tree.set(name, value);
                }
            }

            stack.push(tree);
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
            if (!tree.hasTag(localName)) {
                log.err.println("popping unbalanced tree node: expect: " + localName + ", found " + tree.getTagString());
            }

            // DaisyDiff may generate empty <span class="diff-html-added"></span> and
            // <span class="diff-html-removed"></span> nodes, which get flagged by "tidy", so remove them
            if (isEmptyDiff(tree)) {
                return;
            }

            HtmlTree top = stack.peek();
            // DaisyDiff may generate adjacent nodes that can be merged
            if (canMergeWithPrevious(top, tree)) {
                getPrevious(top).contents().addAll(tree.contents());
            } else {
                top.add(tree);
            }
        }

        private boolean isEmptyDiff(HtmlTree tree) {
            return isDiffAddedRemoved(tree) && tree.contents().isEmpty();
        }

        private boolean canMergeWithPrevious(HtmlTree container, HtmlTree tree) {
            if (!container.contents().isEmpty() && isDiffAddedRemoved(tree)) {
                Content prev = getLast(container.contents());
                if (prev instanceof HtmlTree prevTree) {
                    if (isDiffAddedRemoved(prevTree)) {
                        return tree.get(HtmlAttr.CLASS).equals(prevTree.get(HtmlAttr.CLASS));
                    }
                }
            }
            return false;
        }

        private HtmlTree getPrevious(HtmlTree container) {
            return (HtmlTree) getLast(container.contents());
        }

        private <T> T getLast(List<T> list) {
            return list.get(list.size() - 1);
        }

        private boolean isDiffAddedRemoved(HtmlTree tree) {
            if (tree.hasTag(TagName.SPAN)) {
                String classAttr = tree.get(HtmlAttr.CLASS);
                if (classAttr != null) {
                    switch (classAttr) {
                        case "diff-html-added":
                        case "diff-html-removed":
                            return true;
                    }
                }
            }
            return false;
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

        private HtmlTree getChangeTooltip(String html) {
            // We must return a span (or equivalent phrasing content), but the argument may contain a
            // small number of block items, such as <ul> and <li>. Therefore we "cheat" and convert them
            // to <span> elements with an appropriate class, and fix the display in the CSS.
            List<Content> contents = new ArrayList<>();
            Pattern p = Pattern.compile("(?i)<(/?)([a-z][a-z0-9]*)([^>]*)>");
            Matcher m = p.matcher(html);
            int start = 0;
            while (m.find(start)) {
                if (m.start() > start) {
                    contents.add(new Text(html.substring(start, m.start())));
                }
                if (m.group(1).isEmpty()) {
                    switch (m.group(2)) {
                        case "ul", "li" -> {
                            // ignore any existing attributes in group 3 -- e.g. class
                            contents.add(new RawHtml("<span class=\"hdiffs-tip-" + m.group(2) + "\">"));
                        }
                        case "br" -> {
                            // ignore <br/> scattered in the text
                        }
                        default -> contents.add(new RawHtml(m.group()));
                    }
                } else {
                    switch (m.group(2)) {
                        case "ul", "li" -> contents.add(new RawHtml("</span>"));
                        default -> contents.add(new RawHtml(m.group()));
                    }
                }
                start = m.end();
            }
            if (start < html.length()) {
                contents.add(new Text(html.substring(start)));
            }
            return HtmlTree.SPAN(contents).setClass("hdiffs-tooltip");
        }

        private void flushText() {
            if (text.length() > 0) {
                stack.peek().add(text);
                text.setLength(0);
            }
        }
    }


}

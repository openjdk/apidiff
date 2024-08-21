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

package jdk.codetools.apidiff.html;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * An HTML element.
 */
public final class HtmlTree extends Content {

    private static final boolean coalesceText = Boolean.getBoolean("coalesceText");


    //-----------------------------------------------------
    //
    // Head items

    /**
     * Creates a {@code <head>} with a given charset and title.
     *
     * @param charset the charset
     * @param title the title
     * @return the {@code <head>} element
     */
    public static HtmlTree HEAD(String charset, String title) {
        return new HtmlTree(TagName.HEAD)
                .add(new HtmlTree(TagName.META)
                        .set(HtmlAttr.CHARSET, charset))
                .add(new HtmlTree(TagName.TITLE).add(title));
    }

    /**
     * Creates a {@code <base>} with a given {@code href} attribute.
     *
     * @param href the value for the {@code href} attribute
     * @return the {@code <base>} element
     */
    public static HtmlTree BASE(String href) {
        return new HtmlTree(TagName.BASE)
                .set(HtmlAttr.HREF, encodeURL(href));
    }

    /**
     * Creates a {@code <link>} with a given {@code rel} and {@code href} attributes.
     *
     * @param rel the value for the {@code rel} attribute
     * @param href the value for the {@code href} attribute
     * @return the {@code <link>} element
     */
    public static HtmlTree LINK(String rel, String href) {
        return new HtmlTree(TagName.LINK)
                .set(HtmlAttr.REL, rel)
                .set(HtmlAttr.TYPE, MediaType.forPath(href).contentType)
                .set(HtmlAttr.HREF, encodeURL(href));
    }

    /**
     * Creates a {@code <link>} for an icon for the page.
     *
     * @param iconURI the value for the {@code href} attribute
     * @return the {@code <link>} element
     */
    public static HtmlTree LINK_ICON(String iconURI) {
        // see https://stackoverflow.com/questions/48956465/favicon-standard-2018-svg-ico-png-and-dimensions
        // for suggestion that IE expects "shortcut icon"
        HtmlTree link =  LINK("icon", iconURI);
        Pattern p = Pattern.compile("[0-9]+x[0-9]+");
        Matcher m = p.matcher(iconURI);
        if (m.find()) {
            link.set(HtmlAttr.SIZES, m.group(0));
        }
        return link;
    }

    /**
     * Creates a {@code <link>} for a stylesheet for the page.
     *
     * @param stylesheetURI the value for the {@code href} attribute
     * @return the {@code <link>} element
     */
    public static HtmlTree LINK_STYLESHEET(String stylesheetURI) {
        return LINK("stylesheet", stylesheetURI);
    }

    /**
     * Creates a {@code <meta>} element with given name and content attributes.
     *
     * @param name the value for the {@code name} attribute
     * @param content the value for the {@code content} attribute
     * @return the {@code <meta>} element
     */
    public static HtmlTree META(String name, String content) {
        return new HtmlTree(TagName.META)
                .set(HtmlAttr.NAME, name)
                .set(HtmlAttr.CONTENT, content);
    }

    /**
     * Creates a {@code <meta>} element for a default viewport.
     *
     * @return the {@code <meta>} element
     */
    public static HtmlTree META_VIEWPORT() {
        return META_VIEWPORT("width=device-width, initial-scale=1.0");
    }

    /**
     * Creates a {@code <meta>} element for a specific viewport.
     *
     * @param content the value for the {@code content} attribute
     * @return the {@code <meta>} element
     */
    public static HtmlTree META_VIEWPORT(String content) {
        return META("viewport", content);
    }

    //-----------------------------------------------------
    //
    // Body, regions, div


    public static HtmlTree BODY() {
        return new HtmlTree(TagName.BODY);
    }

    public static HtmlTree BODY(List<Content> contents) {
        return new HtmlTree(TagName.BODY, contents);
    }

    public static HtmlTree HEADER(Content... contents) {
        return new HtmlTree(TagName.HEADER, contents);
    }

    public static HtmlTree MAIN(Content... contents) {
        return new HtmlTree(TagName.MAIN, contents);
    }

    public static HtmlTree NAV(Content... contents) {
        return new HtmlTree(TagName.NAV, contents);
    }

    public static HtmlTree FOOTER(Content... contents) {
        return new HtmlTree(TagName.FOOTER, contents);
    }

    public static HtmlTree DIV(Content... contents) {
        return new HtmlTree(TagName.DIV, contents);
    }

    public static HtmlTree DIV(List<? extends Content> contents) {
        return new HtmlTree(TagName.DIV, contents);
    }
    public static HtmlTree SECTION(Content... contents) {
        return new HtmlTree(TagName.SECTION, contents);
    }

    public static HtmlTree SECTION(List<? extends Content> contents) {
        return new HtmlTree(TagName.SECTION, contents);
    }

    public static HtmlTree PRE(Content... contents) {
        return new HtmlTree(TagName.PRE, contents);
    }

    public static HtmlTree PRE(List<Content> contents) {
        return new HtmlTree(TagName.PRE, contents);
    }

    public static HtmlTree DETAILS(Content... contents) {
        return new HtmlTree(TagName.DETAILS, contents);
    }

    public static HtmlTree SUMMARY(Content... contents) {
        return new HtmlTree(TagName.SUMMARY, contents);
    }

    //-----------------------------------------------------
    //
    // Headers

    public static HtmlTree H1(Content... contents) {
        return new HtmlTree(TagName.H1, contents);
    }

    public static HtmlTree H2(Content... contents) {
        return new HtmlTree(TagName.H2, contents);
    }

    public static HtmlTree H3(Content... contents) {
        return new HtmlTree(TagName.H3, contents);
    }

    //-----------------------------------------------------
    //
    // Table items

    public static HtmlTree TABLE(Content... contents) {
        return new HtmlTree(TagName.TABLE, contents);
    }

    public static HtmlTree CAPTION(Content... contents) {
        return new HtmlTree(TagName.CAPTION, contents);
    }

    public static HtmlTree THEAD(Content... contents) {
        return new HtmlTree(TagName.THEAD, contents);
    }

    public static HtmlTree TBODY(Content... contents) {
        return new HtmlTree(TagName.TBODY, contents);
    }

    public static HtmlTree TFOOT(Content... contents) {
        return new HtmlTree(TagName.TFOOT, contents);
    }

    public static HtmlTree TR(Content... contents) {
        return new HtmlTree(TagName.TR, contents);
    }

    public static HtmlTree TH(Content... contents) {
        return new HtmlTree(TagName.TH, contents);
    }

    public static HtmlTree TD(Content... contents) {
        return new HtmlTree(TagName.TD, contents);
    }

    //-----------------------------------------------------
    //
    // List items

    public static HtmlTree UL(Content... contents) {
        return new HtmlTree(TagName.UL, contents);
    }

    public static HtmlTree UL(List<? extends Content> contents) {
        return new HtmlTree(TagName.UL, contents);
    }

    public static HtmlTree OL(Content... contents) {
        return new HtmlTree(TagName.OL, contents);
    }

    public static HtmlTree LI(Content... contents) {
        return new HtmlTree(TagName.LI, contents);
    }

    public static HtmlTree LI(List<Content> contents) {
        return new HtmlTree(TagName.LI, contents);
    }

    public static HtmlTree DL(Content... contents) {
        return new HtmlTree(TagName.DL, contents);
    }

    public static HtmlTree DT(Content... contents) {
        return new HtmlTree(TagName.DT, contents);
    }

    public static HtmlTree DD(Content... contents) {
        return new HtmlTree(TagName.DD, contents);
    }

    //-----------------------------------------------------
    //
    // Basic text items

    public static HtmlTree A(String href, Content... contents) {
        return new HtmlTree(TagName.A)
                .set(HtmlAttr.HREF, encodeURL(href))
                .add(contents);
    }

    public static HtmlTree A(URI href, Content... contents) {
        return A(href.toString(), contents);
    }

    public static HtmlTree B(Content... contents) {
        return new HtmlTree(TagName.B, contents);
    }

    public static HtmlTree P(Content... contents) {
        return new HtmlTree(TagName.P, contents);
    }

    public static HtmlTree SPAN(Content... contents) {
        return new HtmlTree(TagName.SPAN, contents);
    }

    public static HtmlTree SPAN(List<Content> contents) {
        return new HtmlTree(TagName.SPAN, contents);
    }

    //-----------------------------------------------------
    //
    // An HTMLTree is a tag name, a collection of attributes and a sequence of contents.
    // The tag name is normally represented by a TagName, but we need to model the
    // HTML being compared in HtmlDiffBuilder, which may use tag names that are not
    // present in the TagName enum. Likewise, the attribute names are normally
    // represented by HtmlAttr, but we need to model attributes that are not present
    // in the enum.  Therefore, both the tag name and attribute name are represented
    // by an Object, which is either the relevant enum, or a normalized (upper-case)
    // string.

    private final Object tag;  // TagName or String
    private final Map<Object, String> attrs; // key is HtmlAttr or String
    private final List<Content> contents;

    public HtmlTree(TagName tag) {
        this.tag = tag;
        attrs = new LinkedHashMap<>();
        contents = new ArrayList<>();
    }

    public HtmlTree(String tag) {
        this.tag = tag.toUpperCase(Locale.ROOT);
        attrs = new LinkedHashMap<>();
        contents = new ArrayList<>();
    }

    public HtmlTree(TagName tag, Content... contents) {
        this(tag);
        add(contents);
    }

    public HtmlTree(TagName tag, List<? extends Content> contents) {
        this(tag);
        add(contents);
    }

    public HtmlTree add(Content content) {
        if (coalesceText && !contents.isEmpty()) {
            if (content instanceof Text) {
                return add(((Text) content).s);
            } else {
                Content c = contents.get(contents.size() - 1);
                if (c instanceof TextBuilder) {
                    ((TextBuilder) c).trimToSize();
                }
            }
        }
        contents.add(content);
        return this;
    }

    public HtmlTree add(Content... contents) {
        List.of(contents).forEach(this::add);
        return this;
    }

    public HtmlTree add(List<? extends Content> contents) {
        contents.forEach(this::add);
        return this;
    }

    public HtmlTree add(Stream<? extends Content> contents) {
        contents.forEach(this::add);
        return this;
    }

    public HtmlTree add(CharSequence text) {
        if (text.length() == 0) {
            return this;
        }

        if (coalesceText && !contents.isEmpty()) {
            Content c = contents.get(contents.size() - 1);
            if (c instanceof TextBuilder) {
                ((TextBuilder) c).append(text);
                return this;
            } else if (c instanceof Text) {
                TextBuilder mt = new TextBuilder(((Text) c).s).append(text);
                contents.set(contents.size() - 1, mt);
                return this;
            }
        }

        contents.add(new Text(text));
        return this;
    }

    public String getTagString() {
        return (tag instanceof TagName) ? ((TagName) tag).name() : ((String) tag);
    }

    public boolean hasTag(TagName t) {
        return tag == t;
    }

    public boolean hasTag(String t) {
        return getTagString().equalsIgnoreCase(t);
    }

    public String get(HtmlAttr attr) {
        return attrs.get(attr);
    }

    public HtmlTree set(HtmlAttr attr, String value) {
        attrs.put(attr, value);
        return this;
    }

    public HtmlTree set(String unknownAttr, String value) {
        attrs.put(unknownAttr, value);
        return this;
    }


    public HtmlTree setId(String id) {
        return set(HtmlAttr.ID, id);
    }

    public HtmlTree setClass(String id) {
        return set(HtmlAttr.CLASS, id);
    }

    public HtmlTree setTitle(String text) {
        return set(HtmlAttr.TITLE, text);
    }

    public List<Content> contents() {
        return contents;
    }

    @Override
    public void write(Writer out) throws IOException {
        if (tag == TagName.HTML) {
            out.write("<!DOCTYPE html>\n");
        }
        out.write("<");
        out.write(toLowerCase(tag));
        for (Map.Entry<Object, String> e : attrs.entrySet()) {
            out.write(" ");
            out.write(toLowerCase(e.getKey()));
            if (e.getValue() != null) {
                out.write("=\"");
                writeEscaped(out, e.getValue());  // should also escape " as &quot;
                out.write("\"");
            }
        }
        out.write(">");
        for (Content c : contents) {
            c.write(out);
        }

        if (tag instanceof TagName) {
            var tn = (TagName) tag;
            switch (tn) {
                case BASE:
                case LINK:
                case META:
                case BR:
                case HR:
                    break;
                default:
                    out.write("</");
                    out.write(toLowerCase(tag));
                    out.write(">");
            }

            switch (tn) {
                case A:
                case B:
                case I:
                case LI:
                case SPAN:
                    break;
                default:
                    out.write("\n");
            }
        } else {
            out.write("</");
            out.write(toLowerCase(tag));
            out.write(">");
        }
    }

    private String toLowerCase(Object o) {
        return (o instanceof Enum ? ((Enum) o).name() : o.toString()).toLowerCase(Locale.ROOT);
    }



    /*
     * The sets of ASCII URI characters to be left unencoded.
     * See "Uniform Resource Identifier (URI): Generic Syntax"
     * IETF RFC 3986. https://tools.ietf.org/html/rfc3986
     */
    public static final BitSet MAIN_CHARS;
    public static final BitSet QUERY_FRAGMENT_CHARS;

    static {
        BitSet alphaDigit = bitSet(bitSet('A', 'Z'), bitSet('a', 'z'), bitSet('0', '9'));
        BitSet unreserved = bitSet(alphaDigit, bitSet("-._~"));
        BitSet genDelims = bitSet(":/?#[]@");
        BitSet subDelims = bitSet("!$&'()*+,;=");
        MAIN_CHARS = bitSet(unreserved, genDelims, subDelims);
        BitSet pchar = bitSet(unreserved, subDelims, bitSet(":@"));
        QUERY_FRAGMENT_CHARS = bitSet(pchar, bitSet("/?"));
    }

    private static BitSet bitSet(String s) {
        BitSet result = new BitSet();
        for (int i = 0; i < s.length(); i++) {
            result.set(s.charAt(i));
        }
        return result;
    }

    private static BitSet bitSet(char from, char to) {
        BitSet result = new BitSet();
        result.set(from, to + 1);
        return result;
    }

    private static BitSet bitSet(BitSet... sets) {
        BitSet result = new BitSet();
        for (BitSet set : sets) {
            result.or(set);
        }
        return result;
    }

    /**
     * Apply percent-encoding to a URL.
     * This is similar to {@link java.net.URLEncoder} but
     * is less aggressive about encoding some characters,
     * like '(', ')', ',' which are used in the anchor
     * names for Java methods in HTML5 mode.
     *
     * @param url the url to be percent-encoded.
     * @return a percent-encoded string.
     */
    public static String encodeURL(String url) {
        BitSet nonEncodingChars = MAIN_CHARS;
        StringBuilder sb = new StringBuilder();
        for (byte c : url.getBytes(StandardCharsets.UTF_8)) {
            if (c == '?' || c == '#') {
                sb.append((char) c);
                // switch to the more restrictive set inside
                // the query and/or fragment
                nonEncodingChars = QUERY_FRAGMENT_CHARS;
            } else if (nonEncodingChars.get(c & 0xFF)) {
                sb.append((char) c);
            } else {
                sb.append(String.format("%%%02X", c & 0xFF));
            }
        }
        return sb.toString();
    }
}

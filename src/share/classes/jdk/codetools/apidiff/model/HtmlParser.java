/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.codetools.apidiff.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A basic HTML parser.
 *
 * Override the protected methods as needed to get notified of significant items
 * in any file that is read.
 */
public abstract class HtmlParser {

    private Path file;
    private Reader in;
    private int ch;
    private long charNumber;
    private int lineNumber;
    private boolean inScript;
    private boolean xml;

    /**
     * Creates an instance of an HTML parser.
     */
    public HtmlParser() { }

    /**
     * Read a file.
     *
     * <p>Ideally, we should honor a charset found in a {@code <meta>} element in the head of the document,
     * but the reality is that all documents use one of ASCII, ISO-8859-1 or UTF-8, and generally
     * specify either ISO-8859-1 or UTF-8. UTF-8 is backwards compatible with both ASCII and ISO-8859-1,
     * and so we assume the use of UTF-8, and use a decoder to replace bad values with the
     * standard Unicode REPLACEMENT CHARACTER U+FFFD.</p>
     *
     * <p>As alternatives, we could initially assume an 8-bit encoding (e.g. ASCII or ISO-8859-1,
     * and switch to UTF-8 if needed (but note {@link java.io.InputStreamReader} may read ahead
     * some bytes for efficiency, making it hard to know the state of the stream if and when we
     * need to switch.  Or, we could read ahead some amount looking for a charset, and then
     * reset the stream and start over with the specified charset.</p>
     *
     * @param file the file to be read
     */
    public void read(Path file) {
        try {
            readBuffer(file);
        } catch (IOException e) {
            error(file, -1, e);
        }

        this.file = file;
        startFile(file);
        try {
            int startContentIndex = 0;
            charNumber = 0;
            lineNumber = 1;
            xml = false;
            nextChar();

            while (ch != -1) {
                switch (ch) {
                    case '<' -> {
                        if (bufferIndex > startContentIndex + 1) {
                            int from = startContentIndex;
                            int to = bufferIndex - 1;
                            content(() -> getBufferString(from, to));
                        }
                        html();
                        startContentIndex = bufferIndex - 1;
                    }

                    case '\n' -> {
                        int from = startContentIndex;
                        int to = bufferIndex;
                        content(() -> getBufferString(from, to));
                        startContentIndex = bufferIndex;
                        nextChar();
                    }

                    default -> {
                        nextChar();
                    }
                }
            }
        } finally {
            endFile();
        }
    }

    /**
     * The contents of the file being processed.
     */
    private char[] buffer;

    /**
     * The position of the next character to be read.
     */
    private int bufferIndex;

    /**
     * The position of the last character in the buffer.
     */
    private int maxBufferIndex;

    /**
     * Read a file into the buffer.
     * Bad characters in the input are simply replaced with U+FFFD.
     *
     * @param file the file
     * @throws IOException if an IO exception occurs
     */
    private void readBuffer(Path file) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("\ufffd");
        float factor = decoder.averageCharsPerByte() * 0.8f
                + decoder.maxCharsPerByte() * 0.2f;

        // overestimate buffer size, to avoid reallocation
        long byteSize = Files.size(file);
        int bufferSize = (int) (byteSize * factor) + 128;

        if (buffer == null || buffer.length < bufferSize) {
            buffer = new char[bufferSize];
        }

        try (InputStream is = Files.newInputStream(file);
             Reader r = new BufferedReader(new InputStreamReader(is, decoder))) {
            int offset = 0;
            int n;
            while ((n = r.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += n;
                if (offset == buffer.length) {
                    // should not happen, but just in case...
                    char[] newBuffer = new char[buffer.length + buffer.length / 4];
                    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                    buffer = newBuffer;
                }
            }

            bufferIndex = 0;
            maxBufferIndex = offset;
        }
    }

    /**
     * Returns the position in the file of the next character to be read.
     *
     * @return the position
     */
    protected int getBufferIndex() {
        return bufferIndex;
    }

    /**
     * Returns a substring of content in the buffer.
     *
     * @param from the position of the first character of the substring
     * @param to   the position of the first character after the substring
     *
     * @return the substring
     */
    protected String getBufferString(int from, int to) {
        return new String(buffer, from, to - from);
    }

    /**
     * Returns a substring of content in the buffer, excluding leading and trailing whitespace.
     *
     * @param from the position of the first character of the substring
     * @param to   the position of the first character after the substring
     *
     * @return the substring
     */
    protected String getTrimBufferString(int from, int to) {
        while (from < to && Character.isWhitespace(buffer[from])) {
            from++;
        }
        while (to > from && Character.isWhitespace(buffer[to - 1])) {
            to--;
        }
        return getBufferString(from, to);
    }

    /**
     * Returns the position in the file of the most recently read character.
     *
     * @return the position
     */
    protected long charNumber() {
        return charNumber;
    }

    /**
     * Returns the line number in the file of the most recently read character.
     *
     * @return the line number
     */
    protected int getLineNumber() {
        return lineNumber;
    }

    /**
     * Called when a file has been opened, before parsing begins.
     * This is always the first notification when reading a file.
     * This implementation does nothing.
     *
     * @param file the file
     */
    protected void startFile(Path file) { }

    /**
     * Called when the parser has finished reading a file.
     * This is always the last notification when reading a file,
     * unless any errors occur while closing the file.
     * This implementation does nothing.
     */
    protected void endFile() { }

    /**
     * Called when a doctype declaration is found, at the beginning of the file.
     * This implementation does nothing.
     * @param s a supplier for the doctype declaration
     */
    protected void doctype(Supplier<String> s) { }

    /**
     * Called when the opening tag of an HTML element is encountered.
     * This implementation does nothing.
     * @param name the name of the tag
     * @param attrs the attribute
     * @param selfClosing whether this is a self-closing tag
     */
    protected void startElement(String name, Map<String,String> attrs, boolean selfClosing) { }

    /**
     * Called when the closing tag of an HTML tag is encountered.
     * This implementation does nothing.
     * @param name the name of the tag
     */
    protected void endElement(String name) { }

    /**
     * Called for sequences of character content.
     * @param content a supplier for the character content
     */
    protected void content(Supplier<String> content) { }

    /**
     * Called for sequences of comment.
     * @param comment a supplier for the comment
     */
    protected void comment(Supplier<String> comment) { }

    /**
     * Called when an error has been encountered.
     * @param file the file being read
     * @param lineNumber the line number of line containing the error
     * @param message a description of the error
     */
    protected abstract void error(Path file, int lineNumber, String message);

    /**
     * Called when an exception has been encountered.
     * @param file the file being read
     * @param lineNumber the line number of the line being read when the exception was found
     * @param t the exception
     */
    protected abstract void error(Path file, int lineNumber, Throwable t);

    /**
     * Reads the next character from the buffer and returns it.
     *
     * @return the character
     */
    protected int nextChar() {
        if (bufferIndex == maxBufferIndex) {
            ch =  -1;
        } else {
            ch = buffer[bufferIndex++];
            charNumber++;
            if (ch == '\n')
                lineNumber++;
        }
        return ch;
    }

    /**
     * Read the start or end of an HTML tag, or the doctype declaration,
     * skipping any HTML comments.
     *
     * Syntax:
     * {@literal <identifier attrs> } or {@literal </identifier> }
     */
    protected void html() {
        nextChar();
        if (isIdentifierStart((char) ch)) {
            String name = readIdentifier().toLowerCase(Locale.ROOT);
            Map<String,String> attrs = htmlAttrs();
            if (attrs != null) {
                boolean selfClosing = false;
                if (ch == '/') {
                    nextChar();
                    selfClosing = true;
                }
                if (ch == '>') {
                    nextChar();
                    startElement(name, attrs, selfClosing);
                    if (name.equals("script")) {
                        inScript = true;
                    }
                    return;
                }
            }
        } else if (ch == '/') {
            nextChar();
            if (isIdentifierStart((char) ch)) {
                String name = readIdentifier().toLowerCase(Locale.ROOT);
                skipWhitespace();
                if (ch == '>') {
                    nextChar();
                    endElement(name);
                    if (name.equals("script")) {
                        inScript = false;
                    }
                    return;
                }
            }
        } else if (ch == '!') {
            nextChar();
            if (ch == '-') {
                nextChar();
                if (ch == '-') {
                    nextChar();
                    int startCommentIndex = bufferIndex - 1;
                    while (ch != -1) {
                        int dash = 0;
                        while (ch == '-') {
                            dash++;
                            nextChar();
                        }
                        // Strictly speaking, a comment should not contain "--"
                        // so dash > 2 is an error, dash == 2 implies ch == '>'
                        // See http://www.w3.org/TR/html-markup/syntax.html#syntax-comments
                        // for more details.
                        if (dash >= 2 && ch == '>') {
                            int to = bufferIndex - 3;
                            comment(() -> getBufferString(startCommentIndex, to));
                            nextChar();
                            return;
                        }

                        nextChar();
                    }
                }
            } else if (ch == '[') {
                nextChar();
                if (ch == 'C') {
                    nextChar();
                    if (ch == 'D') {
                        nextChar();
                        if (ch == 'A') {
                            nextChar();
                            if (ch == 'T') {
                                nextChar();
                                if (ch == 'A') {
                                    nextChar();
                                    if (ch == '[') {
                                        while (true) {
                                            nextChar();
                                            if (ch == ']') {
                                                nextChar();
                                                if (ch == ']') {
                                                    nextChar();
                                                    if (ch == '>') {
                                                        nextChar();
                                                        return;
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                int startDocTypeIndex = bufferIndex - 1;
                while (ch != -1 && ch != '>') {
                    nextChar();
                }
                nextChar();
                Pattern p = Pattern.compile("(?is)doctype\\s+html\\s?.*");
                String s = getBufferString(startDocTypeIndex, bufferIndex - 2);
                if (p.matcher(s).matches()) {
                    doctype(() -> s);
                    return;
                }
            }
        } else if (ch == '?') {
            nextChar();
            if (ch == 'x') {
                nextChar();
                if (ch == 'm') {
                    nextChar();
                    if (ch == 'l') {
                        Map<String,String> attrs = htmlAttrs();
                        if (ch == '?') {
                            nextChar();
                            if (ch == '>') {
                                nextChar();
                                xml = true;
                                return;
                            }
                        }
                    }
                }

            }
        }

        if (!inScript) {
            error(file, lineNumber, "bad html");
        }
    }

    /**
     * Read a series of HTML attributes, terminated by {@literal > }.
     * Each attribute is of the form {@literal identifier[=value] }.
     * "value" may be unquoted, single-quoted, or double-quoted.
     */
    private Map<String,String> htmlAttrs() {
        Map<String, String> map = Collections.emptyMap(); // default, for common case
        skipWhitespace();

        while (isIdentifierStart((char) ch)) {
            String name = readAttributeName().toLowerCase(Locale.ROOT);
            skipWhitespace();
            String value = null;
            if (ch == '=') {
                nextChar();
                skipWhitespace();
                if (ch == '\'' || ch == '"') {
                    char quote = (char) ch;
                    nextChar();
                    int startValueIndex = bufferIndex - 1;
                    while (ch != -1 && ch != quote) {
                        nextChar();
                    }
                    value = replaceSimpleEntities(getBufferString(startValueIndex, bufferIndex - 1));
                    nextChar();
                } else {
                    int startValueIndex = bufferIndex - 1;
                    while (ch != -1 && !isUnquotedAttrValueTerminator((char) ch)) {
                        nextChar();
                    }
                    value = getBufferString(startValueIndex, bufferIndex - 1);
                }
                skipWhitespace();
            }
            if (map.isEmpty()) {
                // change to a mutable map
                map = new LinkedHashMap<>();
            }
            map.put(name, value);
        }

        return map;
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isUnicodeIdentifierStart(ch);
    }

    private String readIdentifier() {
        int startIndex = bufferIndex - 1;
        nextChar();
        while (ch != -1 && Character.isUnicodeIdentifierPart(ch)) {
            nextChar();
        }
        return getBufferString(startIndex, bufferIndex - 1);
    }

    private String readAttributeName() {
        int startIndex = bufferIndex - 1;
        nextChar();
        while (ch != -1 && Character.isUnicodeIdentifierPart(ch)
                || ch == '-'
                || xml && ch == ':') {
            nextChar();
        }
        return getBufferString(startIndex, bufferIndex - 1);
    }

    private boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    private void skipWhitespace() {
        while (isWhitespace((char) ch)) {
            nextChar();
        }
    }

    private String replaceSimpleEntities(String s) {
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private boolean isUnquotedAttrValueTerminator(char ch) {
        return switch (ch) {
            case '\f', '\n', '\r', '\t', ' ', '"', '\'', '`', '=', '<', '>' -> true;
            default -> false;
        };
    }
}

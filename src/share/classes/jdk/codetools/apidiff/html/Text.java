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

/**
 * Textual content.
 *
 * @see TextBuilder
 */
public class Text extends Content {
    // TODO: provide additional constants (somewhere?) for common strings

    /**
     * An object providing a single space character.
     *
     * @see Entity#NBSP
     */
    public static final Text SPACE = new Text(" ");

    final String s;

    /**
     * Creates textual content from a sequence of characters.
     *
     * @param text the characters
     * @return the textual content
     */
    public static Text of(CharSequence text) {
        return new Text(text);
    }

    /**
     * Creates textual content from a sequence of characters.
     *
     * @param text the characters
     */
    public Text(String text) {
        s = text;
    }

    /**
     * Creates textual content from a sequence of characters.
     *
     * @param text the characters
     */
    public Text(CharSequence text) {
        s = text.toString();
    }

    /**
     * Writes the content.
     * Special characters ('{@code <}', '{@code &}' and '{@code >}') will be escaped.
     *
     * @param out the stream to which to write the content
     * @throws IOException if an IO exception occurs.
     */
    @Override
    public void write(Writer out) throws IOException {
        writeEscaped(out, s);
    }

}

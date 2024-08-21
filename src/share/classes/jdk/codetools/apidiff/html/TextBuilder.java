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
 * Class for generating string content for {@code HTMLTree} nodes.
 * The content is mutable to the extent that additional content may be added.
 *
 * @see Text
 */
public class TextBuilder extends Content {

    private final StringBuilder sb;

    /**
     * Creates a mutable container for textual content.
     *
     * @param text the initial content
     */
    public TextBuilder(CharSequence text) {
        sb = new StringBuilder(text);
    }

    /**
     * Append additional characters to the content.
     *
     * @param text the characters
     *
     * @return this object
     */
    public TextBuilder append(CharSequence text) {
        sb.append(text);
        return this;
    }

    /**
     * Trims the size of the internal string builder.
     *
     * @return this object
     */
    TextBuilder trimToSize() {
        sb.trimToSize();
        return this;
    }

    /**
     * Writes the content.
     * Special characters ('{@code <}', '{@code &}' and '{@code >}') will be escaped.
     *
     * @param out the stream to which to write the content
     *
     * @throws IOException if an IO exception occurs.
     */
    @Override
    public void write(Writer out) throws IOException {
        writeEscaped(out, sb.toString());
    }
}

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
 * Superclass for all items in an HTML tree.
 */
public abstract class Content {
    /**
     * An empty item, which does not generate any output when written to a stream.
     */
    public static final Content empty = new Content() {
        @Override
        protected void write(Writer out) throws IOException { }
    };

    /**
     * Writes this object as a fragment of HTML.
     *
     * @param out the writer
     *
     * @throws IOException if an IO exception occurs
     */
    protected abstract void write(Writer out) throws IOException;

    /**
     * Writes a string, escaping characters {@code <}, {@code >}, {@code &}.
     *
     * @param out the writer
     * @param s   the string
     *
     * @throws IOException if an IO exception occurs
     */
    protected void writeEscaped(Writer out, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '<' -> out.write("&lt;");
                case '>' -> out.write("&gt;");
                case '&' -> out.write("&amp;");
                default ->  out.write(ch);
            }
        }
    }
}

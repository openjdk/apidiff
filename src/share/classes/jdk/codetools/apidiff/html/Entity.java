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
 * An HTML entity.
 *
 * @see <a href="home.unicode.org">Unicode</a>
 * @see <a href="https://www.fileformat.info/info/unicode/">FileFormat.info Unicode</a>
 */
public class Entity extends Content {
    /** Unicode CHECK MARK. */
    public static final Entity CHECK = new Entity("check", 0x2713);
    /** Unicode CIRCLED DIGIT ONE. */
    public static final Entity CIRCLED_DIGIT_ONE = new Entity(null, 0x2460);
    /** Unicode DINGBAT NEGATIVE CIRCLED DIGIT ONE. */
    public static final Entity NEGATIVE_CIRCLED_DIGIT_ONE = new Entity(null, 0x2776);
    /** Unicode BALLOT X. */
    public static final Entity CROSS = new Entity("cross", 0x2717);
    /** Unicode EQUALS SIGN. */
    public static final Entity EQUALS = new Entity("equals", 0x3d);
    /** Unicode NOT EQUAL TO. */
    public static final Entity NE = new Entity("ne", 0x2260);
    /** Unicode NO-BREAK SPACE. */
    public static final Entity NBSP = new Entity("nbsp", 0xa0);

    private static final boolean useNumericEntities = Boolean.getBoolean("useNumericEntities");

    private final String name;
    private final int value;

    private Entity(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    protected void write(Writer out) throws IOException {
        out.write("&");
        out.write(name == null || useNumericEntities ? String.format("#x%x", value) : name);
        out.write(";");
    }
}

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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;

/**
 * Constants for language keywords and reserved identifiers.
 */
public class Keywords {
    private Keywords() { }

    public static final Content ABSTRACT = keyword("abstract");
    public static final Content AT_INTERFACE = keyword("@interface");
    public static final Content BOOLEAN = keyword("boolean");
    public static final Content BYTE = keyword("byte");
    public static final Content CHAR = keyword("char");
    public static final Content CLASS = keyword("class");
    public static final Content DEFAULT = keyword("default");
    public static final Content DOUBLE = keyword("double");
    public static final Content EXPORTS = keyword("exports");
    public static final Content ENUM = keyword("enum");
    public static final Content EXTENDS = keyword("extends");
    public static final Content FALSE = keyword("false");
    public static final Content FINAL = keyword("final");
    public static final Content FLOAT = keyword("float");
    public static final Content IMPLEMENTS = keyword("implements");
    public static final Content INT = keyword("int");
    public static final Content INTERFACE = keyword("interface");
    public static final Content LONG = keyword("long");
    public static final Content NATIVE = keyword("native");
    public static final Content NON_SEALED = keyword("non-sealed");
    public static final Content MODULE = keyword("module");
    public static final Content OPEN = keyword("open");
    public static final Content OPENS = keyword("opens");
    public static final Content PACKAGE = keyword("package");
    public static final Content PERMITS = keyword("permits");
    public static final Content PRIVATE = keyword("private");
    public static final Content PROTECTED = keyword("protected");
    public static final Content PROVIDES = keyword("provides");
    public static final Content PUBLIC = keyword("public");
    public static final Content RECORD = keyword("record");
    public static final Content REQUIRES = keyword("requires");
    public static final Content SEALED = keyword("sealed");
    public static final Content SHORT = keyword("short");
    public static final Content STATIC = keyword("static");
    public static final Content STRICTFP = keyword("strictfp");
    public static final Content SUPER = keyword("super");
    public static final Content SYNCHRONIZED = keyword("synchronized");
    public static final Content THROWS = keyword("throws");
    public static final Content TO = keyword("to");
    public static final Content TRANSIENT = keyword("transient");
    public static final Content TRANSITIVE = keyword("transitive");
    public static final Content TRUE = keyword("true");
    public static final Content USES = keyword("uses");
    public static final Content VOID = keyword("void");
    public static final Content VOLATILE = keyword("volatile");
    public static final Content WITH = keyword("with");

    /**
     * Returns the keyword for a boolean value.
     *
     * @param b the value
     * @return the keyword
     */
    public static Content of(boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Returns the keyword for a modifier.
     *
     * @param m the modifier
     * @return the keyword
     */
    public static Content of(Modifier m) {
        return switch (m) {
            case ABSTRACT -> ABSTRACT;
            case DEFAULT -> DEFAULT;
            case FINAL -> FINAL;
            case NATIVE -> NATIVE;
            case NON_SEALED -> NON_SEALED;
            case PRIVATE -> PRIVATE;
            case PROTECTED -> PROTECTED;
            case PUBLIC -> PUBLIC;
            case SEALED -> SEALED;
            case STATIC -> STATIC;
            case STRICTFP -> STRICTFP;
            case SYNCHRONIZED -> SYNCHRONIZED;
            case TRANSIENT -> TRANSIENT;
            case VOLATILE -> VOLATILE;
        };
    }

    /**
     * Returns the keyword for a primitive type.
     *
     * @param t the type
     * @return the keyword
     */
    public static Content of(PrimitiveType t) {
        return switch (t.getKind()) {
            case BOOLEAN -> BOOLEAN;
            case BYTE -> BYTE;
            case CHAR -> CHAR;
            case DOUBLE -> DOUBLE;
            case FLOAT -> FLOAT;
            case INT -> INT;
            case LONG -> LONG;
            case SHORT -> SHORT;
            default -> throw new IllegalArgumentException((t.toString()));
        };
    }

    /**
     * Returns the keyword for the kind of a type element.
     *
     * @param k the kind
     * @return the keyword
     */
    public static Content of(ElementKind k) {
        switch (k) {
            case ANNOTATION_TYPE:
                return AT_INTERFACE;
            case CLASS:
                return CLASS;
            case ENUM:
                return ENUM;
            case INTERFACE:
                return INTERFACE;
            default:
                if (k.name().equals("RECORD")) {
                    return RECORD;
                }
                throw new IllegalArgumentException((k.toString()));
        }
    }

    private static Content keyword(String name) {
        return HtmlTree.SPAN(Text.of(name)).setClass("keyword");
    }
}

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

package jdk.codetools.apidiff.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor14;

import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link TypeMirror type mirrors}.
 *
 * Type mirrors often occur in collections, such as the superinterfaces of a type
 * or the set of checked exceptions that may be thrown by an executable element.
 *
 * <p>Type mirrors are compared according to their structure ("deep equals") down to
 * the level of element names, which are compared using {@link ElementKey#equals ElementKey.equals}.
 */
public class TypeMirrorComparator {
    private final Set<API> apis;
    private final Reporter reporter;

    /**
     * Creates a comparator to compare type mirrors across a set of APIs.
     *
     * @param apis the set of APIs
     * @param reporter the reporter to which to report differences
     */
    protected TypeMirrorComparator(Set<API> apis, Reporter reporter) {
        this.apis = apis;
        this.reporter = reporter;
    }

    /**
     * Compares all the series of type mirrors at a given position in an API.
     *
     * @param pos the position
     * @param table the table for the type mirrors at that position
     * @return {@code true} if all the type mirrors are equivalent
     */
    boolean compareAll(Function<Integer, Position> pos, IntTable<TypeMirror> table) {
        boolean allEqual = true;
        for (int i = 0; i < table.size(); i++) {
            // TODO: check size of table.entries(i) against api.size()
            allEqual &= compare(pos.apply(i), table.entries(i));
        }
        return allEqual;
    }

    /**
     * Compares all the series of type mirrors at a given position in an API.
     *
     * @param pos the position
     * @param table the table for the type mirrors for that element
     * @return {@code true} if all the type mirrors are equivalent
     */
    boolean compareAll(Function<ElementKey, Position> pos, KeyTable<TypeMirror> table) {
        boolean allEqual = true;
        for (Map.Entry<ElementKey, APIMap<TypeMirror>> entry : table.entries()) {
            ElementKey key = entry.getKey();
            APIMap<TypeMirror> map = entry.getValue();
            allEqual &= compare(pos.apply(key), map);
        }
        return allEqual;
    }

    boolean compare(Position pos, APIMap<TypeMirror> map) {
        if (map.size() == 1)
            return true;

        TypeMirror archetype = map.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
        for (TypeMirror t : map.values()) {
            if (!equal(archetype, t)) {
                reporter.reportDifferentTypes(pos, map);
                return false;
            }
        }
        return true;
    }

    private static boolean equal(Element e1, Element e2) {
        return ElementKey.of(e1).equals(ElementKey.of(e2));
    }

    private static boolean equal(TypeMirror t1, TypeMirror t2) {
        if (t1 == t2)
            return true;
        if (t1 == null || t2 == null)
            return false;
        if (t1.getKind() != t2.getKind())
            return false;
        // TODO: compare type annotations
        return equalVisitor.visit(t1, t2);
    }

    private static boolean equal(List<? extends TypeMirror> l1, List<? extends TypeMirror> l2) {
        if (l1 == l2)
            return true;
        if (l1 == null || l2 == null)
            return false;
        if (l1.size() != l2.size())
            return false;
        Iterator<? extends TypeMirror> iter1 = l1.iterator();
        Iterator<? extends TypeMirror> iter2 = l2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            boolean eq = equal(iter1.next(), iter2.next());
            if (!eq) {
                return false;
            }
        }
        return true;
    }

    static TypeVisitor<Boolean,TypeMirror> equalVisitor = new SimpleTypeVisitor14<>() {

        @Override
        public Boolean visitArray(ArrayType at1, TypeMirror t2) {
            ArrayType at2 = (ArrayType) t2;
            return equal(at1.getComponentType(), at2.getComponentType());
        }

        @Override
        public Boolean visitDeclared(DeclaredType dt1, TypeMirror t2) {
            DeclaredType dt2 = (DeclaredType) t2;
            return equal(dt1.asElement(), dt2.asElement())
                    && equal(dt1.getTypeArguments(), dt2.getTypeArguments());
        }

        @Override
        public Boolean visitNoType(NoType pt, TypeMirror t) {
            return true;
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType pt, TypeMirror t) {
            return true;
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable vt1, TypeMirror t2) {
            TypeVariable vt2 = (TypeVariable) t2;
            return equal(vt1.asElement(), vt2.asElement());
        }

        @Override
        public Boolean visitWildcard(WildcardType wt1, TypeMirror t2) {
            WildcardType wt2 = (WildcardType) t2;
            return equal(wt1.getExtendsBound(), wt2.getExtendsBound())
                    && equal(wt1.getSuperBound(), wt2.getSuperBound());
        }

        @Override
        public Boolean defaultAction(TypeMirror e, TypeMirror t) {
            throw new UnsupportedOperationException(e.getKind() + " " + e);
        }
    };
}

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

import java.util.Set;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link TypeParameterElement type parameters}.
 *
 * <p>Type parameters are declared in a list on the declaration of a type or
 * executable element. Type parameters in different APIs are associated
 * according to their position in the list (and not by their name).
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the name
 *     <li>any annotations
 *     <li>any bounds
 * </ul>
 *
 */
public class TypeParameterComparator extends ElementComparator<TypeParameterElement> {
    /**
     * Creates an instance of a comparator for type parameters.
     *
     * @param apis     the APIs to be compared
     * @param options  the command-line options
     * @param reporter the reporter to which to report any differences.
     */
    public TypeParameterComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compares all the type parameters for a given element in an API.
     *
     * @param ePos the position of the element
     * @param typarams the table for the type parameters for that element
     * @return {@code true} if all the type parameters are equivalent
     */
    public boolean compareAll(Position ePos, IntTable<TypeParameterElement> typarams) {
        return compareAll(ePos::typeParameter, typarams);
    }

    /**
     * Compare instances of a type parameter found at a given position in different APIs.
     *
     * @param pos the position of the type parameter
     * @param map the map giving the instance of the type parameter in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position pos, APIMap<TypeParameterElement> map) {
        boolean allEquals = true;

        if (map.size() < apis.size()) {
            reporter.reportDifferentTypeParameters(pos, map);
            allEquals = false;
        }

        allEquals &= compareNames(pos, map)
                & compareBounds(pos, map)
                & new AnnotationComparator(map.keySet(), accessKind, reporter).compareAll(pos, map);

        return allEquals;
    }

    private boolean compareNames(Position pos, APIMap<TypeParameterElement> map) {
        CharSequence name = null;
        for (TypeParameterElement e : map.values()) {
            if (name == null) {
                name = e.getSimpleName();
            } else {
                if (!e.getSimpleName().contentEquals(name)) {
                    reporter.reportDifferentTypeParameters(pos, map);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean compareBounds(Position pos, APIMap<TypeParameterElement> map) {
        IntTable<TypeMirror> bounds = IntTable.of(map, TypeParameterElement::getBounds);
        return new TypeMirrorComparator(apis, reporter).compareAll(pos::bound, bounds);
    }
}

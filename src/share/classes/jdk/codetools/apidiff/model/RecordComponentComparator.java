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

import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for record component elements.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the "signature" of the element: its annotations, name and type
 * </ul>
 */
public class RecordComponentComparator extends ElementComparator<Element> {

    /**
     * Creates a comparator to compare record component elements across a set of APIs.
     *
     * @param apis     the set of APIs
     * @param options  the command-line options
     * @param reporter the reporter to which to report differences
     */
    public RecordComponentComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compares instances of a record component element found in different APIs.
     *
     * @param rcPos the position of the element
     * @param rcMap the map giving the instance of the variable element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position rcPos, APIMap<Element> rcMap) {
        boolean allEqual = false;
        reporter.comparing(rcPos, rcMap);
        try {
            allEqual = checkMissing(rcPos, rcMap);
            if (rcMap.size() > 1) {
                allEqual &= compareSignatures(rcPos, rcMap);
                // note that record components do not have distinct API descriptions or doc comments
                // of their own; they are documented by @param tags in the enclosing element
            }
        } finally {
            reporter.completed(rcPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareSignatures(Position rcPos, APIMap<Element> rcMap) {
        return compareAnnotations(rcPos, rcMap)
                & compareNames(rcPos, rcMap)
                & compareType(rcPos, rcMap);
    }

    private boolean compareNames(Position rcPos, APIMap<Element> rcMap) {

        if (rcMap.size() == 1)
            return true;

        Name archetype = rcMap.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .get()
                .getSimpleName();

        for (Element e : rcMap.values()) {
            if (!e.getSimpleName().contentEquals(archetype)) {
                reporter.reportDifferentNames(rcPos, rcMap);
                return false;
            }
        }
        return true;
    }

    private boolean compareType(Position rcPos, APIMap<Element> rcMap) {
        TypeMirrorComparator tmc = new TypeMirrorComparator(rcMap.keySet(), reporter);
        APIMap<TypeMirror> tMap = rcMap.map(Element::asType);
        return tmc.compare(rcPos, tMap);
    }
}

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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link VariableElement variable elements}: fields, enum constants, and
 * parameters for executable elements.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the "signature" of the element: its annotations, modifiers and type
 *     <li>the documentation comment for the element
 * </ul>
 */
public class VariableComparator extends ElementComparator<VariableElement> {

    /**
     * Creates a comparator to compare variable elements across a set of APIs.
     *
     * @param apis     the set of APIs
     * @param options  the command-line options
     * @param reporter the reporter to which to report differences
     */
    public VariableComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compares instances of a variable element found in different APIs.
     *
     * @param vPos the position of the element
     * @param vMap the map giving the instance of the variable element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    // TODO: should the comparison include the names for parameters?
    //       they're not significant in the binary API but may be important in the reflective API
    @Override
    public boolean compare(Position vPos, APIMap<VariableElement> vMap) {
        boolean allEqual = false;
        reporter.comparing(vPos, vMap);
        try {
            allEqual = checkMissing(vPos, vMap);
            if (vMap.size() > 1) {
                allEqual &= compareSignatures(vPos, vMap);
                allEqual &= compareDocComments(vPos, vMap);
                allEqual &= compareApiDescriptions(vPos, vMap);
            }
        } finally {
            reporter.completed(vPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareSignatures(Position vPos, APIMap<VariableElement> vMap) {
        boolean parameters = vMap.values().iterator().next().getKind() == ElementKind.PARAMETER;
        return compareAnnotations(vPos, vMap)
                & (parameters || compareModifiers(vPos, vMap)) // ignore modifiers for parameters
                & compareType(vPos, vMap);
    }

    private boolean compareType(Position vPos, APIMap<VariableElement> vMap) {
        TypeMirrorComparator tmc = new TypeMirrorComparator(vMap.keySet(), reporter);
        APIMap<TypeMirror> tMap = vMap.map(VariableElement::asType);
        return tmc.compare(vPos, tMap);
    }
}

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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link ExecutableElement executable elements}: constructors, methods, and members of annotation types.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the "signature" of the element:
 *         its annotations, modifiers, type parameters, parameters, return type (if appropriate), throws
 *         and default value (if appropriate)
 *     <li>the documentation comment for the element
 * </ul>
 */
public class ExecutableComparator extends ElementComparator<ExecutableElement> {

    /**
     * Creates a comparator to compare executable elements across a set of APIs.
     *
     * @param apis     the set of APIs
     * @param options  the command-line options
     * @param reporter the reporter to which to report differences
     */
    public ExecutableComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compare instances of an executable element found at a given position in different APIs.
     *
     * @param ePos the position of the element
     * @param eMap the map giving the instance of the type element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position ePos, APIMap<ExecutableElement> eMap) {
        boolean allEqual = false;
        reporter.comparing(ePos, eMap);
        try {
            allEqual = checkMissing(ePos, eMap);
            if (eMap.size() > 1) {
                allEqual &= compareSignatures(ePos, eMap);
                allEqual &= compareDocComments(ePos, eMap);
                allEqual &= compareApiDescriptions(ePos, eMap);
            }
        } finally {
            reporter.completed(ePos, allEqual);
        }
        return allEqual;
    }

    private boolean compareSignatures(Position ePos, APIMap<ExecutableElement> eMap) {
        // TODO: compare signature: documented annotations, modifiers, kind, type parameters,
        //       return type if method, parameter types, throws, default values if annotation type
        // By construction, the kind (method or constructor) should always be the same,
        // and does not need to be compared. Also by construction, the basic parameter
        // types will be the same, but the documented annotations on the types might differ
        // and so need to be compared.
        return compareAnnotations(ePos, eMap)
                & compareModifiers(ePos, eMap)
                & compareTypeParameters(ePos, eMap)
                & compareReturnType(ePos, eMap)
                & compareReceiverType(ePos, eMap)
                & compareParameters(ePos, eMap)
                & compareThrows(ePos, eMap)
                & compareDefaultValue(ePos, eMap);
    }

    private boolean compareTypeParameters(Position ePos, APIMap<ExecutableElement> eMap) {
        TypeParameterComparator tc = new TypeParameterComparator(eMap.keySet(), options, reporter);
        IntTable<TypeParameterElement> typarams = IntTable.of(eMap, ExecutableElement::getTypeParameters);
        return tc.compareAll(ePos, typarams);
    }

    private boolean compareParameters(Position ePos, APIMap<ExecutableElement> eMap) {
        VariableComparator vc = new VariableComparator(eMap.keySet(), options, reporter);
        IntTable<VariableElement> params = IntTable.of(eMap, ExecutableElement::getParameters);
        return vc.compareAll(ePos::parameter, params);
    }

    private boolean compareReceiverType(Position ePos, APIMap<ExecutableElement> eMap) {
        if (ePos.is(ElementKind.CONSTRUCTOR))
            return true;

        TypeMirrorComparator tc = new TypeMirrorComparator(eMap.keySet(), reporter);
        APIMap<TypeMirror> rMap = eMap.map((api, e) -> {
            TypeMirror t = e.getReceiverType();
            if (t == null) {
                // TODO: make this print optional
                // System.err.println("unexpected null for getReceiverType " + ePos + " " + e);
                t = api.getTypes().getNoType(TypeKind.NONE);
            }
            return t;
        });
        return tc.compare(ePos.receiverType(), rMap);
    }

    private boolean compareReturnType(Position ePos, APIMap<ExecutableElement> eMap) {
        if (ePos.is(ElementKind.CONSTRUCTOR))
            return true;

        TypeMirrorComparator tc = new TypeMirrorComparator(eMap.keySet(), reporter);
        APIMap<TypeMirror> rMap = eMap.map(ExecutableElement::getReturnType);
        return tc.compare(ePos.returnType(), rMap);
    }

    private boolean compareThrows(Position ePos, APIMap<ExecutableElement> eMap) {
        Map<TypeMirrorKey, APIMap<TypeMirror>> map = extractThrownTypes(eMap);

        // compare the groups of thrown types
        Set<ElementKey> first = null;
        boolean allEqual = true;
        for (Map.Entry<TypeMirrorKey, APIMap<TypeMirror>> entry : map.entrySet()) {
            TypeMirrorKey tk = entry.getKey();
            APIMap<TypeMirror> tMap = entry.getValue();
            Position pos = ePos.exception(tk);
            if (tMap.size() < eMap.size()) {
                // Note: using reportDifferentTypes even if some of the thrown types are missing
                eMap.keySet().forEach(a -> tMap.putIfAbsent(a, null));
                reporter.reportDifferentTypes(pos, tMap);
                allEqual = false;
            } else {
                TypeMirrorComparator tmc = new TypeMirrorComparator(eMap.keySet(), reporter);
                allEqual = allEqual & tmc.compare(pos, tMap);
            }
        }

        if (allEqual) {
            return true;
        } else {
            APIMap<List<? extends TypeMirror>> thrownTypes = APIMap.of();
            eMap.forEach((api, ee) -> thrownTypes.put(api, ee.getThrownTypes()));
            reporter.reportDifferentThrownTypes(ePos, thrownTypes);
            return false;
        }
    }

    private Map<TypeMirrorKey, APIMap<TypeMirror>> extractThrownTypes(APIMap<ExecutableElement> eMap) {
        // The order in which thrown types may be listed is not significant,
        // so group the thrown types by their TypeMirrorKey.
        // Note that thrown types can be type variables, and even annotated
        Map<TypeMirrorKey, APIMap<TypeMirror>> map = new TreeMap<>();
        for (Map.Entry<API, ExecutableElement> entry : eMap.entrySet()) {
            API api = entry.getKey();
            ExecutableElement ee = entry.getValue();
            for (TypeMirror tm : ee.getThrownTypes()) {
                map.computeIfAbsent(TypeMirrorKey.of(tm), _k -> APIMap.of()).put(api, tm);
            }
        }
        return map;
    }

    private boolean compareDefaultValue(Position ePos, APIMap<ExecutableElement> eMap) {
        boolean noDefaultValues = eMap.values().stream()
                .map(ExecutableElement::getDefaultValue)
                .allMatch(Objects::isNull);
        if (noDefaultValues) {
            return true;
        }

        APIMap<AnnotationValue> defaultValues = APIMap.of();
        eMap.forEach((api, ee) -> {
            AnnotationValue dv = ee.getDefaultValue();
            if (dv != null) {
                defaultValues.put(api, dv);
            }
        });

        Position dvPos = new RelativePosition<Void>(ePos, RelativePosition.Kind.DEFAULT_VALUE);

        return new AnnotationComparator(apis, accessKind, reporter)
                .new AnnotationValueComparator(dvPos).compare(defaultValues);
    }
}

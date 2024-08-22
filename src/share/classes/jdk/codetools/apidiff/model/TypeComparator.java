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
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link TypeElement type elements}: classes, interfaces, enums and annotation types.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the "signature" of the type: its annotations, modifiers, kind, type parameters, supertypes
 *     <li>the documentation comment for the type
 *     <li>the selected members in the type
 * </ul>
 */
public class TypeComparator extends ElementComparator<TypeElement> {

    private final ElementExtras elementExtras = ElementExtras.instance();

    /**
     * Creates a comparator to compare type elements across a set of APIs.
     *
     * @param apis the set of APIs
     * @param options the command-line options
     * @param reporter the reporter to which to report differences
     */
    public TypeComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compare instances of a type element found at a given position in different APIs.
     *
     * @param tPos the position of the element
     * @param tMap the map giving the instance of the type element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position tPos, APIMap<TypeElement> tMap) {
        boolean allEqual = false;
        reporter.comparing(tPos, tMap);
        try {
            allEqual = checkMissing(tPos, tMap);
            if (tMap.size() > 1) {
                allEqual &= compareSignatures(tPos, tMap);
                allEqual &= compareDocComments(tPos, tMap);
                allEqual &= compareApiDescriptions(tPos, tMap);
                allEqual &= compareMembers(tPos, tMap);
                allEqual &= compareSerializedForms(tPos, tMap);
            }
        } finally {
            reporter.completed(tPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareSerializedForms(Position tPos, APIMap<TypeElement> tMap) {
        APIMap<SerializedForm> forms = APIMap.of();
        tMap.forEach((api, te) -> {
            SerializedForm sf = api.getSerializedForm(te);
            if (sf != null) {
                forms.put(api, sf);
            }
        });

        if (forms.isEmpty()) {
            return true;
        } else {
            SerializedFormComparator sfc = new SerializedFormComparator(tMap.keySet(), options, reporter);
            return sfc.compare(tPos.serializedForm(), forms);
        }
    }

    // TODO: in time, the members of a record type may include record components
    private boolean compareMembers(Position ePos, APIMap<TypeElement> tMap) {
        KeyTable<TypeElement> nestedTypes = new KeyTable<>();
        KeyTable<ExecutableElement> constructors = new KeyTable<>();
        KeyTable<ExecutableElement> methods = new KeyTable<>();
        KeyTable<VariableElement> enumConstants = new KeyTable<>();
        KeyTable<VariableElement> fields = new KeyTable<>();
        IntTable</*RecordComponent*/Element> recordComponents = new IntTable<>();

        for (Map.Entry<API, TypeElement> e : tMap.entrySet()) {
            API api = e.getKey();
            TypeElement te = e.getValue();
            for (Element member : te.getEnclosedElements()) {
                if (!accessKind.accepts(member)) {
                    continue;
                }
                ElementKey key = ElementKey.of(member);
                switch (member.getKind()) {
                    case ENUM, RECORD, CLASS, INTERFACE, ANNOTATION_TYPE ->
                        nestedTypes.put(key, api,  (TypeElement) member);

                    case ENUM_CONSTANT ->
                        enumConstants.put(key, api, (VariableElement) member);

                    case FIELD ->
                        fields.put(key, api, (VariableElement) member);

                    case CONSTRUCTOR ->
                        constructors.put(key, api, (ExecutableElement) member);

                    case METHOD ->
                        methods.put(key, api, (ExecutableElement) member);

                    case RECORD_COMPONENT ->
                        recordComponents.add(api, (VariableElement) member);

                    case STATIC_INIT, INSTANCE_INIT -> {
                        // expected but ignored, since it is never part of any API
                    }

                    default -> throw new Error("unexpected element: " + member.getKind() + " " + member);
                }
            }
        }

        Set<API> tMapApis = tMap.keySet();
        TypeComparator tc = new TypeComparator(tMapApis, options, reporter); // can we use "this"?
        VariableComparator vc = new VariableComparator(tMapApis, options, reporter);
        ExecutableComparator ec = new ExecutableComparator(tMapApis, options, reporter);
        RecordComponentComparator rc = new RecordComponentComparator(tMapApis, options, reporter);

        return tc.compareAll(nestedTypes)
                & rc.compareAll(ePos::recordComponent, recordComponents)
                & vc.compareAll(enumConstants)
                & vc.compareAll(fields)
                & ec.compareAll(constructors)
                & ec.compareAll(methods);
    }

    // TODO: in time, the signature of a sealed type may include the sealed modifier
    //       and its permits list; likewise the signature of a non-sealed subtype of
    //       a sealed type may include the non-sealed modifier
    private boolean compareSignatures(Position tPos, APIMap<TypeElement> tMap) {
        return compareAnnotations(tPos, tMap)
                & compareModifiers(tPos, tMap)
                & compareKinds(tPos, tMap)
                & compareTypeParameters(tPos, tMap)
                & compareSuperclass(tPos, tMap)
                & compareInterfaces(tPos, tMap)
                & comparePermittedSubclasses(tPos, tMap);
    }

    private boolean compareKinds(Position tPos, APIMap<TypeElement> tMap) {
        if (tMap.size() == 1)
            return true;

        ElementKind baseline = null;
        for (TypeElement te : tMap.values()) {
            if (baseline == null) {
                baseline = te.getKind();
            } else if (te.getKind() != baseline) {
                reporter.reportDifferentKinds(tPos, tMap);
                return false;
            }
        }
        return true;
    }

    private boolean compareTypeParameters(Position tPos, APIMap<TypeElement> tMap) {
        TypeParameterComparator tc = new TypeParameterComparator(tMap.keySet(), options, reporter);
        IntTable<TypeParameterElement> typarams = IntTable.of(tMap, TypeElement::getTypeParameters);
        return tc.compareAll(tPos, typarams);
    }

    private boolean compareSuperclass(Position pos, APIMap<TypeElement> eMap) {
        TypeMirrorComparator tc = new TypeMirrorComparator(eMap.keySet(), reporter);
        APIMap<TypeMirror> sMap = eMap.map(TypeElement::getSuperclass);
        return tc.compare(pos.superclass(), sMap); // null-friendly comparison
    }

    private boolean compareInterfaces(Position ePos, APIMap<TypeElement> tMap) {
        Map<ElementKey, APIMap<TypeMirror>> map = extractInterfaces(tMap);

        // TODO: the following could be a variant of TypeMirrorComparator::compareAll
        //       and shared with compareThrownTypes
        // compare the groups of superinterfaces
        Set<ElementKey> first = null;
        boolean allEqual = true;
        for (Map.Entry<ElementKey, APIMap<TypeMirror>> entry : map.entrySet()) {
            ElementKey ik = entry.getKey();
            APIMap<TypeMirror> iMap = entry.getValue();
            Position pos = ePos.superinterface(ik);
            if (iMap.size() < tMap.size()) {
                // Note: using reportDifferentTypes even if some of the superinterfaces are missing
                tMap.keySet().forEach(a -> iMap.putIfAbsent(a, null));
                reporter.reportDifferentTypes(pos, iMap);
                allEqual = false;
            } else {
                TypeMirrorComparator tmc = new TypeMirrorComparator(tMap.keySet(), reporter);
                allEqual = allEqual & tmc.compare(pos, iMap);
            }
        }

        if (allEqual) {
            return true;
        } else {
            APIMap<List<? extends TypeMirror>> superinterfaces = APIMap.of();
            tMap.forEach((api, te) -> superinterfaces.put(api, te.getInterfaces()));
            reporter.reportDifferentSuperinterfaces(ePos, superinterfaces);
            return false;
        }
    }

    // TODO: share with extractThrownTypes?
    private Map<ElementKey, APIMap<TypeMirror>> extractInterfaces(APIMap<TypeElement> tMap) {
        // The order in which superinterfaces may be listed is not significant,
        // so group the superinterfaces by their ElementKey.
        // Note that thrown types can be type variables, and even annotated
        Map<ElementKey, APIMap<TypeMirror>> map = new TreeMap<>();
        for (Map.Entry<API, TypeElement> entry : tMap.entrySet()) {
            API api = entry.getKey();
            TypeElement ee = entry.getValue();
            for (TypeMirror tm : ee.getInterfaces()) {
                Element e = api.getTypes().asElement(tm);
                map.computeIfAbsent(ElementKey.of(e), _k -> APIMap.of()).put(api, tm);
            }
        }
        return map;
    }

    private boolean comparePermittedSubclasses(Position ePos, APIMap<TypeElement> tMap) {
        Map<ElementKey, APIMap<TypeMirror>> map = extractPermittedSubclasses(tMap);

        // TODO: the following could be a variant of TypeMirrorComparator::compareAll
        //       and shared with compareInterfaces, compareThrownTypes
        // compare the groups of permitted subtypes
        Set<ElementKey> first = null;
        boolean allEqual = true;
        for (Map.Entry<ElementKey, APIMap<TypeMirror>> entry : map.entrySet()) {
            ElementKey sk = entry.getKey();
            APIMap<TypeMirror> sMap = entry.getValue();
            Position pos = ePos.permittedSubclass(sk);
            if (sMap.size() < tMap.size()) {
                // Note: using reportDifferentTypes even if some of the permitted subtypes are missing
                tMap.keySet().forEach(a -> sMap.putIfAbsent(a, null)); // TODO: is null OK here?
                reporter.reportDifferentTypes(pos, sMap);
                allEqual = false;
            } else {
                TypeMirrorComparator tmc = new TypeMirrorComparator(tMap.keySet(), reporter);
                allEqual = allEqual & tmc.compare(pos, sMap);
            }
        }

        if (allEqual) {
            return true;
        } else {
            APIMap<List<? extends TypeMirror>> subclasses = APIMap.of();
            tMap.forEach((api, te) -> subclasses.put(api, elementExtras.getPermittedSubclasses(te)));
            reporter.reportDifferentPermittedSubclasses(ePos, subclasses);
            return false;
        }
    }

    // TODO: share with extractThrownTypes?
    private Map<ElementKey, APIMap<TypeMirror>> extractPermittedSubclasses(APIMap<TypeElement> tMap) {
        // The order in which permitted subtypes may be listed is not significant,
        // so group the permitted subtypes by their ElementKey.
        // Note that permitted subtypes can be type variables, and even annotated
        Map<ElementKey, APIMap<TypeMirror>> map = new TreeMap<>();
        for (Map.Entry<API, TypeElement> entry : tMap.entrySet()) {
            API api = entry.getKey();
            TypeElement ee = entry.getValue();
            for (TypeMirror tm : elementExtras.getPermittedSubclasses(ee)) {
                Element e = api.getTypes().asElement(tm);
                map.computeIfAbsent(ElementKey.of(e), _k -> APIMap.of()).put(api, tm);
            }
        }
        return map;
    }
}

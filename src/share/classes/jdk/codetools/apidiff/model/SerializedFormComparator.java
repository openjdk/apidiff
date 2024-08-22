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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link SerializedForm serialized forms}.
 */
public class SerializedFormComparator {
    /** The APIs to be compared. */
    protected final Set<API> apis;
    /** The command-line options. */
    protected final Options options;
    /** The reporter to which to report any differences. */
    protected final Reporter reporter;

    /**
     * Creates a comparator to compare the serialized forms for an element across a set of APIs.
     *
     * @param apis     the set of APIs
     * @param options  the command-line options
     * @param reporter the reporter to which to report differences
     */
    public SerializedFormComparator(Set<API> apis, Options options, Reporter reporter) {
        this.apis = apis;
        this.options = options;
        this.reporter = reporter;
    }

    /**
     * Compares the serialized forms for a type element found at the given position in different APIs.
     *
     * @param sfPos the position for the serialized form
     * @param forms the map giving the serialized forms in the different APIs
     *
     * @return {@code true} if all the elements are equal
     */
    public boolean compare(Position sfPos, APIMap<SerializedForm> forms) {
        if (forms.isEmpty()) {
            return true;
        }

        boolean allEqual = false;
        reporter.comparing(sfPos, forms);
        try {
            allEqual = checkMissing(sfPos, forms);
            //if (forms.size() > 1) {
                allEqual &= compareSerialVersionUIDs(sfPos, forms);
                allEqual &= compareSerializationOverviews(sfPos, forms);
                allEqual &= compareSerializedFields(sfPos, forms);
                allEqual &= compareSerializationMethods(sfPos, forms);
            //}
        } finally {
            reporter.completed(sfPos, allEqual);
        }
        return allEqual;
    }

    private boolean checkMissing(Position pos, Map<API, ?> map) {
        Set<API> missing = apis.stream()
                .filter(a -> !map.containsKey(a))
                .collect(Collectors.toSet()); // warning: unordered

        if (missing.isEmpty()) {
            return true;
        } else {
            reporter.reportMissing(pos, missing);
            return false;
        }
    }

    private boolean compareSerialVersionUIDs(Position sfPos, APIMap<SerializedForm> fMap) {
        // Just direct comparison of long values, explicit or default;
        // there is no doc comment, but there may be an item in the serialized-form.html file
        Position uPos = sfPos.serialVersionUID();
        APIMap<Long> uMap = APIMap.of();
        fMap.forEach((api, sf) -> uMap.put(api, sf.getSerialVersionUID()));

        boolean allEqual = false;
        reporter.comparing(uPos, uMap);
        try {
            allEqual = checkMissing(uPos, uMap);
            if (fMap.size() > 1) {
                long archetype = uMap.values().iterator().next();
                boolean eq = uMap.values().stream()
                        .allMatch(u -> u == archetype);
                if (!eq) {
                    reporter.reportDifferentValues(uPos, uMap);
                }
                allEqual &= eq;
            }
            allEqual &= compareDescriptions(uPos, fMap, SerializedFormDocs::getSerialVersionUID);
        } finally {
            reporter.completed(uPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareSerializationOverviews(Position sfPos, APIMap<SerializedForm> fMap) {
        Position oPos = sfPos.serializationOverview();

        boolean allEqual = false;
        reporter.comparing(oPos, fMap);
        try {
            // there no derivative map other than the map of descriptions created in compareDescriptions
            allEqual = compareDescriptions(oPos, fMap, SerializedFormDocs::getOverview);
        } finally {
            reporter.completed(oPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareDescriptions(Position pos, APIMap<SerializedForm> fMap,
                                        Function<SerializedFormDocs, String> f) {
        APIMap<String> descriptions = fMap.map((api, sf) -> {
            SerializedFormDocs docs = sf.getDocs();
            return (docs == null) ? null : f.apply(docs);
        });

        // Descriptions are equal if none of the serialized forms has a description,
        // or if they all have the same description.
        // TODO: checkMissing?
        boolean allEqual = descriptions.isEmpty()
                || descriptions.size() == fMap.size() && descriptions.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentApiDescriptions(pos, descriptions);
        }
        return allEqual;
    }

    private boolean compareSerializedFields(Position sfPos, APIMap<SerializedForm> forms) {
        Map<String, APIMap<SerializedForm.Field>> fieldsMap = new TreeMap<>();
        forms.forEach((api, sf) -> {
            for (SerializedForm.Field f : sf.getFields()) {
                fieldsMap.computeIfAbsent(f.getName().toString(), __ -> APIMap.of()).put(api, f);
            }
        });

        boolean allEqual = true;
        for (Map.Entry<String, APIMap<SerializedForm.Field>> e : fieldsMap.entrySet()) {
            String name = e.getKey();
            APIMap<SerializedForm.Field> fMap = e.getValue();
            boolean equal = compare(sfPos, name, fMap);
            allEqual &= equal;
        }
        return allEqual;
    }

    private boolean compare(Position sfPos, String name, APIMap<SerializedForm.Field> fMap) {
        RelativePosition<String> fPos = sfPos.serializedField(name);
        boolean allEqual = false;
        reporter.comparing(fPos, fMap);
        try {
            allEqual = checkMissing(fPos, fMap);
            if (fMap.size() > 1) {
                allEqual &= compareSignatures(fPos, fMap);
                allEqual &= compareDocComments(fPos, fMap);
                allEqual &= compareSerializedFieldDescriptions(fPos, fMap);
            }
        } finally {
            reporter.completed(fPos, allEqual);
        }
        return allEqual;

    }

    private boolean compareSignatures(Position fPos, APIMap<SerializedForm.Field> fMap) {
        // If any field being compared has type of NONE, the field's type could not be resolved.
        // (Ideally, the representation would use ERROR instead of NONE, but that is not possible.)
        // Treat all such instances are automatically different to anything else.
        APIMap<TypeMirror> tMap = APIMap.of();
        fMap.forEach((api, f) -> tMap.put(api, f.getType()));
        if (fMap.values().stream().anyMatch(f -> f.getType().getKind() == TypeKind.NONE)) {
            reporter.reportDifferentTypes(fPos, tMap);
            return false;
        } else {
            TypeMirrorComparator tmc = new TypeMirrorComparator(fMap.keySet(), reporter);
            return tmc.compare(fPos, tMap);
        }
    }

    private boolean compareDocComments(Position fPos, APIMap<SerializedForm.Field> fMap) {
        APIMap<String> rawDocComments = APIMap.of();
        for (Map.Entry<API, SerializedForm.Field> entry : fMap.entrySet()) {
            API api = entry.getKey();
            SerializedForm.Field f = entry.getValue();
            String c = getDocComment(f);
            if (c != null) {
                rawDocComments.put(api, c);
            }
        }
        // raw doc comments are equal if none of the elements has a doc comment,
        // or if they all have the same doc comment.
        // TODO: checkMissing?
        boolean allEqual = rawDocComments.isEmpty()
                || rawDocComments.size() == fMap.size() && rawDocComments.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentRawDocComments(fPos, rawDocComments);
        }
        return allEqual;
    }

    private String getDocComment(SerializedForm.Field f) {
        List<? extends DocTree> trees = f.getDocComment();
        if (trees == null) {
            return null;
        }
        return trees.stream().map(Object::toString).collect(Collectors.joining());
    }

    private boolean compareSerializedFieldDescriptions(Position fPos, APIMap<SerializedForm.Field> fMap) {
        APIMap<String> descriptions = fMap.map((api, f) -> {
            SerializedForm form = api.getSerializedForm(f.getEnclosingTypeElement());
            if (form == null) return null;
            SerializedFormDocs docs = form.getDocs();
            if (docs == null) return null;
            return docs.getFieldDescription(f.getName().toString());
        });

        // Descriptions are equal if none of the fields has a description,
        // or if they all have the same description.
        // TODO: checkMissing?
        boolean allEqual = descriptions.isEmpty()
                || descriptions.size() == fMap.size() && descriptions.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentApiDescriptions(fPos, descriptions);
        }
        return allEqual;
    }

    private boolean compareSerializationMethods(Position sfPos, APIMap<SerializedForm> forms) {
        Map<RelativePosition<String>, APIMap<ExecutableElement>> mMap = new TreeMap<>(RelativePosition.stringIndexComparator);
        forms.forEach((api, sf) -> {
            for (ExecutableElement m : sf.getMethods()) {
                RelativePosition<String> mPos = sfPos.serializationMethod(m.getSimpleName().toString());
                mMap.computeIfAbsent(mPos, p -> APIMap.of()).put(api, m);
            }
        });

        ExecutableComparator ec = new ExecutableComparator(apis, options, reporter) {
            /**
             * Returns the API description found in the serialized-form.html file.
             */
            @Override
            protected String getApiDescription(API api, Element e) {
                SerializedFormDocs docs = forms.get(api).getDocs();
                return docs == null ? null : docs.getMethodDescription(e.getSimpleName().toString());
            }
        };

        boolean allEqual = true;
        for (Map.Entry<RelativePosition<String>, APIMap<ExecutableElement>> entry : mMap.entrySet()) {
            allEqual &= ec.compare(entry.getKey(), entry.getValue());
        }
        return allEqual;
    }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractAnnotationValueVisitor14;

import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for the {@link AnnotationMirror annotations} on {@link AnnotatedConstruct annotated constructs}.
 *
 * <p>Annotations are compared according to their structure ("deep equals") down to
 * the level of element names, which are compared using {@link ElementKey#equals ElementKey.equals}.
 */
public class AnnotationComparator {
    /** The APIs to be compared. */
    protected final Set<API> apis;
    /** The access kind. */
    private final AccessKind accessKind;
    /** The reporter to which to report any differences. */
    protected final Reporter reporter;

    /**
     * Creates an instance of comparator for annotations.
     *
     * @param apis       the APIs being compared
     * @param accessKind the access kind
     * @param reporter   the reporter to which to report any differences.
     */
    protected AnnotationComparator(Set<API> apis, AccessKind accessKind, Reporter reporter) {
        this.apis = apis;
        this.accessKind = accessKind;
        this.reporter = reporter;
    }

    /**
     * Compares all the annotations at a given position within instances of an API.
     *
     * @param pos the position of the annotations to be compared
     * @param acMap the annotated constructs whose annotations are being compared
     * @return {@code true} if and only if all the annotations are equal
     */
    public boolean compareAll(Position pos, APIMap<? extends AnnotatedConstruct> acMap) {
        Map<ElementKey, APIMap<AnnotationMirror>> annoMap = extractAnnotations(acMap);

        boolean allEqual = true;
        for (Map.Entry<ElementKey, APIMap<AnnotationMirror>> e : annoMap.entrySet()) {
            ElementKey k = e.getKey();
            APIMap<AnnotationMirror> v = e.getValue();
            boolean equal = compare(pos.annotation(k), v);
            allEqual &= equal;
        }
        return allEqual;
    }

    private Map<ElementKey, APIMap<AnnotationMirror>> extractAnnotations(APIMap<? extends AnnotatedConstruct> acMap) {
        Map<ElementKey, APIMap<AnnotationMirror>> annoMap = new HashMap<>();
        for (Map.Entry<API, ? extends AnnotatedConstruct> entry : acMap.entrySet()) {
            API api = entry.getKey();
            AnnotatedConstruct c = entry.getValue();
            for (AnnotationMirror am : c.getAnnotationMirrors()) {
                if (isIncluded(api, am)) {
                    annoMap.computeIfAbsent(ElementKey.of(am.getAnnotationType().asElement()), e -> APIMap.of())
                            .put(api, am);
                }
            }
        }
        return annoMap;
    }

    private boolean isIncluded(API api, AnnotationMirror am) {
        return switch (accessKind) {
            case PUBLIC, PROTECTED -> api.isDocumented(am);
            default -> true;
        };
    }

    /**
     * Compares the annotations found at a given position in the instances of
     * the APIs being compared.
     *
     * @param pos the position
     * @param map the annotations
     * @return {@code true} if and only if all the annotations are equal
     */
    public boolean compare(Position pos, APIMap<AnnotationMirror> map) {
        Map<ElementKey, APIMap<AnnotationValue>> valueMap = new HashMap<>();
        for (Map.Entry<API, AnnotationMirror> entry : map.entrySet()) {
            API api = entry.getKey();
            AnnotationMirror am = entry.getValue();
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = api.getAnnotationValuesWithDefaults(am);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : values.entrySet()) {
                ExecutableElement ee = e.getKey();
                AnnotationValue av = e.getValue();
                valueMap.computeIfAbsent(ElementKey.of(ee), e_ -> APIMap.of()).put(api, av);
            }
        }

        boolean allEqual = true;
        reporter.comparing(pos, map);
        try {
            for (Map.Entry<ElementKey, APIMap<AnnotationValue>> entry : valueMap.entrySet()) {
                ElementKey key = entry.getKey();
                APIMap<AnnotationValue> avMap = entry.getValue();
                allEqual &= new AnnotationValueComparator(pos.annotationValue(key)).compare(avMap);
            }
        } finally {
            reporter.completed(pos, allEqual);
        }

        return allEqual;
    }

    class AnnotationValueComparator extends AbstractAnnotationValueVisitor14<Boolean, APIMap<AnnotationValue>> {

        private final Position pos;

        AnnotationValueComparator(Position pos) {
            this.pos = pos;
        }

        boolean compare(APIMap<AnnotationValue> avMap) {
            boolean equal = false;
            reporter.comparing(pos, avMap);
            try {
                AnnotationValue baseline = avMap.values().stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if (baseline == null) {
                    // no non-null value found, so all must be null, and hence equal
                    return true;
                }
                equal = visit(baseline, avMap);
            } finally {
                reporter.completed(pos, equal);
            }
            return equal;
        }

        @Override
        public Boolean visitAnnotation(AnnotationMirror a, APIMap<AnnotationValue> avMap) {
            APIMap<AnnotationMirror> amMap = APIMap.of();
            for (Map.Entry<API, AnnotationValue> entry : avMap.entrySet()) {
                API api = entry.getKey();
                AnnotationValue av = entry.getValue();
                Object avo = av.getValue();
                if (avo instanceof AnnotationMirror) {
                    amMap.put(api, (AnnotationMirror) avo);
                } else {
                    reporter.reportDifferentAnnotationValues(pos, avMap);
                    return false;
                }
            }
            return AnnotationComparator.this.compare(pos, amMap);
        }

        @Override
        public Boolean visitArray(List<? extends AnnotationValue> l, APIMap<AnnotationValue> avMap) {
            IntTable<AnnotationValue> t = new IntTable<>();
            for (Map.Entry<API, AnnotationValue> entry : avMap.entrySet()) {
                API api = entry.getKey();
                AnnotationValue av = entry.getValue();
                if (av.getValue() instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationValue> avList = (List<? extends AnnotationValue>) av.getValue();
                    t.put(api, avList);
                } else {
                    reporter.reportDifferentAnnotationValues(pos, avMap);
                    return false;
                }
            }

            boolean allEqual = true;
            for (int i = 0; i < t.size(); i++) {
                APIMap<AnnotationValue> map = t.entries(i);
                allEqual &= new AnnotationValueComparator(pos.annotationArrayIndex(i)).compare(map);
            }
            return allEqual;
        }

        @Override
        public Boolean visitBoolean(boolean b, APIMap<AnnotationValue> avMap) {
            return compare(b, avMap);
        }

        @Override
        public Boolean visitByte(byte b, APIMap<AnnotationValue> avMap) {
            return compare(b, avMap);
        }

        @Override
        public Boolean visitChar(char c, APIMap<AnnotationValue> avMap) {
            return compare(c, avMap);
        }

        @Override
        public Boolean visitDouble(double d, APIMap<AnnotationValue> avMap) {
            return compare(d, avMap);
        }

        @Override
        public Boolean visitEnumConstant(VariableElement ve, APIMap<AnnotationValue> avMap) {
            return compare(ElementKey.of(ve), avMap, o -> {
                if (!(o instanceof VariableElement))
                    return false;
                return ElementKey.of((VariableElement) o);
            });
        }

        @Override
        public Boolean visitFloat(float f, APIMap<AnnotationValue> avMap) {
            return compare(f, avMap);
        }

        @Override
        public Boolean visitInt(int i, APIMap<AnnotationValue> avMap) {
            return compare(i, avMap);
        }

        @Override
        public Boolean visitLong(long l, APIMap<AnnotationValue> avMap) {
            return compare(l, avMap);
        }

        @Override
        public Boolean visitShort(short s, APIMap<AnnotationValue> avMap) {
            return compare(s, avMap);
        }

        @Override
        public Boolean visitString(String s, APIMap<AnnotationValue> avMap) {
            return compare(s, avMap);
        }

        @Override
        public Boolean visitType(TypeMirror t, APIMap<AnnotationValue> avMap) {
            return compare(TypeMirrorKey.of(t), avMap, o -> {
                if (!(o instanceof TypeMirror))
                    return false;
                return TypeMirrorKey.of((TypeMirror) o);
            });
        }

        @Override
        public Boolean visitUnknown(AnnotationValue av, APIMap<AnnotationValue> avMap) {
            // should not happen!
            reporter.reportDifferentAnnotationValues(pos, avMap);
            return false;
        }

        /**
         * Compares an object against all the given annotation values, using {@code Object.equals}.
         * @param o the object
         * @param avMap the collection of annotation values
         * @return {@code true} if the object equals all the annotation values
         */
        private boolean compare(Object o,  APIMap<AnnotationValue> avMap) {
            for (AnnotationValue v : avMap.values()) {
                if (v == null || !o.equals(v.getValue())) {
                    reporter.reportDifferentAnnotationValues(pos, avMap);
                    return false;
                }
            }
            return true;
        }

        /**
         * Compares an object against all the given annotation values, a function to derive a value
         * to be compare with using {@code Object.equals}.
         * @param o the object
         * @param avMap the collection of annotation values
         * @return {@code true} if the object equals all the transformed annotation values
         */
        private boolean compare(Object o, APIMap<AnnotationValue> avMap, Function<Object, ?> f) {
            for (AnnotationValue v : avMap.values()) {
                if (v == null || !o.equals(f.apply(v.getValue()))) {
                    reporter.reportDifferentAnnotationValues(pos, avMap);
                    return false;
                }
            }
            return true;
        }
    }

}

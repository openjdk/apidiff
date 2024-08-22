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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A base class for comparators that compare elements.
 *
 * @param <E> the type of element to be compared
 */
public abstract class ElementComparator<E extends Element> {
    /** The APIs to be compared. */
    protected final Set<API> apis;
    /** The command-line options. */
    protected final Options options;
    /** The access kind. */
    protected final AccessKind accessKind;
    /** The reporter to which to report any differences. */
    protected final Reporter reporter;

    /**
     * Creates an instance of comparator for elements.
     *
     * @param apis     the APIs being compared
     * @param options  the command line options
     * @param reporter the reporter to which to report any differences.
     */
    protected ElementComparator(Set<API> apis, Options options, Reporter reporter) {
        this.apis = apis;
        this.options = options;
        this.accessKind = options.getAccessKind();
        this.reporter = reporter;
    }

    /**
     * Compares all the elements with equivalent keys within instances of an API.
     *
     * @param table the table containing the elements to be compared
     * @return {@code true} if and only if all the elements are equal
     */
    public boolean compareAll(KeyTable<E> table) {
        boolean allEqual = true;
        for (Map.Entry<ElementKey, APIMap<E>> e : table.entries()) {
            ElementKey k = e.getKey();
            APIMap<E> v = e.getValue();
            boolean equal = compare(k, v);
            allEqual &= equal;
        }
        return allEqual;
    }

    /**
     * Compares all the elements at equivalent positions within instances of an API.
     *
     * @param f a function to provide the position of each row in the table
     * @param table the table containing the elements to be compared
     * @return {@code true} if and only if all the elements are equal
     */
    public boolean compareAll(Function<Integer, Position> f, IntTable<E> table) {
        boolean allEqual = true;
        for (int i = 0; i < table.size(); i++) {
            allEqual &= compare(f.apply(i), table.entries(i));
        }
        return allEqual;
    }

    /**
     * Compares the elements found at the given position in different APIs.
     *
     * <p>This implementation delegates to {@link #compare(Position, APIMap)}.
     *
     * @param eKey the key for the element
     * @param eMap the map giving the elements in the different APIs
     * @return {@code true} if all the elements are equal
     */
    public boolean compare(ElementKey eKey, APIMap<E> eMap) {
        return compare(Position.of(eKey), eMap);
    }

    /**
     * Compares the elements found at the given position in different APIs.
     *
     * @param ePos the position of the element
     * @param eMap the map giving the elements in the different APIs
     * @return {@code true} if all the elements are equal
     */
    public abstract boolean compare(Position ePos, APIMap<E> eMap);

    /**
     * Checks whether any expected elements are missing in any APIs.
     * Missing elements will be reported to the comparator's reporter.
     *
     * @param ePos the position of the element
     * @param eMap the map giving the elements in the different APIs
     * @return {@code true} if all the expected elements are found
     */
    public boolean checkMissing(Position ePos, APIMap<E> eMap) {
        Set<API> missing = apis.stream()
                .filter(a -> !eMap.containsKey(a))
                .collect(Collectors.toSet()); // warning: unordered

        if (missing.isEmpty()) {
            return true;
        } else {
            reporter.reportMissing(ePos, missing);
            return false;
        }
    }

    /**
     * Compares the annotations for elements at a given position in
     * different instances of an API.
     *
     * @param ePos the position
     * @param eMap the map of elements
     * @return {@code true} if and only if all the annotations are equal
     */
    protected boolean compareAnnotations(Position ePos, APIMap<E> eMap) {
        AnnotationComparator ac = new AnnotationComparator(eMap.keySet(), accessKind, reporter);
        return ac.compareAll(ePos, eMap);
    }

    /**
     * Compares the documentation for the elements at a given position in
     * different instances of an API.
     *
     * @param ePos the position
     * @param eMap the map of elements
     * @return {@code true} if and only if all the instances of the documentation are equal
     */
    protected boolean compareDocComments(Position ePos, APIMap<E> eMap) {
        if (!options.compareDocComments()) {
            return true;
        }

//        APIMap<DocCommentTree> docComments = APIMap.of();
//        for (Map.Entry<API, E> e : eMap.entrySet()) {
//            API api = e.getKey();
//            Element te = e.getValue();
//            DocCommentTree dct = api.getDocComment(te);
//            if (dct != null) {
//                docComments.put(api, dct);
//            }
//        }
//        DocCommentComparator dc = new DocCommentComparator(eMap.keySet());
//        return dc.compare(ePos, docComments);

        APIMap<String> rawDocComments = APIMap.of();
        for (Map.Entry<API, E> entry : eMap.entrySet()) {
            API api = entry.getKey();
            Element e = entry.getValue();
            String c = getDocComment(api, e);
            if (c != null) {
                rawDocComments.put(api, c);
            }
        }
        // raw doc comments are equal if none of the elements has a doc comment,
        // or if they all have the same doc comment.
        boolean allEqual = rawDocComments.isEmpty()
                || rawDocComments.size() == eMap.size() && rawDocComments.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentRawDocComments(ePos, rawDocComments);
        }
        return allEqual;
    }

    /**
     * Returns the doc comment for an element in a given API.
     *
     * <p>This implementation uses {@link API#getElements()}.getDocComment(Element)}.
     *
     * @param api the API
     * @param e   the element
     *
     * @return the doc comment
     */
    protected String getDocComment(API api, Element e) {
        return api.getElements().getDocComment(e);
    }

    /**
     * Compares the API descriptions for the elements at a given position
     * in different instances of an API.
     *
     * @param ePos the position
     * @param eMap the map of elements
     *
     * @return {@code true} if and only if all the instances of the API description are equal
     */
    protected boolean compareApiDescriptions(Position ePos, APIMap<E> eMap) {
        if (!options.compareApiDescriptions()) {
            return true;
        }

        APIMap<String> apiDescriptions = APIMap.of();
        for (Map.Entry<API, E> entry : eMap.entrySet()) {
            API api = entry.getKey();
            Element e = entry.getValue();
            String c = getApiDescription(api, e);
            if (c != null) {
                apiDescriptions.put(api, c);
            }
        }
        // API descriptions are equal if none of the elements has a description,
        // or if they all have the same doc comment.
        boolean allEqual = apiDescriptions.isEmpty()
                || apiDescriptions.size() == eMap.size() && apiDescriptions.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentApiDescriptions(ePos, apiDescriptions);
        }
        return allEqual;
    }


    /**
     * Returns the API description for an element in a given API.
     *
     * <p>This implementation uses {@link API#getApiDescription(Element)}.
     *
     * @param api the API
     * @param e   the element
     *
     * @return the API description
     */
    protected String getApiDescription(API api, Element e) {
        return api.getApiDescription(e);
    }

    /**
     * Compares the doc files for a module or package at a given position
     * in different instance of an API.
     *
     * @param ePos the position
     * @param eMap the map of elements
     *
     * @return {@code true} if and only if all the doc files are equal
     */
    protected boolean compareDocFiles(Position ePos, APIMap<? extends Element> eMap) {
        Map<String, APIMap<DocFile>> fMap = DocFile.listDocFiles(eMap);
        DocFilesComparator dfc = new DocFilesComparator(apis, options, reporter);
        return dfc.compareAll(ePos, fMap);
    }

    /**
     * Compares the modifiers for the elements at a given position in
     * different instances of an API.
     *
     * @param ePos the position
     * @param eMap the map of elements
     * @return {@code true} if and only if all the modifiers are equal
     */
    protected boolean compareModifiers(Position ePos, APIMap<E> eMap) {
        if (eMap.size() == 1)
            return true;

        Set<Modifier> baseline = null;
        for (E e : eMap.values()) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.NATIVE) || modifiers.contains(Modifier.SYNCHRONIZED)) {
                modifiers = EnumSet.copyOf(modifiers);
                modifiers.removeAll(EnumSet.of(Modifier.NATIVE, Modifier.SYNCHRONIZED));
            }
            if (baseline == null) {
                baseline = modifiers;
            } else if (!baseline.equals(modifiers)) {
                reporter.reportDifferentModifiers(ePos, eMap);
                return false;
            }
        }

        return true;
    }

}

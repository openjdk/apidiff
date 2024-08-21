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
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.API.LocationKind;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link PackageElement package elements}.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the documentation comment for the package
 *     <li>the selected types in the package
 *     <li>additional documentation files for the package
 * </ul>
 */
public class PackageComparator extends ElementComparator<PackageElement> {

    /**
     * Creates a comparator to compare package elements across a set of APIs.
     *
     * @param apis     the set of APIs
     * @param options  the command-line options
     * @param reporter the reporter to which to report differences
     */
    public PackageComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compares instances of a package element found in different APIs.
     *
     * @param pPos the position of the element
     * @param pMap the map giving the instance of the package element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position pPos, APIMap<PackageElement> pMap) {
        boolean allEqual = false;
        reporter.comparing(pPos, pMap);
        try {
            allEqual = checkMissing(pPos, pMap);
            if (pMap.size() > 1) {
                allEqual &= compareAnnotations(pPos, pMap);
                allEqual &= compareDocComments(pPos, pMap);
                allEqual &= compareApiDescriptions(pPos, pMap);
                allEqual &= compareTypes(pPos, pMap);
                allEqual &= compareDocFiles(pPos, pMap);
            }
        } finally {
            reporter.completed(pPos, allEqual);
        }
        return allEqual;
    }

    private boolean compareTypes(Position pPos, APIMap<PackageElement> pMap) {
        TypeComparator tc = new TypeComparator(pMap.keySet(), options, reporter);
        KeyTable<TypeElement> types = KeyTable.of(pMap, API::getTypeElements);
        return tc.compareAll(types);
    }

    /**
     * Returns the doc comment for an element in a given API.
     *
     * If the element is a package element, and no comment is found in the
     * package's {@code package-info.java} file, the methods looks for
     * a {@code package.html} file as a fallback.
     *
     * @param api the API
     * @param e   the element
     *
     * @return the doc comment
     */
    @Override
    protected String getDocComment(API api, Element e) {
        String s = super.getDocComment(api, e);
        if (s != null) {
            return s;
        }

        if (e.getKind() == ElementKind.PACKAGE) {
            // There is no easy API to get the package.html file directly.
            // Trees.getDocCommentTree(Element e, String relativeName) only looks on
            // the source path; ideally, it should realize the package is in a module
            // and use the correct entry from the module source path. But even so,
            // this method is somewhat lower-level, and just wants the raw text,
            // and not the parsed doc comment tree.
            for (JavaFileObject fo : api.listFiles(LocationKind.SOURCE, e, "",
                    EnumSet.of(JavaFileObject.Kind.HTML), false)) {
                if (fo.getName().endsWith("/package.html")) {
                    return api.getApiDescription(fo);
                }
            }
        }

        return null;
    }
}

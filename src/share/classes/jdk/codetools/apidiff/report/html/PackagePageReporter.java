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

package jdk.codetools.apidiff.report.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.AccessKind;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.Position;

/**
 * A reporter that generates an HTML page for the differences in
 * a package declaration.
 */
class PackagePageReporter extends PageReporter<PackageElementKey> {

    PackagePageReporter(HtmlReporter parent, ElementKey pKey) {
        super(parent, (PackageElementKey) pKey);
    }

    /**
     * Writes a page containing details for a single package element in a given API.
     *
     * @param api the API containing the element
     * @param pkg the package element
     */
    void writeFile(API api, PackageElement pkg) {
        Position pagePos = Position.of(pageKey);
        APIMap<PackageElement> apiMap = APIMap.of(api, pkg);
        comparing(pagePos, apiMap);
        Set<API> missing = parent.apis.stream()
                .filter(a -> a != api)
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            reportMissing(pagePos, missing);
        }
        completed(pagePos, true);
    }

    @Override
    protected void writeFile() {
        // If this is the only copy of the package in the APIs being compared,
        // its enclosed elements will not have been compared or reported, and
        // so will not be written out as a side effect of reporting the
        // comparison.  So, write the files for the enclosed classes and
        // interfaces now.
        APIMap<? extends Element> pMap = getElementMap(pageKey);
        if (pMap.size() == 1) {
            AccessKind accessKind = parent.options.getAccessKind();
            Map.Entry<API, ? extends Element> e = pMap.entrySet().iterator().next();
            API api = e.getKey();
            PackageElement pkg = (PackageElement) e.getValue();
            for (TypeElement te : ElementFilter.typesIn(pkg.getEnclosedElements())) {
                if (!accessKind.accepts(te)) {
                    continue;
                }
                TypePageReporter r = (TypePageReporter) parent.getPageReporter(ElementKey.of(te));
                r.writeFile(api, te);
            }
        }

        super.writeFile();
    }

    @Override
    protected String getTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append("package ");
        if (pageKey.moduleKey != null) {
            ModuleElementKey mKey = (ModuleElementKey) pageKey.moduleKey;
            sb.append(mKey.name).append("/");
        }
        sb.append(pageKey.name);
        return sb.toString();
    }

    @Override
    protected Content buildSignature() {
        List<Content> contents = new ArrayList<>();
        contents.addAll(buildAnnotations(Position.of(pageKey)));
        contents.add(Keywords.PACKAGE);
        contents.add(Text.SPACE);
        contents.add(Text.of(pageKey.name));
        return HtmlTree.DIV(contents).setClass("signature");
    }

    @Override
    protected List<Content> buildEnclosedElements() {
        List<Content> list = new ArrayList<>();
        addEnclosedElements(list, "heading.types", ek -> ek.kind == ElementKey.Kind.TYPE);
        addDocFiles(list);
        return list;
    }

}

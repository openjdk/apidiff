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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.Entity;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;

/**
 * A class to build a list for the different versions of an API fragment
 * in different instances of an API.
 *
 * <p>The various {@code build} methods all take a collection providing
 * a list of alternatives. While we could use a {@code <ul>} list to display
 * the alternatives, in general it is expected that the alternatives will
 * be presented inline, and not as a typical bulleted list. In addition,
 * using {@code <ul>} would require that all enclosing elements are able to
 * accept flow content.  Therefore, instead of using {@code <ul>} and a series
 * of {@code <li> nodes}, the output is a {@code <span>} with class "diffs"
 * containing a series of {@code <span>} elements for the alternatives.
 */
public class DiffBuilder {
    Content build(List<Content> list) {
        HtmlTree outerSpan = newDiffsSpan();
        list.stream()
                .map(HtmlTree::SPAN)
                .forEach(outerSpan::add);
        return outerSpan;
    }

    Content build(APIMap<Content> alternatives) {
        HtmlTree outerSpan = newDiffsSpan();
        alternatives.forEach((api, c) -> outerSpan.add(buildItem(api, c)));
        return outerSpan;
    }

    <T> Content build(APIMap<T> alternatives, Function<T, Content> f) {
        HtmlTree outerSpan = newDiffsSpan();
        alternatives.forEach((api, t) -> outerSpan.add(buildItem(api, f.apply(t))));
        return outerSpan;
    }

    <T> Content build(Set<API> apis, APIMap<T> alternatives, Function<T, Content> f) {
        HtmlTree outerSpan = newDiffsSpan();
        for (API api : apis) {
            T item = alternatives.get(api);
            outerSpan.add(buildItem(api, item == null ? Entity.NBSP : f.apply(item)));
        }
        return outerSpan;
    }

    private HtmlTree newDiffsSpan() {
        return HtmlTree.SPAN().setClass("diffs");
    }

    private Content buildItem(API api, Content c) {
        return HtmlTree.SPAN(c).setClass("api").setTitle(api.name); // TODO: improve class with api name/index
    }
}

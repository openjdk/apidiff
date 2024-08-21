/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;

/**
 * A class to build HTML to display pairwise differences between groups of API-specific items.
 *
 * @param <T> the type of the items.
 */
public abstract class PairwiseDiffBuilder<T> {
    private final Set<API> apis;
    protected final Log log;
    protected final Messages msgs;

    /**
     * Creates an instance of a {@code PairwiseDiffBuilder}.
     *
     * @param log the log to which to report any problems while using this builder
     */
    public PairwiseDiffBuilder(Set<API> apis, Log log, Messages msgs) {
        this.apis = apis;
        this.log = log;
        this.msgs = msgs;
    }

    /**
     * Builds HTML that displays the differences between API-specific items,
     * by doing pair-wise comparisons between a reference API and the focus API.
     *
     * @param map the map of API-specific items
     * @return the HTML nodes
     */
    public List<Content> build(APIMap<T> map, Consumer<ResultTable.CountKind> counter) {
        List<API> apiList = new ArrayList<>(apis);
        API focusAPI = apiList.get(apiList.size() - 1);

        // first, determine the equivalence groups,
        Map<String, List<API>> groups = new LinkedHashMap<>();
        for (API api : apis) {
            groups.computeIfAbsent(getKeyString(map.get(api)), k -> new ArrayList<>()).add(api);
        }

        List<API> focusGroup = groups.values().stream()
                .filter(l -> l.contains(focusAPI))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ref group not found"));
        String focusNames = getNameList(focusGroup);
        T focusItem = map.get(focusAPI);

        List<Content> contents = new ArrayList<>();
        // Now, compare each group against the group containing the reference API.
        // Since there is only one group for all items with no content, there are
        // effectively only 3 possibilities:
        // 1. Different text in each group, display as a pair
        // 2. Text only in one of the groups, but not the reference group
        // 3. Text only in the reference group, not in the other one of the pair
        for (Map.Entry<String, List<API>> entry : groups.entrySet()) {
            String key = entry.getKey();
            List<API> apis = entry.getValue();
            if (apis == focusGroup) {
                // if all the entries are same, there will only be one group,
                // which will contain the focus, so no output will be generated;
                // if the entries are not all the same, skip pairwise comparison
                // of the focusGroup with itself!
                continue;
            }

            T other = map.get(apis.get(0));

            /*
            String otherNames = getNameList(apis);
            if (focusItem != null && key != null) {
                // item in both groups: display the comparison
                Content title = Text.of(String.format("Comparing %s with %s", otherNames, focusNames)); // TODO: improve
                contents.add(build(title, other, focusItem, counter));
            } else if (key == null) {
                Content title = Text.of(String.format("Not in %s; only in %s", otherNames, focusNames)); // TODO: improve
                contents.add(build(title, focusItem));
            } else if (focusItem == null) {
                Content title = Text.of(String.format("Only in %s; not in %s", otherNames, focusNames)); // TODO: improve
                contents.add(build(title, other));
            }
            */

            contents.add(build(apis, other, focusGroup, focusItem, counter));
        }

        return contents;
    }

    protected abstract Content build(List<API> refAPIs, T refItem,
                                     List<API> focusAPIs, T focusItem,
                                     Consumer<ResultTable.CountKind> counter);

    protected abstract String getKeyString(T t);

//    protected abstract Content build(Content title, T refItem, T modItem,
//                                     Consumer<ResultTable.CountKind> counter);
//
//    protected abstract Content build(Content title, T item);

    protected String getNameList(List<API> apis) {
        return apis.stream()
                .map(a -> a.name)
                .collect(Collectors.joining(", "));
    }
}


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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A table of {@code (int x API) -> T}.
 *
 * The table can be used associate elements from different APIs
 * that are associated by their position in their enclosing element.
 *
 * @param <T> the type of item stored in this table
 */
public class IntTable<T> {
    private List<APIMap<T>> list = new ArrayList<>();

    /**
     * Creates a table from an API map and a function to provide a list of related items to populate the table.
     *
     * @param map the map
     * @param f   the function
     * @param <T> the type of values in the map
     * @param <R> the type of values in the list returned by the function
     *
     * @return the table
     *
     * @see KeyTable#of
     */
    // Used to get type parameters (of type and executable), parameters, and bounds of type parameters
    static <T, R> IntTable<R> of(APIMap<? extends T> map, Function<T, List<? extends R>> f) {
        IntTable<R> result = new IntTable<>();
        for (Map.Entry<API, ? extends T> e : map.entrySet()) {
            result.put(e.getKey(), f.apply(e.getValue()));
        }
        return result;
    }

    /**
     * Updates the table with a series of values for a given API.
     * The effect is to set the values in a column of the table,
     * extending the number of rows if necessary.
     *
     * @param api the api
     * @param items the items
     */
    public void put(API api, List<? extends T> items) {
        int i = 0;
        for (T item : items) {
            APIMap<T> m;
            if (i < list.size()) {
                m = list.get(i);
            } else {
                m = APIMap.of();
                list.add(m);
            }
            m.put(api, item);
            i++;
        }
    }

    /**
     * Adds a new entry for a given API.
     *
     * @param api the API
     * @param item the item
     */
    public void add(API api, T item) {
        for (APIMap<T> m : list) {
            if (!m.containsKey(api)) {
                m.put(api, item);
                return;
            }
        }
        APIMap<T> m = APIMap.of();
        list.add(m);
        m.put(api, item);
    }

    /**
     * Returns the number of rows in the table.
     *
     * @return the number of rows in the table.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the entries for a given row in the table.
     *
     * @param index the index of the row.
     * @return the entries
     */
    public APIMap<T> entries(int index) {
        return list.get(index);
    }
}

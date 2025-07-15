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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import javax.lang.model.element.Element;

/**
 * A table of {@code (ElementKey x API) -> T}.
 *
 * The table can be used associate items from different APIs that are associated
 * by means of a key representing the item.
 *
 * @param <T> the type of item stored in this table
 */
public class KeyTable<T> {
    private Map<ElementKey, APIMap<T>> map = new TreeMap<>();

    /**
     * Creates a table from an API map and a function to provide a set of related items to populate the table.
     *
     * @param map the map
     * @param f   the function
     * @param <T> the type of values in the map
     * @param <R> the type of values in the set returned by the function
     *
     * @return the table
     *
     * @see IntTable#of
     */
    // Used to get packages from a module, and types from a package
    static <T, R extends Element> KeyTable<R> of(APIMap<? extends T> map, BiFunction<API, T, Set<? extends R>> f) {
        KeyTable<R> result = new KeyTable<>();
        for (Map.Entry<API, ? extends T> entry : map.entrySet()) {
            API api = entry.getKey();
            for (R e : f.apply(api, entry.getValue())) {
                result.put(ElementKey.of(e), api, e);
            }
        }
        return result;
    }

    /**
     * Puts an item into the table, according to a key and the api in which it is
     * an instance.
     *
     * @param key the key
     * @param api the api
     * @param item the item
     * @return the previous value, if any
     */
    public T put(ElementKey key, API api, T item) {
        return map.computeIfAbsent(key, _k -> APIMap.of()).put(api, item);
    }

    /**
     * Puts an item into the table if it does not exist, according to a key and the api in which it is
     * an instance.
     *
     * @param key the key
     * @param api the api
     * @param item the item
     * @return the previous value, if any
     */
    public T putIfAbsent(ElementKey key, API api, T item) {
        return map.computeIfAbsent(key, _k -> APIMap.of()).putIfAbsent(api, item);
    }

    /**
     * Returns an iterator for the collections of items within the table
     * for a given key.
     *
     * @return an iterable for the collections of items associated with a given key
     */
    public Iterable<Map.Entry<ElementKey,APIMap<T>>> entries() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}

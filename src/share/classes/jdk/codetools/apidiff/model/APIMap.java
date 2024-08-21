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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map in which to store the instances of corresponding items in different
 * instances of an API.
 *
 * <p>Null values are not permitted in the map.
 *
 * <p>The representation of the map is intended to be opaque.
 *
 * @param <T> the type of the items in this map
 */
public class APIMap<T> extends LinkedHashMap<API, T> {
    private static final long serialVersionUID = 0;

    private APIMap() { }

    /**
     * Creates an instance of an {@code APIMap}.
     *
     * @param <T> the type of the instances in the map.
     * @return the map
     */
    public static <T> APIMap<T> of()  {
        return new APIMap<>();
    }

    /**
     * Creates an instance of an {@code APIMap} containing an initial entry.
     *
     * @param <T> the type of the instances in the map.
     * @param api the API
     * @param t   the instance of the item for the given API
     * @return the map
     */
    public static <T> APIMap<T> of(API api, T t)  {
        APIMap<T> map = new APIMap<>();
        map.put(api, t);
        return map;
    }

    @Override
    public T put(API api, T t) {
        Objects.requireNonNull(api);
        Objects.requireNonNull(t);
        return super.put(api, t);
    }

    /**
     * Creates a new map by applying a function to each of the values in this map.
     * If the function returns {@code null} for an entry, no corresponding entry
     * is put in the new map.
     *
     * @param f   the function
     * @param <R> the type of entries in the new map
     *
     * @return the new map
     */
    public <R> APIMap<R> map(Function<T, R> f) {
        APIMap<R> result = APIMap.of();
        for (Map.Entry<API, ? extends T> e : entrySet()) {
            R r = f.apply(e.getValue());
            if (r != null) {
                result.put(e.getKey(), r);
            }
        }
        return result;
    }

    /**
     * Creates a new map by applying a bi-function to each of the entries in this map.
     * If the function returns {@code null} for an entry, no corresponding entry
     * is put in the new map.
     *
     * @param f   the function
     * @param <R> the type of entries in the new map
     *
     * @return the new map
     */
    public <R> APIMap<R> map(BiFunction<API, T, R> f) {
        APIMap<R> result = APIMap.of();
        for (Map.Entry<API, ? extends T> e : entrySet()) {
            R r = f.apply(e.getKey(), e.getValue());
            if (r != null) {
                result.put(e.getKey(), r);
            }
        }
        return result;
    }
}

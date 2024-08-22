/*
 * Copyright (c) 2018,2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.codetools.apidiff;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * A utility class to provide localized messages, based on resources in a resource bundle.
 */
public class Messages {
    static final Map<String, Messages> map = new HashMap<>();

    /**
     * Returns a singleton instance of a {@code Messages} object for a given
     * resource bundle name.
     *
     * @param name the name of the resource bundle
     * @return the instance
     */
    // TODO: Locale?
    public static Messages instance(String name) {
        synchronized (map) {
            return map.computeIfAbsent(name, n -> new Messages(name));
        }
    }

    /**
     * Gets an entry from the resource bundle.
     * If the resource cannot be found, a message is printed to the console
     * and the result will be a string containing the method parameters.
     * @param key the name of the entry to be returned
     * @param args an array of arguments to be formatted into the result using
     *      {@link java.text.MessageFormat#format}
     * @return the formatted string
     */
    public String getString(String key, Object... args) {
        try {
            return MessageFormat.format(bundle.getString(key), args);
        } catch (MissingResourceException e) {
            System.err.println("WARNING: missing resource: " + key + " for " + name);
            return key + Arrays.toString(args);
        }
    }

    /**
     * Returns the set of keys defined in the resource bundle.
     * @return the keys
     */
    public Set<String> getKeys() {
        return bundle.keySet();
    }

    /**
     * Creates a resource bundle for the given name.
     * @param name the name of the resource bundle
     */
    private Messages(String name) {
        this.name = name;
        bundle = ResourceBundle.getBundle(name);
    }

    private final String name;
    private final ResourceBundle bundle;
}


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

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.List;

/**
 * A filter to determine whether modules, packages and types within
 * an API are "selected", according to a series of "include" and "exclude"
 * options.
 *
 * <p>The following patterns are accepted:
 * <ul>
 *     <li>module/**            all types in a module
 *     <li>module/package.**    all types in a package and its subpackages
 *     <li>module/package.*     all types in a package
 *     <li>module/package.Type  a specific type
 *     <li>package.**           all types in a package and its subpackages
 *                              that are not in a named module
 *     <li>package.*            all types in a package is not in a named module
 *     <li>package.Type         a specific type in a package that is not in a named module
 * </ul>
 * where:
 * <ul>
 *     <li>"module" is a qualified identifier, optionally ending in '.*'
 *          to indicate all modules beginning with the given prefix,
 *     <li>"package" is a qualified identifier indicating a package of that name
 *     <li>"not in a named module" is case when using versions of the platform
 *          prior to the introduction of the module system, or in the unnamed module
 *          thereafter.
 * </ul>
 */
public class Selector {
    static class Entry {
        final String pattern;

        final String modulePart;
        final String packagePart;
        final String typePart;

        final Predicate<String> includeModule;
        final Predicate<String> includePackage;
        final Predicate<String> includeType;

        final Predicate<String> excludeModule;
        final Predicate<String> excludePackage;
        final Predicate<String> excludeType;

        static final Predicate<String> ALWAYS = s -> true;
        static final Predicate<String> NEVER = s -> false;

        Entry(String pattern) {
            this.pattern = pattern;
            int sep = pattern.indexOf("/");
            String head, tail;
            if (sep == -1) {
                head = null;
                tail = pattern;
            } else {
                head = pattern.substring(0, sep);
                tail = pattern.substring(sep + 1);
            }

            if (head == null) {
                includeModule = s -> s == null || s.isEmpty();
            } else if (head.endsWith(".*")) {
                String mdlPrefix = head.substring(0, head.length() - 1);
                includeModule = s -> s != null && s.startsWith(mdlPrefix);
            } else {
                includeModule = s -> s != null && s.equals(head);
            }
            modulePart = head;

            if (tail.isEmpty() || tail.equals("**")) {
                includePackage = ALWAYS;
                includeType = ALWAYS;
                excludeModule = includeModule;
                excludePackage = ALWAYS;
                excludeType = ALWAYS;
                packagePart = "";
                typePart = "**";
            } else if (tail.equals("*")) {
                includePackage = String::isEmpty;
                includeType = s -> true;
                excludeModule = NEVER;
                excludePackage = includePackage;
                excludeType = ALWAYS;
                packagePart = "";
                typePart = "*";
            } else if (tail.endsWith(".*")) {
                String pkgName = tail.substring(0, tail.length() - 2);
                includePackage = s -> s.equals(pkgName);
                includeType = s -> true;
                excludeModule = NEVER;
                excludePackage = includePackage;
                excludeType = ALWAYS;
                packagePart = pkgName;
                typePart = "*";
            } else if (tail.endsWith(".**")) {
                String pkgName = tail.substring(0, tail.length() - 3);
                String pkgPrefix = tail.substring(0, tail.length() - 2);
                includePackage = s -> s.equals(pkgName) || s.startsWith(pkgPrefix);
                includeType = s -> true;
                excludeModule = NEVER;
                excludePackage = includePackage;
                excludeType = ALWAYS;
                packagePart = pkgName;
                typePart = "**";
            } else {
                int lastDot = tail.lastIndexOf(".");
                if (lastDot == -1) {
                    includePackage = String::isEmpty;
                    includeType = s -> s.equals(tail);
                    packagePart = "";
                    typePart = tail;
                } else {
                    String pkgName = tail.substring(0, lastDot);
                    String typeName = tail.substring(lastDot + 1);
                    includePackage = s -> s.equals(pkgName);
                    includeType = s -> s.equals(typeName);
                    packagePart = pkgName;
                    typePart = typeName;
                }

                excludeModule = NEVER;
                excludePackage = NEVER;
                excludeType = includeType;
            }
        }

        boolean includesModule(String moduleName) {
            return includeModule.test(moduleName);
        }

        boolean excludesModule(String moduleName) {
            return excludeModule.test(moduleName);
        }

        boolean includesPackage(String moduleName, String packageName) {
            return includeModule.test(moduleName) && includePackage.test(packageName);
        }

        boolean excludesPackage(String moduleName, String packageName) {
            return excludesModule(moduleName)
                    || includesModule(moduleName) && excludePackage.test(packageName);
        }

        boolean includesType(String moduleName, String packageName, String typeName) {
            return includesPackage(moduleName, packageName) && includeType.test(typeName);
        }

        boolean excludesType(String moduleName, String packageName, String typeName) {
            return excludesPackage(moduleName, packageName)
                    || includesPackage(moduleName, packageName) && excludeType.test(typeName);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[modulePart:" + modulePart
                    + ",packagePart:" + packagePart
                    + ",typePart:" + typePart
                    + "]";
        }
    }

    final List<Entry> includes;
    final List<Entry> excludes;

    /**
     * Creates a selector based upon a series of "includes" and "excludes" options.
     *
     * @param includes the list of patterns describing the elements to be included
     * @param excludes the list of patterns describing the elements to be excluded
     */
    public Selector(List<String> includes, List<String> excludes) {
        this.includes = includes.stream().map(Entry::new).collect(Collectors.toList());
        this.excludes = excludes.stream().map(Entry::new).collect(Collectors.toList());
    }

    /**
     * Returns whether a module is selected, according to the configured options.
     *
     * @param name the module name
     * @return {@code true} if the module is selected
     */
    public boolean acceptsModule(String name) {
        return (includes.isEmpty() || includes.stream().anyMatch(e -> e.includesModule(name)))
                && excludes.stream().noneMatch(e -> e.excludesModule(name));
    }

    /**
     * Returns whether a package is selected, according to the configured options.
     *
     * @param moduleName the name of the module enclosing the package
     * @param packageName the package name
     * @return {@code true} if the package is selected
     */
    public boolean acceptsPackage(String moduleName, String packageName) {
        return (includes.isEmpty() || includes.stream().anyMatch(e -> e.includesPackage(moduleName, packageName)))
                && excludes.stream().noneMatch(e -> e.excludesPackage(moduleName, packageName));
    }

    /**
     * Returns whether a top-level type is selected, according to the configured options.
     *
     * @param moduleName the name of the module enclosing the type
     * @param packageName the name of the package enclosing the type
     * @param typeName the simple name of the type
     * @return {@code true} if the type is selected
     */
    public boolean acceptsType(String moduleName, String packageName, String typeName) {
        return (includes.isEmpty() || includes.stream().anyMatch(e -> e.includesType(moduleName, packageName, typeName)))
                && excludes.stream().noneMatch(e -> e.excludesType(moduleName, packageName, typeName));
    }
}

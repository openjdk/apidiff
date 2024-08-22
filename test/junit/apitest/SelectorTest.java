/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package apitest;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jdk.codetools.apidiff.model.Selector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SelectorTest {

    public static Stream<Arguments> provideCases() {
        // The names here are just example strings; they are not interpreted as actual element names
        return Stream.of(
                Arguments.of(
                    // module, all packages and types in the module; no excludes
                    List.of("java.base/**"),
                    List.of(),
                    Map.of("java.base/java.lang", true,
                            "java.base/java.lang.String", true,
                            "java.se/java.io.IOException", false)
                ),
                Arguments.of(
                    // module, one package; no excludes
                    List.of("java.base/java.lang.*"),
                    List.of(),
                    Map.of("java.base/java.lang", true,
                            "java.base/java.lang.String", true,
                            "java.base/java.lang.reflect.Method", false,
                            "java.base/java.util.Map", false,
                            "java.se/java.io.IOException", false)
                ),
                Arguments.of(
                    // module, all packages and types in the module; subpackage excluded
                    List.of("java.base/**"),
                    List.of("java.base/java.lang.reflect.*"),
                    Map.of("java.base/java.lang.String", true,
                            "java.lang.String", false,
                            "java.base/java.lang.reflect.Method", false,
                            "java.se/java.io.IOException", false)
                ),
                Arguments.of(
                        List.of("java.lang.**"),
                        List.of("java.lang.reflect.*"),
                        Map.of("java.base/java.lang.String", false,
                                "java.lang.String", true,
                                "java.lang.reflect.Method", false,
                                "java.se.IOException", false)
                )
        );
    }

    // optional module, package, optional capitalized type
    Pattern pattern = Pattern.compile("((?<m>[A-Za-z0-9.]+)/)?(?<p>[a-z0-9.]+)(\\.(?<t>[A-Z][A-Za-z0-9.]*))?");

    @ParameterizedTest
    @MethodSource("provideCases")
    public void test(List<String> includes, List<String> excludes, Map<String, Boolean> cases) {
        System.out.println("includes: " + includes);
        System.out.println("excludes: " + excludes);
        Selector s = new Selector(includes, excludes);
        boolean ok = true;
        for (var e : cases.entrySet()) {
            String c = e.getKey();
            boolean expect = e.getValue();

            Matcher m = pattern.matcher(c);
            if (!m.matches()) {
                throw new IllegalArgumentException(c);
            }

            String mdl = m.group("m");
            String pkg = m.group("p");
            String typ = m.group("t");
            System.out.println("  m:" + mdl + " p:" + pkg + " t:" + typ);

            boolean found = typ == null
                    ? s.acceptsPackage(mdl, pkg)
                    : s.acceptsType(mdl, pkg, typ);
            if (found == expect) {
                System.out.println("  OK");
            } else {
                System.out.println("  expect: " + expect + ", found: " + found);
                ok = false;
            }
        }
        Assertions.assertTrue(ok);
    }
}

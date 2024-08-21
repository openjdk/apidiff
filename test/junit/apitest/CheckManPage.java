/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.codetools.apidiff.Options.Option;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CheckManPage {
    @Test
    public void checkManPage() throws Exception {
        Path base = findBaseDir();
        Path manPage = base.resolve("src/share/doc/apidiff.md".replace("/", File.separator));
        if (!Files.exists(manPage)) {
            throw new  Error("man page not found: " + manPage);
        }

        Set<String> documented = new TreeSet<>();
        Pattern p = Pattern.compile("<[a-z]+[^>]* id=\"option-([a-z0-9-]+)\"");
        for (String line : Files.readAllLines(manPage)) {
            Matcher m = p.matcher(line);
            while (m.find()) {
                documented.add(m.group(1));
            }
        }
        System.err.println("documented: " + documented);

        Set<String> declared = Stream.of(Option.values())
                .map(o -> o.getNames().get(0).replaceFirst("^-+", ""))
                .collect(Collectors.toCollection(TreeSet::new));
        declared.add("at"); // special case for @file
        System.err.println("declared: " + declared);

        if (!documented.equals(declared)) {
            Set<String> s1 = new TreeSet<>(declared);
            s1.removeAll(documented);
            if (!s1.isEmpty()) {
                System.err.println("declared but not documented:");
                s1.stream().forEach(s -> System.err.println("  " + s));
            }
            Set<String> s2 = new TreeSet<>(documented);
            s2.removeAll(declared);
            if (!s2.isEmpty()) {
                System.err.println("documented but not declared:");
                s2.stream().forEach(s -> System.err.println("  " + s));
            }
            Assertions.fail("discrepancies found");
        }
    }

    private Path findBaseDir() {
        Path d = Path.of(".").toAbsolutePath().normalize();
        while (d != null) {
            if (Files.isDirectory(d.resolve("src"))) {
                return d;
            }
            d = d.getParent();
        }
        throw new Error("can't find base directory");
    }
}

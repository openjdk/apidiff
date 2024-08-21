/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

/**
 * A standalone utility to get one or more system properties.
 * The command line arguments should either be {@code -all}
 * or a series of system property names.
 */
public class GetSystemProperty
{
    /**
     * The main program.
     * @param args a series of property names, or {@code -all}.
     */
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-all")) {
            try {
                System.getProperties().store(System.out, "system properties");
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
            }
        } else {
            for (String arg : args) {
                String v = System.getProperty(arg);
                System.out.println(arg + "=" + (v == null ? "" : v));
            }
        }
    }
}

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

import java.util.Set;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;

import jdk.codetools.apidiff.Abort;
import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for APIs.
 */
public class APIComparator {
    private final Set<API> apis;
    private final Options options;
    private final Options.Mode mode;
    private final Reporter reporter;
    private final Log log;

    /**
     * Creates an comparator to compare the specifications of the declared elements
     * in a series of APIs, using a reporter to generate a report of the results.
     * Depending on the mode of the comparison, the items to be compared will be
     * read from the overall module path or from the source and class path.
     *
     * @param apis the APIs
     * @param options the command-line options
     * @param reporter the reporter
     * @param log the log
     */
    public APIComparator(Set<API> apis, Options options, Reporter reporter, Log log) {
        this.apis = apis;
        this.options = options;
        this.mode = options.getMode();
        this.reporter = reporter;
        this.log = log;
    }

    /**
     * Compares the collection of APIs specified for this comparator.
     *
     * @return {@code true} if the APIs are equivalent according to the configured settings
     */
    public boolean compare() {
        boolean equal = switch (mode) {
            case MODULE ->  compareModules();
            case PACKAGE -> comparePackages();
        };
        if (log.errorCount() > 0) {
            throw new Abort();
        }
        reporter.completed(equal);
        return equal;
    }

    private boolean compareModules() {
        KeyTable<ModuleElement> allModules = new KeyTable<>();

        for (API api: apis) {
            for (ModuleElement me : api.getModuleElements()) {
                allModules.put(ElementKey.of(me), api, me);
            }
        }

        ModuleComparator mc = new ModuleComparator(apis, options, reporter);
        return mc.compareAll(allModules);
    }

    private boolean comparePackages() {
        KeyTable<PackageElement> allPackages = new KeyTable<>();

        for (API api: apis) {
            for (PackageElement pe : api.getPackageElements()) {
                allPackages.put(ElementKey.of(pe), api, pe);
            }
        }

        PackageComparator pc = new PackageComparator(apis, options, reporter);
        return pc.compareAll(allPackages);
    }
}

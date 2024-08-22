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

package jdk.codetools.apidiff.report;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.Options.VerboseKind;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.DocFile;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.Position;

/**
 * A reporter that reports messages to a log.
 */
public class LogReporter implements Reporter {
    private final Log log;
    private final Set<ElementKey.Kind> shouldReport;
    private final boolean shouldReportDifferences;
    private final boolean shouldReportMissing;

    /**
     * Creates a reporter that reports messages to a log.
     *
     * @param log the log
     * @param options the command-line options
     */
    public LogReporter(Log log, Options options) {
        this.log = log;

        shouldReport = EnumSet.noneOf(ElementKey.Kind.class);
        if (options.isVerbose(VerboseKind.MODULE)) {
            shouldReport.add(ElementKey.Kind.MODULE);
        }
        if (options.isVerbose(VerboseKind.PACKAGE)) {
            shouldReport.add(ElementKey.Kind.MODULE);
            shouldReport.add(ElementKey.Kind.PACKAGE);
        }
        if (options.isVerbose(VerboseKind.TYPE)) {
            shouldReport.add(ElementKey.Kind.MODULE);
            shouldReport.add(ElementKey.Kind.PACKAGE);
            shouldReport.add(ElementKey.Kind.TYPE);
        }
        shouldReportDifferences = options.isVerbose(VerboseKind.DIFFERENCES);
        shouldReportMissing = options.isVerbose(VerboseKind.MISSING);
    }

    private final Map<Position, APIMap<?>> apiMaps = new HashMap<>();

    @Override
    public void comparing(Position pos, APIMap<?> map) {
        apiMaps.put(pos, map);

        if (shouldReport(pos)) {
            log.report("logReport.comparing", pos);
        }
    }

    @Override
    public void completed(Position pos, boolean equal) {
        if (shouldReport(pos)) {
            log.report("logReport.completed", pos, asInt(equal));
        }

        if (pos.isElement()) {
            apiMaps.remove(pos);
        }
    }

    @Override
    public void completed(boolean equal) {
        log.report("logReport.finished", asInt(equal));
    }

    private static int asInt(boolean b) {
        return b ? 1 : 0;
    }

    private boolean shouldReport(Position pos) {
        return pos.isElement() && shouldReport.contains(pos.asElementKey().kind);
    }

    @Override
    public void reportMissing(Position pos, Set<API> apis) {
        if (shouldReportMissing) {
            for (API api : apis) {
                log.report("logReport.item-not-found", api.name, toString(pos));
            }
        }
    }

    @Override
    public void reportDifferentAnnotations(Position pos, APIMap<? extends AnnotationMirror> amMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-annotations", toString(pos));
        }
    }

    @Override
    public void reportDifferentAnnotationValues(Position pos, APIMap<? extends AnnotationValue> dMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-annotation-values", toString(pos));
        }
    }

    @Override
    public void reportDifferentDirectives(Position pos, APIMap<? extends Directive> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-directives", toString(pos));
        }
    }

    @Override
    public void reportDifferentKinds(Position pos, APIMap<? extends Element> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-kinds", toString(pos));
        }
    }

    @Override
    public void reportDifferentNames(Position pos, APIMap<? extends Element> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-names", toString(pos));
        }
    }

    @Override
    public void reportDifferentTypeParameters(Position pos, APIMap<? extends TypeParameterElement> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-type-parameters", toString(pos));
        }
    }

    @Override
    public void reportDifferentModifiers(Position pos, APIMap<? extends Element> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-modifiers", toString(pos));
        }
    }

    @Override
    public void reportDifferentTypes(Position pos, APIMap<? extends TypeMirror> tMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-types", toString(pos));
        }
    }

    @Override
    public void reportDifferentThrownTypes(Position pos, APIMap<List<? extends TypeMirror>> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-thrown-types", toString(pos));
        }
    }

    @Override
    public void reportDifferentSuperinterfaces(Position pos, APIMap<List<? extends TypeMirror>> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-superinterfaces", toString(pos));
        }
    }

    @Override
    public void reportDifferentPermittedSubclasses(Position pos, APIMap<List<? extends TypeMirror>> eMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-permitted-subclasses", toString(pos));
        }
    }

    @Override
    public void reportDifferentValues(Position pos, APIMap<?> vMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-values", toString(pos));
        }
    }

    @Override
    public void reportDifferentRawDocComments(Position pos, APIMap<String> cMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-raw-doc-comments", toString(pos));
        }
    }

    @Override
    public void reportDifferentApiDescriptions(Position pos, APIMap<String> dMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-api-descriptions", toString(pos));
        }
    }

    @Override
    public void reportDifferentDocFiles(Position pos, APIMap<DocFile> fMap) {
        if (shouldReportDifferences) {
            log.report("logReport.different-doc-files", toString(pos));
        }
    }

    String toString(Position pos) {
        StringBuilder sb = new StringBuilder();
        pos.accept(new SignatureVisitor(apiMaps), sb);
        return sb.toString();
    }
}

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

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.DocFile;
import jdk.codetools.apidiff.model.Position;

/**
 * A class to broadcast messages to a series of reporters.
 */
public class MultiplexReporter implements Reporter {
    private final List<Reporter> reporters;

    /**
     * Creates a reporter to broadcast messages to a series of reporters.
     *
     * @param reporters the reporters
     */
    public MultiplexReporter(List<Reporter> reporters) {
        this.reporters = reporters;
    }

    @Override
    public void comparing(Position pos, APIMap<?> apiMap) {
        reporters.forEach(r -> r.comparing(pos, apiMap));
    }

    @Override
    public void completed(boolean equal) {
        reporters.forEach(r -> r.completed(equal));
    }

    @Override
    public void completed(Position pos, boolean equal) {
        reporters.forEach(r -> r.completed(pos, equal));
    }

    @Override
    public void reportMissing(Position pos, Set<API> apis) {
        reporters.forEach(r -> r.reportMissing(pos, apis));
    }

    @Override
    public void reportDifferentAnnotations(Position pos, APIMap<? extends AnnotationMirror> amMap) {
        reporters.forEach(r -> r.reportDifferentAnnotations(pos, amMap));
    }

    @Override
    public void reportDifferentAnnotationValues(Position pos, APIMap<? extends AnnotationValue> avMap) {
        reporters.forEach(r -> r.reportDifferentAnnotationValues(pos, avMap));
    }

    @Override
    public void reportDifferentDirectives(Position pos, APIMap<? extends Directive> dMap) {
        reporters.forEach(r -> r.reportDifferentDirectives(pos, dMap));
    }

    @Override
    public void reportDifferentKinds(Position pos, APIMap<? extends Element> eMap) {
        reporters.forEach(r -> r.reportDifferentKinds(pos, eMap));
    }

    @Override
    public void reportDifferentNames(Position pos, APIMap<? extends Element> eMap) {
        reporters.forEach(r -> r.reportDifferentNames(pos, eMap));
    }

    @Override
    public void reportDifferentTypeParameters(Position pos, APIMap<? extends TypeParameterElement> eMap) {
        reporters.forEach(r -> r.reportDifferentTypeParameters(pos, eMap));
    }

    @Override
    public void reportDifferentModifiers(Position pos, APIMap<? extends Element> eMap) {
        reporters.forEach(r -> r.reportDifferentModifiers(pos, eMap));
    }

    @Override
    public void reportDifferentTypes(Position pos, APIMap<? extends TypeMirror> tMap) {
        reporters.forEach(r -> r.reportDifferentTypes(pos, tMap));
    }

    @Override
    public void reportDifferentThrownTypes(Position pos, APIMap<List<? extends TypeMirror>> tMap) {
        reporters.forEach(r -> r.reportDifferentThrownTypes(pos, tMap));
    }

    @Override
    public void reportDifferentSuperinterfaces(Position pos, APIMap<List<? extends TypeMirror>> tMap) {
        reporters.forEach(r -> r.reportDifferentSuperinterfaces(pos, tMap));
    }

    @Override
    public void reportDifferentPermittedSubclasses(Position pos, APIMap<List<? extends TypeMirror>> tMap) {
        reporters.forEach(r -> r.reportDifferentPermittedSubclasses(pos, tMap));
    }

    @Override
    public void reportDifferentValues(Position pos, APIMap<?> vMap) {
        reporters.forEach(r -> r.reportDifferentValues(pos, vMap));
    }

    @Override
    public void reportDifferentRawDocComments(Position pos, APIMap<String> cMap) {
        reporters.forEach(r -> r.reportDifferentRawDocComments(pos, cMap));
    }

    @Override
    public void reportDifferentApiDescriptions(Position pos, APIMap<String> cMap) {
        reporters.forEach(r -> r.reportDifferentApiDescriptions(pos, cMap));
    }

    @Override
    public void reportDifferentDocFiles(Position pos, APIMap<DocFile> fMap) {
        reporters.forEach(r -> r.reportDifferentDocFiles(pos, fMap));
    }
}

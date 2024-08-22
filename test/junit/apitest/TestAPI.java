/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.SerializedForm;

/**
 * A dummy API for use in testing. It has a name. All methods return null.
 * Create subclasses to provide additional, specific properties.
 */
public class TestAPI extends API {
    TestAPI(String name) {
        super(new Options.APIOptions(name), null, null, null);
    }

    @Override
    public Set<PackageElement> getPackageElements() {
        return null;
    }

    @Override
    public Set<ModuleElement> getModuleElements() {
        return null;
    }

    @Override
    public Set<PackageElement> getPackageElements(ModuleElement m) {
        return null;
    }

    @Override
    public Set<PackageElement> getExportedPackageElements(ModuleElement m) {
        return null;
    }

    @Override
    public Set<TypeElement> getTypeElements(PackageElement p) {
        return null;
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getAnnotationValuesWithDefaults(AnnotationMirror am) {
        return null;
    }

    @Override
    public SerializedForm getSerializedForm(TypeElement e) {
        return null;
    }

    @Override
    public DocCommentTree getDocComment(Element e) {
        return null;
    }

    @Override
    public DocCommentTree getDocComment(JavaFileObject fo) {
        return null;
    }

    @Override
    public String getApiDescription(Element e) {
        return null;
    }

    @Override
    public String getApiDescription(JavaFileObject fo) {
        return null;
    }

    @Override
    public byte[] getAllBytes(JavaFileObject fo) {
        return null;
    }

    @Override
    public List<JavaFileObject> listFiles(LocationKind kind, Element e, String subdirectory, Set<JavaFileObject.Kind> kinds, boolean recurse) {
        return null;
    }

    @Override
    public Elements getElements() {
        return null;
    }

    @Override
    public Types getTypes() {
        return null;
    }

    @Override
    public DocTrees getTrees() {
        return null;
    }
}

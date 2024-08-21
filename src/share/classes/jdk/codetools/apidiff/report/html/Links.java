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

package jdk.codetools.apidiff.report.html;

import java.util.Locale;
import java.util.stream.Collectors;

import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeParameterElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.model.TypeMirrorKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.ArrayTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.DeclaredTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.PrimitiveTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.TypeVariableKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.WildcardTypeKey;

/**
 * Factory for links within the generated report.
 */
public class Links {
    private GetFileVisitor getFile = new GetFileVisitor();
    private final DocPath file;
    private final DocPath pathToRoot;

    Links(DocPath file) {
        this.file = file;
        pathToRoot = file.parent().invert();
    }

    DocPath getPath(String path) {
        return pathToRoot.resolve(path);
    }

    DocPath getPath(DocPath path) {
        return pathToRoot.resolve(path);
    }

    HtmlTree createLink(ElementKey key) {
        return createLink(key, getName(key));
    }

    HtmlTree createLink(ElementKey key, CharSequence name) {
        DocPath keyPath = getFile.getFile(key);
        String id = idVisitor.getId(key);
        DocLink keyLink = new DocLink(pathToRoot.resolve(keyPath), null, id);

        return HtmlTree.A(keyLink.toString(), Text.of(name));
    }

    private CharSequence getName(ElementKey eKey) {
        return switch (eKey.kind) {
            case MODULE -> ((ModuleElementKey) eKey).name;
            case PACKAGE -> ((PackageElementKey) eKey).name;
            case TYPE -> ((TypeElementKey) eKey).name;
            default -> throw new IllegalArgumentException(eKey.toString());
        };
    }

    String getId(Position pos) {
        if (pos.isElement()) {
            return getId(pos.asElementKey());
        } else if (pos.isRelative()) {
            RelativePosition<?> rPos = (RelativePosition<?>) pos;
            switch (rPos.kind) {
                case SERIALIZED_FIELD:
                    return "serial-field-" + rPos.index;
                case SERIALIZATION_METHOD:
                    return "serial-method-" + rPos.index;
            }
        }
        throw new IllegalArgumentException(pos.toString());
    }

    String getId(ElementKey eKey) {
        return idVisitor.getId(eKey);
    }

    private final IdVisitor idVisitor = new IdVisitor();

    private class IdVisitor
            implements ElementKey.Visitor<CharSequence, Void>,
            TypeMirrorKey.Visitor<CharSequence, Void> {

        String getId(ElementKey eKey) {
            CharSequence cs = eKey.accept(this, null);
            return (cs == null) ? null : cs.toString();
        }

        @Override
        public CharSequence visitModuleElement(ModuleElementKey mKey, Void aVoid) {
            return null;
        }

        @Override
        public CharSequence visitPackageElement(PackageElementKey pKey, Void aVoid) {
            return null;
        }

        @Override
        public CharSequence visitTypeElement(TypeElementKey tKey, Void aVoid) {
            return null;
        }

        @Override
        public CharSequence visitExecutableElement(ExecutableElementKey k, Void aVoid) {
            return k.name + k.params.stream()
                    .map(this::toString)
                    .collect(Collectors.joining(",", "(", ")"));
        }

        @Override
        public CharSequence visitVariableElement(VariableElementKey k, Void aVoid) {
            return k.name;
        }

        @Override
        public CharSequence visitTypeParameterElement(TypeParameterElementKey k, Void aVoid) {
            throw new UnsupportedOperationException();
        }

        String toString(TypeMirrorKey eKey) {
            return eKey.accept(this, null).toString();
        }

        @Override
        public CharSequence visitArrayType(ArrayTypeKey k, Void aVoid) {
            return toString(k.componentKey) + "[]";
        }

        @Override
        public CharSequence visitDeclaredType(DeclaredTypeKey k, Void aVoid) {
            ElementKey eKey = k.elementKey;
            return switch (eKey.kind) {
                case TYPE -> ((TypeElementKey) eKey).name;
                case TYPE_PARAMETER -> ((TypeParameterElementKey) eKey).name;
                default -> throw new UnsupportedOperationException();
            };
        }

        @Override
        public CharSequence visitPrimitiveType(PrimitiveTypeKey k, Void aVoid) {
            return k.kind.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public CharSequence visitTypeVariable(TypeVariableKey k, Void aVoid) {
            return k.name;
        }

        @Override
        public CharSequence visitWildcardType(WildcardTypeKey k, Void aVoid) {
            throw new UnsupportedOperationException();
        }

    }
}

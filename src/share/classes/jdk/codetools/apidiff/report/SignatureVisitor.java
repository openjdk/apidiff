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

package jdk.codetools.apidiff.report;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.MemberElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeParameterElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.ElementPosition;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.model.TypeMirrorKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.ArrayTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.DeclaredTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.PrimitiveTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.TypeVariableKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.WildcardTypeKey;

/**
 * A utility class to obtain the signature for an element key.
 */
public class SignatureVisitor implements
        ElementKey.Visitor<StringBuilder, StringBuilder>,
        TypeMirrorKey.Visitor<StringBuilder, StringBuilder>,
        Position.Visitor<StringBuilder, StringBuilder> {

    private final Map<Position, APIMap<?>> apiMaps;

    /**
     * Creates an instance of a signature visitor.
     *
     * @param apiMaps the map containing the API elements to be used in the signature
     */
    public SignatureVisitor(Map<Position, APIMap<?>> apiMaps) {
        this.apiMaps = apiMaps;
    }

    /**
     * Returns the signature for the elements identified by an element key.
     *
     * @param key the key
     * @return the signature
     */
    public String getSignature(ElementKey key) {
        return getSignature(key, new StringBuilder()).toString();
    }

    /**
     * Returns the signature for the elements at a given position.
     *
     * @param pos the position
     * @return the signature
     */
    public String getSignature(Position pos) {
        return pos.accept(this, new StringBuilder()).toString();
    }

    private StringBuilder getSignature(ElementKey key, StringBuilder sb) {
        sb.append(getPrefix(key)).append(" ");
        key.accept(this, sb);
        return sb;
    }

    private String getPrefix(ElementKey key) {
        return switch (key.kind) {
            case MODULE ->
                "module";

            case PACKAGE ->
                "package";

            case TYPE -> {
                @SuppressWarnings("unchecked")
                APIMap<? extends Element> map = (APIMap<? extends Element>) apiMaps.get(Position.of(key));
                Set<ElementKind> eKinds = (map == null) ? Collections.emptySet()
                        : map.values().stream().map(Element::getKind).collect(Collectors.toSet());
                yield switch (eKinds.size()) {
                    case 0 -> "(unknown)";
                    case 1 -> getPrefix(eKinds.iterator().next());
                    default -> "(various)";
                };
            }

            case EXECUTABLE, VARIABLE ->
                getPrefix(((MemberElementKey) key).elementKind);

            case TYPE_PARAMETER ->
                "type parameter";
        };
    }

    private String getPrefix(ElementKind kind) {
        return switch (kind) {
            case ANNOTATION_TYPE -> "@interface";
            case CLASS -> "class";
            case CONSTRUCTOR -> "constructor";
            case ENUM -> "enum";
            case ENUM_CONSTANT -> "enum constant";
            case FIELD -> "field";
            case INTERFACE -> "interface";
            case METHOD -> "method";
            case MODULE -> "module";
            case PACKAGE -> "package";
            case PARAMETER -> "parameter";
            case RECORD -> "record";
            case RECORD_COMPONENT -> "record component";
            case TYPE_PARAMETER -> "type parameter";
            default -> throw new IllegalArgumentException(kind.toString());
        };

    }

    @Override
    public StringBuilder visitModuleElement(ModuleElementKey k, StringBuilder sb) {
        return sb.append(k.name);
    }

    @Override
    public StringBuilder visitPackageElement(PackageElementKey k, StringBuilder sb) {
        if (k.moduleKey != null) {
            k.moduleKey.accept(this, sb).append("/");
        }
        return sb.append(k.name);
    }

    @Override
    public StringBuilder visitTypeElement(TypeElementKey tek, StringBuilder sb) {
        return tek.enclosingKey.accept(this, sb).append(".").append(tek.name);
    }

    @Override
    public StringBuilder visitExecutableElement(ExecutableElementKey k, StringBuilder sb) {
        k.typeKey.accept(this, sb);
        sb.append("#");
        if (k.elementKind == ElementKind.CONSTRUCTOR) {
            TypeElementKey tek = (TypeElementKey) k.typeKey;
            sb.append(tek.name);
        } else {
            sb.append(k.name);
        }
        sb.append("(");
        boolean first = true;
        for (TypeMirrorKey tmk : k.params) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            tmk.accept(this, sb);
        }
        sb.append(")");
        return sb;
    }

    @Override
    public StringBuilder visitVariableElement(VariableElementKey k, StringBuilder sb) {
        return k.typeKey.accept(this, sb).append("#").append(k.name);
    }

    @Override
    public StringBuilder visitTypeParameterElement(TypeParameterElementKey k, StringBuilder sb) {
        return k.typeKey.accept(this, sb).append("<").append(k.name).append(">");
    }

    @Override
    public StringBuilder visitArrayType(ArrayTypeKey k, StringBuilder sb) {
        return k.componentKey.accept(this, sb).append("[]");
    }

    @Override
    public StringBuilder visitDeclaredType(DeclaredTypeKey k, StringBuilder sb) {
        k.elementKey.accept(this, sb);
        if (!k.typeArgKeys.isEmpty()) {
            sb.append("<");
            boolean first = true;
            for (TypeMirrorKey tmk : k.typeArgKeys) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                tmk.accept(this, sb);
            }
            sb.append(">");
        }
        return sb;
    }

    @Override
    public StringBuilder visitPrimitiveType(PrimitiveTypeKey k, StringBuilder sb) {
        return sb.append(toString(k.kind));
    }

    private String toString(TypeKind kind) {
        return switch (kind) {
            case BOOLEAN -> "boolean";
            case BYTE -> "byte";
            case CHAR -> "char";
            case DOUBLE -> "double";
            case FLOAT -> "float";
            case INT -> "int";
            case LONG -> "long";
            case SHORT -> "short";
            case VOID -> "void";
            default -> throw new IllegalStateException(kind.toString());
        };

    }

    @Override
    public StringBuilder visitTypeVariable(TypeVariableKey k, StringBuilder sb) {
        return sb.append(k.name);
    }

    @Override
    public StringBuilder visitWildcardType(WildcardTypeKey k, StringBuilder sb) {
        sb.append("?");
        if (k.extendsBoundKey != null) {
            sb.append(" extends ");
            k.extendsBoundKey.accept(this, sb);
        }
        if (k.superBoundKey != null) {
            sb.append(" super ");
            k.superBoundKey.accept(this, sb);
        }
        return sb;
    }

    @Override
    public StringBuilder visitElementPosition(ElementPosition kp, StringBuilder sb) {
        return sb.append(getSignature(kp.key));
    }

    @Override
    public StringBuilder visitRelativePosition(RelativePosition<?> ip, StringBuilder sb) {
        // TODO: improve for non-integer indexes, perhaps by resolving String.format in each branch
        String suffix = switch (ip.kind) {
            case ANNOTATION -> " @%s";
            case ANNOTATION_ARRAY_INDEX -> "[%d]";
            case ANNOTATION_VALUE -> ", value %s";
            case BOUND -> ", bound %d";
            case DEFAULT_VALUE -> ", default value";
            case DOC_FILE -> ", doc-file %s";
            case EXCEPTION -> ", throws %s";
            case MODULE_EXPORTS -> ", exports %s";
            case MODULE_REQUIRES -> ", requires %s";
            case MODULE_OPENS -> ", opens %s";
            case MODULE_PROVIDES -> ", provides %s";
            case MODULE_USES -> ", uses %s";
            case PARAMETER -> ", parameter %d";
            case PERMITTED_SUBCLASS -> ", permitted subclass %s";
            case RECEIVER_TYPE -> " receiver type";
            case RECORD_COMPONENT -> ", record component %d";
            case RETURN_TYPE -> " return type";
            case SERIAL_VERSION_UID -> ", serial version UID";
            case SERIALIZATION_METHOD -> ", serialization method %s";
            case SERIALIZATION_OVERVIEW -> ", serialization overview";
            case SERIALIZED_FIELD -> ", serialized field %s";
            case SERIALIZED_FORM -> " serialized form";
            case SUPERCLASS -> " superclass";
            case SUPERINTERFACE -> " superinterface %s";
            case TYPE_PARAMETER -> ", type parameter %d";
        };
        return ip.parent.accept(this, sb).append(String.format(suffix, ip.index));
    }
}

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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor14;

import jdk.codetools.apidiff.model.ElementKey.Cache;

/**
 * A wrapper for instances of type mirror that is independent of any API environment and
 * that can be used to associate corresponding type mirrors in different instances of an API.
 */
public abstract sealed class TypeMirrorKey implements Comparable<TypeMirrorKey> {
    static TypeVisitor<TypeMirrorKey,Void> factory = new SimpleTypeVisitor14<>() {

        @Override
        public TypeMirrorKey visitArray(ArrayType t, Void _p) {
            return new ArrayTypeKey(t);
        }

        @Override
        public TypeMirrorKey visitDeclared(DeclaredType t, Void _p) {
            return new DeclaredTypeKey(t);
        }

        @Override
        public TypeMirrorKey visitPrimitive(PrimitiveType t, Void _p) {
            return new PrimitiveTypeKey(t);
        }

        @Override
        public TypeMirrorKey visitTypeVariable(TypeVariable t, Void _p) {
            return new TypeVariableKey(t);
        }

        @Override
        public TypeMirrorKey visitWildcard(WildcardType t, Void _p) {
            return new WildcardTypeKey(t);
        }

        @Override
        public TypeMirrorKey defaultAction(TypeMirror e, Void _p) {
            throw new UnsupportedOperationException(e.getKind() + " " + e);
        }
    };

    private static final Cache<TypeMirror, TypeMirrorKey> cache = new Cache<>(t -> factory.visit(t, null));

    /**
     * Returns a key for a type mirror.
     * @param t the type mirror
     * @return the key
     */
    public static TypeMirrorKey of(TypeMirror t) {
        return (t == null) ? null : cache.get(t);
    }

    /**
     * The kind of the type mirror used to create this key.
     */
    public final TypeKind kind;

    TypeMirrorKey(TypeMirror t) {
        kind = t.getKind();
    }

    /**
     * Applies a visitor to this key.
     * @param v the visitor
     * @param p a visitor-specified parameter
     * @param <R> the type of the result
     * @param <P> the type of the parameter
     * @return a visitor-specified result
     */
    public abstract <R, P> R accept(Visitor<R,P> v, P p);

    /**
     * A visitor of type mirror keys, in the style of the visitor design pattern.
     * Classes implementing this interface are used to operate on a type mirror key
     * when the kind of key is unknown at compile time.
     * When a visitor is passed to a key's {@code accept} method,
     * the <code>visit<em>Xyz</em></code> method applicable to that key is invoked.
     *
     * @param <R> the return type of this visitor's methods.
     *              Use Void for visitors that do not need to return results.
     * @param <P> the type of the additional parameter to this visitor's methods.
     *              Use Void for visitors that do not need an additional parameter.
    */
    public interface Visitor<R,P> {
        /**
         * Visits a key for an array type.
         * @param k the key to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitArrayType(ArrayTypeKey k, P p);

        /**
         * Visits a key for a declared type.
         * @param k the key to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitDeclaredType(DeclaredTypeKey k, P p);

        /**
         * Visits a key for a primitive type.
         * @param k the key to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitPrimitiveType(PrimitiveTypeKey k, P p);

        /**
         * Visits a key for a type variable.
         * @param k the key to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitTypeVariable(TypeVariableKey k, P p);

        /**
         * Visits a key for a wildcard type.
         * @param k the key to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitWildcardType(WildcardTypeKey k, P p);
    }

    /**
     * A key for an array type.
     */
    public static final class ArrayTypeKey extends TypeMirrorKey {
        /**
         * A key for the component type of the array.
         */
        public final TypeMirrorKey componentKey;
        private int hashCode;

        ArrayTypeKey(ArrayType t) {
            super(t);
            componentKey = Objects.requireNonNull(TypeMirrorKey.of(t.getComponentType()));
        }

        @Override
        public int compareTo(TypeMirrorKey other) {
            int ck = kind.compareTo(other.kind);
            return (ck != 0) ? ck : componentKey.compareTo(((ArrayTypeKey) other).componentKey);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                return componentKey.equals(((ArrayTypeKey) other).componentKey);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = componentKey.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "ArrayKey[" + componentKey + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R, P> v, P p) {
            return v.visitArrayType(this, p);
        }
    }

    /**
     * A key for a declared type.
     */
    public static final class DeclaredTypeKey extends TypeMirrorKey {
        /**
         * The key for the element corresponding to this type.
         */
        public final ElementKey elementKey;
        /**
         * The keys for any type arguments.
         */
        public final List<TypeMirrorKey> typeArgKeys;
        private int hashCode;

        DeclaredTypeKey(DeclaredType t) {
            super(t);
            elementKey = ElementKey.of(t.asElement());
            typeArgKeys = t.getTypeArguments().stream()
                    .map(TypeMirrorKey::of)
                    .collect(Collectors.toList());
        }

        @Override
        public int compareTo(TypeMirrorKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }
            DeclaredTypeKey otherKey = (DeclaredTypeKey) other;
            int ce = elementKey.compareTo(otherKey.elementKey);
            if (ce != 0) {
                return ce;
            }
            return ElementKey.compare(typeArgKeys, otherKey.typeArgKeys);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                DeclaredTypeKey otherKey = (DeclaredTypeKey) other;
                return elementKey.equals(otherKey.elementKey) && typeArgKeys.equals(otherKey.typeArgKeys);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = kind.hashCode() * 37 + elementKey.hashCode() * 5 + typeArgKeys.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            String typeArgs = typeArgKeys.isEmpty() ? ""
                    : typeArgKeys.stream().map(Object::toString).collect(Collectors.joining(",", "<", ">"));
            return "DeclaredTypeKey[" + elementKey + typeArgs + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R, P> v, P p) {
            return v.visitDeclaredType(this, p);
        }
    }

    /**
     * A key for a primitive type:
     * {@code boolean}, {@code byte}, {@code char}, {@code double},
     * {@code float}, {@code int}, {@code long}, {@code short}.
     *
     * The kind of primitive type is identified by the {@link TypeMirrorKey#kind kind}.
     */
    public static final class PrimitiveTypeKey extends TypeMirrorKey {
        PrimitiveTypeKey(PrimitiveType t) {
            super(t);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                return kind.equals(((PrimitiveTypeKey) other).kind);
            }
        }

        @Override
        public int hashCode() {
            return kind.hashCode();
        }

        @Override
        public int compareTo(TypeMirrorKey other) {
            // TODO: compare by group?
            return kind.compareTo(other.kind);
        }

        @Override
        public String toString() {
            return "PrimitiveTypeKey[" + kind + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R, P> v, P p) {
            return v.visitPrimitiveType(this, p);
        }
    }

    /**
     * A key for a type variable.
     */
    public static final class TypeVariableKey extends TypeMirrorKey {
        /**
         * The name of the type variable.
         */
        public final Name name; // TODO: should this be the ElementKey?
        private int hashCode;

        TypeVariableKey(TypeVariable t) {
            super(t);
            this.name = t.asElement().getSimpleName();
        }

        @Override
        public int compareTo(TypeMirrorKey other) {
            int ck = kind.compareTo(other.kind);
            return (ck != 0) ? ck : ElementKey.compare(name, ((TypeVariableKey) other).name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                return name.equals(((TypeVariableKey) other).name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = ElementKey.hashCode(name);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "TypeVariableTypeMirrorKey[" + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
        }
    }

    /**
     * A key for a wildcard type.
     */
    public static final class WildcardTypeKey extends TypeMirrorKey {
        /**
         * A key for the {@code extends} bound, if any, or null.
         */
        public final TypeMirrorKey extendsBoundKey;
        /**
         * A key for the {@code super} bound, if any, or null.
         */
        public final TypeMirrorKey superBoundKey;
        private int hashCode;

        WildcardTypeKey(WildcardType t) {
            super(t);
            extendsBoundKey = TypeMirrorKey.of(t.getExtendsBound());
            superBoundKey = TypeMirrorKey.of(t.getSuperBound());
        }

        @Override
        public int compareTo(TypeMirrorKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }
            WildcardTypeKey otherWildcardTypeKey = (WildcardTypeKey) other;
            int ce = compare(extendsBoundKey, otherWildcardTypeKey.extendsBoundKey);
            if (ce != 0) {
                return ce;
            }
            return compare(superBoundKey, otherWildcardTypeKey.superBoundKey);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                WildcardTypeKey otherWildcardTypeKey = (WildcardTypeKey) other;
                return Objects.equals(extendsBoundKey, otherWildcardTypeKey.extendsBoundKey)
                        && Objects.equals(superBoundKey, otherWildcardTypeKey.superBoundKey);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                if (extendsBoundKey != null) {
                    hashCode = extendsBoundKey.hashCode();
                }
                if (superBoundKey != null) {
                    hashCode = hashCode * 37 + superBoundKey.hashCode();
                }
                return hashCode;
            }
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("WildcardTypeKey[");
            String sep = "";
            if (extendsBoundKey != null) {
                sb.append("extends:").append(extendsBoundKey);
                sep = ",";
            }
            if (superBoundKey != null) {
                sb.append(sep).append("super:").append(superBoundKey);
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public <R, P> R accept(Visitor<R, P> v, P p) {
            return v.visitWildcardType(this, p);
        }

        private static int compare(TypeMirrorKey k1, TypeMirrorKey k2) {
            if (k1 == null && k2 == null) {
                return 0;
            }
            if (k1 == null) {
                return -1;
            }
            if (k2 == null) {
                return 1;
            }
            return k1.compareTo(k2);
        }
    }
}

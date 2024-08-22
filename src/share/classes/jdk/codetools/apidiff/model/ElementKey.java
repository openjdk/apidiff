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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleElementVisitor14;

/**
 * A lightweight wrapper for an element that is independent of any API environment and
 * that can be used to associate corresponding elements in different instances of an API.
 * It can also be used to check for nominal equality of reference types in a type mirror.
 *
 * <p>Values are cached in a memory-sensitive cache.
 *
 * @see KeyTable
 */
public abstract sealed class ElementKey implements Comparable<ElementKey> {
    /**
     * The {@code kind} of an element key.
     */
    public enum Kind {
        /** A module. */
        MODULE,
        /** A package. */
        PACKAGE,
        /** A class or interface. */
        TYPE,
        /** An executable element. */
        EXECUTABLE,
        /** A variable (field) element. */
        VARIABLE,
        /** A type parameter element. */
        TYPE_PARAMETER
    }

    private static final ElementVisitor<ElementKey,Void> factory = new SimpleElementVisitor14<>() {
        @Override
        public ElementKey visitModule(ModuleElement e, Void _p) {
            return new ModuleElementKey(e);
        }
        @Override
        public ElementKey visitPackage(PackageElement e, Void _p) {
            return new PackageElementKey(e);
        }
        @Override
        public ElementKey visitType(TypeElement e, Void _p) {
            return new TypeElementKey(e);
        }
        @Override
        public ElementKey visitExecutable(ExecutableElement e, Void _p) {
            return new ExecutableElementKey(e);
        }
        @Override
        public ElementKey visitVariable(VariableElement e, Void _p) {
            return new VariableElementKey(e);
        }
        @Override
        public ElementKey visitTypeParameter(TypeParameterElement e, Void _p) {
            return new TypeParameterElementKey(e);
        }
        @Override
        public ElementKey visitRecordComponent(RecordComponentElement e, Void _p) {
            return new VariableElementKey((VariableElement) e);
        }
        @Override
        public ElementKey defaultAction(Element e, Void _p) {
            throw new UnsupportedOperationException(e.getKind() + " " + e);
        }
    };

    // TODO: handle javax.lang.model.element.UnknownElementException; maybe handle visitUnknown in the factory visitor;
    //       the error may arise for unknown annotation types
    private static final Cache<Element,ElementKey> cache = new Cache<>(e -> factory.visit(e, null));

    /**
     * Returns the key for an element.
     *
     * @param e the element
     *
     * @return they key
     */
    public static ElementKey of(Element e) {
        return cache.get(e);
    }

    public final Kind kind;

    /**
     * Creates a key with a given kind.
     *
     * @param kind the kind.
     */
    protected ElementKey(Kind kind) {
        this.kind = kind;
    }

    /**
     * Returns the enclosing element key, or {@code null} if none.
     *
     * @return the enclosing element key
     */
    public abstract ElementKey getEnclosingKey();

    /**
     * Applies a visitor to this key.
     *
     * @param v   the visitor
     * @param p   a visitor-specified parameter
     * @param <R> the type of the result
     * @param <P> the type of the parameter
     *
     * @return a visitor-specified result
     */
    public abstract <R, P> R accept(Visitor<R,P> v, P p);

    /**
     * Checks whether this key is of a given kind.
     *
     * @param kind the kind
     *
     * @return {@code true} if and only if the key is of the given kind
     */
    public boolean is(Kind kind) {
        return kind == this.kind;
    }

    /**
     * Checks whether this key is of a given kind.
     *
     * @param kind the kind
     *
     * @return {@code true} if and only if the key is of the given kind
     */
    public abstract boolean is(ElementKind kind);

    /**
     * Compares two names.
     *
     * @param n1 the first name
     * @param n2 the second name
     *
     * @return the result of the comparison
     */
    protected static int compare(Name n1, Name n2) {
        // For now, just do a simple lexicographic compare.
        // We may want to upgrade this to do a case-ignore comparison first,
        // and only do a case-significant comparison if the case-ignore reports equal.
        // This would be to get the following example sort order:
        //   double, Double, float, Float, int, Integer, etc
        return CharSequence.compare(n1, n2);
    }

    /**
     * Compares two lists of items.
     *
     * @param <T> the type of items in the list
     * @param l1  the first list
     * @param l2  the second list
     *
     * @return the result of the comparison
     */
    protected static <T extends Comparable<T>> int compare(List<T> l1, List<T> l2) {
        if (l1.isEmpty() && l2.isEmpty()) {
            return 0;
        }
        Iterator<T> iter1 = l1.iterator();
        Iterator<T> iter2 = l2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            int i = iter1.next().compareTo(iter2.next());
            if (i != 0) {
                return i;
            }
        }
        return iter1.hasNext() ? +1 : iter2.hasNext() ? -1 : 0;
    }

    /**
     * Compares two names for equality.
     *
     * @param n1 the first name
     * @param n2 the second name
     *
     * @return {@code true} if and only if the two names have the exact same contents
     */
    protected static boolean equals(Name n1, Name n2) {
        return n1.contentEquals(n2);
    }

    /**
     * Returns a non-zero hash code for a name.
     *
     * @param n the name
     *
     * @return the hash code
     */
    protected static int hashCode(Name n) {
        int hashCode = n.toString().hashCode();
        return (hashCode == 0) ? 1 : hashCode;
    }

    /**
     * A visitor of element keys, in the style of the visitor design pattern.
     * Classes implementing this interface are used to operate on an element key
     * when the kind of key is unknown at compile time.
     * When a visitor is passed to an element key's {@code accept} method,
     * the <code>visit<em>Xyz</em></code> method applicable to that key is invoked.
     *
     * @param <R> the return type of this visitor's methods.
     *            Use Void for visitors that do not need to return results.
     * @param <P> the type of the additional parameter to this visitor's methods.
     *            Use Void for visitors that do not need an additional parameter.
     */
    public interface Visitor<R,P> {
        /**
         * Visits a key for a module element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitModuleElement(ModuleElementKey k, P p);

        /**
         * Visits a key for a package element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitPackageElement(PackageElementKey k, P p);

        /**
         * Visits a key for a type element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitTypeElement(TypeElementKey k, P p);

        /**
         * Visits a key for an executable element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitExecutableElement(ExecutableElementKey k, P p);

        /**
         * Visits a key for a variable element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitVariableElement(VariableElementKey k, P p);

        /**
         * Visits a key for a type parameter element.
         *
         * @param k the key to visit
         * @param p a visitor-specified parameter
         *
         * @return a visitor-specified result
         */
        R visitTypeParameterElement(TypeParameterElementKey k, P p);
    }

    /**
     * A memory-sensitive cache of values generated by a factory.
     * The cache is a {@link WeakHashMap} of {@link SoftReference} values.
     *
     * @param <K> the type of keys for the cache
     * @param <V> the type of values stored in the cache
     */
    static class Cache<K,V> {
        private final Function<K, V> factory;
        private final WeakHashMap<K, SoftReference<V>> map = new WeakHashMap<>();

        /**
         * Creates a cache for values generated by a factory.
         *
         * @param factory the factory for values to be cached
         */
        Cache(Function<K,V> factory) {
            this.factory = factory;
        }

        /**
         * Gets the value for a key, creating it if it does not already exist.
         * Because values are stored in the cache with soft references, there
         * is no guarantee that the same value will be returned for the same key.
         *
         * @param k the key
         *
         * @return the value
         */
        synchronized V get(K k) {
            SoftReference<V> vr = map.get(k);
            V v = (vr == null) ? null : vr.get();
            if (v == null) {
                v = factory.apply(k);
                map.put(k, new SoftReference<>(v));
            }
            return v;
        }
    }

    /**
     * An element key for a module element.
     */
    public static final class ModuleElementKey extends ElementKey {

        public final Name name;
        private int hashCode;

        ModuleElementKey(ModuleElement me) {
            super(Kind.MODULE);
            name = me.getQualifiedName();
        }

        @Override
        public ElementKey getEnclosingKey() {
            return null;
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            return (ck != 0) ? ck : compare(name, ((ModuleElementKey) other).name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                return equals(name, ((ModuleElementKey) other).name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = hashCode(name);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "ModuleKey[" + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitModuleElement(this, p);
        }

        @Override
        public boolean is(ElementKind kind) {
            return (kind == ElementKind.MODULE);
        }

    }

    /**
     * An element key for a package element.
     */
    public static final class PackageElementKey extends ElementKey {
        public final ElementKey moduleKey;
        public final Name name;
        private int hashCode;

        PackageElementKey(PackageElement pe) {
            super(Kind.PACKAGE);
            ModuleElement me = (ModuleElement) pe.getEnclosingElement();
            moduleKey = (me == null || me.isUnnamed()) ? null : ElementKey.of(me);
            name = pe.getQualifiedName();
        }

        @Override
        public ElementKey getEnclosingKey() {
            return moduleKey;
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }
            PackageElementKey otherPackage = (PackageElementKey) other;
            int cm;
            if (moduleKey == null) {
                cm = otherPackage.moduleKey == null ? 0 : -1;
            } else if (otherPackage.moduleKey == null) {
                cm = 1;
            } else {
                cm = moduleKey.compareTo(otherPackage.moduleKey);
            }
            return (cm != 0) ? cm : compare(name, otherPackage.name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                PackageElementKey otherPackage = (PackageElementKey) other;
                return Objects.equals(moduleKey, otherPackage.moduleKey)
                        && equals(name, otherPackage.name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = (moduleKey == null ? 0 : moduleKey.hashCode() * 37) + hashCode(name);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "PackageKey[" + (moduleKey == null ? "" : moduleKey + ",") + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitPackageElement(this, p);
        }

        @Override
        public boolean is(ElementKind kind) {
            return (kind == ElementKind.PACKAGE);
        }

    }

    /**
     * An element key for a type element.
     */
    public static final class TypeElementKey extends ElementKey {
        public final ElementKey enclosingKey; // A type can be enclosed in a package or another type
        public final Name name;
        private int hashCode;

        TypeElementKey(TypeElement te) {
            super(Kind.TYPE);
            enclosingKey = ElementKey.of(te.getEnclosingElement());
            name = te.getSimpleName();
        }

        @Override
        public ElementKey getEnclosingKey() {
            return enclosingKey;
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }
            TypeElementKey otherType = (TypeElementKey) other;
            int ce = enclosingKey.compareTo(otherType.enclosingKey);
            return (ce != 0) ? ce : compare(name, otherType.name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                TypeElementKey otherType = (TypeElementKey) other;
                return enclosingKey.equals(otherType.enclosingKey)
                        && equals(name, otherType.name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = enclosingKey.hashCode() * 37 + hashCode(name);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "TypeKey[" + enclosingKey + "," + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitTypeElement(this, p);
        }

        @Override
        public boolean is(Kind kind) {
            return (kind == Kind.TYPE);
        }

        /**
         * {@inheritDoc}
         * By design, {@code TypeElementKey} does not include details about the element kind,
         * and so always throws {@code UnsupportedOperationException} for any element kind
         * that is a type.
         *
         * @param kind the kind
         *
         * @return {@code false} for all element kinds that are not the kind of a type
         * @throws UnsupportedOperationException if the element kind is the kind of a type
         */
        @Override
        public boolean is(ElementKind kind) {
            return switch (kind) {
                case ANNOTATION_TYPE, CLASS, ENUM, INTERFACE -> throw new UnsupportedOperationException();
                default -> false;
            };
        }

    }

    public static abstract sealed class MemberElementKey extends ElementKey {
        public final ElementKey typeKey;
        public final ElementKind elementKind;
        public final Name name;

        protected MemberElementKey(Kind kind, Element e) {
            super(kind);
            this.typeKey = ElementKey.of(e.getEnclosingElement());
            this.elementKind = e.getKind();
            this.name = e.getSimpleName();
        }

        @Override
        public ElementKey getEnclosingKey() {
            return typeKey;
        }

        @Override
        public boolean is(ElementKind kind) {
            return kind == elementKind;
        }

    }

    /**
     * An element key for an executable element.
     */
    public static final class ExecutableElementKey extends MemberElementKey {

        public final List<TypeMirrorKey> params;
        private int hashCode;

        ExecutableElementKey(ExecutableElement ee) {
            super(Kind.EXECUTABLE, ee);
            params = ee.getParameters().stream()
                    .map(e -> TypeMirrorKey.of(e.asType()))
                    .collect(Collectors.toList());
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }

            ExecutableElementKey otherExecutable = (ExecutableElementKey) other;

            int ct = typeKey.compareTo(otherExecutable.typeKey);
            if (ct != 0) {
                return ct;
            }

            int cek = elementKind.compareTo(otherExecutable.elementKind);
            if (cek != 0) {
                return cek;
            }

            int cn = CharSequence.compare(name, otherExecutable.name);
            if (cn != 0) {
                return cn;
            }

            return compare(params, otherExecutable.params);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                ExecutableElementKey otherExecutable = (ExecutableElementKey) other;
                return kind == otherExecutable.kind
                        && elementKind == otherExecutable.elementKind
                        && name.contentEquals(otherExecutable.name)
                        && params.equals(otherExecutable.params);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = kind.ordinal() * 37 + typeKey.hashCode();
                hashCode = hashCode * 37 + elementKind.hashCode();
                hashCode = hashCode * 37 + hashCode(name);
                hashCode = hashCode * 37 + params.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            String p = params.stream().map(Object::toString).collect(Collectors.joining(","));
            return "ExecutableKey[" + elementKind + ":" + name + "(" + p + ")]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitExecutableElement(this, p);
        }
    }


    /**
     * An element key for a variable element.
     */
    public static final class VariableElementKey extends MemberElementKey {

        private int hashCode;

        VariableElementKey(VariableElement ve) {
            super(Kind.VARIABLE, ve);
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }

            VariableElementKey otherVariable = (VariableElementKey) other;

            int ct = typeKey.compareTo(otherVariable.typeKey);
            if (ct != 0) {
                return ct;
            }

            int cek = elementKind.compareTo(otherVariable.elementKind);
            if (cek != 0) {
                return cek;
            }

            return CharSequence.compare(name, otherVariable.name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                VariableElementKey otherVariable = (VariableElementKey) other;
                return kind == otherVariable.kind
                        && elementKind == otherVariable.elementKind
                        && name.contentEquals(otherVariable.name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = typeKey.hashCode() * 37 + hashCode(name);
                hashCode = hashCode * 37 + elementKind.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "VariableKey[" + elementKind + ":" + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitVariableElement(this, p);
        }
    }

    /**
     * An element key for a type parameter element.
     */
    public static final class TypeParameterElementKey extends ElementKey {

        public final ElementKey typeKey;
        public final Name name;
        private int hashCode;

        TypeParameterElementKey(TypeParameterElement ve) {
            super(Kind.TYPE_PARAMETER);
            typeKey = ElementKey.of(ve.getEnclosingElement());
            name = ve.getSimpleName();
        }

        @Override
        public ElementKey getEnclosingKey() {
            return typeKey;
        }

        @Override
        public int compareTo(ElementKey other) {
            int ck = kind.compareTo(other.kind);
            if (ck != 0) {
                return ck;
            }

            TypeParameterElementKey otherTypeParameter = (TypeParameterElementKey) other;

            int ct = typeKey.compareTo(otherTypeParameter.typeKey);
            if (ct != 0) {
                return ct;
            }

            return CharSequence.compare(name, otherTypeParameter.name);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                TypeParameterElementKey otherVariable = (TypeParameterElementKey) other;
                return kind == otherVariable.kind
                        && name.contentEquals(otherVariable.name);
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = typeKey.hashCode() * 37 + hashCode(name);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "TypeParameterElementKey[" + name + "]";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitTypeParameterElement(this, p);
        }

        @Override
        public boolean is(ElementKind kind) {
            return (kind == ElementKind.TYPE_PARAMETER);
        }
    }


}

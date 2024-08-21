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

import java.util.Comparator;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement.DirectiveKind;

/**
 * An abstract description of the position in an API of some item
 * to be compared.
 *
 * <p>Positions are initially created from element keys;
 * additional positions describing positions within an item
 * can be created by various factory methods. These methods
 * may throw {@code UnsupportedOperationException} if the
 * factory method is not applicable for the parent position.
 */
public abstract class Position {
    /**
     * Creates a position for a given element key, which must be
     * for a module, package, type, constructor, method, enum
     * constant or field.
     *
     * <p>Positions for parameters and type parameters should
     * be created using the appropriate factory method, providing
     * the necessary index value.
     *
     * @param k the key for the element
     * @return the position
     */
    public static Position of(ElementKey k) {
        return switch (k.kind) {
            case MODULE, PACKAGE, TYPE, VARIABLE, EXECUTABLE -> new ElementPosition(k);
            default -> throw new IllegalArgumentException(k.toString());
        };
    }

    /**
     * Returns whether this position is the position of an element.
     *
     * @return {@code true} if and ony if the position is the position of an element
     */
    public boolean isElement() {
        return false;
    }

    /**
     * Returns whether this position represents the given kind of element.
     *
     * @param kind the kind
     *
     * @return {@code true} if and only if the position represents the given kind of element
     */
    public boolean is(ElementKind kind) {
        return false;
    }

    /**
     * Returns whether this position is a relative position.
     *
     * @return {@code true} if and only if the position is a relative position
     */
    public boolean isRelative() {
        return false;
    }

    /**
     * Returns whether this position represents the given kind of relative position.
     *
     * @param kind the kind
     *
     * @return {@code true} if and only if the position represents the given kind of relative position
     */
    public boolean is(RelativePosition.Kind kind) {
        return false;
    }

    /**
     * Creates a position for an annotation on an element,
     * identified by the annotation type.
     *
     * <p>Repeated annotations are assumed to be enclosed within
     * a container annotation.
     *
     * @param key the key of the annotation type
     *
     * @return the position
     */
    public abstract RelativePosition<ElementKey> annotation(ElementKey key);

    /**
     * Creates a position for a value within an array of annotation values.
     *
     * @param index the position within the array
     *
     * @return the position
     */
    public RelativePosition<Integer> annotationArrayIndex(int index) {
        throw unsupported();
    }

    /**
     * Creates a position for value within an annotation,
     * identified by the annotation type element.
     *
     * @param key the key of the annotation type member
     *
     * @return the position
     */
    public RelativePosition<ElementKey> annotationValue(ElementKey key) {
        throw unsupported();
    }

    /**
     * Creates a position for a bound of a type parameter element.
     *
     * @param index the index of the bound within the list of bounds
     *
     * @return the position
     */
    public RelativePosition<Integer> bound(int index) {
        throw unsupported();
    }

    /**
     * Creates a position for the default value of an annotation element.
     *
     * @return the position
     */
    public RelativePosition<Void> defaultValue() {
        throw unsupported();
    }

    /**
     * Creates a position for a directive within a module declaration,
     * based on the kind of the directive and the primary type described
     * by the directive. The kind of the primary type depends on the
     * kind of directive.
     *
     * @param kind the kind of directive
     * @param key  the key for the primary type of the directory
     *
     * @return the position
     */
    public RelativePosition<?> directive(DirectiveKind kind, ElementKey key) {
        throw unsupported();
    }

    /**
     * Creates a position for a doc file of a module or package, identified by
     * the name relative to the enclosing {@code doc-files} directory.
     * The name always uses {@code /} as the internal file separator
     * (and not the platform file separator.
     *
     * @param name the name
     *
     * @return the position
     */
    public RelativePosition<String> docFile(String name) {
        throw unsupported();
    }

    /**
     * Creates a position for an exception that is thrown from an executable element.
     *
     * @param key the key of the exception that is thrown
     *
     * @return the position
     */
    public RelativePosition<TypeMirrorKey> exception(TypeMirrorKey key) {
        throw unsupported();
    }

    /**
     * Creates a position for a parameter of an executable element.
     *
     * @param index the index of the parameter within the list of parameters
     *
     * @return the position
     */
    public RelativePosition<Integer> parameter(int index) {
        throw unsupported();
    }

    /**
     * Creates a position for a permitted subtype of a sealed class.
     *
     * @param key the element key for the subtype
     *
     * @return the position
     */
    public RelativePosition<ElementKey> permittedSubclass(ElementKey key) {
        throw unsupported();
    }

    /**
     * Creates a position for the receiver type of a method.
     *
     * @return the position
     */
    public RelativePosition<Void> receiverType() {
        throw unsupported();
    }

    /**
     * Creates a position for a record component of a record.
     *
     * @param index the index of the component within the list of components
     *
     * @return the position
     */
    public RelativePosition<Integer> recordComponent(int index) {
        throw unsupported();
    }

    /**
     * Creates a position for the return type of a method.
     *
     * @return the position
     */
    public RelativePosition<Void> returnType() {
        throw unsupported();
    }

    /**
     * Creates a position for the {@code serialVersionUID} of a type element or its serialized form.
     *
     * @return the position
     */
    public RelativePosition<Void> serialVersionUID() {
        throw unsupported();
    }

    /**
     * Creates a position for a serialization method of a type element or its serialized form.
     *
     * @param name the name of the method
     *
     * @return the position
     */
    public RelativePosition<String> serializationMethod(String name) {
        throw unsupported();
    }

    /**
     * Creates a position for a serialization overview of a type element or its serialized form.
     *
     * @return the position
     */
    public RelativePosition<Void> serializationOverview() {
        throw unsupported();
    }

    /**
     * Creates a position for a serialized field of a type element or its serialized form.
     *
     * @param name the name of the field
     *
     * @return the position
     */
    public RelativePosition<String> serializedField(String name) {
        throw unsupported();
    }

    /**
     * Creates a position for the serialized form of a type element.
     *
     * @return the position
     */
    public RelativePosition<Void> serializedForm() {
        throw unsupported();
    }

    /**
     * Creates a position for the superclass of a class type.
     *
     * @return the position
     */
    public RelativePosition<Void> superclass() {
        throw unsupported();
    }

    /**
     * Creates a position for a superinterface of a class or interface type.
     *
     * @param eKey the index of the parameter within the list of parameters
     * @return the position
     */
    public RelativePosition<ElementKey> superinterface(ElementKey eKey) {
        throw unsupported();
    }

    /**
     * Creates a position for a type parameter of a type or executable element.
     *
     * @param index the index of the type parameter within the list of type parameters
     * @return the position
     */
    public RelativePosition<Integer> typeParameter(int index) {
        throw unsupported();
    }

    /**
     * Returns the element key, if this position directly represents an element.
     * @return the element key
     * @throws UnsupportedOperationException if this position does not directly represent an element
     */
    public ElementKey asElementKey() {
        throw unsupported();
    }

    /**
     * Returns the position as one with the given index class.
     * The method provides an easy safe way to downcast a {@code RelativePosition<?>}
     * to a {@code RelativePosition<T>} where {@code T} is the kind of the index
     * for the expected kind of the position.
     *
     * @param kind the expected kind of the position
     * @param c    the expected class of the index
     * @param <T>  the expected type of the index
     *
     * @return the position
     */
    public <T> RelativePosition<T> as(RelativePosition.Kind kind, Class<T> c) {
        throw unsupported();
    }

    /**
     * Returns the element key for a position.
     *
     * The element key for an element position is its key.
     * The element key for a relative position is the element key of its parent.
     *
     * @return the element key for a position
     */
    public abstract ElementKey getElementKey();

    // Abstract, to force all subtypes to implement this method.
    @Override
    public abstract boolean equals(Object other);

    // Abstract, to force all subtypes to implement this method.
    @Override
    public abstract int hashCode();

    /**
     * Applies a visitor to this position.
     * @param v the visitor
     * @param p a visitor-specified parameter
     * @param <R> the type of the result
     * @param <P> the type of the parameter
     * @return a visitor-specified result
     */
    public abstract <R,P> R accept(Visitor<R,P> v, P p);

    /**
     * Throws an {@code UnsupportedOperationException} if a given condition is {@code false}.
     *
     * @param cond the condition
     */
    protected void check(boolean cond) {
        if (!cond) {
            throw unsupported();
        }
    }

    /**
     * Creates an {@code UnsupportedOperationException} for this position.
     * @return the exception
     */
    protected UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(getClass().getSimpleName() + " " + toString());
    }

    /**
     * A visitor of positions, in the style of the visitor design pattern.
     * Classes implementing this interface are used to operate on a position
     * when the kind of position is unknown at compile time.
     * When a visitor is passed to a position's {@code accept} method,
     * the <code>visit<em>Xyz</em></code> method applicable to that position is invoked.
     *
     * @param <R> the return type of this visitor's methods.
     *              Use Void for visitors that do not need to return results.
     * @param <P> the type of the additional parameter to this visitor's methods.
     *              Use Void for visitors that do not need an additional parameter.
     */
    public interface Visitor<R,P> {
        /**
         * Visits a position identified by an element key.
         * @param kp the position to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitElementPosition(ElementPosition kp, P p);

        /**
         * Visits a relative position.
         * @param ip the position to visit
         * @param p a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitRelativePosition(RelativePosition<?> ip, P p);
    }

    /**
     * A position identified by an element key.
     */
    public static class ElementPosition extends Position {
        /** The element key. */
        public final ElementKey key;

        ElementPosition(ElementKey key) {
            this.key = key;
        }

        @Override
        public boolean is(ElementKind kind) {
            return key.is(kind);
        }

        @Override
        public boolean isElement() {
            return true;
        }

        @Override
        public RelativePosition<ElementKey> annotation(ElementKey key) {
            return new RelativePosition<>(this, RelativePosition.Kind.ANNOTATION, key);
        }

        @Override
        public RelativePosition<Void> defaultValue() {
            check(key.is(ElementKind.METHOD));
            return new RelativePosition<>(this, RelativePosition.Kind.DEFAULT_VALUE);
        }

        @Override
        public RelativePosition<ElementKey> directive(DirectiveKind kind, ElementKey typeKey) {
            check(key.is(ElementKey.Kind.MODULE));
            return new RelativePosition<>(this, kind, typeKey);
        }

        @Override
        public RelativePosition<String> docFile(String name) {
            check(key.is(ElementKey.Kind.MODULE) || key.is(ElementKey.Kind.PACKAGE));
            return new RelativePosition<>(this, RelativePosition.Kind.DOC_FILE, name);
        }

        @Override
        public RelativePosition<TypeMirrorKey> exception(TypeMirrorKey key) {
            return new RelativePosition<>(this, RelativePosition.Kind.EXCEPTION, key);
        }

        @Override
        public RelativePosition<Integer> parameter(int index) {
            check(key.is(ElementKey.Kind.EXECUTABLE));
            return new RelativePosition<>(this, RelativePosition.Kind.PARAMETER, index);
        }

        @Override
        public RelativePosition<ElementKey> permittedSubclass(ElementKey key) {
            check(key.is(ElementKey.Kind.TYPE));
            return new RelativePosition<>(this, RelativePosition.Kind.PERMITTED_SUBCLASS, key);
        }

        @Override
        public RelativePosition<Void> receiverType() {
            check(key.is(ElementKind.METHOD));
            return new RelativePosition<>(this, RelativePosition.Kind.RECEIVER_TYPE);
        }

        @Override
        public RelativePosition<Integer> recordComponent(int index) {
            //check(key.is(ElementKey.Kind.RECORD));
            return new RelativePosition<>(this, RelativePosition.Kind.RECORD_COMPONENT, index);
        }

        @Override
        public RelativePosition<Void> returnType() {
            check(key.is(ElementKind.METHOD));
            return new RelativePosition<>(this, RelativePosition.Kind.RETURN_TYPE);
        }

        @Override
        public RelativePosition<Void> serialVersionUID() {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SERIAL_VERSION_UID);
        }

        @Override
        public RelativePosition<String> serializationMethod(String name) {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SERIALIZATION_METHOD, name);
        }

        @Override
        public RelativePosition<Void> serializationOverview() {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SERIALIZATION_OVERVIEW);
        }

        @Override
        public RelativePosition<String> serializedField(String name) {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SERIALIZED_FIELD, name);
        }

        @Override
        public RelativePosition<Void> serializedForm() {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SERIALIZED_FORM);
        }

        @Override
        public RelativePosition<Void> superclass() {
            // Although superclass is only meaningful for elementKind CLASS,
            // we have to tolerate requesting a position for the superclass
            // on ANNOTATION_TYPE, ENUM and INTERFACE, because we might be
            // comparing elements that changed kind, such as from CLASS to INTERFACE.
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SUPERCLASS);
        }

        @Override
        public RelativePosition<ElementKey> superinterface(ElementKey eKey) {
            check(key.kind == ElementKey.Kind.TYPE);
            return new RelativePosition<>(this, RelativePosition.Kind.SUPERINTERFACE, eKey);
        }

        @Override
        public RelativePosition<Integer> typeParameter(int index) {
            // Strictly speaking, enum types and annotation types cannot be generic
            // but that will never arise in a well-formed program.
            check(key.kind == ElementKey.Kind.TYPE || key.kind == ElementKey.Kind.EXECUTABLE);
            return new RelativePosition<>(this, RelativePosition.Kind.TYPE_PARAMETER, index);
        }

        @Override
        public ElementKey asElementKey() {
            return key;
        }

        @Override
        public ElementKey getElementKey() {
            return key;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                return key.equals(((ElementPosition) other).key);
            }

        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return "{" + key + "}";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitElementPosition(this, p);
        }
    }

    /**
     * A position within an parent position, specified by its kind
     * and possible index.
     *
     * <p>Depending on the kind, the index should be an integer,
     * a name (element key) or null (void).
     */
    public static class RelativePosition<T> extends Position {
        /**
         * The kind of index for an {@code RelativePosition}.
         */
        public enum Kind {
            /** The index of an annotation on an annotated construct. */
            ANNOTATION,
            /** The index of an item in an annotation array value. */
            ANNOTATION_ARRAY_INDEX,
            /** The index of a value in an annotation. */
            ANNOTATION_VALUE,
            /** The index of a bound in a type parameter. */
            BOUND,
            /** The default value of an annotation element. */
            DEFAULT_VALUE,
            /** A doc file in a module or package. */
            DOC_FILE,
            /** The index of a exception that is thrown. */
            EXCEPTION,
            /** The index of an {@code exports} directive in a module declaration. */
            MODULE_EXPORTS,
            /** The index of an {@code requires} directive in a module declaration. */
            MODULE_REQUIRES,
            /** The index of an {@code opens} directive in a module declaration. */
            MODULE_OPENS,
            /** The index of an {@code provides} directive in a module declaration. */
            MODULE_PROVIDES,
            /** The index of an {@code uses} directive in a module declaration. */
            MODULE_USES,
            /** The index of a parameter. */
            PARAMETER,
            /** The index of a permitted subclass of a type element. */
            PERMITTED_SUBCLASS,
            /** The receiver type of a method. */
            RECEIVER_TYPE,
            /** The index of a record component. */
            RECORD_COMPONENT,
            /** The return type of a method. */
            RETURN_TYPE,
            /** The {@code serialVersionUID} of a serializable class. */
            SERIAL_VERSION_UID,
            /** A serialization method in a serializable class. */
            SERIALIZATION_METHOD,
            /** The serialization overview of a serializable class. */
            SERIALIZATION_OVERVIEW,
            /** A serialized field in a serializable class. */
            SERIALIZED_FIELD,
            /** The serialized form of a serializable class. */
            SERIALIZED_FORM,
            /** The superclass of a type. */
            SUPERCLASS,
            /** A superinterface of a type. */
            SUPERINTERFACE,
            /** The index of a type parameter. */
            TYPE_PARAMETER
        }

        /** The enclosing position. */
        public final Position parent;
        /** The kind of item given by the relative position. */
        public final Kind kind;
        /** The index given by the relative position. */
        public final T index;

        RelativePosition(Position parent, Kind kind) {
            this.parent = Objects.requireNonNull(parent);
            this.kind = Objects.requireNonNull(kind);
            this.index = null;
        }

        RelativePosition(Position parent, Kind kind, T index) {
            this.parent = Objects.requireNonNull(parent);
            this.kind = Objects.requireNonNull(kind);
            this.index = index;
        }

        RelativePosition(Position parent, DirectiveKind kind, T index) {
            this(parent, getKind(kind), index);
        }

        private static Kind getKind(DirectiveKind k) {
            return switch (k) {
                case EXPORTS -> Kind.MODULE_EXPORTS;
                case OPENS -> Kind.MODULE_OPENS;
                case PROVIDES -> Kind.MODULE_PROVIDES;
                case REQUIRES -> Kind.MODULE_REQUIRES;
                case USES -> Kind.MODULE_USES;
            };
        }

        @Override
        public boolean isRelative() {
            return true;
        }

        @Override
        public boolean is(RelativePosition.Kind kind) {
            return this.kind == kind;
        }

        @Override
        public RelativePosition<ElementKey> annotation(ElementKey key) {
            return new RelativePosition<>(this, Kind.ANNOTATION, key);
        }

        @Override
        public RelativePosition<Integer> annotationArrayIndex(int i) {
            return new RelativePosition<>(this, Kind.ANNOTATION_ARRAY_INDEX,  i);
        }

        @Override
        public RelativePosition<ElementKey> annotationValue(ElementKey key) {
            return new RelativePosition<>(this, Kind.ANNOTATION_VALUE,  key);
        }

        @Override
        public RelativePosition<Integer> bound(int i) {
            return new RelativePosition<>(this, Kind.BOUND,  i);
        }

        @Override
        public RelativePosition<TypeMirrorKey> exception(TypeMirrorKey type) {
            check(kind == Kind.SERIALIZATION_METHOD);
            return new RelativePosition<>(parent, Kind.EXCEPTION, type);
        }

        @Override
        public RelativePosition<Integer> parameter(int index) {
            check(kind == Kind.SERIALIZATION_METHOD);
            return new RelativePosition<>(this, Kind.PARAMETER, index);
        }

        @Override
        public RelativePosition<Void> receiverType() {
            check(kind == Kind.SERIALIZATION_METHOD);
            return new RelativePosition<>(this, Kind.RECEIVER_TYPE);
        }

        @Override
        public RelativePosition<Void> returnType() {
            check(kind == Kind.SERIALIZATION_METHOD);
            return new RelativePosition<>(this, Kind.RETURN_TYPE);
        }

        @Override
        public RelativePosition<Void> serialVersionUID() {
            check(kind == Kind.SERIALIZED_FORM);
            return new RelativePosition<>(parent, RelativePosition.Kind.SERIAL_VERSION_UID);
        }

        @Override
        public RelativePosition<String> serializationMethod(String name) {
            check(kind == Kind.SERIALIZED_FORM);
            return new RelativePosition<>(parent, RelativePosition.Kind.SERIALIZATION_METHOD, name);
        }

        @Override
        public RelativePosition<Void> serializationOverview() {
            check(kind == Kind.SERIALIZED_FORM);
            return new RelativePosition<>(parent, RelativePosition.Kind.SERIALIZATION_OVERVIEW);
        }

        @Override
        public RelativePosition<String> serializedField(String name) {
            check(kind == Kind.SERIALIZED_FORM);
            return new RelativePosition<>(parent, RelativePosition.Kind.SERIALIZED_FIELD, name);
        }

        @Override
        public RelativePosition<Void> serializedForm() {
            return switch (kind) {
                case SERIAL_VERSION_UID, SERIALIZATION_METHOD, SERIALIZATION_OVERVIEW, SERIALIZED_FIELD ->
                        new RelativePosition<>(parent, Kind.SERIALIZED_FORM);
                default -> throw unsupported();
            };
        }

        @Override
        public ElementKey getElementKey() {
            return parent.getElementKey();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other == null || other.getClass() != getClass()) {
                return false;
            } else {
                RelativePosition<?> ip = (RelativePosition<?>) other;
                return parent.equals(ip.parent)
                        && kind == ip.kind
                        && Objects.equals(index, ip.index);
            }

        }

        @Override
        public <U> RelativePosition<U> as(Kind kind, Class<U> c) {
            @SuppressWarnings("unchecked")
            RelativePosition<U> p = (RelativePosition<U>) this;
            return p;
        }

        @Override
        public int hashCode() {
            int hashCode = parent.hashCode();
            hashCode = hashCode * 37 + kind.hashCode();
            hashCode = hashCode * 37 + Objects.hashCode(index);
            return hashCode;
        }

        @Override
        public String toString() {
            // TODO: convert to a simple debug print, and use SignatureVisitor or pretty print.
            return "{" + parent + ": " + kind + " #" + index + "}";
        }

        @Override
        public <R, P> R accept(Visitor<R,P> v, P p) {
            return v.visitRelativePosition(this, p);
        }
    }

    /**
     * A comparator for relative positions whose index is an element key.
     */
    public static final Comparator<RelativePosition<?>> elementKeyIndexComparator = (rp1, rp2) -> {
        if (rp1.parent != rp2.parent) {
            throw new IllegalArgumentException("parents not equal");
        }
        if (rp1.kind != rp2.kind) {
            throw new IllegalArgumentException("kinds not equal");
        }
        ElementKey i1 = (ElementKey) rp1.index;
        ElementKey i2 = (ElementKey) rp2.index;
        return i1.compareTo(i2);
    };

    /**
     * A comparator for relative positions whose index is a string.
     */
    public static final Comparator<RelativePosition<?>> stringIndexComparator = (rp1, rp2) -> {
        if (rp1.parent != rp2.parent) {
            throw new IllegalArgumentException("parents not equal");
        }
        if (rp1.kind != rp2.kind) {
            throw new IllegalArgumentException("kinds not equal");
        }
        String i1 = (String) rp1.index;
        String i2 = (String) rp2.index;
        return i1.compareTo(i2);
    };
}

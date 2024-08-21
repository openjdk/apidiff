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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;

/**
 * A "serialized form" is a container for the constants, fields and methods
 * of a class related to Java object serialization, as defined in the
 * "Java Object Serialization Specification".
 *
 * In the context of apidiff, it provides access to fields and methods
 * which were declared as private (and so not included in the primary
 * access-based selection) but which nevertheless may contribute to
 * the serialized form of a type element.  But, note that the actual
 * serialized form may be specified narratively by the {@code serialData}
 * tag on one of a number of serialization methods, or by a series of
 * {@code @serialField} tags on the {@code serialPersistentFields}
 * member of a serializable class.
 *
 * Finally, note that classes may of may not be included in the documentation
 * by using {@code @serial include | exclude} on the class or package
 * documentation.
 */
public class SerializedForm {

    private final long serialVersionUID;
    private final List<Field> fields;
    private final List<ExecutableElement> methods;
    private final SerializedFormDocs docs;

    SerializedForm(long serialVersionUID, List<Field> fields, List<ExecutableElement> methods,
            SerializedFormDocs docs) {
        this.serialVersionUID = serialVersionUID;
        this.fields = fields;
        this.methods = methods;
        this.docs = docs;
    }

    /**
     * Returns the serial version UID for the element.
     *
     * @return the serial version UID
     */
    public long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     * Returns the list of fields in the serialized form.
     *
     * @return the list of fields in the serialized form.
     */
    public List<? extends Field> getFields() {
        return fields;
    }

    public Field getField(CharSequence name) {
        return getFields().stream()
                .filter(f -> f.getName().contentEquals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the list of serialization methods.
     *
     * @return the list of serialization.
     */
    public List<? extends ExecutableElement> getMethods() {
        return methods;
    }

    /**
     * Returns the descriptions for the items in the serialized form.
     *
     * @return the descriptions
     */
    public SerializedFormDocs getDocs() {
        return docs;
    }

    /**
     * A serialized field.
     *
     * Instances are created by information in the {@code @serialField} tags of the
     * {@code serialPersistentFields} member, or from the default set of serializable fields.
     */
    public interface Field {
        /**
         * Returns the type element enclosing this field.
         *
         * @return the type element
         */
        TypeElement getEnclosingTypeElement();

        /**
         * Returns the name of the field.
         *
         * @return the name of the field
         */
        Name getName();

        /**
         * Returns the type of the field, or a type with of kind {@code NONE} if the type
         * could not be determined. The value may have kind {@code} if the field is
         * described by information in a {@code @serialField} tag, and the type specification
         * could not be resolved.
         *
         * Note: the use of {@code NONE} is non-standard in this context. It would be better
         * if the type were of kind {@code ERROR} if the signature cannot be resolved,
         * but that is not possible with the current API. {@code null} is not used because
         * that is generally used to mean "missing" instead of "error".
         *
         * @return the type of the field, or a type of kind {@code NONE}.
         */
        TypeMirror getType();

        /**
         * Returns the documentation comment of the field.
         *
         * If the field is described by information in a {@code @serialField} tag,
         * the comment is taken from that tag.
         * If the field is a default serializable field, the comment is the
         * full comment of that field.
         *
         * @return the documentation comment of the field.
         */
        List<? extends DocTree> getDocComment();

        /**
         * Returns the signature of the type of the field.
         * The signature is always available, even if {@link #getType()} returns a
         * type of kind {@code NONE}.
         *
         * If the field is described by information in a {@code @serialField} tag,
         * the signature is as found in that tag.
         * If the field is a default serializable field, the signature is the
         * result of {@code getType().toString()}.
         *
         * @return the type signature of the field
         */
        String getSignature();
    }
}

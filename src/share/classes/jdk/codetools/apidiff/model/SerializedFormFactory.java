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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;

/**
 * A factory to create the {@link SerializedForm} object for a
 * type element if appropriate.
 *
 * A serialized form is only created for a type element if
 * it is {@code Serializable} but not an enum, and if it
 * is marked with {@code @serial include}, or the enclosing
 * package is not marked with {@code serial exclude} and
 * the type is {@code public} or {@code protected}.
 */
public class SerializedFormFactory {

    private final Map<PackageElement, Boolean> excludedPackages;

    private final Elements elements;
    private final Types types;
    private final DocTrees trees;

    private final TypeMirror serializable;
    private final TypeMirror externalizable;
    private final TypeMirror objectInput;
    private final TypeMirror objectInputStream;
    private final TypeMirror objectOutput;
    private final TypeMirror objectOutputStream;
    private final TypeMirror objectStreamField;

    private final Name readExternal;
    private final Name writeExternal;
    private final Name readObject;
    private final Name readObjectNoData;
    private final Name writeObject;
    private final Name readResolve;
    private final Name writeReplace;
    private final Name serialPersistentFields;
    private final Name serialVersionUID;

    private Set<Modifier> privateStaticFinal = Set.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

    /**
     * Creates an instance of {@code SerializedFormFactory} using the utility objects for
     * {@code Elements}, {@code Types} and {@code Trees} available from an API.
     *
     * @param api the API
     */
    public SerializedFormFactory(API api) {
        this(api.getElements(), api.getTypes(), api.getTrees());
    }

    /**
     * Creates an instance of {@code SerializedFormFactory} using the given utility objects.
     *
     * @param elements the {@code Elements} utility class to be used
     * @param types    the {@code Types} utility class to be used
     * @param trees    the {@code DocTrees} utility class to be used
     */
    public SerializedFormFactory(Elements elements, Types types, DocTrees trees) {
        this.elements = elements;
        this.types = types;
        this.trees = trees;

        excludedPackages = new HashMap<>();

        ModuleElement javaBase = elements.getModuleElement("java.base");

        serializable            = getType(javaBase, "java.io.Serializable");
        externalizable          = getType(javaBase, "java.io.Externalizable");
        objectInput             = getType(javaBase, "java.io.ObjectInput");
        objectInputStream       = getType(javaBase, "java.io.ObjectInputStream");
        objectOutput            = getType(javaBase, "java.io.ObjectOutput");
        objectOutputStream      = getType(javaBase, "java.io.ObjectOutputStream");
        objectStreamField       = getType(javaBase, "java.io.ObjectStreamField");

        readExternal            = elements.getName("readExternal");
        writeExternal           = elements.getName("writeExternal");
        readObject              = elements.getName("readObject");
        readObjectNoData        = elements.getName("readObjectNoData");
        writeObject             = elements.getName("writeObject");
        readResolve             = elements.getName("readResolve");
        writeReplace            = elements.getName("writeReplace");
        serialPersistentFields  = elements.getName("serialPersistentFields");
        serialVersionUID        = elements.getName("serialVersionUID");
    }

    /**
     * Returns the instance of {@code SerializedFormDocs} containing the information
     * related to a given type element, or {@code null} if no such information is available.
     *
     * This implementation returns {@code null}.
     *
     * @param te the type element
     *
     * @return the instance of {@code SerializedFormDocs} containing the information
     */
    public SerializedFormDocs getSerializedFormDocs(TypeElement te) {
        return null;
    }

    /**
     * Returns a type of an element with a given canonical name, as seen from the given module.
     *
     * @param me   the module
     * @param name the name
     *
     * @return the type of the element
     */
    private TypeMirror getType(ModuleElement me, String name) {
        return elements.getTypeElement(me, name).asType();
    }

    /**
     * Returns the {@code SerializedForm} object for a type element, or null if it does not have one.
     *
     * @param te the type element
     *
     * @return the {@code SerializedForm} object
     */
    public SerializedForm get(TypeElement te) {
        if (!isIncluded(te)) {
            return null;
        }

        long serialVersionUID = getSerialVersionUID(te);

        List<SerializedForm.Field> fields;
        List<ExecutableElement> methods;

        if (types.isAssignable(te.asType(), externalizable)) {
            fields = List.of();
            methods = getExternalizableMethods(te);
        } else {
            fields = getSerializableFields(te);
            methods = getSerializableMethods(te);
        }

        SerializedFormDocs docs = getSerializedFormDocs(te);

        return new SerializedForm(serialVersionUID, fields, methods, docs);
    }

    //<editor-fold desc="Inclusion">

    /**
     * Determines if a type element has a specific serialized form.
     *
     * A type element has a specific serialized form if
     * it is {@code Serializable} but not an enum, and if it
     * is marked with {@code @serial include}, or the enclosing
     * package is not marked with {@code serial exclude} and
     * the type is {@code public} or {@code protected}.
     *
     * @param te the type element
     *
     * @return {@code true} if and only if the type element has a specific
     * serialized form
     */
    private boolean isIncluded(TypeElement te) {
        if (te.getKind() == ElementKind.ENUM
                || !types.isAssignable(te.asType(), serializable)) {
            return false;
        }

        Optional<SerialTree> serialTrees = getSerialTrees(te);
        if (matches(serialTrees, "include")) {
            return true;
        }

        if (matches(serialTrees, "exclude")
            || excludedPackages.computeIfAbsent(elements.getPackageOf(te),
                p -> matches(getSerialTrees(p), "exclude"))) {
            return false;
        }

        Set<Modifier> modifiers = te.getModifiers();
        return modifiers.contains(Modifier.PUBLIC)
                || modifiers.contains(Modifier.PROTECTED);
    }

    /**
     * Returns whether an optional {@code SerialTree} object matches the given kind.
     * The kind is typically "include" or "exclude".
     *
     * @param optSerial the optional {@code SerialTree}
     * @param kind      the kind
     *
     * @return {@code true} if and only if a match is found
     */
    private boolean matches(Optional<SerialTree> optSerial, String kind) {
        return optSerial.isPresent() && optSerial.get().getDescription().toString().equals(kind);
    }

    /**
     * Returns the {@code {@serial ...}} tag, if any, in the doc comment for an element.
     *
     * @param e the element
     *
     * @return the tag
     */
    private Optional<SerialTree> getSerialTrees(Element e) {
        DocCommentTree dct = trees.getDocCommentTree(e);
        if (dct == null) {
            return Optional.empty();
        }

        return dct.getBlockTags().stream()
                .filter(t -> t.getKind() == DocTree.Kind.SERIAL)
                .map(t -> (SerialTree) t)
                .findFirst();
    }

    //</editor-fold>

    //<editor-fold desc="Serial Version UID">

    /**
     * Returns the {@code serialVersionUID} for a type element.
     * If the type element defines an appropriate field, the constant value
     * of the field is returned; otherwise, the default value is computed.
     *
     * @param te the type element
     *
     * @return the serial version UID
     */
    private long getSerialVersionUID(TypeElement te) {
        VariableElement ve = te.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD
                        && e.getSimpleName() == serialVersionUID)
                .map(e -> (VariableElement) e)
                .findFirst()
                .orElse(null);

        if (ve != null
                && ve.getModifiers().contains(Modifier.STATIC)
                && ve.getModifiers().contains(Modifier.FINAL)
                && types.isSameType(ve.asType(), types.getPrimitiveType(TypeKind.LONG))) {
            Object o = ve.getConstantValue();
            if (o instanceof Long) {
                return (Long) o;
            }
        }

        return computeDefaultSUID(te);
    }

    /**
     * Computes the default serial version UID value for the given class.
     *
     * This code is translated from the corresponding code in {@code java.io.ObjectStreamClass},
     * converting it from using runtime reflection to compile-time reflection.
     */
    private long computeDefaultSUID(TypeElement te) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            dout.writeUTF(te.getQualifiedName().toString());

            int classMods = IntModifier.getModifiers(te) &
                    (IntModifier.PUBLIC | IntModifier.FINAL |
                            IntModifier.INTERFACE | IntModifier.ABSTRACT);

            /*
             * compensate for javac bug in which ABSTRACT bit was set for an
             * interface only if the interface declared methods
             */
            List<ExecutableElement> methods = ElementFilter.methodsIn(te.getEnclosedElements());
            if ((classMods & IntModifier.INTERFACE) != 0) {
                classMods = (methods.size() > 0) ?
                        (classMods | IntModifier.ABSTRACT) :
                        (classMods & ~IntModifier.ABSTRACT);
            }
            dout.writeInt(classMods);

            if (te.asType().getKind() != TypeKind.ARRAY) {
                /*
                 * compensate for change in 1.2FCS in which
                 * Class.getInterfaces() was modified to return Cloneable and
                 * Serializable for array classes.
                 */
                List<? extends TypeMirror> interfaces = te.getInterfaces();
                List<String> ifaceNames = interfaces.stream()
                        .map(SerializedFormFactory::getInterfaceName)
                        .sorted()
                        .collect(Collectors.toList());
                for (String n : ifaceNames) {
                    dout.writeUTF(n);
                }
            }

            List<? extends VariableElement> fields = ElementFilter.fieldsIn(te.getEnclosedElements());
            List<MemberSignature> fieldSigs = fields.stream()
                    .map(MemberSignature::new)
                    .sorted(Comparator.comparing(ms -> ms.name))
                    .collect(Collectors.toList());
            for (MemberSignature sig : fieldSigs) {
                int mods = IntModifier.getModifiers(sig.member) &
                        (IntModifier.PUBLIC | IntModifier.PRIVATE | IntModifier.PROTECTED |
                                IntModifier.STATIC | IntModifier.FINAL | IntModifier.VOLATILE |
                                IntModifier.TRANSIENT);
                if (((mods & IntModifier.PRIVATE) == 0) ||
                        ((mods & (IntModifier.STATIC | IntModifier.TRANSIENT)) == 0))
                {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }

            if (hasStaticInitializer(te)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(IntModifier.STATIC);
                dout.writeUTF("()V");
            }

            List<? extends ExecutableElement> cons = ElementFilter.constructorsIn(te.getEnclosedElements());
            List<MemberSignature> consSigs = cons.stream()
                    .map(MemberSignature::new)
                    .sorted(Comparator.comparing(ms -> ms.signature))
                    .collect(Collectors.toList());
            for (MemberSignature sig : consSigs) {
                int mods = IntModifier.getModifiers(sig.member) &
                        (IntModifier.PUBLIC | IntModifier.PRIVATE | IntModifier.PROTECTED |
                                IntModifier.STATIC | IntModifier.FINAL |
                                IntModifier.SYNCHRONIZED | IntModifier.NATIVE |
                                IntModifier.ABSTRACT | IntModifier.STRICT);
                if ((mods & IntModifier.PRIVATE) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            List<MemberSignature> methSigs = methods.stream()
                    .map(MemberSignature::new)
                    .sorted(Comparator.comparing((MemberSignature ms) -> ms.name)
                            .thenComparing(ms -> ms.signature))
                    .collect(Collectors.toList());
            for (MemberSignature sig : methSigs) {
                int mods = IntModifier.getModifiers(sig.member) &
                        (IntModifier.PUBLIC | IntModifier.PRIVATE | IntModifier.PROTECTED |
                                IntModifier.STATIC | IntModifier.FINAL |
                                IntModifier.SYNCHRONIZED | IntModifier.NATIVE |
                                IntModifier.ABSTRACT | IntModifier.STRICT);
                if ((mods & IntModifier.PRIVATE) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            dout.flush();

            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException ex) {
            throw new InternalError(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalError(ex.getMessage());
        }
    }

    /**
     * Returns the fully qualified name for a type mirror representing an interface,
     * such as found in the superinterfaces of a class.
     *
     * @param t the type mirror
     *
     * @return the name
     */
    private static String getInterfaceName(TypeMirror t) {
        Element e = ((DeclaredType) t).asElement();
        return ((TypeElement) e).getQualifiedName().toString();
    }

    /**
     * Returns whether a type element has, or will have, a static initializer.
     * A type has a static initializer if it has an executable member named {@code <clinit>}.
     * This may arise due to explicit presence of {@code static { ... }} in source code,
     * or to hold the initialization of static fields with a non-constant value.
     *
     * @param te the type element
     *
     * @return if the type element has or will have a static initializer
     */
    private boolean hasStaticInitializer(TypeElement te) {
        if (te.getEnclosedElements().stream().anyMatch(e -> e.getKind() == ElementKind.STATIC_INIT)) {
            return true;
        }

        // if the source is available, scan the AST for the element, looking for
        // either 'static { ... }' or static variables with non-constant initializers
        TreePath p = trees.getPath(te);
        if (p != null && p.getLeaf() instanceof ClassTree) {
            ClassTree ct = (ClassTree) p.getLeaf();
            for (Tree t : ct.getMembers()) {
                switch (t.getKind()) {
                    case BLOCK -> {
                        BlockTree bt = (BlockTree) t;
                        if (bt.isStatic()) {
                            // found an explicit static initializer block
                            return true;
                        }
                    }
                    case VARIABLE -> {
                        VariableTree vt = (VariableTree) t;
                        if (vt.getModifiers().getFlags().contains(Modifier.STATIC)
                                && vt.getInitializer() != null) {
                            Element e = trees.getElement(new TreePath(p, vt));
                            if (e != null && e.getKind() == ElementKind.FIELD) {
                                Object cv = ((VariableElement) e).getConstantValue();
                                if (cv == null) {
                                    // found field with an initializer that is not a constant
                                    // expression, and so will require an implicit static initializer block
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * A wrapper around runtime modifiers.
     * These are distinct from {@link javax.lang.model.element.Modifier},
     * and while they are similar, there is not a direct one-to-one correspondence.
     * For example, {@code javax.lang.model} models interfaces differently,
     * and runtime reflection has not explicit value equivalent for DEFAULT.
     *
     * Note, the spec for the computing the default serialVersionUID is defined
     * in terms of the runtime kind of modifiers.
     */
    private static class IntModifier {

        static final int ABSTRACT       = java.lang.reflect.Modifier.ABSTRACT;
        static final int FINAL          = java.lang.reflect.Modifier.FINAL;
        static final int INTERFACE      = java.lang.reflect.Modifier.INTERFACE;
        static final int NATIVE         = java.lang.reflect.Modifier.NATIVE;
        static final int PRIVATE        = java.lang.reflect.Modifier.PRIVATE;
        static final int PROTECTED      = java.lang.reflect.Modifier.PROTECTED;
        static final int PUBLIC         = java.lang.reflect.Modifier.PUBLIC;
        static final int STATIC         = java.lang.reflect.Modifier.STATIC;
        static final int STRICT         = java.lang.reflect.Modifier.STRICT;
        static final int SYNCHRONIZED   = java.lang.reflect.Modifier.SYNCHRONIZED;
        static final int TRANSIENT      = java.lang.reflect.Modifier.TRANSIENT;
        static final int VOLATILE       = java.lang.reflect.Modifier.VOLATILE;

        static int getModifiers(Element e) {
            int mods = 0;
            for (Modifier m : e.getModifiers()) {
                switch (m) {
                    case ABSTRACT:      mods |= ABSTRACT;       break;
                    case DEFAULT:       /* no equivalent */     break;
                    case FINAL:         mods |= FINAL;          break;
                    case NATIVE:        mods |= NATIVE;         break;
                    case PRIVATE:       mods |= PRIVATE;        break;
                    case PROTECTED:     mods |= PROTECTED;      break;
                    case PUBLIC:        mods |= PUBLIC;         break;
                    case STATIC:        mods |= STATIC;         break;
                    case STRICTFP:      mods |= STRICT;         break;
                    case SYNCHRONIZED:  mods |= SYNCHRONIZED;   break;
                    case TRANSIENT:     mods |= TRANSIENT;      break;
                    case VOLATILE:      mods |= VOLATILE;       break;
                }
            }

            if (e.getKind().isInterface()) {
                mods |= INTERFACE;
            }

            return mods;
        }
    }

    /**
     * A simple container for a field or executable member of a type element,
     * providing the information that will be used to computer the default serialVersionUID.
     */
    private static class MemberSignature {
        String name;
        Element member;
        String signature;

        MemberSignature(Element ve) {
            name = ve.getSimpleName().toString();
            member = ve;
            signature = descriptorVisitor.visit(ve.asType(), new StringBuilder()).toString();
        }
    }

    /**
     * A visitor to compute the signature (descriptor) for members of a type element.
     */
    private static TypeVisitor<StringBuilder,StringBuilder> descriptorVisitor = new SimpleTypeVisitor14<>() {
        @Override
        public StringBuilder defaultAction(TypeMirror t, StringBuilder sb) {
            throw new Error(t.getKind() + ": " + t.toString());
        }

        @Override
        public StringBuilder visitArray(ArrayType t, StringBuilder sb) {
            sb.append("[");
            return t.getComponentType().accept(this, sb);
        }

        @Override
        public StringBuilder visitDeclared(DeclaredType t, StringBuilder sb) {
            return sb.append("L")
                    .append(((TypeElement) t.asElement()).getQualifiedName().toString().replace(".", "/"))
                    .append(";");
        }

        @Override
        public StringBuilder visitExecutable(ExecutableType t, StringBuilder sb) {
            sb.append('(');
            for (TypeMirror p : t.getParameterTypes()) {
                p.accept(this, sb);
            }
            sb.append(')');
            return t.getReturnType().accept(this, sb);
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariable t, StringBuilder sb) {
            return sb.append("Ljava/lang/Object;"); // TODO: use bounds? types.erasure(t).accept(this, sb) ?
        }

        @Override
        public StringBuilder visitNoType(NoType t, StringBuilder sb) {

            if (t.getKind() != TypeKind.VOID) {
                throw new IllegalArgumentException((t.toString()));
            }
            return sb.append('V');
        }

        @Override
        public StringBuilder visitPrimitive(PrimitiveType t, StringBuilder sb) {
            char ch = switch (t.getKind()) {
                case BYTE -> 'B';
                case CHAR -> 'C';
                case DOUBLE -> 'D';
                case FLOAT -> 'F';
                case INT -> 'I';
                case LONG -> 'L';
                case SHORT -> 'S';
                case BOOLEAN -> 'Z';
                default -> throw new IllegalArgumentException(t.toString());
            };
            return sb.append(ch);
        }
    };

    //</editor-fold>

    //<editor-fold desc="Serialized Methods">

    /**
     * Returns the list of methods related to the serialization in a type element
     * that is externalizable.
     *
     * The list includes: {@code readExternal}, {@code writeExternal}, {@code readResolve}
     * and {@code writeReplace}.
     *
     * @param te the type element
     *
     * @return the list
     */
    private List<ExecutableElement> getExternalizableMethods(TypeElement te) {
        return getMethods(te, ee ->
                   isMethod(ee, readExternal, objectInput)
                || isMethod(ee, writeExternal, objectOutput)
                || isMethod(ee, readResolve)
                || isMethod(ee, writeReplace));
    }

    /**
     * Returns the list of methods related to the serialization in a type element
     * that is serializable (but not externalizable).
     *
     * The list includes: {@code readObject}, {@code readObjectNoData}, {@code writeObject},
     * {@code readResolve} and {@code writeReplace}.
     *
     * @param te the type element
     *
     * @return the list
     */
    private List<ExecutableElement> getSerializableMethods(TypeElement te) {
        return getMethods(te, ee ->
                   isMethod(ee, readObject, objectInputStream)
                || isMethod(ee, readObjectNoData)
                || isMethod(ee, writeObject, objectOutputStream)
                || isMethod(ee, readResolve)
                || isMethod(ee, writeReplace));
    }

    /**
     * Returns the list of methods in a type element that match a given predicate.
     *
     * @param te     the type element
     * @param filter the predicate
     *
     * @return the list
     */
    private List<ExecutableElement> getMethods(TypeElement te, Predicate<ExecutableElement> filter) {
        Map<Name, ExecutableElement> map = new HashMap<>();
        for (Element e : elements.getAllMembers(te)) {
            if (e.getKind() != ElementKind.METHOD) {
                continue;
            }

            ExecutableElement ee = (ExecutableElement) e;
            if (filter.test(ee)) {
                ExecutableElement prev = map.get(ee.getSimpleName());
                if (prev == null || elements.overrides(ee, prev, te)) {
                    map.put(ee.getSimpleName(), ee);
                }
            }
        }
        List<ExecutableElement> list = new ArrayList<>(map.values());
        list.sort((e1, e2) -> CharSequence.compare(e1.getSimpleName(), e2.getSimpleName()));
        return list;
    }

    /**
     * Returns whether an executable element has a given name and no parameters.
     *
     * @param ee   the element
     * @param name the name
     *
     * @return true if the element has the given name and no parameters
     */
    private boolean isMethod(ExecutableElement ee, Name name) {
        return ee.getSimpleName() == name
                && ee.getParameters().isEmpty();
    }

    /**
     * Returns whether an executable element has a given name and a single parameter
     * of a given type.
     *
     * @param ee    the element
     * @param name  the name
     * @param param the parameter type
     *
     * @return true if the element has the given name and no parameters
     */
    private boolean isMethod(ExecutableElement ee, Name name, TypeMirror param) {
        return ee.getSimpleName() == name
                && ee.getParameters().size() == 1
                && types.isSameType(ee.getParameters().get(0).asType(), param);
    }
    //</editor-fold>

    //<editor-fold desc="Serialized Fields">

    /**
     * Returns the list of fields related to the serialization in a type element
     * that is serializable (but not externalizable).
     *
     * The list contains the default set of fields to be serialized.
     * This set is determined from the {@code @serialField} tags on the {@code persistentSerialFields}
     * (if defined), or the list of non-static non-transient fields declared
     * in the type element.
     *
     * The list also contains the fields for {@code serialVersionUID} and {@code persistentSerialFields},
     * if present. They can be distinguished from the default set of fields to be serialized
     * by name and by being declared to be {@code static}.
     *
     *
     * @param  te the type element
     *
     * @return the list of fields in the serialized form
     */
    private List<SerializedForm.Field> getSerializableFields(TypeElement te) {
        List<SerializedForm.Field> list = new ArrayList<>();

        VariableElement spf = te.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .filter(this::isSerialPersistentFields)
                .findFirst()
                .orElse(null);

        if (spf != null) {
            DocCommentTree dct = trees.getDocCommentTree(spf);
            if (dct != null) {
                DocTreePathScanner<Void,List<SerializedForm.Field>> scanner = new DocTreePathScanner<>() {
                    @Override
                    public Void visitSerialField(SerialFieldTree tree, List<SerializedForm.Field> list) {
                        list.add(new DocumentedField(te, getCurrentPath()));
                        return null;
                    }
                };
                scanner.scan(new DocTreePath(trees.getPath(te), dct), list);
            }
        } else {
            for (VariableElement ve : ElementFilter.fieldsIn(te.getEnclosedElements())) {
                Set<Modifier> modifiers = ve.getModifiers();
                if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)) {
                    continue;
                }
                list.add(new VariableElementField(ve));
            }
        }

        return list;
    }

    /**
     * Returns whether a field is a valid declaration of {@code serialPersistentFields}.
     *
     * @param ve the field
     *
     * @return {@code true} if and only if this is a valid declaration of {@code serialPersistentFields}
     */
    private boolean isSerialPersistentFields(VariableElement ve) {
        return ve.getSimpleName() == serialPersistentFields
                && ve.getModifiers().equals(privateStaticFinal)
                && types.isSameType(ve.asType(), types.getArrayType(objectStreamField));
    }

    /**
     * Details for a field in a serialized form, that is derived from information
     * in {@code @serialField} tags on the {@code serialPersistentFields} field.
     */
    private class DocumentedField implements SerializedForm.Field {
        private final TypeElement enclosingTypeElement;
        private final Name name;
        private final TypeMirror type;
        private final List<? extends DocTree> description;
        private final String signature;

        DocumentedField(TypeElement te, DocTreePath p) {
            enclosingTypeElement = te;
            DocTree t = p.getLeaf();
            if (t.getKind() != DocTree.Kind.SERIAL_FIELD) {
                throw new IllegalArgumentException(t.getKind().toString());
            }
            SerialFieldTree sft = (SerialFieldTree) t;
            name = sft.getName().getName();
            type = getType(p, sft.getType());
            description = sft.getDescription();
            signature = sft.getType().toString();
        }

        @Override
        public TypeElement getEnclosingTypeElement() {
            return enclosingTypeElement;
        }

        /**
         * Returns the type for the signature found in a {@code @serialField} tag,
         * or a type of kind {@code NONE} if the type cannot be resolved.
         *
         * Note: it would be better if it was possible to use a type of kind ERROR
         * instead of NONE, but that cannot be done with the current API.
         *
         * javac does not directly support array signatures, so count and remove
         * the trailing '[]' characters, look up the base type, and then convert
         * to the appropriate number of levels of array.
         *
         * @param serialFieldPath the path for {@code serialField} tag
         * @param refTree           the reference tree within the {@code serialField} tag
         *
         * @return the type
         */
        private TypeMirror getType(DocTreePath serialFieldPath, ReferenceTree refTree) {

            String sig = refTree.getSignature();
            int dims = 0;
            int index = sig.length();
            while (index > 2) {
                if (sig.charAt(index - 2) == '[' && sig.charAt(index - 1) == ']') {
                    dims++;
                    index -= 2;
                } else {
                    break;
                }
            }

            String baseSig = sig.substring(0, index);
            TypeMirror t;
            switch (baseSig) {
                case "boolean" -> t = types.getPrimitiveType(TypeKind.BOOLEAN);
                case "byte" -> t = types.getPrimitiveType(TypeKind.BYTE);
                case "char" -> t = types.getPrimitiveType(TypeKind.CHAR);
                case "double" -> t = types.getPrimitiveType(TypeKind.DOUBLE);
                case "float" -> t = types.getPrimitiveType(TypeKind.FLOAT);
                case "int" -> t = types.getPrimitiveType(TypeKind.INT);
                case "long" -> t = types.getPrimitiveType(TypeKind.LONG);
                case "short" -> t = types.getPrimitiveType(TypeKind.SHORT);
                case "void" -> t = types.getPrimitiveType(TypeKind.VOID);
                default -> {
                    DocTreePath refPath = new DocTreePath(serialFieldPath,
                            dims == 0 ? refTree : trees.getDocTreeFactory().newReferenceTree(baseSig));
                    Element e = trees.getElement(refPath);
                    if (e == null) {
                        // ideally, we would be able to use an instance of an ERROR type,
                        // but that is not available in the API, so use NONE as a marker value instead.
                        return types.getNoType(TypeKind.NONE);
                    }
                    t = e.asType();
                }
            }

            while (dims > 0) {
                t = types.getArrayType(t);
                dims--;
            }

            return t;
        }

        @Override
        public Name getName() {
            return name;
        }

        @Override
        public TypeMirror getType() {
            return type;
        }

        @Override
        public List<? extends DocTree> getDocComment() {
            return description;
        }

        @Override
        public String getSignature() {
            return signature;
        }
    }

    /**
     * Details for a field in a serialized form, that is derived from a field
     * in the type element.
     */
    private class VariableElementField implements SerializedForm.Field {
        VariableElement ve;

        VariableElementField(VariableElement ve) {
            this.ve = ve;
        }

        @Override
        public TypeElement getEnclosingTypeElement() {
            return (TypeElement) ve.getEnclosingElement();
        }

        @Override
        public Name getName() {
            return ve.getSimpleName();
        }

        @Override
        public TypeMirror getType() {
            return ve.asType();
        }

        @Override
        public List<? extends DocTree> getDocComment() {
            DocCommentTree dct = trees.getDocCommentTree(ve);
            return dct == null ? null : List.of(dct);
        }

        @Override
        public String getSignature() {
            return ve.asType().toString();
        }
    }
    //</editor-fold>
}

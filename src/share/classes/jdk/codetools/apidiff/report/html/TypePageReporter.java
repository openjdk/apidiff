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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.Entity;
import jdk.codetools.apidiff.html.HtmlAttr;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.AccessKind;
import jdk.codetools.apidiff.model.ElementExtras;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.IntTable;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.model.SerializedForm;
import jdk.codetools.apidiff.model.TypeMirrorKey;
import jdk.codetools.apidiff.report.SignatureVisitor;

/**
 * A reporter that generates an HTML page for the differences in
 * a type declaration.
 */
class TypePageReporter extends PageReporter<TypeElementKey> {
    private APIMap<? extends TypeElement> tMap = null;

    private final ElementExtras elementExtras = ElementExtras.instance();

    TypePageReporter(HtmlReporter parent, ElementKey tKey) {
        super(parent, (TypeElementKey) tKey);
    }

    /**
     * Writes a page containing details for a single type element in a given API.
     *
     * @param api the API containing the element
     * @param te  the type element
     */
    void writeFile(API api, TypeElement te) {
        Position pagePos = Position.of(pageKey);
        APIMap<TypeElement> apiMap = APIMap.of(api, te);
        comparing(pagePos, apiMap);
        Set<API> missing = parent.apis.stream()
                .filter(a -> a != api)
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            reportMissing(pagePos, missing);
        }
        completed(pagePos, true);
    }

    @Override
    protected void writeFile() {
        // If this is the only copy of the type in the APIs being compared,
        // its enclosed elements will not have been compared or reported, and
        // so will not be written out as a side effect of reporting the
        // comparison.  So, simulate the comparison of the enclosed members now,
        // to initialize various data structures, and write out files for
        // any nested classes and interfaces.
        APIMap<? extends Element> tMap = getElementMap(pageKey);
        if (tMap.size() == 1) {
            AccessKind accessKind = parent.options.getAccessKind();
            Map.Entry<API, ? extends Element> entry = tMap.entrySet().iterator().next();
            API api = entry.getKey();
            TypeElement te = (TypeElement) entry.getValue();
            for (Element e : te.getEnclosedElements()) {
                if (!accessKind.accepts(e)) {
                    continue;
                }
                if (e.getKind().isClass() || e.getKind().isInterface()) {
                    TypePageReporter r = (TypePageReporter) parent.getPageReporter(ElementKey.of(e));
                    r.writeFile(api, (TypeElement) e);
                }
                ElementKey eKey = ElementKey.of(e);
                Position ePos = Position.of(eKey);
                comparing(ePos, APIMap.of(api, e));
                completed(ePos, true);
            }
        }

        super.writeFile();
    }

    @Override
    protected String getTitle() {
        return new SignatureVisitor(apiMaps).getSignature(pageKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void comparing(Position pos, APIMap<?> apiMap) {
        super.comparing(pos, apiMap);
        if (pos.isElement() && pos.asElementKey() == pageKey) {
            this.tMap = (APIMap<? extends TypeElement>) apiMap;
        }
    }

    @Override
    protected Content buildSignature() {
        Position pagePos = Position.of(pageKey);
        List<Content> contents = new ArrayList<>();
        contents.addAll(buildAnnotations(pagePos));
        contents.add(new ModifiersBuilder().build(pagePos, tMap));
        contents.add(Text.SPACE);
        contents.add(buildKind());
        contents.add(Text.SPACE);
        contents.add(Text.of(pageKey.name));

        Content typarams = new TypeParameterBuilder().build(pagePos, tMap);
        if (typarams != Content.empty) {
            contents.add(typarams);
        }

        Content components = new RecordComponentBuilder().build(pagePos, tMap);
        if (components != Content.empty) {
            contents.add(components);
        }

        contents.add(Text.SPACE);

        Content superclass = buildSuperclass();
        if (superclass != Content.empty) {
            contents.add(superclass);
        }

        Content superinterfaces = buildSuperinterfaces();
        if (superinterfaces != Content.empty) {
            contents.add(superinterfaces);
        }

        Content permittedSubclasses = buildPermittedSubclasses();
        if (permittedSubclasses != Content.empty) {
            contents.add(permittedSubclasses);
        }

        return HtmlTree.DIV(contents).setClass("signature");
    }

    Content buildKind() {
        Position pos = Position.of(pageKey);
        if (differentKinds.containsKey(pos)) {
            return new DiffBuilder().build(tMap, te -> Keywords.of(te.getKind()));
        } else {
            TypeElement te = (TypeElement) tMap.values().iterator().next();
            return Keywords.of(te.getKind());
        }
    }

    private Content buildSuperclass() {
        boolean noSuperclasses = tMap.values().stream()
                .map(e -> ((TypeElement) e).getSuperclass())
                .allMatch(e -> e.getKind() == TypeKind.NONE);
        if (noSuperclasses) {
            return Content.empty;
        }

        Content superclass = buildType(Position.of(pageKey).superclass(),
                () -> ((TypeElement) tMap.values().iterator().next()).getSuperclass());
        return HtmlTree.DIV(Keywords.EXTENDS, Text.SPACE, superclass).setClass("superclass");
    }

    private Content buildSuperinterfaces() {
        boolean noSuperinterfaces = tMap.values().stream()
                .map(e -> ((TypeElement) e).getInterfaces())
                .allMatch(List::isEmpty);
        if (noSuperinterfaces) {
            return Content.empty;
        }

        List<Content> contents = new ArrayList<>();
        Set<Content> keywords = tMap.values().stream()
                .map(e -> {
                    return switch (e.getKind()) {
                        case CLASS, ENUM, RECORD -> Keywords.IMPLEMENTS;
                        case ANNOTATION_TYPE, INTERFACE -> Keywords.EXTENDS;
                        // for newer kinds, not supported by the default version of JDK
                        // used to compile apidiff, use a reflective comparison
                        default -> throw new IllegalStateException((e.getKind().toString()));
                    };
                })
                .collect(Collectors.toCollection(LinkedHashSet::new)); // preserve order of discovery
        if (keywords.size() == 1) {
            contents.add(keywords.iterator().next());
        } else {
            contents.add(new DiffBuilder().build(new ArrayList<>(keywords)));
        }
        contents.add(Text.SPACE);

        // build the map of all superinterfaces for the type
        Map<ElementKey, APIMap<TypeMirror>> allSuperinterfaces = new TreeMap<>();
        for (Map.Entry<API, ? extends Element> entry : tMap.entrySet()) {
            API api = entry.getKey();
            TypeElement te = (TypeElement) entry.getValue();
            for (TypeMirror tm : te.getInterfaces()) {
                Element i = api.getTypes().asElement(tm);
                allSuperinterfaces.computeIfAbsent(ElementKey.of(i), _t -> APIMap.of()).put(api, tm);
            }
        }

        // TODO: should this be a list? the mildly tricky part is adding the comma separator
        Set<API> apis = tMap.keySet();
        boolean needComma = false;
        for (Map.Entry<ElementKey, APIMap<TypeMirror>> entry : allSuperinterfaces.entrySet()) {
            ElementKey ek = entry.getKey();
            APIMap<TypeMirror> tMap = entry.getValue();
            if (needComma) {
                contents.add(Text.of(", "));
            } else {
                needComma = true;
            }
            // TODO: use factory method
            Position pos = Position.of(pageKey).superinterface(ek);
            contents.add(buildType(pos, apis, () -> tMap.values().iterator().next()));
        }

        return HtmlTree.DIV(contents).setClass("superinterfaces");
    }

    private Content buildPermittedSubclasses() {
        boolean noPermittedSubclasses = tMap.values().stream()
                .map(e -> elementExtras.getPermittedSubclasses((TypeElement) e))
                .allMatch(List::isEmpty);
        if (noPermittedSubclasses) {
            return Content.empty;
        }

        List<Content> contents = new ArrayList<>();
        contents.add(Keywords.PERMITS);
        contents.add(Text.SPACE);

        // build the map of all permitted subclasses for the type
        Map<ElementKey, APIMap<TypeMirror>> allPermittedSubclasses = new TreeMap<>();
        for (Map.Entry<API, ? extends Element> entry : tMap.entrySet()) {
            API api = entry.getKey();
            TypeElement te = (TypeElement) entry.getValue();
            for (TypeMirror tm : elementExtras.getPermittedSubclasses(te)) {
                Element i = api.getTypes().asElement(tm);
                allPermittedSubclasses.computeIfAbsent(ElementKey.of(i), _t -> APIMap.of()).put(api, tm);
            }
        }

        // TODO: should this be a list? the mildly tricky part is adding the comma separator
        Set<API> apis = tMap.keySet();
        boolean needComma = false;
        for (Map.Entry<ElementKey, APIMap<TypeMirror>> entry : allPermittedSubclasses.entrySet()) {
            ElementKey ek = entry.getKey();
            APIMap<TypeMirror> scMap = entry.getValue();
            if (needComma) {
                contents.add(Text.of(", "));
            } else {
                needComma = true;
            }
            // TODO: use factory method
            Position pos = Position.of(pageKey).permittedSubclass(ek);
            contents.add(buildType(pos, apis, scMap));
        }

        return HtmlTree.DIV(contents).setClass("permitted-subclasses");
    }

    private Content buildType(Position tPos, Supplier<TypeMirror> archetype) {
        APIMap<? extends TypeMirror> types;
        if ((types = differentTypes.get(tPos)) != null) {
            return new DiffBuilder().build(types, this::buildType);
        } else {
            TypeMirror tm = archetype.get();
            if (tm == null) {
                return Content.empty;
            }
            return buildType(tm);
        }
    }

    private Content buildType(Position tPos, Set<API> apis, Supplier<TypeMirror> archetype) {
        APIMap<? extends TypeMirror> types;
        if ((types = differentTypes.get(tPos)) != null) {
            return new DiffBuilder().build(apis, types, this::buildType);
        } else {
            TypeMirror tm = archetype.get();
            if (tm == null) {
                return Content.empty;
            }
            return buildType(tm);
        }
    }

    private Content buildType(Position tPos, Set<API> apis, APIMap<TypeMirror> tMap) {
        if (differentTypes.get(tPos) != null || tMap.size() < apis.size()) {
            return new DiffBuilder().build(apis, tMap, this::buildType);
        } else {
            return buildType(tMap.values().iterator().next());
        }
    }

    // TODO: maybe use shared TypeBuilder and handle null there
    private Content buildType(TypeMirror t) {
        return (t == null) ? Entity.NBSP : new TypeBuilder().build(t);
    }

    @Override
    protected List<Content> buildEnclosedElements() {
        List<Content> list = new ArrayList<>();
        addEnclosedElements(list, "heading.nested-types", ek -> ek.is(ElementKey.Kind.TYPE));
        addEnclosedElements(list, "heading.enum-constants", ek -> ek.is(ElementKind.ENUM_CONSTANT));
        addEnclosedElements(list, "heading.fields", ek -> ek.is(ElementKind.FIELD));
        addEnclosedElements(list, "heading.constructors", ek -> ek.is(ElementKind.CONSTRUCTOR));
        boolean isAnnoType = tMap.values().stream()
                .map(Element::getKind)
                .allMatch(k -> k == ElementKind.ANNOTATION_TYPE);
        String mTitle = isAnnoType ? "heading.elements" : "heading.methods";
        addEnclosedElements(list, mTitle, ek -> ek.is(ElementKind.METHOD));

        list.add(new SerializedFormBuilder().build());

        return list;
    }

    /**
     * Build content for an enclosed element.
     *
     * <p>If the element is an executable element or variable element, the content is generated
     * inline; if the element is a nested type, a link to the page for the type is generated.
     *
     * @param eKey the key for the enclosed element
     *
     * @return the content
     */
    @Override
    protected Content buildEnclosedElement(ElementKey eKey) {
        return switch (eKey.kind) {
            case TYPE -> super.buildEnclosedElement(eKey);
            case EXECUTABLE -> new ExecutableBuilder((ExecutableElementKey) eKey).build();
            case VARIABLE -> new VariableBuilder((VariableElementKey) eKey).build();
            default -> throw new IllegalArgumentException((eKey.toString()));
        };
    }

    /**
     * A builder for different instances of a set of modifiers in different instances of an API.
     */
    // TODO: display implicit modifiers in gray
    //       ... e.g. 'abstract' on interface, 'final' on enum declaration, etc.
    protected class ModifiersBuilder {
        private final Set<Modifier> allAccessMods = EnumSet.of(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE);

        public Content build(ElementKey eKey, APIMap<? extends Element> eMap) {
            return build(Position.of(eKey), eMap);
        }

        public Content build(Position ePos, APIMap<? extends Element> eMap) {
            if (differentModifiers.containsKey(ePos)) {
                // full build, with differences
                return build(eMap);
            } else {
                // fast-track build, since all are the same
                List<Content> contents = new ArrayList<>();
                Element e = eMap.values().iterator().next();
                simpleAddModifiers(contents, e.getModifiers());
                return wrap(contents);
            }
        }

        public Content build(APIMap<? extends Element> eMap) {
            List<Content> contents = new ArrayList<>();

            if (eMap.size() == 1) {
                simpleAddModifiers(contents, eMap.values().iterator().next().getModifiers());
            } else {
                addAccessModifiers(contents, eMap);

                for (Modifier m : Modifier.values()) {
                    switch (m) {
                        case PUBLIC:
                        case PROTECTED:
                        case PRIVATE:
                        case NATIVE:
                        case SYNCHRONIZED:
                            break;

                        default:
                            addModifier(contents, m, eMap);
                    }
                }
            }

            return wrap(contents);
        }

        void simpleAddModifiers(List<Content> content, Set<Modifier> mods) {
            for (Modifier m : mods) {
                switch (m) {
                    case NATIVE:
                    case SYNCHRONIZED:
                        continue;
                }

                if (!content.isEmpty()) {
                    content.add(Text.SPACE);
                }
                content.add(Keywords.of(m));
            }
        }


        void addAccessModifiers(List<Content> content, APIMap<? extends Element> eMap) {
            // deal with the access modifiers as a group of which at most one may be set
            boolean allEqual = true;
            Set<Modifier> accessMods = null;
            for (Element e : eMap.values()) {
                Set<Modifier> mods;
                if (e.getModifiers().isEmpty()) {
                    mods = Collections.emptySet();
                } else {
                    mods = EnumSet.copyOf(e.getModifiers());
                    mods.retainAll(allAccessMods);
                }
                if (accessMods == null) {
                    accessMods = mods;
                } else {
                    if (!mods.equals(accessMods)) {
                        allEqual = false;
                        break;
                    }
                }
            }
            assert accessMods != null;

            if (allEqual) {
                for (Modifier m : allAccessMods) {
                    if (accessMods.contains(m)) {
                        content.add(Keywords.of(m));
                        break;
                    }
                }
            } else {
                APIMap<Content> diffs = APIMap.of();
                for (Map.Entry<API, ? extends Element> entry : eMap.entrySet()) {
                    API api = entry.getKey();
                    Element e = entry.getValue();
                    Set<Modifier> eMods = e.getModifiers();
                    Modifier m = null;
                    for (Modifier am : allAccessMods) {
                        if (eMods.contains(am)) {
                            m = am;
                            break;
                        }
                    }
                    diffs.put(api, (m != null) ? Keywords.of(m) : Entity.NBSP);
                }
                content.add(new DiffBuilder().build(diffs));
            }
        }

        void addModifier(List<Content> content, Modifier m, APIMap<? extends Element> eMap) {
            boolean allEqual = true;
            Boolean present = null;
            for (Element e : eMap.values()) {
                boolean b = e.getModifiers().contains(m);
                if (present == null) {
                    present = b;
                } else {
                    if (b != present) {
                        allEqual = false;
                        break;
                    }
                }
            }
            assert present != null;

            if (allEqual) {
                if (present) {
                    if (!content.isEmpty()) {
                        content.add(Text.SPACE);
                    }
                    content.add(Keywords.of(m));
                }
            } else {
                if (!content.isEmpty()) {
                    content.add(Text.SPACE);
                }
                content.add(new DiffBuilder().build(eMap, e ->
                        (e.getModifiers().contains(m)) ? Keywords.of(m) : Entity.NBSP));
            }
        }

        private Content wrap(List<Content> content) {
            return HtmlTree.SPAN(content).setClass("modifiers");
        }

    }

    private class ExecutableBuilder {
        private final Position ePos;

        ExecutableBuilder(ExecutableElementKey eKey) {
            this.ePos = Position.of(eKey);
        }

        ExecutableBuilder(Position ePos) {
            this.ePos = ePos;
        }

        Content build() {
            Content eq = getResultGlyph(ePos);

            // TODO: could move to final field
            APIMap<? extends Element> eMap = getElementMap(ePos);
            // by design, they should all have the same ElementKind,
            // so pick the first
            Element e = eMap.values().iterator().next();
            ElementKind eKind = e.getKind();

            List<Content> contents = new ArrayList<>();
            contents.addAll(buildAnnotations(ePos));
            contents.add(new ModifiersBuilder().build(ePos, eMap));
            contents.add(Text.SPACE);

            Content typarams = new TypeParameterBuilder().build(ePos, eMap);
            if (typarams != Content.empty) {
                contents.add(typarams);
                contents.add(Text.SPACE);
            }

            switch (eKind) {
                case CONSTRUCTOR ->
                    // no return type
                    contents.add(Text.of(pageKey.name.toString()));

                case METHOD -> {
                    contents.add(buildType(ePos.returnType(),
                            ((ExecutableElement) e)::getReturnType));
                    contents.add(Text.SPACE);
                    contents.add(Text.of(e.getSimpleName()));
                }

                default -> throw new IllegalStateException(eKind.toString());
            }

            contents.add(Text.of("("));
            Content parameters = buildParameters(ePos);
            if (parameters != Content.empty) {
                contents.add(parameters);
            }
            contents.add(Text.of(")"));

            Content defaultValue = buildDefaultValue(ePos);
            if (defaultValue != Content.empty) {
                contents.add(Text.SPACE);
                contents.add(Keywords.DEFAULT);
                contents.add(Text.SPACE);
                contents.add(defaultValue);
            }

            Content throwsTypes = buildThrows(ePos);
            if (throwsTypes != Content.empty) {
                contents.add(Text.SPACE);
                contents.add(throwsTypes);
            }

            HtmlTree signature = HtmlTree.DIV(contents).setClass("signature");
            Content docComments = buildDocComments(ePos);
            Content apiDescriptions = buildAPIDescriptions(ePos);

            return HtmlTree.DIV(eq, buildMissingInfo(ePos), buildNotes(ePos),
                    signature, docComments, apiDescriptions)
                    .setClass("element")
                    .set(HtmlAttr.ID, links.getId(ePos));
        }

        private Content buildParameters(Position ePos) {
            APIMap<? extends Element> eMap = getElementMap(ePos);

            boolean noReceiverAnnos = eMap.entrySet().stream()
                    .map(e -> {
                        API api = e.getKey();
                        ExecutableElement ee = (ExecutableElement) e.getValue();
                        TypeMirror tm = ee.getReceiverType();
                        // The following conditional expression is a workaround;
                        // The spec says that NoType should be returned instead of null.
                        return tm == null ? api.getTypes().getNoType(TypeKind.NONE) : tm;
                    })
                    .map(TypeMirror::getAnnotationMirrors)
                    .allMatch(List::isEmpty);

            int parameterCount = eMap.values().stream()
                    .mapToInt(e -> ((ExecutableElement) e).getParameters().size())
                    .max()
                    .orElse(0);

            if (noReceiverAnnos && parameterCount == 0) {
                return Content.empty;
            }

            List<Content> contents = new ArrayList<>();
            boolean needComma = false;
            if (!noReceiverAnnos) {
                contents.add(todo("receiver"));
                needComma = true;
            }

            if (parameterCount > 0) {
                IntTable<VariableElement> pTable = new IntTable<>();
                eMap.forEach((api, e) -> pTable.put(api, ((ExecutableElement) e).getParameters()));
                for (int i = 0; i < parameterCount; i++) {
                    Position pPos = ePos.parameter(i);
                    Content parameter = buildParameter(pPos, pTable.entries(i));
                    if (needComma) {
                        contents.add(Text.of(", "));
                    }
                    contents.add(parameter);
                    needComma = true;
                }
            }
            return HtmlTree.SPAN(contents).setClass("parameters");
        }

        Content buildParameter(Position vPos, APIMap<VariableElement> vMap) {
            List<Content> contents = new ArrayList<>();
            contents.addAll(buildAnnotations(vPos));

            VariableElement ve = vMap.values().iterator().next();
            // no modifiers for parameters
            contents.add(buildType(vPos, ve::asType));
            contents.add(Text.SPACE);

            Name vName = ve.getSimpleName();
            boolean sameName = vMap.values().stream()
                    .map(Element::getSimpleName)
                    .allMatch(n -> n.contentEquals(vName));
            if (sameName) {
                contents.add(Text.of(vName));
            } else {
                contents.add(new DiffBuilder().build(vMap, e -> Text.of(e.getSimpleName())));
            }

            return HtmlTree.SPAN(contents).setClass("parameter");
        }

        private Content buildThrows(Position ePos) {
            APIMap<? extends Element> eMap = getElementMap(ePos);
            boolean noThrows = eMap.values().stream()
                    .map(e -> ((ExecutableElement) e).getThrownTypes())
                    .allMatch(List::isEmpty);
            if (noThrows) {
                return Content.empty;
            }

            // TODO: use latest apiMaps
            // build the map of all throws for the executable
            Map<TypeMirrorKey, APIMap<TypeMirror>> allThrows = new TreeMap<>();
            for (Map.Entry<API, ? extends Element> entry : eMap.entrySet()) {
                API api = entry.getKey();
                ExecutableElement ee = (ExecutableElement) entry.getValue();
                for (TypeMirror tm : ee.getThrownTypes()) {
                    allThrows.computeIfAbsent(TypeMirrorKey.of(tm), _t -> APIMap.of()).put(api, tm);
                }
            }

            // TODO: should this be a list? the mildly tricky part is adding the comma separator
            Set<API> apis = eMap.keySet();
            List<Content> contents = new ArrayList<>();
            boolean needComma = false;
            for (Map.Entry<TypeMirrorKey, APIMap<TypeMirror>> entry : allThrows.entrySet()) {
                TypeMirrorKey tmk = entry.getKey();
                APIMap<TypeMirror> tMap = entry.getValue();
                if (needComma) {
                    contents.add(Text.of(", "));
                } else {
                    needComma = true;
                }
                // TODO: use factory method
                Position pos = ePos.exception(tmk);
                contents.add(buildType(pos, apis, () -> tMap.values().iterator().next()));
            }
            return HtmlTree.SPAN(Keywords.THROWS, Text.SPACE).setClass("throws").add(contents);
        }

        private Content buildDefaultValue(Position ePos) {
            APIMap<? extends Element> eMap = getElementMap(ePos);
            boolean noDefaultValues = eMap.values().stream()
                    .map(e -> ((ExecutableElement) e).getDefaultValue())
                    .allMatch(Objects::isNull);
            if (noDefaultValues) {
                return Content.empty;
            }
            List<Content> contents = new ArrayList<>();
            RelativePosition<?> dvPos = ePos.defaultValue();
            contents.addAll(new AnnotationValueBuilder().buildAnnoValue(dvPos));
            return HtmlTree.SPAN(contents).setClass("defaultValue");
        }
    }

    private class VariableBuilder {

        private final VariableElementKey vKey;
        private final Position vPos;

        VariableBuilder(VariableElementKey vKey) {
            this.vKey = vKey;
            vPos = Position.of(vKey);
        }

        private Content build() {
            Content eq = getResultGlyph(vPos);

            APIMap<? extends Element> vMap = getElementMap(vKey);
            // by design, they should all have the same ElementKind,
            // so pick the first
            VariableElement ve = (VariableElement) vMap.values().iterator().next();
            ElementKind eKind = ve.getKind();

            List<Content> contents = new ArrayList<>();
            contents.addAll(buildAnnotations(vPos));

            switch (eKind) {
                case ENUM_CONSTANT ->
                    // no modifiers type needed
                    contents.add(Text.of(ve.getSimpleName()));

                case FIELD -> {
                    contents.add(new ModifiersBuilder().build(vKey, vMap));
                    contents.add(Text.SPACE);
                    contents.add(buildType(vPos, ve::asType));
                    contents.add(Text.SPACE);
                    contents.add(Text.of(ve.getSimpleName()));
                    Content value = buildValue(vMap);
                    if (value != Content.empty) {
                        contents.add(Text.of(" = "));
                        contents.add(value);
                    }
                }
            }

            HtmlTree signature = HtmlTree.DIV(contents).setClass("signature");
            Content docComments = buildDocComments(vPos);
            Content apiDescriptions = buildAPIDescriptions(vPos);

            return HtmlTree.DIV(eq, buildMissingInfo(vPos), buildNotes(vKey),
                    signature, docComments, apiDescriptions)
                    .setClass("element")
                    .set(HtmlAttr.ID, links.getId(vKey));
        }

        private Content buildValue(APIMap<? extends Element> vMap) {
            Object v = ((VariableElement) vMap.values().iterator().next()).getConstantValue();
            boolean allEqual = vMap.values().stream()
                    .map(e -> ((VariableElement) e).getConstantValue())
                    .allMatch(v1 -> Objects.equals(v1, v));
            if (allEqual) {
                return (v == null) ? Content.empty : buildValue(v);
            } else {
                APIMap<Content> values = APIMap.of();
                vMap.forEach((api, ve) -> values.put(api, buildValue(((VariableElement) ve).getConstantValue())));
                return new DiffBuilder().build(values);
            }
        }

        private Content buildValue(Object o) {
            if (o == null) {
                return Entity.NBSP;
            } else if (o instanceof Number) {
                return Text.of(String.valueOf(o));
            } else if (o instanceof Boolean) {
                return Keywords.of((Boolean) o);
            } else if (o instanceof Character) {
                return Text.of("'" + escape(String.valueOf(o)) + "'");
            } else if (o instanceof String) {
                return Text.of("\"" + escape(String.valueOf(o)) + "\"");
            } else {
                throw new IllegalArgumentException(o.getClass() + "(" + o + ")");
            }
        }

        private String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isSurrogate(c)) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    switch (c) {
                        case '\b':
                            sb.append("\\b");
                            break;
                        case '\f':
                            sb.append("\\f");
                            break;
                        case '\n':
                            sb.append("\\n");
                            break;
                        case '\r':
                            sb.append("\\r");
                            break;
                        case '\t':
                            sb.append("\\t");
                            break;
                        case '\\':
                            sb.append("\\\\");
                            break;
                        case '\'':
                            sb.append("\\'");
                            break;
                        case '\"':
                            sb.append("\\\"");
                            break;
                        default:
                            if (Character.isISOControl(c)) {
                                sb.append(String.format("\\u%04x", (int) c));
                            } else {
                                sb.append(c);
                            }
                    }
                }
            }
            return sb.toString();
        }
    }


    /**
     * A builder for different instances of a type parameter in different instances of an API.
     */
    protected class TypeParameterBuilder {
        protected Content build(Position ePos, APIMap<? extends Element> eMap) {
            int maxTypeParameters = eMap.values().stream()
                    .mapToInt(e -> ((Parameterizable) e).getTypeParameters().size())
                    .max().orElse(0);
            if (maxTypeParameters == 0) {
                return Content.empty;
            }

            // use elements from this list when no differences given
            List<? extends TypeParameterElement> archetype =
                    ((Parameterizable) eMap.values().iterator().next()).getTypeParameters();

            List<Content> contents = new ArrayList<>();
            contents.add(Text.of("<"));
            boolean needComma = false;
            for (int i = 0; i < maxTypeParameters; i++) {
                if (needComma) {
                    contents.add(Text.of(", "));
                } else {
                    needComma = true;
                }

                Position tpePos = ePos.typeParameter(i);
                APIMap<? extends TypeParameterElement> typarams;
                if ((typarams = differentTypeParameters.get(tpePos)) != null) {
                    contents.add(new DiffBuilder().build(eMap.keySet(), typarams, this::buildTypeParameter));
                } else {
                    contents.add(buildTypeParameter(archetype.get(i)));
                }
            }
            contents.add(Text.of(">"));
            return HtmlTree.SPAN(contents).setClass("typarams");
        }

        private Content buildTypeParameter(TypeParameterElement tpe) {
            List<Content> contents = new ArrayList<>();
            // TODO: annotations
            contents.add(Text.of(tpe.getSimpleName()));
            // TODO: bounds
            return HtmlTree.SPAN(contents);
        }
    }

    /**
     * A builder for different instances of the record components in different instances of an API.
     */
    protected class RecordComponentBuilder {
        protected Content build(Position ePos, APIMap<? extends TypeElement> eMap) {
            int maxComponents = eMap.values().stream()
                    .mapToInt(e -> getComponents(e).size())
                    .max().orElse(0);
            if (maxComponents == 0) {
                return Content.empty;
            }

            IntTable<Element> rcTable = new IntTable<>();
            eMap.forEach((api, e) -> rcTable.put(api, getComponents(e)));
            List<Content> contents = new ArrayList<>();
            contents.add(Text.of("("));
            boolean needComma = false;
            for (int i = 0; i < maxComponents; i++) {
                Position rcPos = ePos.recordComponent(i);
                Content component = buildComponent(rcPos, eMap.keySet(), rcTable.entries(i));
                if (needComma) {
                    contents.add(Text.of(", "));
                }
                contents.add(component);
                needComma = true;
            }
            contents.add(Text.of(")"));
            return HtmlTree.SPAN(contents).setClass("components");
        }

        private Content buildComponent(Position rcPos, Set<API> apis, APIMap</*RecordComponent*/Element> rcMap) {
            List<Content> contents = new ArrayList<>();
            contents.addAll(buildAnnotations(rcPos));

            // no modifiers for record components
            APIMap<TypeMirror> tMap = rcMap.map(Element::asType);
            contents.add(buildType(rcPos, apis, tMap));
            contents.add(Text.SPACE);

            Name rcn = rcMap.values().iterator().next().getSimpleName();
            boolean sameName = rcMap.size() == apis.size() && rcMap.values().stream()
                    .map(Element::getSimpleName)
                    .allMatch(n -> n.contentEquals(rcn));
            if (sameName) {
                contents.add(Text.of(rcn));
            } else {
                contents.add(new DiffBuilder().build(apis, rcMap, e -> Text.of(e.getSimpleName())));
            }

            return HtmlTree.SPAN(contents).setClass("parameter");
        }

        private List<? extends Element> getComponents(TypeElement e) {
            return elementExtras.getRecordComponents(e);
        }
    }

    /**
     * A builder for different instances of the serialized form of a type element.
     */
     private class SerializedFormBuilder {

        Content build() {
            Position sfPos = Position.of(pageKey).serializedForm();
            Boolean equal = results.get(sfPos);
            if (equal == null) {
                return Content.empty;
            }

            HtmlTree section = HtmlTree.SECTION(HtmlTree.H2(Text.of(msgs.getString("serial.serialized-form")))).setClass("serial-form");
            section.add(getResultGlyph(sfPos)).add(buildMissingInfo(sfPos));
            addSerialVersionUID(section);
            addSerializedFields(section);
            addSerializationMethods(section);
            return section;
        }

        private void addSerialVersionUID(HtmlTree section) {
            Position uPos = Position.of(pageKey).serialVersionUID();
            APIMap<?> values = apiMaps.get(uPos);

            // TODO: weave in the text from serialized-form.html

            section.add(HtmlTree.H3(Text.of("serialVersionUID")));
            section.add(getResultGlyph(uPos));
            if (differentValues.containsKey(uPos)) {
                APIMap<Content> alternatives = APIMap.of();
                values.forEach((api, v) -> alternatives.put(api, Text.of(String.valueOf(v))));
                section.add(new DiffBuilder().build(alternatives));
            } else {
                section.add(Text.of(String.valueOf(values.values().iterator().next())));
            }

// TODO: The serialVersion info appears in two places, which should provide the same value.
//    It should appear in the source or class file, and it should appear in the serialized form,
//    available via the SerializedFormDocs object

//            Content c = buildAPIDescriptions(uPos);
//            if (c != Content.empty) {
//                // TODO: show the check/cross/etc glyph if any description present
//                //       implies need for reporter.comparing(oPos, map) with a map of descriptions
//                section.add(c);
//            }
        }

        private void addSerializedFields(HtmlTree tree) {
            RelativePosition<?> oPos = Position.of(pageKey).serializationOverview();
            Content c = buildAPIDescriptions(oPos);
            if (c != Content.empty) {
                // TODO: show the check/cross/etc glyph if any overview present
                //       implies need for reporter.comparing(oPos, map) with a map of descriptions
                HtmlTree section = HtmlTree.SECTION().setClass("enclosed");
                section.add(HtmlTree.H3(Text.of(msgs.getString("serial.serialization-overview"))));
                section.add(c);
                tree.add(section);
            }

            Set<RelativePosition<?>> fields = results.keySet().stream()
                    .filter(Position::isRelative)
                    .map(p -> (RelativePosition<?>) p)
                    .filter(p -> p.kind == RelativePosition.Kind.SERIALIZED_FIELD)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(Position.stringIndexComparator)));

            if (!fields.isEmpty()) {
                HtmlTree section = HtmlTree.SECTION().setClass("enclosed");
                section.add(HtmlTree.H3(Text.of(msgs.getString("serial.serialized-fields"))));
                HtmlTree ul = HtmlTree.UL();
                for (RelativePosition<?> pos : fields) {
                    @SuppressWarnings("unchecked")
                    RelativePosition<String> fPos = (RelativePosition<String>) pos;
                    HtmlTree li = HtmlTree.LI(buildSerializedField(fPos));
                    ul.add(li);
                }
                section.add(ul);
                tree.add(section);
            }
        }

        private Content buildSerializedField(RelativePosition<String> fPos) {
            @SuppressWarnings("unchecked")
            APIMap<SerializedForm.Field> fMap = (APIMap<SerializedForm.Field>) apiMaps.get(fPos);
            Content glyph = getResultGlyph(fPos);

            Content type;
            APIMap<? extends TypeMirror> types;
            // differentTypes for a serializedField position is somewhat different than usual
            // in that a type of NONE represents an unresolved type
            // (Ideally, the representation would use ERROR instead of NONE, but that is not possible.)
            if ((types = differentTypes.get(fPos)) != null) {
                if (types.values().stream().allMatch(t -> t.getKind() != TypeKind.NONE)) {
                    // no unresolved types, use standard type builder
                    type = buildType(fPos, () -> types.values().iterator().next());
                } else {
                    // some unresolved types, use custom DiffBuilder
                    type = new DiffBuilder().build(fMap, this::buildFieldType);
                }
            } else {
                // types all equal: build the archetype
                SerializedForm.Field archetype = fMap.values().iterator().next();
                type = buildFieldType(archetype);
            }

            Content signature = HtmlTree.DIV(type, Entity.NBSP, Text.of(fPos.index)).setClass("signature");
            Content docComments = buildDocComments(fPos);
            Content descriptions = buildAPIDescriptions(fPos);

            return HtmlTree.DIV(glyph, buildMissingInfo(fPos),
                    signature, docComments, descriptions)
                    .setClass("element");
        }

        private Content buildFieldType(SerializedForm.Field f) {
            TypeMirror t = f.getType();
            if (t.getKind() == TypeKind.NONE) {
                return HtmlTree.SPAN(Text.of(f.getSignature()))
                        .setClass("unresolved")
                        .setTitle("name could not be resolved"); // TODO: L10N
            } else {
                return TypePageReporter.this.buildType(t);
            }
        }

        private void addSerializationMethods(HtmlTree tree) {
            Set<RelativePosition<?>> methods = results.keySet().stream()
                    .filter(p -> p instanceof RelativePosition)
                    .map(p -> (RelativePosition<?>) p)
                    .filter(p -> p.kind == RelativePosition.Kind.SERIALIZATION_METHOD)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(Position.stringIndexComparator)));

            if (!methods.isEmpty()) {
                HtmlTree section = HtmlTree.SECTION().setClass("enclosed");
                section.add(HtmlTree.H3(Text.of(msgs.getString("serial.serialization-methods"))));
                HtmlTree ul = HtmlTree.UL();
                for (Position pos : methods) {
                    HtmlTree li = HtmlTree.LI(buildSerializedMethod(pos));
                    ul.add(li);
                }
                section.add(ul);
                tree.add(section);
            }
        }

        private Content buildSerializedMethod(Position mPos) {
            return new ExecutableBuilder(mPos).build();
        }

    }
}

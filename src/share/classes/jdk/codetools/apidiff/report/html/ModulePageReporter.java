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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.Entity;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.RelativePosition;

/**
 * A reporter that generates an HTML page for the differences in
 * a module declaration.
 */
class ModulePageReporter extends PageReporter<ModuleElementKey> {
    APIMap<? extends Element> mMap = null;
    private final boolean allDirectiveDetails;

    ModulePageReporter(HtmlReporter parent, ElementKey mKey) {
        super(parent, (ModuleElementKey) mKey);

        allDirectiveDetails = parent.options.getAccessKind().allModuleDetails();
    }

    @Override
    protected String getTitle() {
        return "module " + pageKey.name;
    }

    @Override
    protected Content buildSignature() {
        List<Content> contents = new ArrayList<>();
        contents.addAll(buildAnnotations(Position.of(pageKey)));
        contents.add(buildModifiers());
        contents.add(Text.SPACE);
        contents.add(Keywords.MODULE);
        contents.add(Text.SPACE);
        contents.add(Text.of(pageKey.name));
        return HtmlTree.DIV(contents).setClass("signature");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void comparing(Position pos, APIMap<?> apiMap) {
        super.comparing(pos, apiMap);
        if (pos.isElement()) {
            this.mMap = (APIMap<? extends Element>) apiMap;
        }
    }

// TODO: check if required
//    /**
//     * Writes a page containing details for a single module element in a given API.
//     *
//     * @param api the API containing the element
//     * @param mdle the module element
//     */
//    void writeFile(API api, ModuleElement mdle) {
//        Position pagePos = Position.of(pageKey);
//        APIMap<ModuleElement> apiMap = APIMap.of(api, mdle);
//        comparing(pagePos, apiMap);
//        parent.apis.stream()
//                .filter(a -> a != api)
//                .forEach(a -> reportMissing(pagePos, a));
//        completed(pagePos, true);
//    }

    @Override
    protected void writeFile() {
        // If this is the only copy of the module in the APIs being compared,
        // its enclosed elements will not have been compared or reported, and
        // so will not be written out as a side effect of reporting the
        // comparison.  So, write the files for the enclosed packages now.
        if (mMap.size() == 1) {
            Map.Entry<API, ? extends Element> e = mMap.entrySet().iterator().next();
            API api = e.getKey();
            ModuleElement mdle = (ModuleElement) e.getValue();
            for (PackageElement pkg : api.getPackageElements(mdle)) {
                PackagePageReporter r = (PackagePageReporter) parent.getPageReporter(ElementKey.of(pkg));
                r.writeFile(api, pkg);
            }
        }

        super.writeFile();
    }

    private Content buildModifiers() {
        Position pos = Position.of(pageKey);
        if (differentModifiers.containsKey(pos)) {
            return new DiffBuilder().build(mMap,
                    me -> ((ModuleElement) me).isOpen() ? Keywords.OPEN : Entity.NBSP);
        } else {
            ModuleElement me = (ModuleElement) mMap.values().iterator().next();
            return me.isOpen() ? Keywords.OPEN : Content.empty;
        }
    }

    @Override
    protected List<Content> buildEnclosedElements() {
        List<Content> list = new ArrayList<>();
        addDirectives(list, "heading.exports", rp -> rp.kind == RelativePosition.Kind.MODULE_EXPORTS, this::buildExports);
        addDirectives(list, "heading.opens", rp -> rp.kind == RelativePosition.Kind.MODULE_OPENS, this::buildOpens);
        addDirectives(list, "heading.requires", rp -> rp.kind == RelativePosition.Kind.MODULE_REQUIRES, this::buildRequires);
        addDirectives(list, "heading.provides", rp -> rp.kind == RelativePosition.Kind.MODULE_PROVIDES, this::buildProvides);
        addDirectives(list, "heading.uses", rp -> rp.kind == RelativePosition.Kind.MODULE_USES, this::buildUses);
        addEnclosedElements(list, "heading.packages", ek -> ek.kind == ElementKey.Kind.PACKAGE);
        addDocFiles(list);
        return list;
    }

    private <T> void addDirectives(List<Content> contents,
                               String headingKey,
                               Predicate<RelativePosition<?>> filter,
                               BiFunction<RelativePosition<?>, APIMap<T>, ContentAndResultKind> f) {
        Map<RelativePosition<?>, APIMap<T>> dMaps = new TreeMap<>(RelativePosition.elementKeyIndexComparator);
        // apiMaps will only contain maps for directives which should be compared and displayed;
        // i.e. they have already been filtered according to accessKind.allDirectiveDetails
        for (Map.Entry<Position, APIMap<?>> e : apiMaps.entrySet()) {
            Position p = e.getKey();
            if (p instanceof RelativePosition) {
                RelativePosition<?> rp = (RelativePosition<?>) p;
                if (filter.test((RelativePosition<?>) p)) {
                    @SuppressWarnings("unchecked")
                    APIMap<T> dMap = (APIMap<T>) e.getValue();
                    dMaps.put(rp, dMap);
                }
            }
        }

        List<ContentAndResultKind> converted = dMaps.entrySet().stream().map(e -> f.apply(e.getKey(), e.getValue())).toList();

        if (!converted.isEmpty()) {
            boolean allUnchanged = converted.stream().allMatch(c -> c.resultKind() == ResultKind.SAME);
            HtmlTree section = HtmlTree.SECTION().setClass("enclosed");
            section.add(HtmlTree.H2(Text.of(msgs.getString(headingKey))));
            HtmlTree ul = HtmlTree.UL();
            for (ContentAndResultKind c : converted) {
                HtmlTree item = HtmlTree.LI(c.content());
                if (!allUnchanged && c.resultKind() == ResultKind.SAME) {
                    item.setClass("unchanged");
                }
            }
            converted.forEach(c -> ul.add());
            section.add(ul);
            if (allUnchanged) {
                section = HtmlTree.DIV(section).setClass("unchanged");
            }
            contents.add(section);
        }
    }

    private ContentAndResultKind buildExports(RelativePosition<?> rPos, APIMap<ExportsDirective> apiMap) {
        return buildExportsOpensProvides(rPos, apiMap, Keywords.EXPORTS, Keywords.TO,
                ExportsDirective::getPackage, ExportsDirective::getTargetModules);
    }

    private ContentAndResultKind buildOpens(RelativePosition<?> rPos, APIMap<OpensDirective> apiMap) {
        return buildExportsOpensProvides(rPos, apiMap, Keywords.OPENS, Keywords.TO,
                OpensDirective::getPackage, OpensDirective::getTargetModules);
    }

    private ContentAndResultKind buildProvides(RelativePosition<?> rPos, APIMap<ProvidesDirective> apiMap) {
        // ProvidesDirective is unusual in that part of it (i.e. the implementations)
        // is not part of the public API, and should only be displayed if allDirectiveDetails
        // is true.
        return buildExportsOpensProvides(rPos, apiMap, Keywords.PROVIDES, Keywords.WITH,
                ProvidesDirective::getService,
                pd -> allDirectiveDetails ? pd.getImplementations() : Collections.emptyList());
    }

    private ContentAndResultKind buildUses(RelativePosition<?> rPos, APIMap<UsesDirective> apiMap) {
        return buildExportsOpensProvides(rPos, apiMap, Keywords.USES, Content.empty,
                UsesDirective::getService, d -> Collections.emptyList());
    }

    private <T extends Directive, U extends Element, V extends Element>
    ContentAndResultKind buildExportsOpensProvides(RelativePosition<?> rPos, APIMap<T> apiMap,
                                      Content directiveKeyword, Content sublistKeyword,
                                      Function<T, U> getPrimaryElement,
                                      Function<T, List<V>> getSecondaryElements) {
        List<Content> contents = new ArrayList<>();

        contents.add(directiveKeyword);
        contents.add(Text.SPACE);

        T archetype = apiMap.values().stream().filter(Objects::nonNull).findFirst().orElseThrow();
        contents.add(getName(ElementKey.of(getPrimaryElement.apply(archetype))));

        APIMap<Set<ElementKey>> targetKeys = APIMap.of();
        Set<ElementKey> allTargetKeys = new TreeSet<>();
        apiMap.forEach((api, d) -> {
            List<V> targets = getSecondaryElements.apply(d);
            if (targets != null) {
                Set<ElementKey> s = targets.stream()
                        .map(ElementKey::of)
                        .collect(Collectors.toCollection(TreeSet::new));
                targetKeys.put(api, s);
                allTargetKeys.addAll(s);
            }
        });

        if (!allTargetKeys.isEmpty()) {
            contents.add(Text.SPACE);
            contents.add(sublistKeyword);
            contents.add(Text.SPACE);
            boolean first = true;
            for (ElementKey ek : allTargetKeys) {
                if (first) {
                    first = false;
                } else {
                    contents.add(Text.of(", "));
                }
                boolean allMatch = targetKeys.values().stream().allMatch(s -> s.contains(ek));
                if (allMatch) {
                    contents.add(getName(ek));
                } else {
                    APIMap<Content> map = APIMap.of();
                    targetKeys.forEach((api, set) -> map.put(api, set.contains(ek) ? getName(ek) : Entity.NBSP));
                    contents.add(new DiffBuilder().build(map));
                }
            }
        }

        // TODO: for now, this is stylistically similar to buildEnclosedElement,
        //       but arguably a better way would be to move code for the check or cross into
        //       the enclosing loop that builds the list.

        ResultKind result = getResultGlyph(rPos);

        return new ContentAndResultKind(HtmlTree.SPAN(result.getContent(), Text.SPACE)
                .add(HtmlTree.SPAN(contents).setClass("signature")), result);
    }

    private Content getName(ElementKey ek) {
        return Text.of(getQualifiedName(ek));
    }

    private CharSequence getQualifiedName(ElementKey ek) {
        switch (ek.kind) {
            case MODULE:
                return ((ModuleElementKey) ek).name;
            case PACKAGE:
                return ((PackageElementKey) ek).name;
            case TYPE:
                TypeElementKey tek = (TypeElementKey) ek;
                return tek.enclosingKey == null ? tek.name : getQualifiedName(tek.enclosingKey) + "." + tek.name;
            default:
                throw new IllegalArgumentException((ek.toString()));
        }
    }

    private ContentAndResultKind buildRequires(RelativePosition<?> rPos, APIMap<RequiresDirective> apiMap) {
        List<Content> contents = new ArrayList<>();

        contents.add(Keywords.REQUIRES);
        contents.add(Text.SPACE);

        // TODO: would this be a useful method on APIMap?
        RequiresDirective archetype = apiMap.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();

        boolean allStaticEqual = apiMap.values().stream()
                .allMatch(rd -> rd != null && rd.isStatic() == archetype.isStatic());
        if (allStaticEqual) {
            if (archetype.isStatic()) {
                contents.add(Keywords.STATIC);
                contents.add(Text.SPACE);
            }
        } else {
            APIMap<Content> alternatives = APIMap.of();
            apiMap.forEach((api, rd) -> {
                Content kw = rd != null && rd.isStatic() ? Keywords.STATIC : Entity.NBSP;
                alternatives.put(api, kw);
            });
            contents.add(new DiffBuilder().build(alternatives));
            contents.add(Text.SPACE);
        }

        boolean allTransitiveEqual = apiMap.values().stream()
                .allMatch(rd -> rd != null && rd.isTransitive() == archetype.isTransitive());
        if (allTransitiveEqual) {
            if (archetype.isTransitive()) {
                contents.add(Keywords.TRANSITIVE);
                contents.add(Text.SPACE);
            }
        } else {
            APIMap<Content> alternatives = APIMap.of();
            apiMap.forEach((api, rd) -> {
                Content kw = rd != null && rd.isTransitive() ? Keywords.TRANSITIVE : Entity.NBSP;
                alternatives.put(api, kw);
            });
            contents.add(new DiffBuilder().build(alternatives));
            contents.add(Text.SPACE);
        }

        // TODO: would be nice to link to the module page if it can be determined to be available.
        contents.add(Text.of(archetype.getDependency().getQualifiedName()));

        // TODO: for now, this is stylistically similar to buildEnclosedElement,
        //       but arguably a better way would be to move code for the check or cross into
        //       the enclosing loop that builds the list.
        ResultKind result = getResultGlyph(rPos);

        return new ContentAndResultKind(HtmlTree.SPAN(result.getContent(), Text.SPACE)
                .add(HtmlTree.SPAN(contents).setClass("signature")), result);
    }
}

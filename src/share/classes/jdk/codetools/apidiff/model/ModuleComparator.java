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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.UsesTree;
import com.sun.source.util.DocTreeScanner;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for {@link ModuleElement module elements}.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the annotations of the module
 *     <li>the modifiers of the module
 *     <li>the directives in the module
 *     <li>the documentation comment for the module
 *     <li>the selected packages in the module
 * </ul>
 */
public class ModuleComparator extends ElementComparator<ModuleElement> {

    /**
     * Creates a comparator to compare module elements across a set of APIs.
     *
     * @param apis the set of APIs
     * @param options the command-line options
     * @param reporter the reporter to which to report differences
     */
    public ModuleComparator(Set<API> apis, Options options, Reporter reporter) {
        super(apis, options, reporter);
    }

    /**
     * Compares instances of a module element found in different APIs.
     *
     * @param mPos the position of the element
     * @param mMap the map giving the instance of the module element in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    @Override
    public boolean compare(Position mPos, APIMap<ModuleElement> mMap) {
        boolean allEqual = false;
        reporter.comparing(mPos, mMap);
        try {
            allEqual = checkMissing(mPos, mMap);
            if (mMap.size() > 1) {
                allEqual &= compareAnnotations(mPos, mMap);
                allEqual &= compareModifiers(mPos, mMap);
                allEqual &= compareDirectives(mPos, mMap);
                allEqual &= compareDocComments(mPos, mMap);
                allEqual &= compareApiDescriptions(mPos, mMap);
                allEqual &= comparePackages(mPos, mMap);
                allEqual &= compareDocFiles(mPos, mMap);
            }
        } finally {
            reporter.completed(mPos, allEqual);
        }
        return allEqual;
    }

    private boolean comparePackages(Position mPos, APIMap<ModuleElement> mMap) {
        PackageComparator pc = new PackageComparator(mMap.keySet(), options, reporter);
        KeyTable<PackageElement> packages = options.getAccessKind().compareTo(AccessKind.PROTECTED) <= 0
                ? KeyTable.of(mMap, API::getExportedPackageElements)
                : KeyTable.of(mMap, API::getPackageElements);
        return pc.compareAll(packages);
    }

    /**
     * Compares the modifiers for instances of a module element found in different APIs.
     *
     * <p>For a module element, the only modifier is whether it is an open  module or not.
     */
    @Override
    protected boolean compareModifiers(Position mPos, APIMap<ModuleElement> mMap) {
        if (mMap.size() == 1)
            return true;

        boolean first = true;
        boolean baselineIsOpen = false;
        for (ModuleElement me : mMap.values()) {
            if (first) {
                baselineIsOpen = me.isOpen();
                first = false;
            } else if (me.isOpen() != baselineIsOpen) {
                reporter.reportDifferentModifiers(mPos, mMap);
                return false;
            }
        }

        return true;
    }

    private boolean compareDirectives(Position mPos, APIMap<ModuleElement> mMap) {
        KeyTable<RequiresDirective> requires = new KeyTable<>();
        KeyTable<ExportsDirective> exports = new KeyTable<>();
        KeyTable<OpensDirective> opens = new KeyTable<>();
        KeyTable<ProvidesDirective> provides = new KeyTable<>();
        KeyTable<UsesDirective> uses = new KeyTable<>();

        boolean allDirectiveDetails = accessKind.allModuleDetails();

        for (Map.Entry<API, ModuleElement> e : mMap.entrySet()) {
            API api = e.getKey();
            ModuleElement me = e.getValue();

            for (Directive d : me.getDirectives()) {
                switch (d.getKind()) {
                    case REQUIRES -> {
                        RequiresDirective rd = (RequiresDirective) d;
                        if (allDirectiveDetails || rd.isTransitive()) {
                            requires.put(ElementKey.of(rd.getDependency()), api, rd);
                        }
                    }
                    case EXPORTS -> {
                        ExportsDirective ed = (ExportsDirective) d;
                        if (allDirectiveDetails || ed.getTargetModules() == null) {
                            exports.put(ElementKey.of(ed.getPackage()), api, ed);
                        }
                    }
                    case OPENS -> {
                        OpensDirective od = (OpensDirective) d;
                        if (allDirectiveDetails || od.getTargetModules() == null) {
                            opens.put(ElementKey.of(od.getPackage()), api, od);
                        }
                    }
                    case PROVIDES -> {
                        ProvidesDirective pd = (ProvidesDirective) d;
                        if (allDirectiveDetails || isDocumented(api, me, pd)) {
                            provides.put(ElementKey.of(pd.getService()), api, pd);
                        }
                    }
                    case USES -> {
                        UsesDirective ud = (UsesDirective) d;
                        if (allDirectiveDetails || isDocumented(api, me, ud)) {
                            uses.put(ElementKey.of(ud.getService()), api, ud);
                        }
                    }
                }
            }
        }

        Set<API> mAPIs = mMap.keySet();
        boolean allEqual = new RequiresComparator(mAPIs).compareAll(mPos, requires);
        allEqual &= new ExportsComparator(mAPIs).compareAll(mPos, exports);
        allEqual &= new OpensComparator(mAPIs).compareAll(mPos, opens);
        allEqual &= new ProvidesComparator(mAPIs, allDirectiveDetails).compareAll(mPos, provides);
        allEqual &= new UsesComparator(mAPIs).compareAll(mPos, uses);
        return allEqual;
    }

    /**
     * A cache of the service information derived from doc comments.
     */
    private Map<ModuleElement, Map<DirectiveKind, Set<String>>> documentedServices = new HashMap<>();

    /**
     * Determines if the service type in a "provides" or "uses" directive is documented or not.
     * A service type is documented if it is the subject of a corresponding {@code @provides}
     * or {@code @uses} tag identifying the service type.
     *
     * @param api the API containing the module element
     * @param me  the module element
     * @param d   the directive containing the service type
     *
     * @return {@code true} if the service type is documented in the doc comment for the module
     */
    private boolean isDocumented(API api, ModuleElement me, Directive d) {
        Map<DirectiveKind, Set<String>> servicesForModule =
                documentedServices.computeIfAbsent(me, __ -> getDocumentedServices(api, me));
        String serviceName = switch (d.getKind()) {
            case USES -> ((UsesDirective) d).getService().getQualifiedName().toString();
            case PROVIDES -> ((ProvidesDirective) d).getService().getQualifiedName().toString();
            default -> throw new IllegalArgumentException();
        };
        return servicesForModule.get(d.getKind()).contains(serviceName);
    }

    /**
     * Returns details about which services are documented for a module.
     * This requires the source file for the module to be available;
     * if it is not available, an empty map is returned.
     *
     * @param api the API containing the module element
     * @param me  the module element
     *
     * @return a map which identifies which services are documented for a module.
     */
    private Map<DirectiveKind, Set<String>> getDocumentedServices(API api, ModuleElement me) {
        Map<DirectiveKind, Set<String>> map = new EnumMap<>(DirectiveKind.class);
        map.put(DirectiveKind.PROVIDES, new HashSet<>());
        map.put(DirectiveKind.USES, new HashSet<>());
        DocCommentTree dct = api.getDocComment(me);
        if (dct != null) {
            DocTreeVisitor<Void, Map<DirectiveKind, Set<String>>> serviceScanner = new DocTreeScanner<>() {
                @Override
                public Void visitProvides(ProvidesTree pt, Map<DirectiveKind, Set<String>> map) {
                    map.get(DirectiveKind.PROVIDES).add(pt.getServiceType().getSignature());
                    return null;
                }
                @Override
                public Void visitUses(UsesTree ut, Map<DirectiveKind, Set<String>> map) {
                    map.get(DirectiveKind.USES).add(ut.getServiceType().toString());
                    return null;
                }
            };
            dct.accept(serviceScanner, map);
        }
        return map;
    }

    private abstract class DirectiveComparator<D extends Directive> {
        final Set<API> apis;
        final DirectiveKind kind;

        DirectiveComparator(Set<API> apis, DirectiveKind kind) {
            this.apis = apis;
            this.kind = kind;
        }

        boolean compareAll(Position mPos, KeyTable<D> table) {
            boolean allEqual = true;
            // TODO? use comparing... try { ... } finally { compared... }
            for (Map.Entry<ElementKey, APIMap<D>> e : table.entries()) {
                ElementKey ek = e.getKey();
                APIMap<D> v = e.getValue();
                boolean equal = compare(mPos.directive(kind, ek), v);
                allEqual &= equal;
            }
            return allEqual;
        }

        abstract boolean compare(Position pos, APIMap<D> map);

        protected boolean checkMissing(Position dPos, APIMap<D> dMap) {
            Set<API> missing = apis.stream()
                    .filter(a -> !dMap.containsKey(a))
                    .collect(Collectors.toSet()); // warning: unordered

            if (missing.isEmpty()) {
                return true;
            } else {
                reporter.reportMissing(dPos, missing);
                return false;
            }
        }

        protected <E extends Element> boolean compare(Position dPos, APIMap<D> dMap, Function<D, List<E>> f) {
            boolean allEqual = false;
            reporter.comparing(dPos, dMap);
            try {
                allEqual = checkMissing(dPos, dMap);
                if (dMap.size() > 1) {
                    Set<ElementKey> baseline = null;
                    for (D d : dMap.values()) {
                        List<E> elements = f.apply(d);
                        Set<ElementKey> set = (elements == null) ? Collections.emptySet()
                                : elements.stream().map(ElementKey::of).collect(Collectors.toSet());
                        if (baseline == null) {
                            baseline = set;
                        } else if (!set.equals(baseline)) {
                            allEqual = false;
                            break;
                        }
                    }
                    if (!allEqual) {
                        reporter.reportDifferentDirectives(dPos, dMap);
                    }
                }
            } finally {
                reporter.completed(dPos, allEqual);
            }

            return allEqual;
        }
    }

    private class RequiresComparator extends DirectiveComparator<RequiresDirective> {
        RequiresComparator(Set<API> apis) {
            super(apis, DirectiveKind.REQUIRES);
        }

        @Override
        boolean compare(Position rPos, APIMap<RequiresDirective> rMap) {
            boolean allEqual = false;
            reporter.comparing(rPos, rMap);
            try {
                allEqual = checkMissing(rPos, rMap);
                if (rMap.size() > 1) {
                    boolean first = true;
                    boolean baselineIsStatic = false;
                    boolean baselineIsTransitive = false;
                    for (RequiresDirective rd : rMap.values()) {
                        if (first) {
                            baselineIsStatic = rd.isStatic();
                            baselineIsTransitive = rd.isTransitive();
                            first = false;
                        } else if (rd.isStatic() != baselineIsStatic || rd.isTransitive() != baselineIsTransitive) {
                            allEqual = false;
                            break;
                        }
                    }
                    if (!allEqual) {
                        reporter.reportDifferentDirectives(rPos, rMap);
                    }
                }
            } finally {
                reporter.completed(rPos, allEqual);
            }

            return allEqual;
        }
    }

    private class ExportsComparator extends DirectiveComparator<ExportsDirective> {
        ExportsComparator(Set<API> apis) {
            super(apis, DirectiveKind.EXPORTS);
        }

        @Override
        boolean compare(Position ePos, APIMap<ExportsDirective> eMap) {
            return compare(ePos, eMap, ExportsDirective::getTargetModules);
        }
    }

    private class OpensComparator extends DirectiveComparator<OpensDirective> {
        OpensComparator(Set<API> apis) {
            super(apis, DirectiveKind.OPENS);
        }

        @Override
        boolean compare(Position oPos, APIMap<OpensDirective> oMap) {
            return compare(oPos, oMap, OpensDirective::getTargetModules);
        }
    }

    private class ProvidesComparator extends DirectiveComparator<ProvidesDirective> {
        private final boolean allDirectiveDetails;

        ProvidesComparator(Set<API> apis, boolean allDirectiveDetails) {
            super(apis, DirectiveKind.PROVIDES);
            this.allDirectiveDetails = allDirectiveDetails;
        }

        @Override
        boolean compare(Position pPos, APIMap<ProvidesDirective> pMap) {
            // The implementations listed in the directive are not considered
            // part of the public API, so do not compare (i.e. ignore)
            // that part of the API unless allDirectiveDetails is set.
            return compare(pPos, pMap,
                    d -> allDirectiveDetails ? d.getImplementations() : Collections.emptyList());
        }
    }

    private class UsesComparator extends DirectiveComparator<UsesDirective> {
        UsesComparator(Set<API> apis) {
            super(apis, DirectiveKind.USES);
        }

        @Override
        boolean compare(Position uPos, APIMap<UsesDirective> uMap) {
            return checkMissing(uPos, uMap);
        }
    }
}

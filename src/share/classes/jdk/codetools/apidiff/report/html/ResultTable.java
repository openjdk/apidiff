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

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlAttr;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeParameterElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.TypeMirrorKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.ArrayTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.DeclaredTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.PrimitiveTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.TypeVariableKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.WildcardTypeKey;

/**
 * A class to accumulate statistics about the numbers of different kinds of changes.
 */
public class ResultTable {
    /**
     * The different kinds of count recorded in a result table.
     *
     * The members are ordered according to column order in the display table.
     */
    public enum CountKind {
        ELEMENT_ADDED,
        ELEMENT_CHANGED,
        ELEMENT_REMOVED,
        COMMENT_ADDED,
        COMMENT_CHANGED,
        COMMENT_REMOVED,
        DESCRIPTION_ADDED,
        DESCRIPTION_CHANGED,
        DESCRIPTION_REMOVED
    }

    private final Map<ElementKey, Map<CountKind, Integer>> entries;
    private final Messages msgs;
    private final Links links;

    ResultTable(Messages msgs, Links links) {
        this.msgs = msgs;
        this.links = links;
        entries = new TreeMap<>();
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    void inc(ElementKey eKey, CountKind ck) {
        add(getEntry(eKey), ck, 1);
    }

    void addAll(ElementKey eKey, Map<CountKind, Integer> counts) {
        if (counts.values().stream().anyMatch(Objects::nonNull)) {
            var e = getEntry(eKey);
            counts.forEach((ck, i) -> add(e, ck, i));
        }
    }

    Map<CountKind, Integer> getTotals() {
        Map<CountKind, Integer> totals = new EnumMap<>(CountKind.class);
        entries.values().forEach(e -> e.forEach((ck, i) -> add(totals, ck, i)));
        return totals;
    }

    private Map<CountKind, Integer> getEntry(ElementKey eKey) {
        return entries.computeIfAbsent(eKey, e_ -> new EnumMap<>(CountKind.class));
    }

    private void add(Map<CountKind, Integer> counts, CountKind ck, int i) {
        counts.put(ck, counts.computeIfAbsent(ck, ck_ -> 0) + i);
    }

    Content toContent() {
        Map<CountKind, Integer> totals = getTotals();

        HtmlTree caption = HtmlTree.CAPTION(Text.of(msgs.getString("summary.caption")));

        HtmlTree hRow1 = HtmlTree.TR();
        HtmlTree hRow2 = HtmlTree.TR();
        hRow1.add(HtmlTree.TD().set(HtmlAttr.ROWSPAN, "2"));
        // The following assumes the CountKind values are in column order
        Iterator<CountKind> iter = List.of(CountKind.values()).iterator();
        for (String k : List.of("summary.elements", "summary.comments", "summary.descriptions")) {
            boolean noneAdded = totals.get(iter.next()) == null;
            boolean noneChanged = totals.get(iter.next()) == null;
            boolean noneRemoved = totals.get(iter.next()) == null;
            hRow1.add(getHead(k, noneAdded && noneChanged && noneRemoved).set(HtmlAttr.COLSPAN, "3"));
            hRow2.add(getHead("summary.added", noneAdded));
            hRow2.add(getHead("summary.changed", noneChanged));
            hRow2.add(getHead("summary.removed", noneRemoved));
        }
        hRow1.add(getHead("summary.total", false).set(HtmlAttr.ROWSPAN, "2"));
        HtmlTree head = HtmlTree.THEAD(hRow1, hRow2);

        HtmlTree body = HtmlTree.TBODY();
        entries.forEach((eKey, counts) -> {
            HtmlTree bRow = HtmlTree.TR();
            bRow.add(HtmlTree.TH(links.createLink(eKey, toString(eKey))).set(HtmlAttr.SCOPE, "row"));
            for (CountKind ck : CountKind.values()) {
                HtmlTree cell = HtmlTree.TD();
                Integer c = counts.get(ck);
                if (c != null) {
                    cell.add(Text.of(String.valueOf(c)));
                }
                bRow.add(cell);
            }
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            bRow.add(HtmlTree.TD(Text.of(String.valueOf(total))));
            body.add(bRow);
        });

        HtmlTree fRow = HtmlTree.TR();
        fRow.add(HtmlTree.TH(Text.of(msgs.getString("summary.total"))).set(HtmlAttr.SCOPE, "row"));
        HtmlTree foot = HtmlTree.TFOOT(fRow);
        for (CountKind ck : CountKind.values()) {
            HtmlTree cell = HtmlTree.TD();
            Integer c = totals.get(ck);
            if (c != null) {
                cell.add(Text.of(String.valueOf(c)));
            }
            fRow.add(cell);
        }
        int total = totals.values().stream().mapToInt(Integer::intValue).sum();
        fRow.add(HtmlTree.TD(Text.of(String.valueOf(total))));

        return HtmlTree.TABLE(caption, head, body, foot).setClass("summary");
    }

    private HtmlTree getHead(String key, boolean allZero) {
        HtmlTree th = HtmlTree.TH(Text.of(msgs.getString(key)));
        if (allZero) {
            th.setClass("allZero");
        }
        return th;
    }

    private String toString(ElementKey eKey) {
        return toStringVisitor.toString(eKey);
    }

    private final ToStringVisitor toStringVisitor = new ToStringVisitor();

    private class ToStringVisitor
            implements ElementKey.Visitor<CharSequence, Void>,
                    TypeMirrorKey.Visitor<CharSequence, Void> {
        String toString(ElementKey eKey) {
            return eKey.accept(this, null).toString();
        }

        @Override
        public CharSequence visitModuleElement(ModuleElementKey mKey, Void aVoid) {
            return mKey.name;
        }

        @Override
        public CharSequence visitPackageElement(PackageElementKey pKey, Void aVoid) {
            return pKey.name;
        }

        @Override
        public CharSequence visitTypeElement(TypeElementKey tKey, Void aVoid) {
            return tKey.enclosingKey instanceof TypeElementKey
                    ? toString(tKey.enclosingKey) + "." + tKey.name
                    : tKey.name;
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
            return toString(k.elementKey);
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
            StringBuilder sb = new StringBuilder("?");
            if (k.extendsBoundKey != null) {
                sb.append("extends ").append(toString(k.extendsBoundKey));
            }
            if (k.superBoundKey != null) {
                sb.append("super ").append(toString(k.superBoundKey));
            }
            return sb.toString();
        }

    }
}

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
import java.util.List;
import java.util.stream.Collectors;

import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.RawHtml;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.ElementKey;

/**
 * A page reporter to create a top-level index page.
 */
public class IndexPageReporter extends PageReporter<ElementKey> {
    final NotesTable notesTable;

    IndexPageReporter(HtmlReporter parent) {
        super(parent);
        notesTable = new NotesTable(links);
    }

    /**
     * Returns the name to be used to link to this page in the nav bar.
     * @return the name
     */
    public String getName() {
        return msgs.getString("overview.name");
    }

    @Override
    protected HtmlTree buildPageContent() {
        return new HtmlTree(TagName.HTML, buildHead(), buildBody());
    }

    @Override
    public String getTitle() {
        String t = getOptions().getTitle();
        return t != null ? t : msgs.getString("overview.title");
    }

    @Override
    protected Content buildSignature() {
        throw new Error();
    }

    @Override
    protected HtmlTree buildBody() {
        HtmlTree body = HtmlTree.BODY().setClass("index");
        body.add(buildHeader());
        HtmlTree main = HtmlTree.MAIN();
        main.add(buildPageHeading());
        main.add(buildSummary());
        main.add(buildEnclosedElements());
        main.add(buildNotes());
        main.add(buildResultTable());
        body.add(main);
        body.add(buildFooter());
        if (getOptions().getHiddenOption("show-debug-summary") != null) {
            body.add(new DebugSummary().build());
        }
        return body;
    }

    @Override
    protected Content buildPageHeading() {
        return HtmlTree.H1(Text.of(getTitle()));
    }

    private List<Content> buildSummary() {
        List<Content> summary = new ArrayList<>();
        String d = getOptions().getDescription();
        if (d != null) {
            summary.add(new RawHtml(d));
        }
        summary.add(new HtmlTree(TagName.H2, Text.of(msgs.getString("overview.heading.apis"))));
        summary.add(HtmlTree.UL(getOptions().getAllAPIOptions().values().stream()
                .map(a -> Text.of(a.label == null ? a.name : a.name + ": " + a.label))
                .map(HtmlTree::LI)
                .collect(Collectors.toList())));
        return summary;
    }

    @Override
    protected List<Content> buildEnclosedElements() {
        List<Content> list = new ArrayList<>();
        addEnclosedElements(list, "heading.modules", ek -> ek.kind == ElementKey.Kind.MODULE);
        // This case should only occur when modules are not being compared
        addEnclosedElements(list, "heading.packages", ek -> ek.kind == ElementKey.Kind.PACKAGE);
        // This case should only occur when modules are not being compared,
        // and some types are in the unnamed package.
        // TODO: should we handle the unnamed package better?
        addEnclosedElements(list, "heading.types", ek -> ek.kind == ElementKey.Kind.TYPE);
        return list;
    }

    /**
     * Builds the list of notes (if any).
     *
     * @return the list of notes.
     */
    protected Content buildNotes() {
        if (notesTable.isEmpty()) {
            return Content.empty;
        }

        HtmlTree section = HtmlTree.SECTION(HtmlTree.H2(Text.of(msgs.getString("notes.heading"))))
                .setClass("notes");
        section.add(notesTable.toContent());
        return section;
    }
}

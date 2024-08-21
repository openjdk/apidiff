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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jdk.codetools.apidiff.Notes;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.ElementKey;

/**
 * A table recording the elements associated with each note.
 */
public class NotesTable {
    private final Map<Notes.Entry, List<ElementKey>> table;
    private final Links links;

    /**
     * Creates a table to record the elements associated with each note.
     *
     * @param links a factory to generate links to the associated elements
     */
    NotesTable(Links links) {
        table = new TreeMap<>(Comparator.comparing((Notes.Entry e) -> e.description));
        this.links = links;
    }

    /**
     * Returns whether the table is empty.
     *
     * @return {@code true} if and only if the table is empty
     */
    boolean isEmpty() {
        return table.isEmpty();
    }

    /**
     * Add an association for an element key with a note.
     *
     * @param e    the note
     * @param eKey the element key
     */
    void add(Notes.Entry e, ElementKey eKey) {
        table.computeIfAbsent(e, e_ -> new ArrayList<>()).add(eKey);
    }

    /**
     * Generates HTML content representing the contents of the table.
     *
     * @return the content
     */
    Content toContent() {
        if (table.isEmpty()) {
            return Content.empty;
        }

        HtmlTree dl = HtmlTree.DL();
        table.forEach((e, list) -> add(dl, e, list));

        return dl;
    }

    private void add(HtmlTree dl, Notes.Entry e, List<ElementKey> list) {
        HtmlTree eLink = HtmlTree.A(e.uri, Text.of(e.description));
        dl.add(HtmlTree.DT(eLink));

        // TODO: sort list?

        HtmlTree dd = HtmlTree.DD();
        boolean first = true;
        for (ElementKey eKey : list) {
            if (first) {
                first = false;
            } else {
                dd.add(Text.of(", "));
            }
            dd.add(toContent(eKey));
        }
        dl.add(dd);
    }

    private Content toContent(ElementKey eKey) {
        return links.createLink(eKey, Notes.getName(eKey).replace("#", "."));
    }
}

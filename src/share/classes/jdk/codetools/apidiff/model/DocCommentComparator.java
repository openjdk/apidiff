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

import java.util.Set;

import com.sun.source.doctree.DocCommentTree;

/**
 * A comparator for {@link DocCommentTree documentation comments}.
 */
public class DocCommentComparator {
    private final Set<API> apis;

    /**
     * Creates a comparator for instances of documentation comments found in
     * different APIs.
     *
     * @param apis the APIs
     */
    public DocCommentComparator(Set<API> apis) {
        this.apis = apis;
    }

    /**
     * Compare instances of a documentation comment for an element in different APIs.
     *
     * @param dPos the position of the element
     * @param dMap the map giving the instance of the comment in different APIs
     * @return {@code true} if all the instances are equivalent
     */
    public boolean compare(Position dPos, APIMap<DocCommentTree> dMap) {
        // TODO: compare first sentence, body, sorted block tags
        return true;
    }
}

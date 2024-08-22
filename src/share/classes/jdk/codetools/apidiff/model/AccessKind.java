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
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * The kind of access filters.
 */
public enum AccessKind {
    /**
     * A filter for items declared to be {@code public}.
     */
    PUBLIC {
        @Override
        public boolean accepts(Set<Modifier> modifiers) {
            return modifiers.contains(Modifier.PUBLIC);
        }

        @Override
        public boolean allModuleDetails() {
            return false;
        }
    },

    /**
     * A filter for items declared to be {@code public} or {@code protected}.
     */
    PROTECTED {
        @Override
        public boolean accepts(Set<Modifier> modifiers) {
            return modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED);
        }

        @Override
        public boolean allModuleDetails() {
            return false;
        }
    },

    /**
     * A filter for items declared to be {@code public}, {@code protected} or package-private.
     */
    PACKAGE {
        @Override
        public boolean accepts(Set<Modifier> modifiers) {
            return !modifiers.contains(Modifier.PRIVATE);
        }

        @Override
        public boolean allModuleDetails() {
            return true;
        }
    },

    /**
     * A filter for all items.
     */
    PRIVATE {
        @Override
        public boolean accepts(Set<Modifier> modifiers) {
            return true;
        }

        @Override
        public boolean allModuleDetails() {
            return true;
        }
    };

    /**
     * Returns whether the filter accepts an item with the given set of modifiers.
     *
     * @param modifiers the modifiers
     * @return {@code true} if the item is accepted by the filter
     */
    public abstract boolean accepts(Set<Modifier> modifiers);

    /**
     * Returns whether the filter accepts an element according to its modifiers.
     *
     * @param e the element
     * @return {@code true} if the element is accepted by the filter
     */
    public boolean accepts(Element e) {
        return accepts(e.getModifiers());
    }

    /**
     * Returns whether all module details should be compared and displayed.
     *
     * @return {@code true} if and only if all module details should be compared and displayed
     */
    public abstract boolean allModuleDetails();
}

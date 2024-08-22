/*
 * Copyright (c) 2018,2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.codetools.apidiff.report;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.DocFile;
import jdk.codetools.apidiff.model.Position;

/**
 * A base class for reporting differences found in different instances of an API.
 */
public interface Reporter {
    /**
     * Reports the elements being compared.
     *
     * @param pos the position of an item being compared
     * @param map the map identifying the instances to be compared
     * @see #completed(Position, boolean)
     */
    void comparing(Position pos, APIMap<?> map);

    /**
     * Reports that a comparison has completed.
     *
     * @param ePos the position of an item being compared
     * @param equal {@code true} if the items being compared are equivalent
     * @see #comparing(Position, APIMap)
     */
    void completed(Position ePos, boolean equal);

    /**
     * Reports that the overall comparison has been completed.
     *
     * @param equal {@code true} if the APIs being compared are equivalent
     */
    default void completed(boolean equal) { }

    /**
     * Reports that items were not found at a given position within instances of an API.
     *
     * @param ePos the position of the item being compared
     * @param apis the APIs
     */
    void reportMissing(Position ePos, Set<API> apis);


    /**
     * Reports that different annotations were found at a given position
     * within instances of an API.
     *
     * @param amPos the position of the annotations
     * @param amMap the map identifying the instances to be compared
     */
    void reportDifferentAnnotations(Position amPos, APIMap<? extends AnnotationMirror> amMap);

    /**
     * Reports that different annotation values were found at a given position
     * within instances of an API.
     *
     * @param avPos the position of the annotations
     * @param avMap the map identifying the instances that were compared
     */
    void reportDifferentAnnotationValues(Position avPos, APIMap<? extends AnnotationValue> avMap);

    /**
     * Reports that different directives were found at a position within a module element
     * within instances of an API.
     *
     * @param dPos the position of the directive
     * @param dMap the map identifying the instances that were compared
     */
    void reportDifferentDirectives(Position dPos, APIMap<? extends Directive> dMap);

    /**
     * Reports that different modifiers were found at a position for an element
     * within instances of an API.
     *
     * @param ePos the position of the directive
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentModifiers(Position ePos, APIMap<? extends Element> eMap);

    /**
     * Reports that different kinds of an element were found at a position
     * within instances of an API.  For example, a class in one API may be
     * declared an interface in another.
     *
     * @param ePos the position of the element
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentKinds(Position ePos, APIMap<? extends Element> eMap);

    /**
     * Reports that different names for an element were found at a position
     * within instances of an API.  For example, record components may be
     * named differently.
     *
     * @param ePos the position of the element
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentNames(Position ePos, APIMap<? extends Element> eMap);

    /**
     * Reports that different values were found at a position
     * within instances of an API.  For example, serializable objects may
     * have different serial version UIDs.
     *
     * @param vPos the position of the value
     * @param vMap the map identifying the instances that were compared
     */
    void reportDifferentValues(Position vPos, APIMap<?> vMap);

    /**
     * Reports that different type parameters were found at a position within
     * instances of an API.
     *
     * @param ePos the position of the directive
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentTypeParameters(Position ePos, APIMap<? extends TypeParameterElement> eMap);

    /**
     * Reports that different type mirrors were found at a position
     * within instances of an API.
     *
     * @param tPos the position of the type
     * @param tMap the map identifying the instances that were compared
     */
    void reportDifferentTypes(Position tPos, APIMap<? extends TypeMirror> tMap);

    /**
     * Reports that different sets of thrown types were found at a position
     * within instances of an API.
     *
     * @param ePos the position of the executable element
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentThrownTypes(Position ePos, APIMap<List<? extends TypeMirror>> eMap);

    /**
     * Reports that different sets of superinterfaces were found at a position
     * within instances of an API.
     *
     * @param ePos the position of the type element
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentSuperinterfaces(Position ePos, APIMap<List<? extends TypeMirror>> eMap);

    /**
     * Reports that different sets of permitted subclasses were found at a position
     * within instances of an API.
     *
     * @param ePos the position of the type element
     * @param eMap the map identifying the instances that were compared
     */
    void reportDifferentPermittedSubclasses(Position ePos, APIMap<List<? extends TypeMirror>> eMap);

    /**
     * Reports that different raw doc comments were found at a position
     * within instances of an API.
     *
     * @param ePos the position of the doc comment
     * @param cMap the map identifying the instances that were compared
     */
    void reportDifferentRawDocComments(Position ePos, APIMap<String> cMap);

    /**
     * Reports that different API descriptions were found at a position
     * within instances of an API.
     *
     * @param ePos the position of the doc comment
     * @param cMap the map identifying the instances that were compared
     */
    void reportDifferentApiDescriptions(Position ePos, APIMap<String> cMap);

    /**
     * Reports that different doc files were found for a package
     * within instances of an API.
     *
     * @param fPos the position of the doc file
     * @param fMap the map identifying the instances that were compared
     */
    void reportDifferentDocFiles(Position fPos, APIMap<DocFile> fMap);
}

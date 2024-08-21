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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

/**
 * An object representing a file in a {@code doc-files} subdirectory
 * of the source directory or generated API directory for a package.
 */
public class DocFile {
    /**
     * The standard name of the subdirectory used for doc files.
     */
    public static final String DOC_FILES = "doc-files";

    /**
     * The API for the doc file.
     */
    public final API api;

    /**
     * The element that "contains" the doc file.  It may be a module element or a package element.
     */
    public final Element element;

    /**
     * The source and/or API file.
     * One or the other (but not both) may be null if no such file is found.
     */
    public final Map<API.LocationKind, JavaFileObject> files;

    /**
     * Returns a table built by listing all the files in the {@code doc-files}
     * subdirectory of  the source and generated API directories for a module or package.
     *
     * @param pMap the map of corresponding packages in the APIs being compared
     *
     * @return the table
     */
    static Map<String, APIMap<DocFile>> listDocFiles(APIMap<? extends Element> pMap) {
        Set<JavaFileObject.Kind> allKinds = EnumSet.allOf(JavaFileObject.Kind.class);
        Map<String, APIMap<DocFile>> fMap = new TreeMap<>();
        pMap.forEach((api, e) -> {
            for (API.LocationKind lk : API.LocationKind.values()) {
                for (JavaFileObject fo : api.listFiles(lk, e, DOC_FILES, allKinds, true)) {

                    // There is no supported way to get the name of the file relative to the package in which
                    // the search was done.  JavaFileManager.inferBinaryName comes close, but is not ideal.
                    // The following assumes that "doc-files" only appears once in the path name.
                    // A more rigorous check would be to include the module name or package name,
                    // but even that is not guaranteed to be unique.

                    String name = fo.getName();
                    int index = name.indexOf(DOC_FILES);
                    if (index == -1) {
                        throw new IllegalArgumentException(fo.getName());
                    }
                    String path = name.substring(index + DOC_FILES.length() + 1);

                    APIMap<DocFile> dMap = fMap.computeIfAbsent(path, __ -> APIMap.of());
                    DocFile df = dMap.computeIfAbsent(api, __ -> new DocFile(api, e));
                    df.files.put(lk, fo);
                }
            }
        });
        return fMap;
    }

    private DocFile(API api, Element element) {
        this.api = api;
        this.element = element;
        this.files = new EnumMap<>(API.LocationKind.class);
    }

    /**
     * Returns the kind of these doc files.
     * By construction, they all have the same file name, and hence all have the
     * same kind, so it is sufficient to just pick one.
     *
     * @return the kind
     */
    public JavaFileObject.Kind getKind() {
        return files.values().iterator().next().getKind();
    }
}

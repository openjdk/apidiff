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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.JavaFileObject;

import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.API.LocationKind;
import jdk.codetools.apidiff.report.Reporter;

/**
 * A comparator for the "doc files" for a package.
 * These are the files in the "doc-files" subdirectory of the
 * source directories for the package, or in the "doc-files"
 * subdirectory of the directory for the package in the generated
 * API.
 *
 * <p>The comparison includes:
 * <ul>
 *     <li>the documentation comment for HTML files found in the source directory
 *     <li>the API description for HTML files found in the generated API directory
 *     <li>other files in the source and generated API directories
 * </ul>
 */
public class DocFilesComparator {
    /** The APIs to be compared. */
    protected final Set<API> apis;
    /** The command-line options. */
    protected final Options options;
    /** The reporter to which to report any differences. */
    protected final Reporter reporter;

    /**
     * Creates a comparator to compare "doc files" across a set of APIs.
     *
     * @param apis the set of APIs
     * @param reporter the reporter to which to report differences
     */
    public DocFilesComparator(Set<API> apis, Options options, Reporter reporter) {
        this.apis = apis;
        this.options = options;
        this.reporter = reporter;
    }

    /**
     * Compares all the doc files with the same name within instances of an API.
     *
     * @param pPos the position for the package containing the doc files
     * @param table the table containing the doc files to be compared
     *
     * @return {@code true} if and only if all the elements are equal
     */
    public boolean compareAll(Position pPos, Map<String, APIMap<DocFile>> table) {
        boolean allEqual = true;
        for (Map.Entry<String, APIMap<DocFile>> e : table.entrySet()) {
            String name = e.getKey();
            APIMap<DocFile> fMap = e.getValue();
            boolean equal = compare(pPos.docFile(name), fMap);
            allEqual &= equal;
        }
        return allEqual;
    }

    private boolean compare(Position fPos, APIMap<DocFile> fMap) {
        boolean allEqual = false;
        reporter.comparing(fPos, fMap);
        try {
            allEqual = checkMissing(fPos, fMap);
            if (fMap.size() > 1) {
                // compare doc comments and API description for HTML files;
                // compare file contents for all other files (typically images)
                if (fMap.values().iterator().next().getKind() == JavaFileObject.Kind.HTML) {
                    allEqual &= compareDocComments(fPos, fMap);
                    allEqual &= compareApiDescriptions(fPos, fMap);
                } else {
                    allEqual &= compareFiles(fPos, fMap);
                }
            }
        } finally {
            reporter.completed(fPos, allEqual);
        }
        return allEqual;
    }

    /**
     * Checks whether any expected doc files are missing in any APIs.
     * Missing files will be reported to the comparator's reporter.
     *
     * @param fPos the position of the file
     * @param fMap the map giving the files in the different APIs
     * @return {@code true} if all the expected files are found
     */
    private boolean checkMissing(Position fPos, APIMap<DocFile> fMap) {
        Set<API> missing = apis.stream()
                .filter(a -> !fMap.containsKey(a))
                .collect(Collectors.toSet()); // warning: unordered

        if (missing.isEmpty()) {
            return true;
        } else {
            reporter.reportMissing(fPos, missing);
            return false;
        }
    }


    /**
     * Compares the documentation for the doc files at a given position in
     * different instances of an API.
     *
     * @param fPos the position
     * @param fMap the map of file files
     * @return {@code true} if and only if all the instances of the documentation are equal
     */
    protected boolean compareDocComments(Position fPos, APIMap<DocFile> fMap) {
        if (!options.compareDocComments()) {
            return true;
        }

        // TODO: make this depend on command-line options to compare some combination of
        //       raw doc comments, (parsed) doc comments.
//        APIMap<DocCommentTree> docComments = APIMap.of();
//        for (Map.Entry<API, E> e : eMap.entrySet()) {
//            API api = e.getKey();
//            Element te = e.getValue();
//            DocCommentTree dct = api.getDocComment(te);
//            if (dct != null) {
//                docComments.put(api, dct);
//            }
//        }
//        DocCommentComparator dc = new DocCommentComparator(eMap.keySet());
//        return dc.compare(ePos, docComments);

        APIMap<String> rawDocComments = fMap.map((api, df) -> {
            JavaFileObject fo = df.files.get(LocationKind.SOURCE);
            return fo == null ? "" : api.getTrees().getDocCommentTree(fo).toString();
        });

        // raw doc comments are equal if none of the doc-files has a doc comment,
        // or if they all have the same doc comment.
        boolean allEqual = rawDocComments.isEmpty()
                || rawDocComments.size() == fMap.size() && rawDocComments.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentRawDocComments(fPos, rawDocComments);
        }
        return allEqual;
    }

    /**
     * Compares the API descriptions for the doc files at a given position
     * in different instances of an API.
     *
     * @param fPos the position
     * @param fMap the map of doc files
     *
     * @return {@code true} if and only if all the instances of the API description are equal
     */
    protected boolean compareApiDescriptions(Position fPos, APIMap<DocFile> fMap) {
        if (!options.compareApiDescriptions()) {
            return true;
        }

        APIMap<String> apiDescriptions = fMap.map((api, df) -> {
            JavaFileObject fo = df.files.get(LocationKind.API);
            return fo == null ? "" : api.getApiDescription(fo);
        });

        // API descriptions are equal if none of the doc-files has a description,
        // or if they all have the same description.
        boolean allEqual = apiDescriptions.isEmpty()
                || apiDescriptions.size() == fMap.size() && apiDescriptions.values().stream().distinct().count() == 1;
        if (!allEqual) {
            reporter.reportDifferentApiDescriptions(fPos, apiDescriptions);
        }
        return allEqual;
    }

    private boolean compareFiles(Position fPos, APIMap<DocFile> fMap) {
        return compareFiles(fPos, fMap, API.LocationKind.SOURCE)
                && compareFiles(fPos, fMap, API.LocationKind.API);
    }

    private boolean compareFiles(Position fPos, APIMap<DocFile> fMap, API.LocationKind kind) {
        // If there are no files at all in this location-kind, that's OK,
        // and they are vacuously equal.
        boolean noFiles = fMap.values().stream().allMatch(df -> df.files.get(kind) == null);
        if (noFiles) {
            return true;
        }

        // But if some files are present and some are missing, that's an automatic difference.
        boolean missingFiles = fMap.values().stream().anyMatch(df -> df.files.get(kind) == null);
        if (missingFiles) {
            return false;
        }

        // Otherwise, compare the contents of each file (other than the first) against the first.
        // While it would be possible to open streams on each file, and read/compare the streams
        // in parallel, that seems overall. And note, for reference, we do read the full contents
        // of HTML files into memory.
        byte[] ref = null;
        for (Map.Entry<API, DocFile> entry : fMap.entrySet()) {
            API api = entry.getKey();
            DocFile df = entry.getValue();
            byte[] bytes = api.getAllBytes(df.files.get(kind));
            if (bytes == null) {
                return false;
            }
            if (ref == null) {
                ref = bytes;
            } else if (!Arrays.equals(ref, bytes)) {
                return false;
            }
        }
        return true;
    }
}

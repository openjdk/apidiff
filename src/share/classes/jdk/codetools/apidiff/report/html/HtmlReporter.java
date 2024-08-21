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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import jdk.codetools.apidiff.Abort;
import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.Notes;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.DocFile;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.ElementPosition;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.report.Reporter;

/**
 * The main class to generate the pages for an HTML report.
 * The reporting methods are dispatched to individual reporters
 * that handle the different types of pages.
 */
public class HtmlReporter implements Reporter {
    final Set<API> apis;
    final API latestAPI;
    final API previousAPI;
    final Options options;
    final Path outDir;
    final Notes notes;
    final Log log;
    final Messages msgs;

    /**
     * Creates a reporter that will generate an HTML report.
     *
     * @param apis    the APIs being compared
     * @param options the command-line options
     * @param notes   the notes to be associated with elements
     * @param log     the log to which to write any diagnostic messages
     */
    public HtmlReporter(Set<API> apis, Options options, Notes notes, Log log) {
        if (apis.size() < 2) {
            throw new IllegalArgumentException("too few APIs: " + apis.size());
        }
        this.apis = apis; // TODO: change to List?
        this.options = options;
        this.outDir = options.getOutDir();
        this.notes = notes;
        this.log = log;
        this.msgs = Messages.instance("jdk.codetools.apidiff.report.html.resources.report");

        List<API> apiList = new ArrayList<>(apis);
        latestAPI = apiList.get(apiList.size() - 1);
        previousAPI = apiList.get(apiList.size() - 2);

        indexPageReporter = new IndexPageReporter(this);

        writeStylesheets();
        writeResourceFiles();
    }

    //<editor-fold desc="Implements Reporter">
    @Override
    public void comparing(Position ePos, APIMap<?> apiMap) {
        getPageReporter(ePos).comparing(ePos, apiMap);
    }

    @Override
    public void completed(Position ePos, boolean equal) {
        getPageReporter(ePos).completed(ePos, equal);
        // TODO: if ePos is the pageKey for the pageReporter, can clean out pageReporter from pageVisitor map
    }

    @Override
    public void completed(boolean equal) {
        indexPageReporter.writeFile();

        // Debug reporting of to-do info
        if (!toDoCounts.isEmpty()) {
            log.err.println("ToDo info:");
            toDoCounts.forEach((k, v) -> log.err.println("  " + k + ": " + v));
            Map<Integer, SortedSet<String>> inverseCounts = new TreeMap<>(Comparator.reverseOrder());
            toDoCounts.forEach((k, v) -> inverseCounts.computeIfAbsent(v, s -> new TreeSet<>()).add(k));
            inverseCounts.forEach((k, v) -> log.err.println(String.format("%6d: %s", k, v)));
        }
    }

    @Override
    public void reportMissing(Position ePos, Set<API> apis) {
        getPageReporter(ePos).reportMissing(ePos, apis);
    }

    @Override
    public void reportDifferentAnnotations(Position amPos, APIMap<? extends AnnotationMirror> amMap) {
        getPageReporter(amPos).reportDifferentAnnotations(amPos, amMap);
    }

    @Override
    public void reportDifferentAnnotationValues(Position avPos, APIMap<? extends AnnotationValue> avMap) {
        getPageReporter(avPos).reportDifferentAnnotationValues(avPos, avMap);
    }

    @Override
    public void reportDifferentDirectives(Position dPos, APIMap<? extends Directive> dMap) {
        getPageReporter(dPos).reportDifferentDirectives(dPos, dMap);
    }

    @Override
    public void reportDifferentModifiers(Position ePos, APIMap<? extends Element> eMap) {
        getPageReporter(ePos).reportDifferentModifiers(ePos, eMap);
    }

    @Override
    public void reportDifferentKinds(Position ePos, APIMap<? extends Element> eMap) {
        getPageReporter(ePos).reportDifferentKinds(ePos, eMap);
    }

    @Override
    public void reportDifferentNames(Position ePos, APIMap<? extends Element> eMap) {
        getPageReporter(ePos).reportDifferentNames(ePos, eMap);
    }

    @Override
    public void reportDifferentTypeParameters(Position ePos, APIMap<? extends TypeParameterElement> eMap) {
        getPageReporter(ePos).reportDifferentTypeParameters(ePos, eMap);
    }

    @Override
    public void reportDifferentTypes(Position tPos, APIMap<? extends TypeMirror> tMap) {
        getPageReporter(tPos).reportDifferentTypes(tPos, tMap);
    }

    @Override
    public void reportDifferentThrownTypes(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        getPageReporter(tPos).reportDifferentThrownTypes(tPos, tMap);
    }

    @Override
    public void reportDifferentSuperinterfaces(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        getPageReporter(tPos).reportDifferentSuperinterfaces(tPos, tMap);
    }

    @Override
    public void reportDifferentPermittedSubclasses(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        getPageReporter(tPos).reportDifferentPermittedSubclasses(tPos, tMap);
    }

    @Override
    public void reportDifferentValues(Position vPos, APIMap<?> vMap) {
        getPageReporter(vPos).reportDifferentValues(vPos, vMap);
    }

    @Override
    public void reportDifferentRawDocComments(Position tPos, APIMap<String> cMap) {
        getPageReporter(tPos).reportDifferentRawDocComments(tPos, cMap);
    }

    @Override
    public void reportDifferentApiDescriptions(Position tPos, APIMap<String> dMap) {
        getPageReporter(tPos).reportDifferentApiDescriptions(tPos, dMap);
    }

    @Override
    public void reportDifferentDocFiles(Position fPos, APIMap<DocFile> fMap) {
        getPageReporter(fPos).reportDifferentDocFiles(fPos, fMap);
    }
    //</editor-fold>

    Map<String, Integer> toDoCounts = new TreeMap<>();
    void countToDo(String name) {
        toDoCounts.put(name, toDoCounts.getOrDefault(name, 0) + 1);
    }

    PageReporter<?> getPageReporter(Position pos) {
        return pos.accept(pageVisitor, null);
    }

    PageReporter<?> getPageReporter(ElementKey eKey) {
        return (eKey == null) ? indexPageReporter : eKey.accept(pageVisitor, null);
    }

    final IndexPageReporter indexPageReporter;
    private final PageVisitor pageVisitor = new PageVisitor(this);

    /** The default stylesheet for all generated HTML files. */
    public static final String DEFAULT_STYLESHEET = "apidiff.css";

    List<DocPath> getStylesheets() {
        DocPath resourceDir = new DocPath("resources");
        List<DocPath> list = new ArrayList<>();
        if (options.getMainStylesheet() != null) {
            list.add(resourceDir.resolve(options.getMainStylesheet().getFileName().toString()));
        } else {
            list.add(resourceDir.resolve(DEFAULT_STYLESHEET));
        }
        for (Path extraStylesheet : options.getExtraStylesheets()) {
            list.add(resourceDir.resolve(extraStylesheet.getFileName().toString()));
        }
        return list;
    }

    private void writeStylesheets() throws Abort {
        Path outResourceDir = outDir.resolve("resources");
        try {
             Files.createDirectories(outResourceDir);
        } catch (IOException e) {
            log.error("report.err.cant-create-directory", outResourceDir, e);
            throw new Abort();
        }

        if (options.getMainStylesheet() != null) {
            copyFile(options.getMainStylesheet(), outResourceDir);
        } else {
            copyResource(DEFAULT_STYLESHEET, outResourceDir);
        }
        for (Path extraStylesheet : options.getExtraStylesheets()) {
            copyFile(extraStylesheet, outResourceDir);
        }
    }

    private void writeResourceFiles() throws Abort {
        // in the following map, the key for each entry is the relative path
        // of a resource file to be written in the output directory, and the
        // associated value gives the location from which it should be copied.
        var map = new LinkedHashMap<Path, Path>();

        for (var apiOpts : options.getAllAPIOptions().values()) {
            var apiDir = apiOpts.apiDir;
            if (apiDir != null) {
                listFiles(apiDir, this::isResourceFile)
                        .forEach(file -> map.put(apiDir.relativize(file), file));
                listFiles(apiDir.resolve("resource-files"))
                        .forEach(file -> map.put(apiDir.relativize(file), file));
                var absApiDir = apiDir.toAbsolutePath().normalize();
                for (Path resFile : options.getResourceFiles()) {
                    // resFile may explicitly begin with a specific API directory,
                    // or will be considered as relative to each of the API directories
                    Path absResFile = resFile.toAbsolutePath().normalize();
                    Path pathFromApiDir;
                    if (absResFile.startsWith(absApiDir)) {
                        pathFromApiDir = absApiDir.relativize(absResFile);
                    } else if (!resFile.isAbsolute()) {
                        pathFromApiDir = resFile;
                    } else {
                        // TODO: check during Options.validate
                        continue;
                    }
                    Path pathInApiDir = apiDir.resolve(pathFromApiDir);
                    if (Files.isDirectory(pathInApiDir)) {
                        listFiles(pathInApiDir)
                                .forEach(file -> map.put(apiDir.relativize(file), file));
                    } else if (Files.isRegularFile(pathInApiDir)) {
                        map.put(pathFromApiDir, pathInApiDir);
                    }
                }
            }
        }

        map.forEach((to, from) -> copyFile(from, outDir.resolve(to)));
    }

    private List<Path> listFiles(Path dir) {
        return listFiles(dir, p -> true);
    }

    private List<Path> listFiles(Path dir, DirectoryStream.Filter<? super Path> filter) {
        if (Files.isDirectory(dir)) {
            var list = new ArrayList<Path>();
            try (var ds = Files.newDirectoryStream(dir, filter)) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) {
                        list.add(p);
                    }
                }
                return list;
            } catch (IOException e) {
                log.error("report.err.error-finding-resource-files", dir, e);
                throw new Abort();
            }
        } else {
            return List.of();
        }
    }

    private boolean isResourceFile(Path file) {
        return file.getFileName().toString().endsWith(".svg");

    }

    // in this context: `resource` refers to a resource item in tool's jar file
    private void copyResource(String name, Path dir) throws Abort {
        Path toFile = dir.resolve(name);
        try (InputStream in = getClass().getResourceAsStream("resources/" + name)) {
            Files.copy(in, toFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("report.err.error-writing-file", toFile, e);
            throw new Abort();
        }
    }

    private void copyFile(Path fromFile, Path to) {
        Path toFile = Files.isDirectory(to) ? to.resolve(fromFile.getFileName()) : to;
        try {
            Files.createDirectories(toFile.getParent());
            Files.copy(fromFile, toFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("report.err.error-writing-file", toFile, e);
            throw new Abort();
        }
    }

    /**
     * A visitor to determine the page reporter for any given element or position.
     */
    private static class PageVisitor
            implements Position.Visitor<PageReporter<?>, Void>,
                ElementKey.Visitor<PageReporter<?>, Void> {
        HtmlReporter parent;

        PageVisitor(HtmlReporter parent) {
            this.parent = parent;
        }

        // TODO: should remove entries from the map when no longer required.
        //       when page has been completed, use pageReporters.values().removeIf(p -> p == pr)
        Map<ElementKey, PageReporter<?>> pageReporters = new ConcurrentHashMap<>();

        @Override
        public PageReporter<?> visitModuleElement(ModuleElementKey mek, Void _p) {
            return pageReporters.computeIfAbsent(mek, k -> new ModulePageReporter(parent, k));
        }

        @Override
        public PageReporter<?> visitPackageElement(PackageElementKey pek, Void _p) {
            return pageReporters.computeIfAbsent(pek, k -> new PackagePageReporter(parent, k));
        }

        @Override
        public PageReporter<?> visitTypeElement(TypeElementKey tek, Void _p) {
            return pageReporters.computeIfAbsent(tek, k -> new TypePageReporter(parent, k));
        }

        @Override
        public PageReporter<?> visitExecutableElement(ExecutableElementKey k, Void _p) {
            return k.typeKey.accept(this, _p);
        }

        @Override
        public PageReporter<?> visitVariableElement(VariableElementKey k, Void _p) {
            return k.typeKey.accept(this, _p);
        }

        @Override
        public PageReporter<?> visitTypeParameterElement(ElementKey.TypeParameterElementKey k, Void _p) {
            return k.typeKey.accept(this, _p);
        }

        @Override
        public PageReporter<?> visitElementPosition(ElementPosition kp, Void _p) {
            return kp.key.accept(this, _p);
        }

        @Override
        public PageReporter<?> visitRelativePosition(RelativePosition<?> ip, Void _p) {
            return ip.parent.accept(this, _p);
        }
    }
}

/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;

import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Options.APIOptions;

/**
 * An abstraction of an API, as represented by some combination of source files,
 * class files, and generated documentation.
 */
public abstract class API {
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * Creates an API object for the given parameters.
     *
     * @param opts  the options to configure the API
     * @param s     a selector to filter the set of modules, packages and types to be compared
     * @param ak    the access kind, to filter the set of elements to be compared according
     *              their declared access
     * @param log   a log, to which any problems will be reported
     * @param verboseOptions whether to be verbose about internal opton details
     *
     * @return the API
     */
    public static API of(APIOptions opts, Selector s, AccessKind ak, Log log, boolean verboseOptions) {
        return new JavacAPI(opts, s, ak, log, verboseOptions);
    }

    /**
     * The name of the API, as provided in the API options.
     */
    public final String name;

    /**
     * A short plain text label for the API, as provided in the API options.
     */
    public final String label;

    /**
     * The selector to filter the set of modules, packages and types to be compared.
     */
    protected final Selector selector;

    /**
     * The access kind, to filter the set of elements to be compared according
     * their declared access.
     */
    protected final AccessKind accessKind;

    /**
     * The log, to which any problems will be reported.
     */
    protected final Log log;

    /**
     * The file manager used to read files, derived from the API options.
     */
    protected final StandardJavaFileManager fileManager;

    /**
     * Whether to be verbose about internal option details.
     */
    protected final boolean verboseOptions;

    /**
     * Creates an instance of an API.
     *
     * @param opts  the options for the API
     * @param s     the selector for the elements to be compared
     * @param ak    the access kind for the elements to be compared
     * @param log   the log, to which any problems will be reported
     */
    protected API(APIOptions opts, Selector s, AccessKind ak, Log log, boolean verboseOptions) {
        this.name = opts.name;
        this.label = opts.label;
        this.selector = s;
        this.accessKind = ak;
        this.log = log;
        this.verboseOptions = verboseOptions;

        fileManager = compiler.getStandardFileManager(null, null, null);
    }

    /**
     * Returns the set of packages to be compared that are defined in this API.
     *
     * @return the package to be compared
     */
    public abstract Set<PackageElement> getPackageElements();

    /**
     * Returns the set of modules to be compared that are defined in this API.
     *
     * @return the modules to be compared
     */
    public abstract Set<ModuleElement> getModuleElements();

    /**
     * Returns the set of packages to be compared that are defined in this API in a given module.
     *
     * @param m the module
     *
     * @return the packages to be compared
     */
    public abstract Set<PackageElement> getPackageElements(ModuleElement m);

    /**
     * Returns the set of packages to be compared that are defined in this API
     * and exported to all modules by a given module.
     *
     * @param m the module
     *
     * @return the packages to be compared
     */
    public abstract Set<PackageElement> getExportedPackageElements(ModuleElement m);

    /**
     * Returns the set of types to be compared that are defined in this API in a given package.
     *
     * @param p the module
     *
     * @return the packages to be compared
     */
    public abstract Set<TypeElement> getTypeElements(PackageElement p);

    /**
     * Returns the collection of annotation values, including defaults, for a given annotation mirror.
     *
     * @param am the annotation mirror
     *
     * @return the collection of annotation values
     */
    public abstract Map<? extends ExecutableElement, ? extends AnnotationValue> getAnnotationValuesWithDefaults(AnnotationMirror am);

    /**
     * Returns whether the annotation type of an annotation is {@code @Documented}.
     *
     * @param am the annotation
     *
     * @return {@code true} if and only if the type of the annotation is {@code @Documented}
     */
    public boolean isDocumented(AnnotationMirror am) {
        TypeElement te = (TypeElement) am.getAnnotationType().asElement();
        for (AnnotationMirror a : te.getAnnotationMirrors()) {
            Name n = ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName();
            if (n.contentEquals("java.lang.annotation.Documented")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the serialized form for a type element, or null if none.
     *
     * @param e the type element
     *
     * @return the serialized form
     */
    public abstract SerializedForm getSerializedForm(TypeElement e);

    /**
     * Returns the parsed documentation comment for a given element.
     *
     * @param e the element
     *
     * @return the doc comment tree
     */
    public abstract DocCommentTree getDocComment(Element e);

    /**
     * Returns the parsed documentation comment in a given file.
     * The file must be an HTML file.
     *
     * @param fo the file object
     *
     * @return the doc comment tree
     */
    public abstract DocCommentTree getDocComment(JavaFileObject fo);

    /**
     * Returns the API description for a given element, extracted from the API documentation.
     *
     * @param e the element
     *
     * @return the API description
     */
    public abstract String getApiDescription(Element e);

    /**
     * Returns the API description in a given file.
     * The file must be an HTML file.
     *
     * @param fo the file object
     *
     * @return the API description
     */
    public abstract String getApiDescription(JavaFileObject fo);

    /**
     * Returns the content of a file as an array of bytes, or null if there is
     * an error reading the file.
     *
     * @param fo the file
     *
     * @return the bytes
     */
    public abstract byte[] getAllBytes(JavaFileObject fo);

    /**
     * The kind of location for which to list files.
     *
     * @see #listFiles
     */
    public enum LocationKind {
        /** Source files, as found on the module source path, source path or class path, as appropriate. */
        SOURCE,
        /** API files, as found in the API directory. */
        API
    }

    /**
     * Returns a list of the files found in a subdirectory of the source directories or API directory
     * for a module or package.
     *
     * @param kind         the kind of location to search
     * @param e            the module or package
     * @param subdirectory the optional subdirectory
     * @param kinds        the kinds of files
     * @param recurse      whether to recurse into subdirectories
     *
     * @return the list of files
     */
    public abstract List<JavaFileObject> listFiles(LocationKind kind, Element e, String subdirectory,
                                                   Set<JavaFileObject.Kind> kinds, boolean recurse);

    /**
     * Returns the {@code Elements Elements} utility class for this API.
     *
     * @return the {@code Elements} utility class
     */
    public abstract Elements getElements();

    /**
     * Returns the {@code Types Types} utility class for this API.
     *
     * @return the {@code Types} utility class
     */
    public abstract Types getTypes();

    /**
     * Returns the {@code DocTrees DocTrees} utility class for this API.
     *
     * @return the {@code DocTrees} utility class
     */
    public abstract DocTrees getTrees();

    static class JavacAPI extends API {
        private List<String> javacOpts;
        private List<String> fmOpts; // just for verbose reporting
        private int platformVersion;
        private Elements elements;
        private Types types;
        private DocTrees docTrees;
        private SerializedFormFactory serializedFormFactory;
        private Map<String, SerializedFormDocs> serializedFormDocsMap;
        private final Path apiDir;
        private final APIReader apiReader;
        private final boolean apiModuleDirectories;

        /**
         * A tuple containing a location and the kinds of files that may be read from that location.
         */
        private static class LocationAndKinds {
            final Location locn;
            final Set<JavaFileObject.Kind> kinds;
            LocationAndKinds(Location locn, Set<JavaFileObject.Kind> kinds) {
                this.locn = locn;
                this.kinds = kinds;
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "[locn:" + locn + ",kinds:" + kinds + "]";
            }
        }

        private Map<String, LocationAndKinds> moduleLocationAndKinds;
        private Set<ModuleElement> modules;
        private Map<ModuleElement, Set<PackageElement>> modulePackages;

        private Set<PackageElement> packages;

        /** Map of recently accessed APIDocs, organized as an LRU cache. */
        private static final int MAX_APIDOCS = 20;
        private LinkedHashMap<Path, APIDocs> apiDocs = new LinkedHashMap<>(MAX_APIDOCS, 0.9f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Path,APIDocs> eldest) {
                return size() > MAX_APIDOCS;
            }
        };

        JavacAPI(APIOptions opts, Selector s, AccessKind ak, Log log, boolean verboseOptions) {
            super(opts, s, ak, log, verboseOptions);

            fmOpts = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : opts.fileManagerOpts.entrySet()) {
                String opt = e.getKey();
                fmOpts.add(opt);
                List<String> args = e.getValue();
                for (String arg : args) {
                    fmOpts.add(arg);
                    Iterator<String> argIter = arg == null
                            ? Collections.emptyIterator()
                            : Collections.singleton(arg).iterator();
                    boolean ok = fileManager.handleOption(opt, argIter);
                    if (!ok) {
                        throw new IllegalArgumentException(opt);
                    }
                }
            }

            javacOpts = new ArrayList<>();

            if (opts.release != null && opts.source != null) {
                throw new IllegalArgumentException("both --release and --source");
            } else if (opts.release != null) {
                javacOpts.addAll(List.of("--release", opts.release));
                platformVersion = Integer.parseInt(opts.release);
            } else if (opts.source != null) {
                javacOpts.addAll(List.of("--source", opts.source));
                platformVersion = Integer.parseInt(opts.source);
            } else {
                platformVersion = Runtime.version().feature();
            }

            if (opts.enablePreview) {
                if (opts.release == null && opts.source == null) {
                    throw new IllegalArgumentException("either --release or --source must be specified with --enable-preview");
                }
                javacOpts.add("--enable-preview");
            }

            apiDir = opts.apiDir;
            apiReader = new APIReader(log);

            if (apiDir == null) {
                apiModuleDirectories = false;
            } else {
                boolean foundModuleSummary = false;
                try (Stream<Path> ds = Files.walk(apiDir, 1)) {
                    foundModuleSummary = ds.anyMatch(p -> Files.isDirectory(p) && Files.exists(p.resolve("module-summary.html")));
                } catch (IOException e) {
                    // TODO: report error and exit
                }
                apiModuleDirectories = foundModuleSummary;
            }
        }

        void initJavac(Set<String> selectedModules) {
            if (!selectedModules.isEmpty()) {
                javacOpts.add("--add-modules");
                javacOpts.add(String.join(",", selectedModules));
            }
            javacOpts.add("-proc:only");
            if (verboseOptions) {
                showJavacOptions();
            }
            JavacTask javacTask = (JavacTask) compiler.getTask(log.err, fileManager, this::reportDiagnostic, javacOpts, null, null);
            elements = javacTask.getElements();
            elements.getModuleElement("java.base"); // forces module graph to be instantiated, etc
            types = javacTask.getTypes();
            docTrees = DocTrees.instance(javacTask);
            serializedFormFactory = new SerializedFormFactory(this) {
                @Override
                public SerializedFormDocs getSerializedFormDocs(TypeElement te) {
                    // TODO: should we stop if there is a problem reading the file, other than "file not found"
                    //       related: should this file be read proactively, if it exists, and if comparing API descriptions
                    if (serializedFormDocsMap == null) {
                        if (apiDir == null) {
                            serializedFormDocsMap = Collections.emptyMap();
                        } else {
                            Path file = apiDir.resolve("serialized-form.html");
                            serializedFormDocsMap = SerializedFormDocs.read(log, file);
                        }
                    }
                    String name = te.getQualifiedName().toString();
                    return serializedFormDocsMap.get(name);
                }
            };
        }

        private void showJavacOptions() {
            log.err.println("Effective javac options for API " + name);
            boolean needNewline = false;
            // The following is a convenient fiction: to report all the javac options as "equivalent".
            // In reality, the file manager options have already been handled separately and are
            // now stashed in the file manager, without easy access (except via Locations).
            List<String> allOpts = new ArrayList<>();
            allOpts.addAll(fmOpts);
            allOpts.addAll(javacOpts);
            for (String opt : allOpts) {
                if (opt.startsWith("-")) {
                    if (needNewline) {
                        log.err.println();
                    }
                    log.err.print("  ");
                } else {
                    log.err.print(" ");
                }
                log.err.print(opt);
                needNewline = true;
            }
            if (needNewline) {
                log.err.println();
            }
        }

        @Override
        public Set<PackageElement> getPackageElements() {
            if (packages == null) {
                initJavac(Collections.emptySet());

                packages = new HashSet<>();

                List<LocationAndKinds> locationAndKinds = new ArrayList<>();
                if (fileManager.hasLocation(StandardLocation.SOURCE_PATH)) {
                    locationAndKinds.add(new LocationAndKinds(StandardLocation.SOURCE_PATH,
                            EnumSet.of(JavaFileObject.Kind.SOURCE, JavaFileObject.Kind.HTML, JavaFileObject.Kind.OTHER)));
                    locationAndKinds.add(new LocationAndKinds(StandardLocation.CLASS_PATH,
                            EnumSet.of(JavaFileObject.Kind.CLASS)));
                } else {
                    locationAndKinds.add(new LocationAndKinds(StandardLocation.CLASS_PATH,
                            EnumSet.allOf(JavaFileObject.Kind.class)));
                }

                if (platformVersion <= 8) {
                    locationAndKinds.add(new LocationAndKinds(StandardLocation.PLATFORM_CLASS_PATH,
                            EnumSet.of(JavaFileObject.Kind.CLASS)));
                }

                packages = getPackageElements(null, locationAndKinds);
            }
            return packages;
        }

        @Override
        public Set<ModuleElement> getModuleElements() {
            if (modules == null) {
                // Note: the following code does not support module source code on the source path.
                // If necessary, use the module-specific form of --module-source-path to specify
                // the source path for a single module.
                // While it would be reasonable to check the source path, and even look for
                // module-info.java, determining the module name would require reading and parsing
                // the file. Not impossible, but ...
                List<Location> modulePaths = List.of(
                        StandardLocation.MODULE_SOURCE_PATH,
                        StandardLocation.UPGRADE_MODULE_PATH,
                        StandardLocation.SYSTEM_MODULES,
                        StandardLocation.MODULE_PATH);

                moduleLocationAndKinds = new HashMap<>();
                for (Location mp : modulePaths) {
                    Set<JavaFileObject.Kind> kinds = (mp == StandardLocation.MODULE_SOURCE_PATH)
                            ? EnumSet.of(JavaFileObject.Kind.SOURCE, JavaFileObject.Kind.HTML, JavaFileObject.Kind.OTHER)
                            : EnumSet.of(JavaFileObject.Kind.CLASS);
                    try {
                        for (Set<Location> locns : fileManager.listLocationsForModules(mp)) {
                            for (Location locn : locns) {
                                String mdlName = fileManager.inferModuleName(locn);
                                moduleLocationAndKinds.putIfAbsent(mdlName, new LocationAndKinds(locn, kinds));
                            }
                        }
                    } catch (IOException e) {
                        // ignore for now; eventually save first and suppress the rest
                    }
                }

                Set<String> selectedModules = moduleLocationAndKinds.keySet().stream()
                        .filter(selector::acceptsModule)
                        .collect(Collectors.toSet());

                initJavac(selectedModules);

                modules = selectedModules.stream()
                        .map(elements::getModuleElement)
                        .collect(Collectors.toSet());
            }

            return modules;
        }

        /**
         * {@inheritDoc}
         *
         * @see #getExportedPackageElements(ModuleElement)
         */
        @Override
        public Set<PackageElement> getPackageElements(ModuleElement m) {
            if (modulePackages == null) {
                modulePackages = new LinkedHashMap<>();
            }

            Set<PackageElement> packages = modulePackages.get(m);
            if (packages == null) {
                String moduleName = m.getQualifiedName().toString();
                LocationAndKinds lk = moduleLocationAndKinds.get(moduleName);
                packages = (lk == null) ? Collections.emptySet() : getPackageElements(m, List.of(lk));
                modulePackages.put(m, packages);
            }
            return packages;
        }

        @Override
        public Set<PackageElement> getExportedPackageElements(ModuleElement m) {
            Set<PackageElement> allExported = m.getDirectives().stream()
                    .filter(d -> d.getKind() == ModuleElement.DirectiveKind.EXPORTS)
                    .map(d -> (ModuleElement.ExportsDirective) d)
                    .filter(d -> d.getTargetModules() == null)
                    .map(ModuleElement.ExportsDirective::getPackage)
                    .collect(Collectors.toSet());

            return getPackageElements(m).stream()
                    .filter(allExported::contains)
                    .collect(Collectors.toSet());
        }

        /**
         * Returns the packages found for a given module (or no module) in a series of locations.
         * The Language Model API does not provided an explicit way to obtain the collection of packages
         * for a module (or no module.) And so, the packages are determined by listing files in combinations
         * of the selected packages and given locations. The package names are inferred from the file names,
         * and the packages are then obtained by using {@link Elements#getPackageElement(CharSequence)} or
         * {@link Elements#getPackageElement(ModuleElement, CharSequence)} as appropriate.
         *
         * @param me            the module, or {@code null} for "no module" or the unnamed module
         * @param locationAndKinds the locations and the kinds of files to check in those locations
         *
         * @return the packages
         */
        private Set<PackageElement> getPackageElements(ModuleElement me, List<LocationAndKinds> locationAndKinds) {
            String moduleName = (me == null) ? null : me.getQualifiedName().toString();
            Map<String,Boolean> selectedPackages = new LinkedHashMap<>();
            for (Selector.Entry entry : selector.includes) {
                if (!entry.includeModule.test(moduleName)) {
                    continue;
                }
                String packagePart = entry.packagePart;
                boolean recurse = entry.typePart.equals("**");
                for (LocationAndKinds lk : locationAndKinds) {
                    try {
                        for (JavaFileObject f : fileManager.list(lk.locn, packagePart, lk.kinds, recurse)) {
                            String binaryName = fileManager.inferBinaryName(lk.locn, f);
                            int lastDot = binaryName.lastIndexOf(".");
                            if (lastDot != -1) {
                                String packageName = binaryName.substring(0, lastDot);
                                if (!selectedPackages.containsKey(packageName)) {
                                    selectedPackages.put(packageName, selector.acceptsPackage(moduleName, packageName));
                                }
                            }
                        }
                    } catch (IOException e) {
                        // TODO: ignore for now; eventually save first and suppress the rest, and abort?
                    }
                }
            }

            Function<String,PackageElement> getPackageElement = (me == null)
                    ? pkgName -> elements.getPackageElement(pkgName)
                    : pkgName -> elements.getPackageElement(me, pkgName);

            return selectedPackages.entrySet().stream()
                    .filter(Entry::getValue)
                    .map(e -> getPackageElement.apply(e.getKey()))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<TypeElement> getTypeElements(PackageElement p) {
            ModuleElement me = elements.getModuleOf(p);
            String mn = (me == null) ? null : me.getQualifiedName().toString();
            String pn = p.getQualifiedName().toString();
            Set<TypeElement> types = new HashSet<>();
            for (Element e : p.getEnclosedElements()) {
                if (accessKind.accepts(e)) {
                    TypeElement te = (TypeElement) e;
                    String tn = te.getSimpleName().toString();
                    if (selector.acceptsType(mn, pn, tn)) {
                        types.add(te);
                    }
                }
            }
            return types;
        }

        @Override
        public Map<? extends ExecutableElement, ? extends AnnotationValue> getAnnotationValuesWithDefaults(AnnotationMirror am) {
            return elements.getElementValuesWithDefaults(am);
        }

        @Override
        public SerializedForm getSerializedForm(TypeElement e) {
            return serializedFormFactory.get(e);
        }

        @Override
        public DocCommentTree getDocComment(Element e) {
            return docTrees.getDocCommentTree(e);
        }

        private final ApiDescriptionVisitor apiDescriptionVisitor = new ApiDescriptionVisitor();

        @Override
        public String getApiDescription(Element e) {
            if (apiDir == null) {
                return null;
            }

            APIDocs d = apiDocs.computeIfAbsent(getSpecFile(e), f -> APIDocs.read(apiReader, f));
            return apiDescriptionVisitor.visit(e, d);
        }

        private Path getSpecFile(Element e) {
            switch (e.getKind()) {
                case MODULE:
                    return getSpecDir(e).resolve("module-summary.html");

                case PACKAGE:
                    return getSpecDir(e).resolve("package-summary.html");

                default:
                    var eKind = e.getKind();
                    if (eKind.isClass() || eKind.isInterface()) {
                        StringBuilder typeFile = new StringBuilder(e.getSimpleName() + ".html");
                        while (e.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
                            e = e.getEnclosingElement();
                            typeFile.insert(0, e.getSimpleName() + ".");
                        }
                        return getSpecDir(e).resolve(typeFile.toString());
                    } else {
                        return getSpecFile(e.getEnclosingElement());
                    }
            }
        }

        private Path getSpecDir(Element e) {
            switch (e.getKind()) {
                case MODULE: {
                    ModuleElement me = (ModuleElement) e;
                    return apiDir.resolve(me.getQualifiedName().toString());
                }

                case PACKAGE: {
                    PackageElement pe = (PackageElement) e;
                    ModuleElement me = (ModuleElement) pe.getEnclosingElement();
                    Path dir = (me == null) ? apiDir : getSpecDir(me);
                    String sep = dir.getFileSystem().getSeparator();
                    return dir.resolve(pe.getQualifiedName().toString().replace(".", sep));
                }

                default:
                    return getSpecDir(e.getEnclosingElement());
            }
        }

        @Override
        public DocCommentTree getDocComment(JavaFileObject fo) {
            if (fo.getKind() != JavaFileObject.Kind.HTML) {
                throw new IllegalArgumentException(fo.getName());
            }

            return docTrees.getDocCommentTree(fo);
        }

        @Override
        public String getApiDescription(JavaFileObject fo) {
            if (fo.getKind() != JavaFileObject.Kind.HTML) {
                throw new IllegalArgumentException(fo.getName());
            }

            Path p = fileManager.asPath(fo);
            if (p == null || !Files.exists(p)) {
                return null;
            }

            try {
                // TODO: consider using a new reader, DocFileReader, possibly returning a new object,
                //       containing title and body, or a single more versatile pattern
                String s = Files.readString(p);
                Matcher startMain = Pattern.compile("(?i)<main\\b[^>]*>").matcher(s);
                if (startMain.find()) {
                    int start = startMain.end();
                    Matcher endMain = Pattern.compile("(?i)</main>").matcher(s);
                    if (endMain.find(start)) {
                        int end = endMain.start();
                        return s.substring(start, end);
                    }
                }
                Matcher startBody = Pattern.compile("(?i)<body\\b[^>]*>").matcher(s);
                if (startBody.find()) {
                    int start = startBody.end();
                    Matcher endBody = Pattern.compile("(?i)</body>").matcher(s);
                    if (endBody.find(start)) {
                        int end = endBody.start();
                        return s.substring(start, end);
                    }
                }
                // TODO: report cannot find content
                return null;
            } catch (IOException e) {
                // TODO: should report
                return null;
            }
        }

        @Override
        public byte[] getAllBytes(JavaFileObject fo) {
            Path p = fileManager.asPath(fo);
            if (p == null) {
                return null;
            }
            try {
                return Files.readAllBytes(p);
            } catch (IOException e) {
                // TODO: report error, or propagate
                return null;
            }
        }

        @Override
        public List<JavaFileObject> listFiles(LocationKind kind, Element e, String subdirectory,
                                              Set<JavaFileObject.Kind> kinds, boolean recurse) {
            return switch (kind) {
                case SOURCE -> listSourceFiles(e, subdirectory, kinds, recurse);
                case API ->    listApiFiles(e, subdirectory, kinds, recurse);
            };
        }

        private List<JavaFileObject> listSourceFiles(Element e, String subdirectory,
                                                     Set<JavaFileObject.Kind> kinds, boolean recurse) {
            Location locn;
            ModuleElement me = elements.getModuleOf(e);
            if (me != null && !me.isUnnamed()) {
                LocationAndKinds lk = moduleLocationAndKinds.get(me.getQualifiedName().toString());
                locn = (lk == null) ? null : lk.locn;
            } else if (fileManager.hasLocation(StandardLocation.SOURCE_PATH)) {
                locn = StandardLocation.SOURCE_PATH;
            } else if (fileManager.hasLocation(StandardLocation.CLASS_PATH)) {
                locn = StandardLocation.CLASS_PATH;
            } else {
                locn = null;
            }

            if (locn == null) {
                return Collections.emptyList();
            }

            String dirName = switch (e.getKind()) {
                case MODULE -> "";
                case PACKAGE -> ((PackageElement) e).getQualifiedName().toString().replaceAll("\\.", "/");
                default -> throw new IllegalArgumentException(e.getKind() + " " + e);
            };

            if (subdirectory != null && !subdirectory.isEmpty()) {
                dirName = dirName.isEmpty() ? subdirectory : dirName + "/" + subdirectory;
            }

            List<JavaFileObject> files = new ArrayList<>();
            try {
                for (JavaFileObject f : fileManager.list(locn, dirName, kinds, recurse)) {
                    files.add(f);
                }
            } catch (IOException ex) {
                // TODO: ignore for now; eventually save first and suppress the rest, and abort?
                //       or simply throw up to caller, which can fail the comparison
            }
            return files;

        }

        private List<JavaFileObject> listApiFiles(Element e, String subdirectory,
                                                  Set<JavaFileObject.Kind> kinds, boolean recurse) {
            if (apiDir == null) {
                return Collections.emptyList();
            }

            Path dir = apiDir;
            ModuleElement me = elements.getModuleOf(e);
            if (me != null && !me.isUnnamed()) {
                // handle module directories anomaly from JDK 9
                if (apiModuleDirectories) {
                    dir = dir.resolve(me.getQualifiedName().toString());
                } else {
                    // doc files for named modules are not supported if there is no
                    // module subdirectory.
                    return Collections.emptyList();
                }
            }
            if (e instanceof PackageElement) {
                PackageElement pe = (PackageElement) e;
                if (!pe.isUnnamed()) {
                    dir = dir.resolve(pe.getQualifiedName().toString().replace(".", File.separator));
                }
            }
            if (subdirectory != null && !subdirectory.isEmpty()) {
                dir = dir.resolve(subdirectory);
            }

            List<Path> files = new ArrayList<>();
            try {
                boolean allKinds = kinds.equals(EnumSet.allOf(JavaFileObject.Kind.class));
                Files.walkFileTree(dir, Set.of(), recurse ? Integer.MAX_VALUE : 1, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (allKinds || kinds.contains(getKind(file))) {
                            files.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                // TODO: ignore for now; eventually save first and suppress the rest, and abort?
                //       or simply throw up to caller, which can fail the comparison
            }
            return asFileObjects(files);
        }

        private JavaFileObject.Kind getKind(Path file) {
            String name = file.getFileName().toString();
            int lastDot = name.lastIndexOf(name);
            String extn = lastDot == -1 ? "" : name.substring(lastDot + 1);
            return switch (extn) {
                case "java" -> JavaFileObject.Kind.SOURCE;
                case "class" -> JavaFileObject.Kind.CLASS;
                case "html" -> JavaFileObject.Kind.HTML;
                default -> JavaFileObject.Kind.OTHER;
            };
        }

        private List<JavaFileObject> asFileObjects(List<Path> files) {
            List<JavaFileObject> fileObjects = new ArrayList<>();
            for (JavaFileObject fo : fileManager.getJavaFileObjectsFromPaths(files)) {
                fileObjects.add(fo);
            }
            return fileObjects;
        }

        @Override
        public Elements getElements() {
            return elements;
        }

        @Override
        public Types getTypes() {
            return types;
        }

        @Override
        public DocTrees getTrees() {
            return docTrees;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + name + "]";
        }

        private void reportDiagnostic(Diagnostic<? extends JavaFileObject> d) {
            JavaFileObject fo = d.getSource();
            Path file = fo == null ? null : fileManager.asPath(fo);
            long line = d.getLineNumber();
            String message = d.getMessage(Locale.getDefault());

            switch (d.getKind()) {
                case ERROR -> log.error(file, line, null, message);
                case WARNING -> log.warning(file, line, null, message);
                case NOTE -> log.note(file, line, null, message);
            }
        }

        /**
         * A visitor to access the API description for an element from the collection
         * of API docs read from the appropriate file.
         * The visitor determines the appropriate method and/or signature to use to
         * access the description.
         */
        private static class ApiDescriptionVisitor implements ElementVisitor<String, APIDocs> {
            private ExecutableSignatureVisitor signatureVisitor = new ExecutableSignatureVisitor();

            @Override
            public String visit(Element e, APIDocs d) {
                return e.accept(this, d);
            }

            @Override
            public String visitModule(ModuleElement me, APIDocs d) {
                return d.getDescription();
            }

            @Override
            public String visitPackage(PackageElement pe, APIDocs d) {
                return d.getDescription();
            }

            @Override // CLASS, INTERFACE, ANNOTATION_TYPE, ENUM, etc (RECORD, SEALED_TYPE...)
            public String visitType(TypeElement te, APIDocs d) {
                return d.getDescription();
            }

            @Override // FIELD, ENUM_CONSTANT
            public String visitVariable(VariableElement ve, APIDocs d) {
                return d.getDescription(ve.getSimpleName().toString());
            }

            @Override // METHOD, CONSTRUCTOR
            public String visitExecutable(ExecutableElement ee, APIDocs d) {
                return d.getDescription(signatureVisitor.getSignature(ee));
            }

            @Override
            public String visitRecordComponent(RecordComponentElement tpe, APIDocs d) {
                // record components do not have distinct API descriptions of their own:
                // they are documented by @param tags in the enclosing element
                throw new IllegalArgumentException(tpe.getKind() + " " + tpe.getSimpleName());
            }

            @Override
            public String visitTypeParameter(TypeParameterElement tpe, APIDocs d) {
                // type parameters do not have distinct API descriptions of their own:
                // they are documented by @param tags in the enclosing element
                throw new IllegalArgumentException(tpe.getKind() + " " + tpe.getSimpleName());
            }

            @Override
            public String visitUnknown(Element e, APIDocs d) {
                throw new IllegalArgumentException(e.getKind() + " " + e.getSimpleName());
            }
        }

        /**
         * A visitor to determine the signature for an executable element, as used by
         * {@code javadoc} to identify the description for the element in the page
         * for the enclosing type, and thereby used to identify the description in
         * the appropriate {@code APIDocs} object.
         */
        private static class ExecutableSignatureVisitor
                implements ElementVisitor<Void, StringBuilder>, TypeVisitor<Void, StringBuilder> {
            String getSignature(ExecutableElement ee) {
                StringBuilder sb = new StringBuilder();
                sb.append(ee.getSimpleName());  // automatically uses <init> for constructor
                List<? extends VariableElement> params = ee.getParameters();
                if (params.isEmpty()) {
                    sb.append("()");
                } else {
                    String sep = "(";
                    for (VariableElement ve : ee.getParameters()) {
                        sb.append(sep);
                        ve.asType().accept(this, sb);
                        sep = ",";
                    }
                    sb.append(")");
                }
                return sb.toString();
            }

            @Override
            public Void visit(Element e, StringBuilder sb) {
                return e.accept(this, sb);
            }

            @Override
            public Void visitPackage(PackageElement e, StringBuilder sb) {
                error(e);
                return null;
            }

            @Override
            public Void visitType(TypeElement e, StringBuilder sb) {
                sb.append(e.getQualifiedName());
                return null;
            }

            @Override
            public Void visitVariable(VariableElement e, StringBuilder sb) {
                error(e);
                return null;
            }

            @Override
            public Void visitExecutable(ExecutableElement e, StringBuilder sb) {
                error(e);
                return null;
            }

            @Override
            public Void visitTypeParameter(TypeParameterElement e, StringBuilder sb) {
                sb.append(e.getSimpleName());
                return null;
            }

            @Override
            public Void visitUnknown(Element e, StringBuilder sb) {
                error(e);
                return null;
            }

            private void error(Element e) {
                throw new IllegalArgumentException(e.getKind() + "[" + e + "]");
            }

            @Override
            public Void visit(TypeMirror t, StringBuilder sb) {
                return t.accept(this, sb);
            }

            @Override
            public Void visitPrimitive(PrimitiveType t, StringBuilder sb) {
                sb.append(t.getKind().toString().toLowerCase(Locale.ROOT));
                return null;
            }

            @Override
            public Void visitNull(NullType t, StringBuilder sb) {
                throw new IllegalArgumentException(t.getKind() + " " + t);
            }

            @Override
            public Void visitArray(ArrayType t, StringBuilder sb) {
                visit(t.getComponentType(), sb);
                sb.append("[]");
                return null;
            }

            @Override
            public Void visitDeclared(DeclaredType t, StringBuilder sb) {
                visit(t.asElement(), sb);
                return null;
            }

            @Override
            public Void visitError(ErrorType t, StringBuilder sb) {
                error(t);
                return null;
            }

            @Override
            public Void visitTypeVariable(TypeVariable t, StringBuilder sb) {
                visit(t.asElement(), sb);
                return null;
            }

            @Override
            public Void visitWildcard(WildcardType t, StringBuilder sb) {
                visit(t.getSuperBound(), sb);
                return null;
            }

            @Override
            public Void visitExecutable(ExecutableType t, StringBuilder sb) {
                error(t);
                return null;
            }

            @Override
            public Void visitNoType(NoType t, StringBuilder sb) {
                error(t);
                return null;
            }

            @Override
            public Void visitUnknown(TypeMirror t, StringBuilder sb) {
                error(t);
                return null;
            }

            @Override
            public Void visitUnion(UnionType t, StringBuilder sb) {
                error(t); // TODO? should not happen in args of ExecutableElement
                return null;
            }

            @Override
            public Void visitIntersection(IntersectionType t, StringBuilder sb) {
                error(t); // TODO? should not happen in args of ExecutableElement
                return null;
            }

            private void error(TypeMirror t) {
                throw new IllegalArgumentException(t.getKind() + "[" + t + "]");
            }

        }
    }
}

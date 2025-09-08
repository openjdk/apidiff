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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import java.util.Optional;
import jdk.codetools.apidiff.Abort;
import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.Messages;
import jdk.codetools.apidiff.Notes;
import jdk.codetools.apidiff.Options;
import jdk.codetools.apidiff.Options.InfoTextKind;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.Entity;
import jdk.codetools.apidiff.html.HtmlAttr;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.RawHtml;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.model.API.LocationKind;
import jdk.codetools.apidiff.model.APIMap;
import jdk.codetools.apidiff.model.DocFile;
import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.MemberElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeParameterElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.Position;
import jdk.codetools.apidiff.model.Position.ElementPosition;
import jdk.codetools.apidiff.model.Position.RelativePosition;
import jdk.codetools.apidiff.model.SerializedForm;
import jdk.codetools.apidiff.model.SerializedFormDocs;
import jdk.codetools.apidiff.report.Reporter;
import jdk.codetools.apidiff.report.SignatureVisitor;
import jdk.codetools.apidiff.report.html.ResultTable.CountKind;

/**
 * Base class for reporters handling the different pages of an HTML report.
 */
abstract class PageReporter<K extends ElementKey> implements Reporter {

    /** The enclosing HTML reporter. */
    protected final HtmlReporter parent;

    /** The log to which to report diagnostics. */
    protected final Log log;

    /** The element key for the page. */
    protected final K pageKey;

    /** The file for the page. */
    protected final DocPath file;

    /** A utility class to generate links to other pages. */
    protected final Links links;

    protected final Messages msgs;

    // The following collections accumulate the results reported with _report..._ methods.
    // TODO: the methods that put items into these maps should check they are not
    //       overwriting any existing information

    protected Map<Position, Set<API>> missing;
    protected Map<Position, APIMap<? extends AnnotationMirror>> differentAnnotations;
    protected Map<Position, APIMap<? extends AnnotationValue>> differentAnnotationValues;
    protected Map<Position, APIMap<? extends Directive>> differentDirectives;
    protected Map<Position, APIMap<? extends Element>> differentModifiers;
    protected Map<Position, APIMap<? extends Element>> differentKinds;
    protected Map<Position, APIMap<? extends TypeParameterElement>> differentTypeParameters;
    protected Map<Position, APIMap<? extends TypeMirror>> differentTypes;
    protected Map<Position, APIMap<List<? extends TypeMirror>>> differentThrownTypes;
    protected Map<Position, APIMap<List<? extends TypeMirror>>> differentSuperinterfaces;
    protected Map<Position, APIMap<List<? extends TypeMirror>>> differentPermittedSubclasses;
    protected Map<Position, APIMap<?>> differentValues;
    protected Map<Position, APIMap<String>> differentRawDocComments;
    protected Map<Position, APIMap<String>> differentApiDescriptions;
    protected Map<Position, APIMap<DocFile>> differentDocFiles;

    /** The API maps for the items on this page. */
    protected final Map<Position, APIMap<?>> apiMaps;

    /** The comparison results for the items on this page. */
    protected final Map<Position, Boolean> results;

    protected final ResultTable resultTable;

    protected PageReporter(HtmlReporter parent) {
        this(parent, null, new DocPath("index.html"));
    }

    protected PageReporter(HtmlReporter parent, K eKey) {
        this(parent, eKey, new GetFileVisitor().getFile(eKey));
    }

    private PageReporter(HtmlReporter parent, K eKey, DocPath file) {
        this.parent = parent;
        this.pageKey = eKey;
        this.file = file;

        log = Objects.requireNonNull(parent.log);
        msgs = Objects.requireNonNull(parent.msgs);
        links = new Links(file);
        resultTable = new ResultTable(msgs, links);

        missing = Collections.emptyMap();
        differentAnnotations = Collections.emptyMap();
        differentAnnotationValues = Collections.emptyMap();
        differentDirectives = Collections.emptyMap();
        differentModifiers = Collections.emptyMap();
        differentKinds = Collections.emptyMap();
        differentTypes = Collections.emptyMap();
        differentTypeParameters = Collections.emptyMap();
        differentThrownTypes = Collections.emptyMap();
        differentSuperinterfaces = Collections.emptyMap();
        differentPermittedSubclasses = Collections.emptyMap();
        differentValues = Collections.emptyMap();
        differentRawDocComments = Collections.emptyMap();
        differentApiDescriptions = Collections.emptyMap();
        differentDocFiles = Collections.emptyMap();

        apiMaps = new HashMap<>();
        results = new HashMap<>();
    }

    //<editor-fold desc="Implements Reporter">

    @Override
    public void comparing(Position pos, APIMap<?> apiMap) {
        apiMaps.put(pos, apiMap);
    }

    @Override
    public void completed(Position ePos, boolean equal) {
        results.put(ePos, equal);
        if (ePos.isElement()) {
            ElementKey eKey = ePos.asElementKey();
            if (eKey == pageKey) {
                writeFile();
                ElementKey enclKey = eKey.getEnclosingKey();
                PageReporter<?> enclPage = parent.getPageReporter(enclKey);
                enclPage.completed(ePos, equal);
                enclPage.resultTable.addAll(pageKey, resultTable.getTotals());
            }
        }
    }

    @Override
    public void reportMissing(Position ePos, Set<API> apis) {
        if (missing.isEmpty()) {
            missing = new HashMap<>();
        }
        missing.put(ePos, apis);
        if (apis.contains(parent.latestAPI)) {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_REMOVED);
        } else {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_ADDED);
        }
    }

    @Override
    public void reportDifferentAnnotations(Position amPos, APIMap<? extends AnnotationMirror> amMap) {
        if (differentAnnotations.isEmpty()) {
            differentAnnotations = new HashMap<>();
        }
        differentAnnotations.put(amPos, amMap);
        if (amMap.containsKey(parent.latestAPI) && amMap.containsKey(parent.previousAPI)) {
            resultTable.inc(amPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentAnnotationValues(Position avPos, APIMap<? extends AnnotationValue> avMap) {
        if (differentAnnotationValues.isEmpty()) {
            differentAnnotationValues = new HashMap<>();
        }
        differentAnnotationValues.put(avPos, avMap);
        if (avMap.containsKey(parent.latestAPI) && avMap.containsKey(parent.previousAPI)) {
            resultTable.inc(avPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentDirectives(Position dPos, APIMap<? extends Directive> dMap) {
        if (differentDirectives.isEmpty()) {
            differentDirectives = new HashMap<>();
        }
        differentDirectives.put(dPos, dMap);
        if (dMap.containsKey(parent.latestAPI) && dMap.containsKey(parent.previousAPI)) {
            resultTable.inc(dPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentModifiers(Position ePos, APIMap<? extends Element> eMap) {
        if (differentModifiers.isEmpty()) {
            differentModifiers = new HashMap<>();
        }
        differentModifiers.put(ePos, eMap);
        if (eMap.containsKey(parent.latestAPI) && eMap.containsKey(parent.previousAPI)) {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentKinds(Position ePos, APIMap<? extends Element> eMap) {
        if (differentKinds.isEmpty()) {
            differentKinds = new HashMap<>();
        }
        differentKinds.put(ePos, eMap);
        if (eMap.containsKey(parent.latestAPI) && eMap.containsKey(parent.previousAPI)) {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentNames(Position ePos, APIMap<? extends Element> eMap) {
        // ignore, for now; compared locally for record components
    }

    @Override
    public void reportDifferentValues(Position ePos, APIMap<?> vMap) {
        if (differentValues.isEmpty()) {
            differentValues = new HashMap<>();
        }
        differentValues.put(ePos, vMap);
        if (vMap.containsKey(parent.latestAPI) && vMap.containsKey(parent.previousAPI)) {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentTypeParameters(Position ePos, APIMap<? extends TypeParameterElement> eMap) {
        if (differentTypeParameters.isEmpty()) {
            differentTypeParameters = new HashMap<>();
        }
        differentTypeParameters.put(ePos, eMap);
        if (eMap.containsKey(parent.latestAPI) && eMap.containsKey(parent.previousAPI)) {
            resultTable.inc(ePos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentTypes(Position tPos, APIMap<? extends TypeMirror> tMap) {
        if (differentTypes.isEmpty()) {
            differentTypes = new HashMap<>();
        }
        differentTypes.put(tPos, tMap);
        if (tMap.containsKey(parent.latestAPI) && tMap.containsKey(parent.previousAPI)) {
            resultTable.inc(tPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentThrownTypes(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        if (differentThrownTypes.isEmpty()) {
            differentThrownTypes = new HashMap<>();
        }
        differentThrownTypes.put(tPos, tMap);
        if (tMap.containsKey(parent.latestAPI) && tMap.containsKey(parent.previousAPI)) {
            resultTable.inc(tPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentSuperinterfaces(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        if (differentSuperinterfaces.isEmpty()) {
            differentSuperinterfaces = new HashMap<>();
        }
        differentSuperinterfaces.put(tPos, tMap);
        if (tMap.containsKey(parent.latestAPI) && tMap.containsKey(parent.previousAPI)) {
            resultTable.inc(tPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentPermittedSubclasses(Position tPos, APIMap<List<? extends TypeMirror>> tMap) {
        if (differentPermittedSubclasses.isEmpty()) {
            differentPermittedSubclasses = new HashMap<>();
        }
        differentPermittedSubclasses.put(tPos, tMap);
        if (tMap.containsKey(parent.latestAPI) && tMap.containsKey(parent.previousAPI)) {
            resultTable.inc(tPos.getElementKey(), CountKind.ELEMENT_CHANGED);
        }
    }

    @Override
    public void reportDifferentRawDocComments(Position tPos, APIMap<String> cMap) {
        if (differentRawDocComments.isEmpty()) {
            differentRawDocComments = new HashMap<>();
        }
        differentRawDocComments.put(tPos, cMap);
        // count changes in TextDiffBuilder
    }

    @Override
    public void reportDifferentApiDescriptions(Position tPos, APIMap<String> dMap) {
        if (differentApiDescriptions.isEmpty()) {
            differentApiDescriptions = new HashMap<>();
        }
        differentApiDescriptions.put(tPos, dMap);
        // count changes in HtmlDiffBuilder
    }

    @Override
    public void reportDifferentDocFiles(Position fPos, APIMap<DocFile> fMap) {
        if (differentDocFiles.isEmpty()) {
            differentDocFiles = new HashMap<>();
        }
        differentDocFiles.put(fPos, fMap);
        // count changes in HtmlDiffBuilder
    }
    //</editor-fold>

    protected void writeFile() throws Abort {
        writeFile(file, buildPageContent());
    }

    protected void writeFile(DocPath file, HtmlTree content) {
        Path path = file.resolveAgainst(parent.outDir);

        Path dir = path.getParent();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("report.err.cant-create-directory", dir, e);
            throw new Abort();
        }

        try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            content.write(out);
        } catch (IOException e) {
            log.error("report.err.error-writing-file", path, e);
            throw new Abort();
        }
    }

    protected HtmlTree buildPageContent() {
        return new HtmlTree(TagName.HTML, buildHead(), buildBody());
    }

    /**
     * Returns the {@code <head>} element for the page.
     *
     * The element is mostly standard, with customizations for the
     * {@code <title>} and possibly the set of stylesheets imported
     * from the spec file, if the tool is comparing generated specs.
     *
     * @return the {@code <head>} element for the page.
     */
    protected HtmlTree buildHead() {
        String title = getTitle();
        if (parent.options.getTitle() != null) {
            title = String.format("%s: %s", parent.options.getTitle(), title);
        }
        return HtmlTree.HEAD("UTF-8", title)
                .add(HtmlTree.META("generator", "apidiff"))
                .add(parent.getStylesheets().stream()
                        .map(links::getPath)
                        .map(l -> HtmlTree.LINK("stylesheet", l.getPath())));
    }

    protected abstract String getTitle();

    /**
     * Returns the {@code <body>} element for the page.
     *
     * TODO: the {@code <body>} element should follow a standard pattern:
     *   <ul>
     *     <li>A header, including navigation and a page heading
     *     <li>A report on the full "signature" of this element
     *     <li>A report on the doc comments for this element
     *     <li>A report on any enclosed elements: this will be
     *       a summary table with links for MODULE and PACKAGE,
     *       and inline details for members of TYPE.
     *     <li>A footer, including possible legal/copyright text
     *   </ul>
     *
     * @return the {@code <body>} element for the page.
     */
    protected HtmlTree buildBody() {
        Position pagePos = Position.of(pageKey);
        String pageClass = pageKey.kind.toString().toLowerCase(Locale.ROOT);
        HtmlTree body = HtmlTree.BODY().setClass(pageClass);
        body.add(buildHeader());
        HtmlTree main = HtmlTree.MAIN();
        main.add(buildPageHeading());
        main.add(buildPageElement());
        main.add(buildDocComments(pagePos));
        main.add(buildAPIDescriptions(pagePos));
        main.add(buildEnclosedElements());
        main.add(buildResultTable());
        body.add(main);
        body.add(buildFooter());
        if (parent.options.getHiddenOption("show-debug-summary") != null) {
            body.add(new DebugSummary().build());
        }
        return body;
    }

    /**
     * Builds the header for the page.
     *
     * @return the header
     */
    protected Content buildHeader() {
        List<Content> contents = new ArrayList<>();
        String topText = parent.options.getInfoText(InfoTextKind.TOP);
        if (topText != null) {
            contents.add(HtmlTree.DIV(new RawHtml(topText)).setClass("info"));
        }
        contents.add(buildNav(InfoTextKind.HEADER));
        return new HtmlTree(TagName.HEADER, contents);
    }

    /**
     * Builds the footer for the page.
     *
     * @return the footer
     */
    protected Content buildFooter() {
        List<Content> contents = new ArrayList<>();
        contents.add(buildNav(InfoTextKind.FOOTER));
        String bottomText = parent.options.getInfoText(InfoTextKind.BOTTOM);
        if (bottomText != null) {
            contents.add(HtmlTree.DIV(new RawHtml(bottomText)).setClass("info"));
        }
        return new HtmlTree(TagName.FOOTER, contents);
    }

    /**
     * Builds the main navigation bar for the page.
     *
     * @param kind the kind of info-text to be included in the bar
     *
     * @return the navigation bar
     */
    protected Content buildNav(InfoTextKind kind) {
        List<Content> contents = new ArrayList<>();
        String infoText = parent.options.getInfoText(kind);
        if (infoText == null) {
            infoText = String.join(" : ", parent.options.getAllAPIOptions().keySet());
        }
        contents.add(HtmlTree.DIV(new RawHtml(infoText)).setClass("info"));
        Text index = Text.of(parent.indexPageReporter.getName());
        HtmlTree ul = HtmlTree.UL();
        ul.add(HtmlTree.LI((pageKey == null) ? index : HtmlTree.A(links.getPath("index.html").getPath(), index)));
        contents.add(HtmlTree.NAV(ul));
        return new HtmlTree(TagName.DIV, contents).setClass("bar");
    }

    /**
     * Builds the page heading, based on the pageKey.
     *
     * @return the page heading
     */
    protected Content buildPageHeading() {
        return new PageHeading(Position.of(pageKey)).toContent();
    }

    /**
     * Builds the "element" details for the pageKey, containing the check or cross,
     * details about whether the element is missing in some instances of the API,
     * any notes, and the signature of the element.
     *
     * @return the "element" details
     */
    protected Content buildPageElement() {
        Position pagePos = Position.of(pageKey);
        List<Content> prelude = List.of(getResultGlyph(pagePos).getContent(), buildMissingInfo(pagePos), buildNotes(pageKey));
        Content signature = buildSignature();
        return HtmlTree.DIV().setClass("element").add(prelude).add(signature);
    }

    protected Content buildNotes(Position pos) {
        return pos.isElement() ? buildNotes(((ElementPosition) pos).key) : Content.empty;
    }

    /**
     * Builds the list of notes (if any) associated with a given element key.
     *
     * @param eKey the element key
     *
     * @return the list of notes.
     */
    protected Content buildNotes(ElementKey eKey) {
        if (parent.notes == null) {
            return Content.empty;
        }

        Map<Notes.Entry, Boolean> entries = parent.notes.getEntries(eKey);
        if (entries.isEmpty()) {
            return Content.empty;
        }

        var comp = Comparator.comparing((Notes.Entry e) -> e.description).thenComparing(e -> e.uri);
        Map<Notes.Entry, Boolean> sorted = new TreeMap<>(comp);
        sorted.putAll(entries);

        NotesTable notesTable = parent.indexPageReporter.notesTable;
        sorted.forEach((e, isParent) -> {
            if (!isParent) {
                notesTable.add(e, eKey);
            }
        });

        List<Content> contents = new ArrayList<>();
        contents.add(Text.of(msgs.getString("notes.prefix")));
        contents.add(Text.SPACE);

        boolean first = true;
        for (Notes.Entry e : sorted.keySet()) {
            if (first) {
                first = false;
            } else {
                contents.add(Text.of(", "));
            }
            contents.add(HtmlTree.A(e.uri, Text.of(e.description)));
        }
        contents.add(Text.of("."));

        return HtmlTree.SPAN(contents).setClass("notes");
    }

    /**
     * Builds the signature for the element key for the page.
     *
     * @return the signature
     */
    protected abstract Content buildSignature();

    /**
     * Builds the information about the documentation comments for a position in the API.
     *
     * @param pos the position
     *
     * @return the information about the documentation comments
     */
    protected Content buildDocComments(Position pos) {
        var options = getOptions();
        if (options.compareDocComments()) {
            APIMap<String> docComments = differentRawDocComments.get(pos);
            if (docComments == null) {
                docComments = getDocCommentMap(pos);
            }
            if (docComments != null && !docComments.isEmpty()) {
                TextDiffBuilder b = new TextDiffBuilder(this);
                List<Content> contents = b.build(docComments, ck -> resultTable.inc(pos.getElementKey(), ck));
                return new HtmlTree(TagName.DIV, contents).setClass("rawDocComments");
            }
        }

        return Content.empty;
    }

    private APIMap<String> getDocCommentMap(Position pos) {
        if (pos.is(RelativePosition.Kind.SERIALIZED_FIELD)) {
            // get the APIMap<SerializedForm> and build the field map from that
            @SuppressWarnings("unchecked")
            RelativePosition<String> sfPos = (RelativePosition<String>) pos;
            APIMap<SerializedForm> sfMap = getSerializedFormMap(pos);
            return sfMap == null ? null
                    : sfMap.map((api, sf) -> {
                        SerializedForm.Field f = sf.getField(sfPos.index);
                        List<? extends DocTree> tree = f == null ? null : f.getDocComment();
                        return tree == null ? null : tree.toString(); // TODO check .toString()
                    });
        } else if (pos.is(RelativePosition.Kind.DOC_FILE)) {
            @SuppressWarnings("unchecked")
            APIMap<DocFile> fmap = (APIMap<DocFile>) apiMaps.get(pos);
            return fmap.map((api, df) -> {
                JavaFileObject fo = df.files.get(LocationKind.SOURCE);
                DocCommentTree tree = fo == null ? null : api.getDocComment(fo);
                return tree == null ? null : tree.toString();
            });
        } else {
            APIMap<? extends Element> eMap = getElementMap(pos);
            return eMap.map((api, e) -> {
                DocCommentTree dct = api.getDocComment(e);
                return dct == null ? null : dct.toString();
            });
        }
    }

    /**
     * Builds the information about the API descriptions for a position in the API.
     *
     * @param pos the position
     *
     * @return the information about the documentation comments
     */
    protected Content buildAPIDescriptions(Position pos) {
        var options = getOptions();
        if (options.compareApiDescriptions()) {
            APIMap<String> apiDescriptions = differentApiDescriptions.get(pos);
            if (apiDescriptions == null) {
                apiDescriptions = getAPIDescriptionMap(pos);
            }
            if (apiDescriptions != null && !apiDescriptions.isEmpty()) {
                var b = options.compareApiDescriptionsAsText()
                        ? new TextDiffBuilder(this)
                        : new HtmlDiffBuilder(this);
                var contents = b.build(apiDescriptions, ck -> resultTable.inc(pos.getElementKey(), ck));
                return new HtmlTree(TagName.DIV, contents).setClass("apiDescriptions");
            }
        }

        return Content.empty;
    }

    private APIMap<String> getAPIDescriptionMap(Position pos) {
        // TODO: reorder with using element map first
        if (pos.is(RelativePosition.Kind.SERIALIZATION_OVERVIEW)) {
            // get the APIMap<SerializedForm> and build the overview map from that
            APIMap<SerializedForm> sfMap = getSerializedFormMap(pos);
            return sfMap == null ? null
                    : sfMap.map((api, sf) -> {
                        SerializedFormDocs sfDocs = sf.getDocs();
                        return sfDocs == null ? null : sfDocs.getOverview();
                    });
        } else if (pos.is(RelativePosition.Kind.SERIALIZED_FIELD)) {
            // get the APIMap<SerializedForm> and build the field map from that
            @SuppressWarnings("unchecked")
            RelativePosition<String> sfPos = (RelativePosition<String>) pos;
            APIMap<SerializedForm> sfMap = getSerializedFormMap(pos);
            return sfMap == null ? null
                    : sfMap.map((api, sf) -> {
                        SerializedFormDocs sfDocs = sf.getDocs();
                        return sfDocs == null ? null : sfDocs.getFieldDescription(sfPos.index);
                    });
        } else if (pos.is(RelativePosition.Kind.DOC_FILE)) {
            @SuppressWarnings("unchecked")
            APIMap<DocFile> fmap = (APIMap<DocFile>) apiMaps.get(pos);
            return fmap.map((api, df) -> {
                JavaFileObject fo = df.files.get(LocationKind.SOURCE);
                return fo == null ? null : api.getApiDescription(fo);
            });
        } else {
            APIMap<? extends Element> eMap = getElementMap(pos);
            return eMap.map(API::getApiDescription);
        }
    }

    private APIMap<SerializedForm> getSerializedFormMap(Position pos) {
        Position sfPos = pos.serializedForm();
        @SuppressWarnings("unchecked")
        APIMap<SerializedForm> sfMap = (APIMap<SerializedForm>) apiMaps.get(sfPos);
        return sfMap;
    }

    /**
     * Builds the details for the annotations at the position of an annotated construct in the API.
     *
     * @param acPos the position
     *
     * @return the details about the annotations
     */
    protected List<Content> buildAnnotations(Position acPos) {
        return new AnnotationBuilder().buildAnnotations(acPos);
    }

    /**
     * Builds the details about the enclosed elements of the element of the page.
     *
     * @return the details about the enclosed elements
     */
    protected abstract List<Content> buildEnclosedElements();

    /**
     * Adds the details for selected enclosed elements.
     *
     * @param list     the list to which the details should be added
     * @param titleKey the resource key for a title (heading) for the list of enclosed elements
     * @param filter   a filter to select the enclosed elements to be added
     */
    protected void addEnclosedElements(List<Content> list, String titleKey, Predicate<ElementKey> filter) {
        Set<? extends ElementKey> enclosed = results.keySet().stream()
                .filter(Position::isElement)
                .map(Position::asElementKey)
                .filter(ek -> ek != pageKey)
                .filter(filter)
                .collect(Collectors.toCollection(TreeSet::new));

        List<Content> realEnclosed = enclosed.stream().map(eKey -> buildEnclosedElement(eKey)).filter(opt -> opt.isPresent()).map(opt -> opt.orElseThrow()).toList();
        if (!realEnclosed.isEmpty()) {
            HtmlTree section = HtmlTree.SECTION().setClass("enclosed");
            section.add(HtmlTree.H2(Text.of(msgs.getString(titleKey))));
            HtmlTree ul = HtmlTree.UL();
            for (Content content : realEnclosed) {
                HtmlTree li = HtmlTree.LI(content);
                ul.add(li);
            }
            section.add(ul);
            list.add(section);
        }
    }

    /**
     * Builds the content for an enclosed element.
     *
     * <p>The default is to just generate the check/cross and a link to the enclosed element.
     *
     * @param eKey the key for the enclosed element
     *
     * @return the content
     */
    protected Optional<Content> buildEnclosedElement(ElementKey eKey) {
        // The enclosed element may be on a different page, so use the appropriate page reporter
        PageReporter<?> r = parent.getPageReporter(eKey);
        ResultKind result = r.getResultGlyph(eKey);
        if (result == ResultKind.SAME) {
            return Optional.empty();
        }
        return Optional.of(HtmlTree.SPAN(result.getContent(),
                Text.SPACE,
                links.createLink(eKey)));
    }

    protected void addDocFiles(List<Content> list) {
        Set<? extends RelativePosition<String>> docFiles = results.keySet().stream()
                .filter(p -> p.is(RelativePosition.Kind.DOC_FILE))
                .map(p -> p.as(RelativePosition.Kind.DOC_FILE, String.class))
                .collect(Collectors.toCollection(() -> new TreeSet<>(RelativePosition.stringIndexComparator)));

        if (!docFiles.isEmpty()) {
            HtmlTree section = HtmlTree.SECTION().setClass("doc-files");
            section.add(HtmlTree.H2(Text.of(msgs.getString("heading.files"))));
            HtmlTree ul = HtmlTree.UL();
            for (RelativePosition<String> p : docFiles) {
                DocFilesBuilder b = new DocFilesBuilder(p);
                HtmlTree li = HtmlTree.LI(getResultGlyph(p).getContent(), buildMissingInfo(p));
                String name = p.index;
                if (name.endsWith(".html")) {
                    b.buildFile();
                    li.add(HtmlTree.A("doc-files/" + name, Text.of(name)));
                } else {
                    li.add(Text.of(name));
                    for (LocationKind lk : LocationKind.values()) {
                        li.add(b.buildTable(lk));
                    }
                }
                ul.add(li);
            }
            section.add(ul);
            list.add(section);
        }
    }

    private String getChecksum(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }


    /**
     * Builds the details about any missing items at a position in the API.
     *
     * @param pos the position
     *
     * @return the details
     */
    protected Content buildMissingInfo(Position pos) {
        if (missing.containsKey(pos)) {
            ResultKind result = getResultGlyph(pos);
            // TODO: use an L10N-friendly builder, or use an API list builder, building Content?
            String onlyIn = apiMaps.get(pos).keySet().stream()
                    .map(a -> a.name)
                    .collect(Collectors.joining(", "));
            // The "missing" sets are not guaranteed ordered, so build the list according to
            // the overall order of the APIs.
            Set<API> missingAtPos = missing.get(pos);
            String missingIn = parent.apis.stream()
                    .filter(missingAtPos::contains)
                    .map(a -> a.name)
                    .collect(Collectors.joining(", "));
            String info = msgs.getString("element.onlyInMissingIn", onlyIn, missingIn);
            return HtmlTree.SPAN(Text.of(info)).setClass(result.getCaptionClass() != null ? "missing " + result.getCaptionClass() : "missing");
        } else {
            return Content.empty;
        }
    }

    protected Content buildResultTable() {
        HtmlTree section = HtmlTree.SECTION(HtmlTree.H2(Text.of(msgs.getString("summary.heading"))))
                .setClass("summary");
        if (resultTable.isEmpty()) {
            section.add(Text.of(msgs.getString("summary.no-differences")));
        } else {
            section.add(resultTable.toContent());
        }
        return section;
    }

    protected ResultKind getResultGlyph(ElementKey eKey) {
        Position pos = Position.of(eKey);
        return getResultGlyph(pos, apiMaps.get(pos));
    }

    protected ResultKind getResultGlyph(Position pos) {
        return getResultGlyph(pos, apiMaps.get(pos));
    }

    protected ResultKind getResultGlyph(Position pos, APIMap<?> map) {
        if (map == null) {
            // TODO...
            return ResultKind.UNKNOWN;
        }
        if (map.size() == 1) {
            API api = map.keySet().iterator().next();
            Set<API> apis = parent.apis;
            if (apis.size() == 2) {
                // The following assumes an ordering on the order in which the
                // APIs were defined on the command line: older first, newer second
                Iterator<API> iter = apis.iterator();
                API oldAPI = iter.next();
                API newAPI = iter.next();
                if (api == oldAPI) { // and not in new API
                    return ResultKind.REMOVED;
                } else if (api == newAPI) { // and not in old API
                    return ResultKind.ADDED;
                } else {
                    // should not happen?
                    return ResultKind.PARTIAL;
                }
            }
            return ResultKind.PARTIAL;
        }
        Boolean eq = results.get(pos);
        return (eq == null) ? ResultKind.PARTIAL : eq ? ResultKind.SAME : ResultKind.DIFFERENT;
    }

    // TODO: improve abstraction; these args are typically reversed
    protected ResultKind getResultGlyph(APIMap<?> map, Position pos) {
        if (map.size() == 1) {
            return ResultKind.PARTIAL;
        }
        Boolean eq = results.get(pos);
        return (eq == null) ? ResultKind.PARTIAL : eq ? ResultKind.SAME : ResultKind.DIFFERENT;
    }

    protected APIMap<? extends Element> getElementMap(ElementKey eKey) {
        Position ePos = Position.of(eKey);
        @SuppressWarnings("unchecked")
        APIMap<? extends Element> apiMap = (APIMap<? extends Element>) apiMaps.get(ePos);
        return apiMap;
    }

    protected APIMap<? extends Element> getElementMap(Position pos) {
        if (!(pos.isElement() || pos.is(RelativePosition.Kind.SERIALIZATION_METHOD))) {
            throw new IllegalArgumentException(pos.toString());
        }
        @SuppressWarnings("unchecked")
        APIMap<? extends Element> apiMap = (APIMap<? extends Element>) apiMaps.get(pos);
        return apiMap;
    }

    protected boolean hasMissing(APIMap<?> apiMap) {
        return apiMap.size() < parent.apis.size();
    }

    protected boolean getResult(Position pos) {
        Boolean b = results.get(pos);
        if (b == null) {
            throw new IllegalStateException(); // TODO: should this be an assertion
        }
        return b;
    }

    protected boolean getResult(ElementKey eKey) {
        return getResult(Position.of(eKey));
    }

    protected Content todo(String name) {
        parent.countToDo(name);
        return HtmlTree.SPAN(Text.of(name)).setClass("todo");
    }

    protected Options getOptions() {
        return parent.options;
    }


    /**
     * A utility class to generate the page heading for each page.
     */
    protected class PageHeading implements ElementKey.Visitor<List<Content>, Void> {
        Position pos;

        PageHeading(Position pos) {
            this.pos = pos;
        }

        Content toContent() {
            List<Content> contents;
            if (pos.isElement()) {
                contents = pos.asElementKey().accept(this, null);
            } else if (pos.is(RelativePosition.Kind.DOC_FILE)) {
                @SuppressWarnings("unchecked")
                RelativePosition<String> fPos = (RelativePosition<String>) pos;
                contents = new ArrayList<>();
                ElementKey eKey = fPos.getElementKey();
                switch (eKey.kind) {
                    case MODULE -> {
                        ModuleElementKey mKey = (ModuleElementKey) eKey;
                        contents.add(minor("heading.module", links.createLink(mKey, mKey.name)));
                    }
                    case PACKAGE -> {
                        PackageElementKey pKey = (PackageElementKey) eKey;
                        ModuleElementKey mKey = (ModuleElementKey) pKey.moduleKey;
                        if (mKey != null) {
                            contents.add(minor("heading.module", links.createLink(mKey, mKey.name)));
                        }
                        contents.add(minor("heading.package", links.createLink(pKey, pKey.name)));
                    }
                }
                contents.add(major("heading.file", fPos.index));
            } else {
                throw new IllegalStateException(pos.toString());
            }

            return new HtmlTree(TagName.DIV, contents).setClass("pageHeading");
        }

        @Override
        public List<Content> visitModuleElement(ModuleElementKey mKey, Void _p) {
            return List.of(major("heading.module", mKey.name));
        }

        @Override
        public List<Content> visitPackageElement(PackageElementKey pKey, Void _p) {
            List<Content> contents = new ArrayList<>();
            ModuleElementKey mKey = (ModuleElementKey) pKey.moduleKey;
            if (mKey != null) {
                contents.add(minor("heading.module", links.createLink(mKey, mKey.name)));
            }
            contents.add(major("heading.package", pKey.name));
            return contents;
        }

        @Override
        public List<Content> visitTypeElement(TypeElementKey tKey, Void _p) {
            List<Content> contents = new ArrayList<>();

            String tKind;
            APIMap<? extends Element> tMap = getElementMap(tKey);
            Set<ElementKind> eKinds = (tMap == null) ? Collections.emptySet()
                    : tMap.values().stream().map(Element::getKind).collect(Collectors.toSet());
            switch (eKinds.size()) {
                case 0 ->
                    tKind = "heading.unknown";

                case 1 -> {
                    ElementKind eKind = eKinds.iterator().next();
                    tKind = switch (eKind) {
                        case ANNOTATION_TYPE -> "heading.annotation-type";
                        case CLASS -> "heading.class";
                        case ENUM -> "heading.enum";
                        case INTERFACE -> "heading.interface";
                        case RECORD -> "heading.record";
                        default -> throw new IllegalStateException(eKind.toString());
                    };
                }

                default ->
                    tKind = "heading.mixed";
            }

            StringBuilder tName = new StringBuilder(tKey.name);
            while (tKey.enclosingKey instanceof TypeElementKey) {
                tKey = (TypeElementKey) tKey.enclosingKey;
                tName.insert(0, tKey.name + ".");
            }

            PackageElementKey pKey = (PackageElementKey) tKey.enclosingKey;
            if (pKey != null) {
                ModuleElementKey mKey = (ModuleElementKey) pKey.moduleKey;
                if (mKey != null) {
                    contents.add(minor("heading.module", links.createLink(mKey, mKey.name)));
                }
                contents.add(minor("heading.package", links.createLink(pKey, pKey.name)));
            }

            contents.add(major(tKind, tName));
            return contents;
        }

        @Override
        public List<Content> visitExecutableElement(ExecutableElementKey k, Void aVoid) {
            return null;
        }

        @Override
        public List<Content> visitVariableElement(VariableElementKey k, Void aVoid) {
            return null;
        }

        @Override
        public List<Content> visitTypeParameterElement(TypeParameterElementKey k, Void aVoid) {
            return null;
        }

        private Content minor(String key, HtmlTree link) {
            return HtmlTree.DIV(
                    HtmlTree.SPAN(Text.of(msgs.getString(key))).setClass("label"),
                    Entity.NBSP,
                    link
            );
        }

        private Content major(String key, CharSequence name) {
            return HtmlTree.H1(Text.of(msgs.getString(key)), Entity.NBSP, Text.of(name));
        }
    }

    /**
     * A builder for different instances of an annotation in different instances of an API.
     */
    protected class AnnotationBuilder {
        private AnnotationValueBuilder annoValueBuilder = new AnnotationValueBuilder();

        public List<Content> buildAnnotations(Position acPos) {
            Set<RelativePosition<?>> annos = getAnnotationsAt(acPos);
            if (annos.isEmpty()) {
                return List.of();
            }

            Content terminator = acPos.isElement() ? new HtmlTree(TagName.BR) : Text.SPACE;

            List<Content> contents = new ArrayList<>();
            for (RelativePosition<?> anno : annos) {
                contents.add(build(anno));
                contents.add(terminator);
            }

            return contents;
        }

        private Content build(RelativePosition<?> aPos) {
            // get the apiMap and result;
            // if the anno is equal, get/print the first from the map;
            // otherwise use DiffBuilder to show the differences
            APIMap<? extends AnnotationMirror> aMap = getAnnotationsMap(aPos);
            boolean equal = getResult(aPos);
            AnnotationMirror archetype = aMap.values().iterator().next();
            Element annoType = archetype.getAnnotationType().asElement();
            List<Content> contents = new ArrayList<>();
            contents.add(new Text("@"));
            contents.add(new Text(annoType.getSimpleName().toString()));
            if (equal) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = archetype.getElementValues();
                if (!elementValues.isEmpty()) {
                    contents.add(new Text("("));
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : elementValues.entrySet()) {
                        ExecutableElement ee = e.getKey();
                        AnnotationValue av = e.getValue();
                        Name name = ee.getSimpleName();
                        if (elementValues.size() > 1 || !name.contentEquals("value")) {
                            contents.add(new Text(name));
                            contents.add(new Text("="));
                        }
                        contents.add(annoValueBuilder.build(av));
                    }
                    contents.add(new Text(")"));
                }
            } else {
                // get the position for all the annotation values, sorted by the corresponding executable element
                Set<RelativePosition<?>> values = new TreeSet<>(Position.elementKeyIndexComparator);
                for (Position pos : results.keySet()) {
                    if (pos instanceof RelativePosition) {
                        RelativePosition<?> rp = (RelativePosition<?>) pos;
                        if (rp.kind == RelativePosition.Kind.ANNOTATION_VALUE && rp.parent.equals(aPos)) {
                            values.add(rp);
                        }
                    }
                }
                contents.add(new Text("("));
                contents.addAll(buildAnnoValues(values));
                contents.add(new Text(")"));
            }
            return HtmlTree.SPAN(contents).setClass("annotation");
        }

        Set<RelativePosition<?>> getAnnotationsAt(Position acPos) {
            Set<RelativePosition<?>> annos = new TreeSet<>(Position.elementKeyIndexComparator);
            for (Position pos : results.keySet()) {
                if (pos instanceof RelativePosition) {
                    RelativePosition<?> rp = (RelativePosition<?>) pos;
                    if (rp.kind == RelativePosition.Kind.ANNOTATION && rp.parent.equals(acPos)) {
                        annos.add(rp);
                    }
                }
            }
            return annos;
        }

        @SuppressWarnings("unchecked")
        APIMap<? extends AnnotationMirror> getAnnotationsMap(RelativePosition<?> aPos) {
            return switch (aPos.kind) {
                case ANNOTATION -> (APIMap<? extends AnnotationMirror>) apiMaps.get(aPos);
                default -> throw new IllegalArgumentException(aPos.toString());
            };
        }

        private List<Content> buildAnnoValues(Set<RelativePosition<?>> values) {
            List<Content> contents = new ArrayList<>();
            boolean first = true;
            for (RelativePosition<?> value : values) {
                if (first) {
                    first = false;
                } else {
                    contents.add(new Text(", "));
                }
                // TODO: make this an operation on Position; check RP.kind == ANNOTATION_VALUE
                MemberElementKey meKey = (MemberElementKey) value.index;
                assert meKey != null;
                String name = meKey.name.toString();
                contents.add(new Text(name));
                contents.add(new Text(" = "));
                contents.addAll(annoValueBuilder.buildAnnoValue(value));
            }
            return contents;
        }
    }

    /**
     * A builder for different instances of an annotation value in different instances of an API.
     */
    protected class AnnotationValueBuilder {

        List<Content> buildAnnoValue(RelativePosition<?> avPos) {
            APIMap<? extends AnnotationValue> avMap = getAnnotationValuesMap(avPos);
            boolean equal = getResult(avPos);
            List<Content> contents = new ArrayList<>();
            if (equal) {
                AnnotationValue archetype = avMap.values().iterator().next();
                contents.add(build(archetype));
            } else {
                APIMap<Content> diffs = APIMap.of();
                for (Map.Entry<API, ? extends AnnotationValue> e : avMap.entrySet()) {
                    API api = e.getKey();
                    AnnotationValue av = e.getValue();
                    diffs.put(api, build(av));
                }
                contents.add(new DiffBuilder().build(diffs));
            }
            return contents;
        }

        private Content build(AnnotationValue av) {
            // for now, rely on javax.lang.model .toString() method, which is defined
            // to return a source-friendly string.
            // Note that this implies we can't show nested differences within composite values.
            return new Text(av.toString());
        }

        @SuppressWarnings("unchecked")
        APIMap<? extends AnnotationValue> getAnnotationValuesMap(RelativePosition<?> aPos) {
            return switch (aPos.kind) {
                case ANNOTATION_VALUE, DEFAULT_VALUE ->
                        (APIMap<? extends AnnotationValue>) apiMaps.get(aPos);

                default ->
                        throw new IllegalArgumentException(aPos.toString());
            };
        }

    }

    /**
     * A builder for different instances of a type in different instances of an API.
     */
    protected class TypeBuilder extends SimpleTypeVisitor14<Content, Void> {

        Content build(TypeMirror tm) {
            // TODO: maybe handle annotations here
            return tm.accept(this, null);
        }

        @Override
        protected Content defaultAction(TypeMirror tm, Void _p) {
            return todo(tm.getKind() + " " + tm);
        }

        @Override
        public Content visitArray(ArrayType t, Void _p) {
            List<Content> contents = new ArrayList<>();
            List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
            if (!annos.isEmpty()) {
                contents.add(todo("type annotations " + annos));
                contents.add(Text.SPACE);
            }
            contents.add(visit(t.getComponentType(), _p));
            contents.add(Text.of("[]"));
            return HtmlTree.SPAN(contents);
        }

        @Override
        public Content visitDeclared(DeclaredType t, Void _p) {
            List<Content> contents = new ArrayList<>();
            List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
            if (!annos.isEmpty()) {
                contents.add(todo("type annotations " + annos));
                contents.add(Text.SPACE);
            }
            contents.add(Text.of(elementNameVisitor.visit(t.asElement(), null)));
            List<? extends TypeMirror> typeArgs = t.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                contents.add(Text.of("<"));
                boolean needComma = false;
                for (TypeMirror ta : typeArgs) {
                    if (needComma) {
                        contents.add(Text.of(", "));
                    } else {
                        needComma = true;
                    }
                    contents.add(visit(ta, null));
                }
                contents.add(Text.of(">"));
            }
            return (contents.size() == 1) ? contents.get(0) : HtmlTree.SPAN(contents);
        }

        @Override
        public Content visitNoType(NoType t, Void _p) {
            return switch (t.getKind()) {
                // NONE is most likely to be printed in a "diff" context, so generate a space
                case NONE -> Entity.NBSP;
                case VOID -> Keywords.VOID;
                default -> throw new IllegalArgumentException(t.getKind().toString());
            };
        }

        @Override
        public Content visitPrimitive(PrimitiveType t, Void _p) {
            // TODO: annotations
            return Keywords.of(t);
        }

        @Override
        public Content visitTypeVariable(TypeVariable t, Void _p) {
            List<Content> contents = new ArrayList<>();
            List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
            if (!annos.isEmpty()) {
                contents.add(todo("type annotations " + annos));
                contents.add(Text.SPACE);
            }
            contents.add(Text.of(t.asElement().getSimpleName())); // TODO: link? link to declaring element (type or executable?)
            return (contents.size() == 1) ? contents.get(0) : HtmlTree.SPAN(contents);
        }

        @Override
        public Content visitWildcard(WildcardType t, Void _p) {
            List<Content> contents = new ArrayList<>();
            List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
            if (!annos.isEmpty()) {
                contents.add(todo("type " +
                        "annotations " + annos));
                contents.add(Text.SPACE);
            }
            contents.add(Text.of("?"));
            addBound(contents, Keywords.EXTENDS, t.getExtendsBound());
            addBound(contents, Keywords.SUPER, t.getSuperBound());
            return (contents.size() == 1) ? contents.get(0) : HtmlTree.SPAN(contents);
        }

        private void addBound(List<Content> contents, Content kw, TypeMirror b) {
            if (b == null) {
                return;
            }
            contents.add(Text.SPACE);
            contents.add(kw);
            contents.add(Text.SPACE);
            contents.add(build(b));
        }

        private final ElementVisitor<Name,Void> elementNameVisitor = new SimpleElementVisitor14<>() {
            @Override
            public Name visitType(TypeElement te, Void p) {
                return te.getQualifiedName();
            }
            @Override
            public Name visitTypeParameter(TypeParameterElement tpe, Void p) {
                return tpe.getSimpleName();
            }
        };
    }

    /**
     * A builder for different instances of doc files across instances of an API.
     *
     * Two kinds of output are supported: either a short summary table giving the
     * size and a checksum for the different instances, suitable for any type of file,
     * or a separate file, displaying the differences for HTML files, compared as
     * either doc comments (for instances in the source tree) or as API descriptions
     * (for instances found in the API directory.)
     */
    class DocFilesBuilder {
        private final RelativePosition<String> fPos;
        private final APIMap<DocFile> fMap;
        private final DocPath file;
        private final Links links;

        /**
         * Creates a builder for the doc files at a given position.
         *
         * @param pos the position
         */
        DocFilesBuilder(RelativePosition<String> pos) {
            this.fPos = pos;
            @SuppressWarnings("unchecked")
            APIMap<DocFile> fMap = (APIMap<DocFile>) apiMaps.get(fPos);
            this.fMap = fMap;
            file = PageReporter.this.file.parent().resolve("doc-files").resolve(pos.index);
            links = new Links(file);
        }

        /**
         * Returns a table displaying the size and a checksum for each of the
         * instances of the doc file.  The checksum is just intended to help
         * visualize which files may be equal and which are different.
         * The checksum is a short but sufficient substring of the SHA_256 digest.
         *
         * @param lk the kind of location for the files to be included in the table
         *
         * @return the table
         */
        Content buildTable(LocationKind lk) {
            @SuppressWarnings("unchecked")
            APIMap<DocFile> fMap = (APIMap<DocFile>) apiMaps.get(fPos);
            if (fMap.values().stream().allMatch(Objects::isNull)) {
                return Content.empty;
            }

            String captionKey = switch (lk) {
                case API -> "docfile.details.caption.api";
                case SOURCE -> "docfile.details.caption.source";
            };

            HtmlTree caption = HtmlTree.CAPTION(Text.of(msgs.getString(captionKey, fPos.index)));
            HtmlTree tHead = HtmlTree.THEAD(
                    HtmlTree.TR(
                            HtmlTree.TH(Text.of(msgs.getString("docfile.details.th.api"))),
                            HtmlTree.TH(Text.of(msgs.getString("docfile.details.th.size"))),
                            HtmlTree.TH(Text.of(msgs.getString("docfile.details.th.checksum")))
                    )
            );
            HtmlTree tBody = HtmlTree.TBODY();
            fMap.forEach((api, df) -> {
                    JavaFileObject fo = df.files.get(lk);
                    if (fo != null) {
                        byte[] bytes = api.getAllBytes(fo);
                        int size = bytes.length;
                        String cs = getChecksum(bytes);
                        tBody.add(HtmlTree.TR(
                                HtmlTree.TH(Text.of(api.name)).set(HtmlAttr.SCOPE, "row"),
                                HtmlTree.TD(Text.of(Integer.toString(size))),
                                HtmlTree.TD(Text.of(cs))
                        ));
                    }
            });

            return HtmlTree.TABLE(caption, tHead, tBody).setClass("details");
        }

        /**
         * Builds a file containing the comparison for instances of an HTML file.
         */
        void buildFile() {
            HtmlTree page = new HtmlTree(TagName.HTML, buildHead(), buildBody());
            writeFile(page);
        }

        private void writeFile(HtmlTree content) {
            PageReporter.this.writeFile(file, content);
        }

        private Content buildHead() {
            String title = getTitle();
            if (parent.options.getTitle() != null) {
                title = String.format("%s: %s", parent.options.getTitle(), title);
            }
            return HtmlTree.HEAD("UTF-8", title)
                    .add(HtmlTree.META("generator", "apidiff"))
                    .add(parent.getStylesheets().stream()
                            .map(links::getPath)
                            .map(l -> HtmlTree.LINK("stylesheet", l.getPath())));
        }

        private String getTitle() {
            return file.getPath();
        }

        private Content buildBody() {
            HtmlTree body = HtmlTree.BODY().setClass("doc-files");
            body.add(buildHeader());
            HtmlTree main = HtmlTree.MAIN();
            main.add(buildPageHeading());
            main.add(HtmlTree.SPAN(getResultGlyph(fPos).getContent(), buildMissingInfo(fPos)).setClass("doc-files"));
            main.add(buildDocComments(fPos));
            main.add(buildAPIDescriptions(fPos));
//            main.add(buildEnclosedElements());
//            main.add(buildResultTable()); // TODO: info not broken out; could simulate a ResultTable?
            body.add(main);
            body.add(buildFooter());
            if (parent.options.getHiddenOption("show-debug-summary") != null) {
                body.add(new DebugSummary().build()); // TODO will show package page info
            }
            return body;
        }

        Content buildPageHeading() {
            return new PageHeading(fPos).toContent();
        }
    }

    /**
     * A builder for a summary of the differences reported for different instances of an API.
     */
    protected class DebugSummary {
        protected Content build() {
            List<Content> summary = new ArrayList<>();

            if (!results.isEmpty()) {
                long enclosedDiffs = results.values().stream()
                        .filter(b -> !b)
                        .count();
                if (enclosedDiffs > 0) {
                    summary.add(Text.of(String.format("%d different enclosed elements", enclosedDiffs)));
                }
            }

            List<Content> list = Stream.of(
                    build("missing items", missing),
                    build("different annotations", differentAnnotations),
                    build("different annotation values", differentAnnotationValues),
                    build("different directives", differentDirectives),
                    build("different kinds", differentKinds),
                    build("different type parameters", differentTypeParameters),
                    build("different modifiers", differentModifiers),
                    build("different types", differentTypes),
                    build("different thrown types", differentThrownTypes),
                    build("different superinterfaces", differentSuperinterfaces),
                    build("different raw doc comments", differentRawDocComments))
                    .filter(c -> c != Content.empty)
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                summary.add(HtmlTree.UL(list));
            }

            if (summary.isEmpty()) {
                summary.add(Text.of("no differences found"));
            }

            return new HtmlTree(TagName.SECTION, summary).setClass("debug");
        }

        private Content build(String name, Map<Position, ?> map) {
            if (map.isEmpty()) {
                return Content.empty;
            }

            SignatureVisitor sv = new SignatureVisitor(apiMaps);

            HtmlTree ul = HtmlTree.UL();
            for (Map.Entry<Position, ?> entry : map.entrySet()) {
                Position pos = entry.getKey();
                Object details = entry.getValue();
                HtmlTree detailsTree;
                if (details instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<API> missing = (Set<API>) details;
                    HtmlTree list = HtmlTree.UL();
                    missing.stream()
                            .map(a -> HtmlTree.LI(Text.of(a.name)))
                            .forEach(list::add);
                    detailsTree = list;
                } else if (details instanceof APIMap<?> apiMap) {
                    HtmlTree table = new HtmlTree(TagName.TABLE);
                    apiMap.forEach((api, value) -> table.add(
                            HtmlTree.TR(
                                    HtmlTree.TH(Text.of(api.name)),
                                    HtmlTree.TD(Text.of(Objects.toString(value))))));
                    detailsTree = table;
                } else {
                    detailsTree = HtmlTree.DIV(Text.of(Objects.toString(details)));
                }
                ul.add(HtmlTree.LI(Text.of(sv.getSignature(pos)), detailsTree));
            }

            return new HtmlTree(TagName.SECTION, HtmlTree.H2(Text.of(name)), ul);
        }

    }
    public enum ResultKind {
        UNKNOWN(Text.of("?"), null),
        // The following names are intended to be "semantic" or "abstract" names,
        // distinct from the concrete representations used in the generated documentation.
        // The names are intentionally different from any corresponding entity names.
        /**
         * Used when two elements compare as equal.
         */
        // possible alternatives: Entity.CHECK
        SAME(HtmlTree.SPAN(Entity.EQUALS).setClass("same"), null),

        /**
         * Used when two elements compare as not equal.
         */
        // possible alternatives: Entity.CROSS
        DIFFERENT(HtmlTree.SPAN(Entity.NE).setClass("diff"), null),

        /**
         * Used when an element does not appear in all instances of the APIs being compared.
         * See also {@link #ADDED}, {@link #REMOVED}.
         */
        PARTIAL(HtmlTree.SPAN(Entity.CIRCLED_DIGIT_ONE).setClass("partial"), null),

        /**
         * Used in a 2-way comparison when it is determined that an element has been added.
         */
        // possible alternatives: '>' (for example, as used in text diff tools) or other right-pointing arrows
        ADDED(HtmlTree.SPAN(Entity.PLUS).setClass("add"), "missing-add"),

        /**
         * Used in a 2-way comparison when it is determined that an element has been removed.
         */
        // possible alternatives: '<' (for example, as used in text diff tools) or other left-pointing arrows
        REMOVED(HtmlTree.SPAN(Entity.MINUS).setClass("remove"), "missing-remove"),
        ;

        private final Content content;
        private final String captionClass;

        private ResultKind(Content content, String captionClass) {
            this.content = content;
            this.captionClass = captionClass;
        }

        public Content getContent() {
            return content;
        }

        public String getCaptionClass() {
            return captionClass;
        }
    }
}

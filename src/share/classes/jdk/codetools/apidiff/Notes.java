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

package jdk.codetools.apidiff;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jdk.codetools.apidiff.model.ElementKey;
import jdk.codetools.apidiff.model.ElementKey.ExecutableElementKey;
import jdk.codetools.apidiff.model.ElementKey.ModuleElementKey;
import jdk.codetools.apidiff.model.ElementKey.PackageElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeElementKey;
import jdk.codetools.apidiff.model.ElementKey.TypeParameterElementKey;
import jdk.codetools.apidiff.model.ElementKey.VariableElementKey;
import jdk.codetools.apidiff.model.TypeMirrorKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.ArrayTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.DeclaredTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.PrimitiveTypeKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.TypeVariableKey;
import jdk.codetools.apidiff.model.TypeMirrorKey.WildcardTypeKey;

/**
 * A class to associate URIs and descriptions with elements.
 */
public class Notes {
    /**
     * A class for an individual note that may be associated with one or more elements.
     */
    public static class Entry {
        /**
         * The name of the entry, that is used to identify the associated elements.
         */
        public final String name;

        /**
         * The URI for the note.
         */
        public final URI uri;

        /**
         * The description for the note.
         */
        public final String description;

        /**
         * Whether the note applies to enclosed elements as well.
         */
        public final boolean recursive;

        /**
         * Creates a note
         *
         * @param name        the name of the note
         * @param uri         the URI for the note
         * @param description the description for the note
         * @param recursive   whether the note applies to enclosed elements as well
         */
        Entry(String name, URI uri, String description, boolean recursive) {
            this.name = name;
            this.uri = uri;
            this.description = description;
            this.recursive = recursive;
        }

        @Override
        public String toString() {
            return "Entry[name=" + name + ",uri=" + uri + ",description=" + description + ",recursive=" + recursive + "]";
        }
    }

    private final Map<String, List<Entry>> entries;

    private Notes() {
        entries = new HashMap<>();
    }

    /**
     * Reads a file containing a description of the notes to be used.
     * If there are errors in the content of the file, they will be reported to the log.
     *
     * @param file the file
     * @param log  the log
     *
     * @return the result of reading the file
     * @throws IOException if an IO exception occurs while reading the file
     */
    public static Notes read(Path file, Log log) throws IOException {
        return new Reader(log).read(file);
    }

    /**
     * Returns the entries for a given element key.
     * The entries are returned in a map, that associates a boolean value with each entry,
     * which indicates whether the entry is for an enclosing element.
     *
     * @param k the element key
     * @return a map containing the entries for the key.
     */
    public Map<Entry, Boolean> getEntries(ElementKey k) {
        GetEntriesVisitor v = new GetEntriesVisitor();
        return v.getEntries(k);
    }

    private class GetEntriesVisitor implements ElementKey.Visitor<Void, Boolean> {
        private Map<Entry, Boolean> map = new LinkedHashMap<>();

        Map<Entry, Boolean> getEntries(ElementKey k) {
            k.accept(this, false);
            return map;
        }

        @Override
        public Void visitModuleElement(ModuleElementKey mKey, Boolean isParent) {
            add(nameVisitor.getName(mKey), isParent);
            return null;
        }

        @Override
        public Void visitPackageElement(PackageElementKey pKey, Boolean isParent) {
            if (pKey.moduleKey != null) {
                pKey.moduleKey.accept(this, true);
            }
            add(nameVisitor.getName(pKey), isParent);
            return null;
        }

        @Override
        public Void visitTypeElement(TypeElementKey tKey, Boolean isParent) {
            if (tKey.enclosingKey != null) {
                tKey.enclosingKey.accept(this, true);
            }
            add(nameVisitor.getName(tKey), isParent);
            return null;
        }

        @Override
        public Void visitExecutableElement(ExecutableElementKey eKey, Boolean isParent) {
            add(nameVisitor.getName(eKey), isParent);
            return null;
        }

        @Override
        public Void visitVariableElement(VariableElementKey vKey, Boolean isParent) {
            add(nameVisitor.getName(vKey), isParent);
            return null;
        }

        @Override
        public Void visitTypeParameterElement(TypeParameterElementKey tKey, Boolean all) {
            throw new IllegalArgumentException(tKey.toString());
        }

        private void add(String name, boolean isParent) {
            List<Entry> l = entries.get(name);
            if (l != null) {
                if (isParent) {
                    l.stream().filter(e -> e.recursive).forEach(e -> map.put(e, true));
                } else {
                    l.forEach(e -> map.put(e, false));
                }
            }
        }
    }

    /**
     * Returns the display name for an element key.
     *
     * @param eKey the element key
     *
     * @return the display name
     */
    public static String getName(ElementKey eKey) {
        return nameVisitor.getName(eKey);
    }

    private static final NameVisitor nameVisitor = new NameVisitor();

    private static class NameVisitor implements ElementKey.Visitor<StringBuilder, StringBuilder>, TypeMirrorKey.Visitor<StringBuilder, StringBuilder> {
        String getName(ElementKey k) {
            return k.accept(this, new StringBuilder()).toString();
        }

        @Override
        public StringBuilder visitModuleElement(ModuleElementKey mKey, StringBuilder sb) {
            return sb.append(mKey.name);
        }

        @Override
        public StringBuilder visitPackageElement(PackageElementKey pKey, StringBuilder sb) {
            if (pKey.moduleKey != null) {
                pKey.moduleKey.accept(this, sb);
                sb.append("/");
            }
            return sb.append(pKey.name);
        }

        @Override
        public StringBuilder visitTypeElement(TypeElementKey tKey, StringBuilder sb) {
            if (tKey.enclosingKey != null) {
                tKey.enclosingKey.accept(this, sb);
                sb.append(".");
            }
            return sb.append(tKey.name);
        }

        @Override
        public StringBuilder visitExecutableElement(ExecutableElementKey eKey, StringBuilder sb) {
            eKey.typeKey.accept(this, sb).append("#").append(eKey.name).append("(");
            boolean first = true;
            for (TypeMirrorKey t :  eKey.params) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                t.accept(this, sb);
            }
            return sb.append(")");
        }

        @Override
        public StringBuilder visitVariableElement(VariableElementKey vKey, StringBuilder sb) {
            vKey.typeKey.accept(this, sb);
            return sb.append("#").append(vKey.name);
        }

        @Override
        public StringBuilder visitTypeParameterElement(TypeParameterElementKey k, StringBuilder sb) {
            throw new IllegalArgumentException(k.toString());
        }

        @Override
        public StringBuilder visitArrayType(ArrayTypeKey k, StringBuilder sb) {
            return k.componentKey.accept(this, sb).append("[]");
        }

        @Override
        public StringBuilder visitDeclaredType(DeclaredTypeKey k, StringBuilder sb) {
            ElementKey eKey = k.elementKey;
            return switch (eKey.kind) {
                case TYPE -> sb.append(((TypeElementKey) eKey).name); // simple name only
                case TYPE_PARAMETER -> sb.append(((TypeParameterElementKey) eKey).name); // simple name only
                default -> throw new IllegalArgumentException((k.toString()));
            };
        }

        @Override
        public StringBuilder visitPrimitiveType(PrimitiveTypeKey k, StringBuilder sb) {
            return sb.append(k.kind.toString().toLowerCase(Locale.ROOT));
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariableKey k, StringBuilder sb) {
            return sb.append(k.name);
        }

        @Override
        public StringBuilder visitWildcardType(WildcardTypeKey k, StringBuilder sb) {
            throw new IllegalArgumentException((k.toString()));
        }

    }

    private static class Reader {
        private final Log log;
        private Path file;

        Pattern p = Pattern.compile("\\s*([\\S]+)\\s*(.*?)\\s*");
        Notes notes = new Notes();
        URI uri = null;
        String description = null;
        int lineNumber = 0;
        boolean skipLines = false;

        Reader(Log log) {
            this.log = log;
        }

        Notes read(Path file) throws IOException {
            this.file = file;

            for (String line : Files.readAllLines(file)) {
                lineNumber++;
                process(line);
            }

            return notes;
        }

        void process(String line) {

            if (line.startsWith("#") || line.isBlank()) {
                return;
            }

            Matcher m = p.matcher(line);
            if (!m.matches()) {
                // should not happen, since we already ignored blank lines
                log.error(file, lineNumber, "notes.err.bad-line", line);
                return;
            }

            String first = m.group(1);
            String rest = m.group(2);

            if (first.contains(":")) {
                try {
                    uri = new URI(first);
                    description = rest;
                    skipLines = false;
                } catch (URISyntaxException e) {
                    log.error(file, lineNumber, "notes.err.bad-uri", first);
                    skipLines = true;
                }
                return;
            }

            if (!rest.isEmpty()) {
                log.error(file, lineNumber, "notes.err.bad-line", line);
                return;
            }

            boolean recursive;
            if (first.endsWith("/*") || first.endsWith(".*")) {
                first = first.substring(0, first.length() - 2);
                recursive = true;
            } else {
                recursive = false;
            }

            if (!isValidSignature(first)) {
                log.error(file, lineNumber, "notes.err.bad-signature", first);
                return;
            }

            if (skipLines) {
                // uri has been reported as invalid; can't create entry
                return;
            }

            if (uri == null) {
                log.error(file, lineNumber, "notes.err.no-current-uri");
                skipLines = true;
                return;
            }

            notes.entries.computeIfAbsent(first, __ -> new ArrayList<>())
                    .add(new Entry(first, uri, description, recursive));
        }

        boolean isValidSignature(String sig) {
            int slash = sig.indexOf("/");
            if (slash != -1) {
                if  (!isQualifiedIdentifier(sig.substring(0, slash))) {
                    return false;
                }
                sig = sig.substring(slash + 1);
                if (sig.isEmpty()) {
                    // signature was module/
                    return true;
                }
            }

            int hash = sig.indexOf("#");
            if (hash == -1) {
                // signature was [module/] package-or-type
                return isQualifiedIdentifier(sig);
            }

            String type = sig.substring(0, hash);
            String member = sig.substring(hash + 1);
            if (!isQualifiedIdentifier(type)) {
                // bad [package .] type
                return false;
            }

            int lParen = member.indexOf("(");
            if (lParen == -1) {
                // signature looks like [module/] [package .] type # field
                return isIdentifier(member);
            }

            // signature looks like [module/] type # method ( param-types )
            String method = member.substring(0, lParen);
            String params = member.substring(lParen + 1, member.length() - 1);
            return (isIdentifier(method) || method.equals("<init>"))
                    && (params.isEmpty()
                        || Stream.of(params.split(",", -1)).allMatch(this::isIdentifier));

        }

        boolean isQualifiedIdentifier(String name) {
            return Stream.of(name.split("\\.", -1)).allMatch(this::isIdentifier);
        }

        boolean isIdentifier(String name) {
            if (name.isEmpty()) {
                return false;
            }

            if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                return false;
            }

            for (int i = 1; i < name.length(); i++) {
                if (!Character.isUnicodeIdentifierPart(name.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }
}

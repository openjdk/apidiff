/*
 * Copyright (c) 2020,2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jdk.codetools.apidiff.model.Selector;

/**
 * A class to encapsulate the functionality of the {@code --jdk-build} option,
 * which provides a shorthand for the underlying primitive options when doing
 * a "standard" comparison involving JDK builds.
 */
public class JDKBuildOption {
    private final Path buildDir;
    private final Path imagesDir;

    JDKBuildOption(Path dir) {
        this.buildDir = dir;
        this.imagesDir = dir.resolve("images");
    }

    void expand(Options options, Options.APIOptions apiOptions, Log log) {

        boolean verbose = options.isVerbose(Options.VerboseKind.OPTIONS);
        if (verbose) {
            log.err.println("Expanding --jdk-build for API " + apiOptions.name);
            log.err.println("    --jdk-build: " + buildDir);
        }

        Path system = getSystem();
        if (verbose) {
            log.err.println("  --system " + system);
        }
        apiOptions.addFileManagerOpt("--system", system.toString());

        // proactively get api dir if available, in case we want to subsequently
        // set compareAPIDescriptions by default
        Path apiDir = getAPIDirectory(options, log);
        if (verbose && apiDir != null) {
            log.err.println("  --api-directory " + apiDir);
        }
        apiOptions.apiDir = apiDir;

        if (options.compareDocComments == Boolean.TRUE) {
            Set<String> modules = new LinkedHashSet<>();
            Path tmpDir = unpackSource(options, log, modules);
            for (String m : modules) {
                String patchModule = m + "=" + tmpDir.resolve(m);
                if (verbose) {
                    log.err.println("  --patch-module " + patchModule);
                }
                apiOptions.addFileManagerOpt("--patch-module", patchModule);
            }
            // since we're also setting the --system option,
            // just set the --source option here
            String release = getRelease(log);
            if (verbose) {
                log.err.println("  --source " + release);
            }
            apiOptions.source = release;
        }
    }

    private Path getSystem() {
        return imagesDir.resolve("jdk");
    }

    private Path getAPIDirectory(Options options, Log log) {
        Map<String, Path> dirs = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imagesDir,
                p -> Files.isDirectory(p) && p.getFileName().toString().contains("docs"))) {
            for (Path entry: stream) {
                dirs.put(entry.getFileName().toString(), entry);
            }
        } catch (DirectoryIteratorException e) {
            // I/O error encountered during the iteration; the cause is an IOException
            softError(log, options, "jdkbuild.ioerror-finding-docs", e.getCause());
            return null;
        } catch (IOException e) {
            softError(log, options, "jdkbuild.ioerror-finding-docs", e);
            return null;
        }
        Path docsDir;
        if (dirs.isEmpty()) {
            softError(log, options, "jdkbuild.err.no-docs", imagesDir);
            return null;
        } else if (options.jdkDocs == null) {
            if (dirs.size() > 1) {
                softError(log, options, "jdkbuild.err.multiple-docs",
                        imagesDir,
                        String.join(", ", dirs.keySet()));
                return null;
            } else {
                docsDir = dirs.values().iterator().next();
            }
        } else {
            Path dir = dirs.get(options.jdkDocs);
            if (dir == null) {
                softError(log, options, "jdkbuild.err.cannot-find-docs", options.jdkDocs, imagesDir);
                return null;
            }
            docsDir = dir;
        }
        return docsDir.resolve("api");
    }

    /**
     * Reports a hard error if comparison of API descriptions has been explicitly requested.
     * Otherwise, does nothing.
     *
     * @param log     the log
     * @param options the options
     * @param key     the resource key
     * @param args    the arguments
     */
    void softError(Log log, Options options, String key, Object... args) {
        if (options.compareApiDescriptions == Boolean.TRUE) {
            log.error(key, args);
        }
    }

    private String getRelease(Log log) {
        Map<String, String> map = getReleaseInfo(log);
        return map.get("JAVA_VERSION");
    }

    private Map<String, String> getReleaseInfo(Log log) {
        Map<String, String> map = new LinkedHashMap<>();
        Pattern p = Pattern.compile("(?<name>[A-Z0-9_]+)=\"(?<value>.*)\"$");
        Path releaseFile = getSystem().resolve("release");
        try {
            for (String line : Files.readAllLines(releaseFile)) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    map.put(m.group("name"), m.group("value"));
                }
            }
        } catch (IOException e) {
            log.error("jdkbuild.err.error-reading-release-file", releaseFile, e);
        }
        return map;
    }

    private Path unpackSource(Options options, Log log, Set<String> modules) {
        Selector s = new Selector(options.includes, options.excludes);

        Path tmpSrcDir = buildDir.resolve("apidiff.tmp").resolve("src");
        Path srcZip = buildDir.resolve("support").resolve("src.zip");
        try (ZipFile zf = new ZipFile(srcZip.toFile())) {
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                String name = ze.getName();
                if (!name.endsWith(".java")) {
                    continue;
                }
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                int firstSep = name.indexOf("/"); // after module name
                int lastSep = name.lastIndexOf("/"); // before type name
                if (lastSep > firstSep) { // ensures two or more instances
                    String m = name.substring(0, firstSep);
                    String p = name.substring(firstSep + 1, lastSep).replace("/", ".");
                    String t = name.substring(lastSep + 1).replace(".java", "");
                    if (s.acceptsType(m, p, t)) {
                        try (InputStream in = zf.getInputStream(ze)) {
                            Path outFile = tmpSrcDir.resolve(name.replace("/", File.separator));
                            Files.createDirectories(outFile.getParent());
                            Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        modules.add(m);
                    }
                }
            }
        } catch (IOException e) {
            log.error("jdkbuild.err.error-reading-src.zip", srcZip, e);

        }
        return tmpSrcDir;
    }
}

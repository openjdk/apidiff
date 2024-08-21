/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package apitest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;

import jdk.codetools.apidiff.Main;
import jdk.codetools.apidiff.Main.Result;
import jdk.codetools.apidiff.report.html.HtmlReporter;

import org.junit.jupiter.api.Test;

/**
 * Compare string constants in apidiff classes against keys in apidiff resource bundles.
 */
public class CheckResourceKeys {
    /**
     * Main program.
     * Options:
     * --find-unused-keys
     *      look for keys in resource bundles that are no longer required
     * --find-missing-keys
     *      look for keys in resource bundles that are missing
     *
     * @param args commnd-line arguments
     * @throws Exception if invoked by jtreg and errors occur
     */
    public static void main(String... args) throws Exception {
        CheckResourceKeys c = new CheckResourceKeys();
        if (!c.run(args)) {
            System.exit(1);
        }
    }

    private PrintStream log = System.out;

    /**
     * Main entry point.
     */
    boolean run(String... args) throws Exception {
        boolean findUnusedKeys = false;
        boolean findMissingKeys = false;

        if (args.length == 0) {
            System.err.println("Usage: java CheckResourceKeys <options>");
            System.err.println("where options include");
            System.err.println("  --find-unused-keys    find keys in resource bundles which are no longer required");
            System.err.println("  --find-missing-keys   find keys in resource bundles that are required but missing");
            return true;
        } else {
            for (String arg: args) {
                if (arg.equalsIgnoreCase("--find-unused-keys"))
                    findUnusedKeys = true;
                else if (arg.equalsIgnoreCase("--find-missing-keys"))
                    findMissingKeys = true;
                else
                    error("bad option: " + arg);
            }
        }

        if (errors > 0)
            return false;

        Set<String> codeKeys = getCodeKeys();
        Set<String> resourceKeys = getResourceKeys();

        System.err.println("found " + codeKeys.size() + " keys in code");
        System.err.println("found " + resourceKeys.size() + " keys in resource bundles");

        if (findUnusedKeys)
            findUnusedKeys(codeKeys, resourceKeys);

        if (findMissingKeys)
            findMissingKeys(codeKeys, resourceKeys);

        usageTests();

        return (errors == 0);
    }

    @Test
    public void checkResourceKeys() throws Exception {
        boolean ok = run("--find-unused-keys", "--find-missing-keys");
        if (!ok) {
            throw new Exception("Check failed");
        }
    }

    void usageTests() {
        String[] argarray = { "--help" };
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (new Main(pw, pw).run("--help") == Result.OK) {
            pw.flush();
            String s = sw.toString();
            if (s.isEmpty()) {
                error("no output from apidiff");
                return;
            }
            if (sw.toString().contains("WARNING: missing resource")) {
                log.println(s);
                error("missing resources in output ?");
            }
        } else {
            error("failed to execute apidiff");
        }
    }

    /**
     * Find keys in resource bundles which are probably no longer required.
     * A key is required if there is a string in the code that is a resource key,
     * or if the key is well-known according to various pragmatic rules.
     */
    void findUnusedKeys(Set<String> codeKeys, Set<String> resourceKeys) {
        for (String rk: resourceKeys) {
            // ignore these synthesized keys, tested by usageTests
            if (rk.startsWith("apidiff.usage."))
                continue;
            // ignore these synthesized keys, tested by usageTests
            if (rk.matches("opt\\.(arg|desc)\\.[-a-z]+"))
                continue;
            if (codeKeys.contains(rk))
                continue;

            error("Resource key not found in code: '" + rk + '"');
        }
    }

    /**
     * For all strings in the code that look like they might be
     * a resource key, verify that a key exists.
     */
    void findMissingKeys(Set<String> codeKeys, Set<String> resourceKeys) {
        for (String ck: codeKeys) {
            // ignore keys that are defined in a resource file
            if (resourceKeys.contains(ck))
                continue;
            error("No resource for \"" + ck + "\"");
        }
    }

    /**
     * Get the set of strings from the apidiff classfiles.
     */
    Set<String> getCodeKeys() throws IOException {
        Set<String> results = new TreeSet<>();
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (JavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            List<String> pkgs = List.of("jdk.codetools.apidiff");
            for (String pkg: pkgs) {
                for (JavaFileObject fo: fm.list(StandardLocation.CLASS_PATH,
                        pkg, EnumSet.of(JavaFileObject.Kind.CLASS), true)) {
                    String name = fo.getName();
                    // ignore resource files
                    if (name.matches(".*resources.[A-Za-z_0-9]+\\.class.*"))
                        continue;
                    scan(fo, results);
                }
            }
        }

        return results;
    }

    // depending on how the test is run, javadoc may be on bootclasspath or classpath
    JavaFileManager.Location findJavadocLocation(JavaFileManager fm) {
        JavaFileManager.Location[] locns =
                { StandardLocation.PLATFORM_CLASS_PATH, StandardLocation.CLASS_PATH };
        try {
            for (JavaFileManager.Location l: locns) {
                JavaFileObject fo = fm.getJavaFileForInput(l,
                        "jdk.javadoc.internal.tool.Main", JavaFileObject.Kind.CLASS);
                if (fo != null) {
                    System.err.println("found javadoc in " + l);
                    return l;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        throw new IllegalStateException("Cannot find javadoc");
    }

    /**
     * Get the set of strings from a class file.
     * Only strings that look like they might be a resource key are returned.
     */
    void scan(JavaFileObject fo, Set<String> results) throws IOException {
        //System.err.println("scan " + fo.getName());
        try (InputStream in = fo.openInputStream()) {
            ClassFile cf = ClassFile.read(in);
            for (ConstantPool.CPInfo cpinfo : cf.constant_pool.entries()) {
                if (cpinfo.getTag() == ConstantPool.CONSTANT_Utf8) {
                    String v = ((ConstantPool.CONSTANT_Utf8_info) cpinfo).value;
                    // ignore SourceFile attribute values
                    if (v.matches("[A-Za-z][A-Za-z0-9-]*\\.java")) {
                        continue;
                    }
                    // ignore system names
                    if (v.matches("(java|jdk)\\..*")) {
                        continue;
                    }
                    // ignore standard javadoc file names
                    if (v.matches("((module|package)-summary|serialized-form)\\.html")) {
                        continue;
                    }
                    // ignore standard javadoc CSS class names
                    if (v.matches("(module|package).description")) {
                        continue;
                    }
                    // ignore standard apidiff file names
                    if (v.equals("index.html") || v.equals(HtmlReporter.DEFAULT_STYLESHEET)) {
                        continue;
                    }
                    //  ignore names used by --jdk-build
                    if (v.equals("apidiff.tmp") || v.equals("src.zip")|| v.equals("spec.gmk")) {
                        continue;
                    }
                    // ignore debug options
                    if (v.startsWith("debug.")) {
                        continue;
                    }
                    if (v.matches("[A-Za-z][A-Za-z0-9-]+\\.[A-Za-z0-9-_@.]+"))
                        results.add(v);
                }
            }
        } catch (ConstantPoolException ignore) {
        }
    }

    /**
     * Get the set of keys from the apidiff resource bundles.
     */
    Set<String> getResourceKeys() {
        String[] names = {
                "jdk.codetools.apidiff.resources.help",
                "jdk.codetools.apidiff.resources.log",
                "jdk.codetools.apidiff.report.html.resources.report"
        };
        Set<String> results = new TreeSet<>();
        for (String name : names) {
            ResourceBundle b = ResourceBundle.getBundle(name);
            results.addAll(b.keySet());
        }
        return results;
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}

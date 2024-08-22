/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A class to access version info from the manifest info in a jar file.
 */
public class Version {
    /**
     * Returns the current version.
     * @return the current version
     */
    public static Version getCurrent() {
        if (currentVersion == null)
            currentVersion = new Version();
        return currentVersion;
    }

    private static Version currentVersion;

    /** The name of the product. */
    public final String product;
    /** The version string. */
    public final String version;
    /** The milestone. */
    public final String milestone;
    /** The build number. */
    public final String build;
    /** The version of Java used to build the jar file. */
    public final String buildJavaVersion;
    /** The date on which the jar file was built. */
    public final String buildDate;

    private Version() {
        Properties manifest = getManifestForClass(getClass());
        if (manifest == null)
            manifest = new Properties();

        String prefix = "apidiff";
        product = manifest.getProperty(prefix + "-Name");
        version = manifest.getProperty(prefix + "-Version");
        milestone = manifest.getProperty(prefix + "-Milestone");
        build = manifest.getProperty(prefix + "-Build");
        buildJavaVersion = manifest.getProperty(prefix + "-BuildJavaVersion");
        buildDate = manifest.getProperty(prefix + "-BuildDate");
    }

    void show(PrintWriter out) {
        String thisJavaHome = System.getProperty("java.home");
        String thisJavaVersion = System.getProperty("java.version");

        File classPathFile = getClassPathFileForClass(Main.class);
        String unknown = messages.getString("version.msg.unknown");
        String classPath = (classPathFile == null ? unknown : classPathFile.getPath());

        Object[] versionArgs = {
            product,
            version,
            milestone,
            build,
            classPath,
            thisJavaVersion,
            thisJavaHome,
            buildJavaVersion,
            buildDate
        };

        /*
         * Example format string:
         *
         * {0}, version {1} {2} {3}
         * Installed in {4}
         * Running on platform version {5} from {6}.
         * Built with {7} on {8}.
         *
         * Example output:
         *
         * apidiff, version 1.0 dev b00
         * Installed in /usr/local/apidiff/lib/apidiff.jar
         * Running on platform version 1.8 from /opt/jdk/1.8.0.
         * Built with 1.8 on 09/11/2006 07:52 PM.
         */

        out.println(messages.getString("version.msg.info", versionArgs));
    }

    private Properties getManifestForClass(Class<?> c) {
        URL classPathEntry = getClassPathEntryForClass(c);
        if (classPathEntry == null)
            return null;

        try {
            Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                if (url.getProtocol().equals("jar")) {
                    String path = url.getPath();
                    int sep = path.lastIndexOf("!");
                    URL u = new URL(path.substring(0, sep));
                    if (u.equals(classPathEntry )) {
                        Properties p = new Properties();
                        try (InputStream in = url.openStream()) {
                            p.load(in);
                        }
                        return p;
                    }
                }
            }
        } catch (IOException ignore) {
        }
        return null;
    }

    private URL getClassPathEntryForClass(Class<?> c) {
        try {
            URL url = c.getResource("/" + c.getName().replace('.', '/') + ".class");
            if (url != null && url.getProtocol().equals("jar")) {
                String path = url.getPath();
                int sep = path.lastIndexOf("!");
                return new URL(path.substring(0, sep));
            }
        } catch (MalformedURLException ignore) {
        }
        return null;
    }

    private File getClassPathFileForClass(Class<?> c) {
        URL url = getClassPathEntryForClass(c);
        if (url.getProtocol().equals("file"))
            return new File(url.getPath());
        return null;
    }

    private final Messages messages = Messages.instance("jdk.codetools.apidiff.resources.log");

}

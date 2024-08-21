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

package jdk.codetools.apidiff;

import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * Utilities to write logging messages.
 */
public class Log {
    /**
     * An output stream for "expected" output.
     */
    public final PrintWriter out;

    /**
     * An output stream for "diagnostic" output.
     */
    public final PrintWriter err;

    /**
     * The messages used by this log.
     */
    private final Messages messages = Messages.instance( "jdk.codetools.apidiff.resources.log");

    private String errPrefix = messages.getString("log.err-prefix");
    private String warnPrefix = messages.getString("log.warn-prefix");
    private String notePrefix = messages.getString("log.note-prefix");

    private int errCount = 0;
    private int warnCount = 0;

    /**
     * Creates an instance of a log.
     *
     * @param out the stream to which to write normal output
     * @param err the stream to which to write error output
     */
    public Log(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    public void flush() {
        out.flush();
        err.flush();
    }

    /**
     * Reports an error message, based on a resource key and optional arguments.
     *
     * @param key the resource key
     * @param args the arguments
     */
    public void error(String key, Object... args) {
        err.println(errPrefix + " " + messages.getString(key, args));
        errCount++;
    }

    /**
     * Reports a warning message, based on a resource key and optional arguments.
     *
     * @param key the resource key
     * @param args the arguments
     */
    public void warning(String key, Object... args) {
        err.println(warnPrefix + " " + messages.getString(key, args));
        warnCount++;
    }

    /**
     * Reports a message, based on a resource key and optional arguments.
     *
     * @param key the resource key
     * @param args the arguments
     */
    public void report(String key, Object... args) {
        err.println(messages.getString(key, args));
    }

    /**
     * Reports an error message, with optional file position.
     *
     * @param file the file, or null
     * @param line the line of the file, if any
     * @param key the resource key for the message, or null if the first arg is a localized message
     * @param args the arguments for the message
     */
    public void error(Path file, long line, String key, Object... args) {
        String message = (key == null) ? args[0].toString() : messages.getString(key, args);
        if (file == null) {
            err.println(errPrefix + " " + message);
        } else {
            err.println(file + ":" + line + ": " + message);
        }
        errCount++;
    }

    /**
     * Reports a warning message, with optional file position.
     *
     * @param file the file, or null
     * @param line the line of the file, if any
     * @param key the resource key for the message, or null if the first arg is a localized message
     * @param args the arguments for the message
     */
    public void warning(Path file, long line, String key, Object... args) {
        String message = (key == null) ? args[0].toString() : messages.getString(key, args);
        if (file == null) {
            err.println(warnPrefix + " " + message);
        } else {
            err.println(file + ":" + line + ": " + warnPrefix + " " + message);
        }
        warnCount++;
    }

    /**
     * Reports a note, with optional file position.
     *
     * @param file the file, or null
     * @param line the line of the file, if any
     * @param key the resource key for the message, or null if the first arg is a localized message
     * @param args the arguments for the message
     */
    public void note(Path file, long line, String key, Object... args) {
        String message = key == null ? args[0].toString() : messages.getString(key, args);
        if (file == null) {
            err.println(notePrefix + " " + message);
        } else {
            err.println(file + ":" + line + ": " + notePrefix + " " + message);
        }
    }

    /**
     * Returns the number of errors that have been reported.
     *
     * @return the number of errors
     */
    public int errorCount() {
        return errCount;
    }

    /**
     * Returns the number of warnings that have been reported.
     *
     * @return the number of errors
     */
    public int warningCount() {
        return warnCount;
    }

    /**
     * Reports the number of errors and warnings that have been reported.
     */
    void reportCounts() {
        if (errCount > 0) {
            err.println(messages.getString("log.errors", errCount));
        }

        if (warnCount > 0) {
            err.println(messages.getString("log.warnings", errCount));
        }
    }
}

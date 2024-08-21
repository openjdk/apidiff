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

package jdk.codetools.apidiff.html;

/**
 * A media-type, for use in {@code <link>} nodes.
 */
 public enum MediaType {
    IMAGE_GIF("image/gif"),
     // see https://stackoverflow.com/questions/13827325/correct-mime-type-for-favicon-ico
    IMAGE_ICON("image/vnd.microsoft.icon"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    TEXT_CSS("text/css; charset=UTF-8"),
    TEXT_HTML("text/html; charset=UTF-8"),
    TEXT_PLAIN("text/plain; charset=UTF-8");

    public final String contentType;

    MediaType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Infers the media type for a file, based on the filename extension.
     *
     * @param path the path for the file
     *
     * @return the media type
     */
    public static MediaType forPath(String path){
        if (path.endsWith(".css")) {
            return MediaType.TEXT_CSS;
        } else if (path.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else if (path.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN;
        } else if (path.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (path.endsWith(".jpg")) {
            return MediaType.IMAGE_JPEG;
        } else if (path.endsWith(".ico")) {
            return MediaType.IMAGE_ICON;
        } else if (path.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else {
           throw new IllegalArgumentException(path);
        }
    }

}

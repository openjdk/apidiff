/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This is class {@code S}.
 * This is additional information.
 * @since 1.0
 */
public class S implements Serializable {
    private static final long serialVersionUID = 456;

    /**
     * This is private field {@code f1};
     * This is additional information.
     * @since 1.0
     */
    private int f1;

    /**
     * This is private transient field {@code f2};
     * It is not part of the serial form.
     * @since 1.0
     */
    private transient int f2;

    /**
     * This is private method {@code readObject}.
     * @param stream the serial input stream
     * @throws IOException if an IO exception occurs
     * @throws ClassNotFoundException if a class cannot be found
     * @serialData This is a description of the serial data provided in {@code readObject}.
     *  This is additional information.
     * @since 1.0
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

    }

    /**
     * This is private method {@code writeObject}.
     * @param stream the serial input stream
     * @throws IOException if an IO exception occurs
     * @serialData This is a description of the serial data provided in {@code writeObject}.
     *  This is additional information.
     * @since 1.0
     */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {

    }
}
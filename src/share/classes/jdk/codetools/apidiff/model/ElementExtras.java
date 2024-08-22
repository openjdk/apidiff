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

package jdk.codetools.apidiff.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A class to encapsulate reflective access to recent API, thereby allowing the
 * tool to run on older versions of the platform when such API is not required.
 */
public class ElementExtras {
    public class Fault extends Error {
        Fault(Throwable cause) {
            super(cause);
        }
    }

    private static final ElementExtras instance = new ElementExtras();

    private final Runtime.Version version = Runtime.version();
    private final Method getRecordComponentsMethod;
    private final Method getPermittedSubclassesMethod;

    /**
     * Returns the instance of this class.
     *
     * @return the instance
     */
    public static ElementExtras instance() {
        return instance;
    }

    /**
     * Creates the one instance of this class.
     */
    private ElementExtras() {
        getRecordComponentsMethod = lookup(14, TypeElement.class, "getRecordComponents");
        getPermittedSubclassesMethod = lookup(15, TypeElement.class, "getPermittedSubclasses");
    }

    private Method lookup(int since, Class<?> c, String name, Class<?>... paramTypes) {
        try {
            if (version.feature() >= since) {
                return c.getDeclaredMethod(name, paramTypes);
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("Cannot find " + c + "." + name + "(" +
                    Stream.of(paramTypes).map(Class::getSimpleName).collect(Collectors.joining(","))
                    + ")");
        }
        return null;
    }

    /**
     * Invokes {@code TypeElement.getRecordComponents()} for a type element, if the method is available,
     * and returns the result of calling that method.
     * If the method is not available, {@code null} is returned.
     *
     * @param e the element
     * @return the result of calling {@code TypeElement.getRecordComponent()}, or {@code null}
     */
    public List<? extends Element> getRecordComponents(TypeElement e) {
        if (getRecordComponentsMethod == null) {
            return Collections.emptyList();
        }

        try {
            return (List<? extends Element>) getRecordComponentsMethod.invoke(e);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new Fault(ite);
            }
        } catch (ReflectiveOperationException roe) {
            throw new Fault(roe);
        }
    }

    /**
     * Invokes {@code TypeElement.getPermittedSubclasses()} for a type element, if the method is available,
     * and returns the result of calling that method.
     * If the method is not available, {@code null} is returned.
     *
     * @param e the element
     * @return the result of calling {@code TypeElement.getPermittedSubclasses()}, or {@code null}
     */
    public List<? extends TypeMirror> getPermittedSubclasses(TypeElement e) {
        if (getPermittedSubclassesMethod == null) {
            return Collections.emptyList();
        }

        try {
            return (List<? extends TypeMirror>) getPermittedSubclassesMethod.invoke(e);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new Fault(ite);
            }
        } catch (ReflectiveOperationException roe) {
            throw new Fault(roe);
        }
    }

}

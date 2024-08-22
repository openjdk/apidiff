#!/bin/bash

# Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.


mydir="$(dirname ${BASH_SOURCE[0]})"
BUILDDIR=${mydir}/../../build

WORK=$HOME/Work
JDK=${WORK}/jdk

sh ${BUILDDIR}/make.sh showDocs-demo \
  JDK11HOME=${JDK}/jdk-11.jdk/Contents/Home/ \
  JDK12HOME=${JDK}/jdk-12.jdk/Contents/Home/ \
  JDK13HOME=${JDK}/jdk-13.jdk/Contents/Home/ \
  JDK14HOME=${JDK}/jdk-14.jdk/Contents/Home/ \
  JDK15HOME=${JDK}/jdk-15.jdk/Contents/Home/ \
  JDK16HOME=${JDK}/jdk-16.jdk/Contents/Home/ \
  JDK17HOME=${JDK}/jdk-17.jdk/Contents/Home/ \
  JDK18HOME=${JDK}/jdk-18.jdk/Contents/Home/ \
  JDK19HOME=${JDK}/jdk-19.jdk/Contents/Home/ \
  JDK20HOME=${JDK}/jdk-20.jdk/Contents/Home/ \
  JDK21HOME=${JDK}/jdk-21.jdk/Contents/Home/ \
  JDK22HOME=${JDK}/jdk.ref/build/macosx-aarch64/images/jdk/ \
  JDKHOME=${JDK}/jdk-17.jdk/Contents/Home/


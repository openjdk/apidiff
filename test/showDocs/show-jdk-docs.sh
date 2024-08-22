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

# Analyses JDK docs with APIReader and generates a hierarchy of
# pages displaying the API descriptions read from declaration
# and serialized-form pages.

mydir="$(dirname ${BASH_SOURCE[0]})"
BUILDDIR=${mydir}/../../build

WORK=${WORK:-${HOME}/Work}
JDK=${JDK:-${WORK}/jdk}

# used to run the showDocs tool
JDKHOME=${JDKHOME:-${JDK}/jdk-17.jdk/Contents/Home}

# where to find the docs to analyze;
# the directory must be populated with the API docs for the desired JDK versions
JDKDOCS=${JDK}/docs

OUTDIR=${OUTDIR:-${BUILDDIR}/show-jdk-docs}

showdocs() {
   $JDKHOME/bin/java -jar ${BUILDDIR}/showDocs.jar $*
}

for v in 11 12 13 14 15 16 17 18 19 20 21 jdk.ref; do
  echo $v
  rm -rf ${OUTDIR}/$v
  showdocs -d ${OUTDIR}/$v $JDKDOCS/$v/docs/api
done

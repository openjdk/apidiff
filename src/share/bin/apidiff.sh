#!/bin/sh
#
# Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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
#

# Usage:
#    apidiff ...args....
#       Run the application with the given arguments.
#       The Java runtime used to run apidiff is found as follows:
#       -   $JAVA_HOME/bin/java is used if $JAVA_HOME is set
#           (cf JDK.)
#       -   Otherwise, "java" is used
#
# apidiff requires a version of Java equivalent to JDK 17 or higher.
#
# You can also run the jar file directly, as in
#   java -jar <path>/lib/apidiff.jar ...args...


# Deduce where script is installed
# - should work on most derivatives of Bourne shell, like ash, bash, ksh,
#   sh, zsh, etc, including on Windows, MKS (ksh) and Cygwin (ash or bash)
if type -p type 1>/dev/null 2>&1 && test -z "`type -p type`" ; then
    myname=`type -p "$0"`
elif type type 1>/dev/null 2>&1 ; then
    myname=`type "$0" | sed -e 's/^.* is a tracked alias for //' -e 's/^.* is //'`
elif whence whence 1>/dev/null 2>&1 ; then
    myname=`whence "$0"`
fi
mydir=`dirname "$myname"`
p=`cd "$mydir" ; pwd`
while [ -n "$p" -a "$p" != "/" ]; do
    if [ -r "$p"/lib/apidiff.jar ]; then APIDIFF_HOME="$p" ; break; fi
    p=`dirname "$p"`
done
if [ -z "$APIDIFF_HOME" ]; then
    echo "Cannot determine APIDIFF_HOME"; exit 1
fi

# Normalize APIDIFF_HOME if using Cygwin
case "`uname -s`" in
    CYGWIN* ) cygwin=1 ; APIDIFF_HOME=`cygpath -a -m "$APIDIFF_HOME"` ;;
esac


# Separate out -J* options for the JVM=
# Unset IFS and use newline as arg separator to preserve spaces in args
DUALCASE=1  # for MKS: make case statement case-sensitive (6709498)
saveIFS="$IFS"
nl='
'
for i in "$@" ; do
    IFS=
    if [ -n "$cygwin" ]; then i=`echo $i | sed -e 's|/cygdrive/\([A-Za-z]\)/|\1:/|'` ; fi
    case $i in
    -J* )       javaOpts=$javaOpts$nl`echo $i | sed -e 's/^-J//'` ;;
    *   )       apidiffOpts=$apidiffOpts$nl$i ;;
    esac
    IFS="$saveIFS"
done
unset DUALCASE

# Determine java for apidiff, from JAVA_HOME, java
if [ -n "$JAVA_HOME" ]; then
    APIDIFF_JAVA="$JAVA_HOME/bin/java"
else
    APIDIFF_JAVA=java
fi

# Verify java version 17 or newer used to run apidiff
version=`"$APIDIFF_JAVA" -classpath "${APIDIFF_HOME}/lib/apidiff.jar" jdk.codetools.apidiff.GetSystemProperty java.version 2>&1 |
        grep 'java.version=' | sed -e 's/^.*=//' -e 's/^1\.//' -e 's/\([1-9][0-9]*\).*/\1/'`

if [ -z "$version" ]; then
    echo "Cannot determine version of java to run apidiff"
    exit 1;
elif [ "$version" -lt 17 ]; then
    echo "java version 17 or later is required to run apidiff"
    exit 1;
fi

# And finally ...

IFS=$nl

"${APIDIFF_JAVA}" \
    $javaOpts \
    -Dprogram=`basename "$0"` \
    -jar "${APIDIFF_HOME}/lib/apidiff.jar" \
    $apidiffOpts

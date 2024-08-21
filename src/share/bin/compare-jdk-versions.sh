#!/bin/bash
#
# Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

# Demonstrates how to compare the API docs for different builds or versions of JDK.
#
# The script has three parts or phases:
# 1. Determine which versions to compare
# 2. Build the docs for those versions
# 3. Compare the generated docs

DEFAULT_LATEST_JDK=
DEFAULT_APIDIFF=
DEFAULT_REPO=.
DEFAULT_OUTDIR=build

# Helper used to ensure the correct number of arguments is passed to bash functions
check_arguments() {
    local name="$1"
    local expected="$2"
    local actual="$3"

    if [ ! "${expected}" = "${actual}" ]; then
        echo "Incorrect number of arguments to function '${name}' (expecting ${expected} but got ${actual})" 2>&1
        exit 1
    fi
}

ensure_arg() {
    check_arguments "${FUNCNAME}" 2 $#
    local option="$1"
    local arg_count="$2"
    if [ "$arg_count" -lt "2" ]; then
        echo "The $option option requires an argument" 2>&1
        exit
    fi
}

process_args() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --help|-h )             HELP=1 ;                                    shift ;;
            --apidiff )             ensure_arg "$1" $# ; APIDIFF="$2" ;         shift ; shift ;;
            --jdk )                 ensure_arg "$1" $# ; LATEST_JDK="$2" ;      shift ; shift ;;
            --output )              ensure_arg "$1" $# ; OUTDIR="$2" ;          shift ; shift ;;
            --repo )                ensure_arg "$1" $# ; REPO="$2" ;            shift ; shift ;;
            --latest-ga | --previous | --latest | jdk-* )
                                    versions="${versions} $1" ;                 shift ;;
            * )                     echo "unknown option: '$1'" 1>&2;          exit 1 ;;
        esac
    done
}

usage() {
    echo "Usage: $0 <options> <versions>"
    echo "--help"
    echo "      Show this message."
    echo "--apidiff <dir>"
    echo "      Specify location of apidiff."
    echo "--jdk <dir>"
    echo "      Specify location of JDK used to run comparison tools."
    echo "--repo <dir>"
    echo "      Specify location of repository in which to build docs."
    echo "      Default: ${DEFAULT_REPO}"
    echo "--output <dir>"
    echo "      Specify directory for comparison reports."
    echo "      Default: <repo>/${DEFAULT_OUTDIR}"
    echo "<versions>"
    echo "      0, 1, 2 or 3 of --latest-ga, --previous, --latest, jdk-*"
    echo "      where jdk-* is a git tag for the repo."
    echo "      Default if none specified:     --latest-ga --latest"
    echo "      Default if just one specified:  <version>  --latest"
}

process_args "$@"

if [ -n "${HELP:-}" ]; then
    usage
    exit
fi

if [ -f ~/.config/apidiff/apidiff.conf ]; then
    source ~/.config/apidiff/apidiff.conf
fi

LATEST_JDK=${LATEST_JDK:-${DEFAULT_LATEST_JDK}}
APIDIFF=${APIDIFF:-${DEFAULT_APIDIFF}}
REPO=${REPO:-${DEFAULT_REPO}}
OUTDIR=${OUTDIR:-${REPO}/${DEFAULT_OUTDIR}}

# Sanity check args

if [ -z "${APIDIFF}" ]; then
    echo "no path specified for apidiff" 1>&2 ; exit 1
elif [ ! -r ${APIDIFF}/lib/apidiff.jar ]; then
    echo "invalid path for apidiff" 1>&2 ; exit 1
fi

if [ -z "${LATEST_JDK}" ]; then
    echo "no path specified for latest JDK" 1>&2 ; exit 1
elif [ ! -r ${LATEST_JDK}/bin/java ]; then
    echo "invalid path for latest JDK: ${LATEST_JDK}" 1>&2 ; exit 1
fi

if [ ! -d ${REPO}/.git ]; then
    echo "invalid path for repo: ${REPO}" 1>&2 ; exit 1
fi

# use echo in next line to trim excess whitespace from wc output
case $(echo $(wc -w <<< "${versions}")) in
    0 )   versions="--latest-ga --latest" ;;
    1 )   versions="${versions} --latest" ;;
    2 | 3 )   ;;
    * )   echo "unexpected number of versions given: ${versions}" 1>&2 ; exit 1 ;;
esac

# Determine whether running in a closed+open pair, or just an open repo.
if [ -d ${REPO}/open ]; then
    OPEN=open
else
    OPEN=.
fi

# Phase 1: determine which versions to build and compare,
#          identified by the corresponding `git` tags.
# The versions (and hence tags) are determined automatically, from
# version-numbers.conf and the output of `git tag`.
# The following tags are determined:
#   PREVIOUS_GA_TAG, PREVIOUS_TAG, LATEST_TAG

# ensure the files in the work area are up to date before reading
# version-numbers.conf; it is assumed that the `master` branch
# always has the latest version numbers
git -C ${REPO}/${OPEN} checkout master
source ${REPO}/${OPEN}/make/conf/version-numbers.conf
VERSION_FEATURE=${VERSION_FEATURE:-${DEFAULT_VERSION_FEATURE}}

TAGS=( $(git -C ${REPO} tag --list "jdk-${VERSION_FEATURE}*" | sort --version-sort --reverse) )
LATEST_TAG=${TAGS[0]}
PREVIOUS_TAG=${TAGS[1]}

PREVIOUS_FEATURE=$(( ${VERSION_FEATURE} - 1))
LATEST_GA_TAG="jdk-${PREVIOUS_FEATURE}-ga"

tag() {
    case "$1" in
        --latest-ga ) echo ${LATEST_GA_TAG} ;;
        --previous )  echo ${PREVIOUS_TAG} ;;
        --latest )    echo ${LATEST_TAG} ;;
        jdk-* )       echo $1 ;;
        * ) echo "bad tag: $1" 1>&2 ; exit 1 ;;
    esac
}

check_tag() {
    local tag="$1"
    if ! git -C "$REPO" rev-parse "$tag" > /dev/null 2>&1 ; then
        echo tag "$tag" not found in repo "$REPO"
        exit 1
    fi
    if [ "${OPEN}" = "open" ]; then
        if ! git -C "$REPO"/$OPEN rev-parse "$tag" > /dev/null 2>&1 ; then
            echo tag "$tag" not found in repo "$OPEN"/$OPEN
            exit 1
        fi
    fi
}

# Phase 2: build the docs to be compared
# $1 is the tag for the version to checkout and build.
# It should be one of `--latest-ga`, `--previous`, `--latest` or an actual `jdk-*` tag.
# The build is skipped if `images/jdk` and `images/docs-reference` both exist.
#
# Configure and use the `docs-reference` target with the $LATEST_JDK.
# The same version of JDK should be used for all versions to be compared.
# `apidiff` also requires a JDK image for the comparison.
#
# Note: building the JDK image and docs for each version to be compared
# may take a while.

configure_jdk() {
    if [ -n "${APIDIFF_CONFIGURE_JDK}" ]; then
        "${APIDIFF_CONFIGURE_JDK}" "$@" ;
    elif [ -r jib.sh -a -r closed/bin/jib.sh ]; then
        sh jib.sh configure -- "$@"
    elif [ -r bin/jib.sh -a -r ../closed/make/conf/jib-install.conf ]; then
        sh bin/jib.sh configure -- "$@"
    elif [ -r bin/jib.sh -a -n "${JIB_SERVER}" ]; then
        sh bin/jib.sh configure -- "$@"
    else
        sh ./configure "$@"
    fi
}

build_reference_docs() {
    TAG=$(tag $1)
    if [ -d ${REPO}/build/${TAG}/images/jdk -a -d ${REPO}/build/${TAG}/images/docs-reference ]; then
        echo "Skipping build for ${TAG}"
        return
    fi

    git -C ${REPO} checkout --detach ${TAG}
    if [ "${OPEN}" = "open" ]; then
        git -C ${REPO}/open checkout --detach ${TAG}
    fi

    (   cd $REPO
        configure_jdk \
            --with-conf-name=${TAG} \
            --enable-full-docs \
            --with-docs-reference-jdk=${LATEST_JDK} \
            --quiet
        make CONF_NAME=${TAG} jdk-image docs-reference
    )
}

# Phase 3: Compare the documentation and generate reports.

apidiff_javase() {
    for t in "$@" ; do tags="$tags $(tag $t)" ; done
    for t in $tags ; do check_tag $t ; done
    for t in $tags ; do build_reference_docs $t ; done
    title="Comparing Java SE modules for $(echo $tags | sed -e 's/ /, /g' -e 's/\(.*\),\(.*\)/\1 and\2/')"
    outdir="${OUTDIR}/apidiff/javase--$(echo $tags | sed -e 's/ /--/g')"
    echo "${title}"
    JAVA_HOME=${LATEST_JDK} ${APIDIFF}/bin/apidiff \
        $(for t in $tags ; do echo "--api $t --jdk-build ${REPO}/build/$t" ; done) \
        --include java.*/java.** --include java.*/javax.** \
        --exclude java.smartcardio/ \
        --jdk-docs docs-reference \
        --output-directory ${outdir} \
        --title "${title}" \
        "${EXTRA_APIDIFF_OPTIONS[@]}"
    echo "Results written to ${outdir}"
}

apidiff_javase ${versions}

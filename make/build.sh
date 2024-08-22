#!/bin/sh

#
# Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

# This script will download/build the dependencies for apidiff and then
# build apidiff. Downloaded files are verified against known/specified
# specified checksums.

# The default version to use when building apidiff can be found in the
# make/version-numbers file, where the default versions and
# corresponding known checksums for the dependencies are also
# specified. Almost all of the defaults can be overridden by setting
# the respective environment variables.

# For each of the dependency the following steps are applied and the
# first successful one is used:
#
# 1. Check if the dependency is available locally
# 2. Download a prebuilt version of the dependency
#
# In particular, when not found locally the dependencies will be
# handled as follows:
#
# * JUnit, Java Diff Utils, and HtmlCleaner are by default downloaded from Maven Central.
# * Daisy Diff is by default downloaded from Google Code Archive.
# * The JDK dependency is downloaded. No default URL is set.
#

# Some noteworthy control variables:
#
# MAVEN_REPO_URL_BASE (e.g. "https://repo1.maven.org/maven2")
#     The base URL for the maven central repository.
#
# GOOGLE_CODE_URL_BASE (e.g. "https://code.google.com/archive/p")
#     The base URL for the Google Code Archive repository.
#
# APIDIFF_VERSION         (e.g. "1.0")
# APIDIFF_VERSION_STRING  (e.g. "apidiff-1.0+8"
# APIDIFF_BUILD_NUMBER    (e.g. "8")
# APIDIFF_BUILD_MILESTONE (e.g. "dev")
#     The version information to use for when building apidiff.
#
# MAKE_ARGS (e.g. "-j4 all")
#     Additional arguments to pass to make when building apidiff.
#
# WGET
#     The wget-like executable to use when downloading files.
#
# WGET_OPTS (e.g. "-v")
#     Additional arguments to pass to WGET when downloading files.
#
# CURL (e.g. "/path/to/my/wget")
#     The curl-like executable to use when downloading files.
#     Note: If available, wget will be prefered.
#
# CURL_OPTS (e.g. "-v")
#     Additional arguments to pass to CURL when downloading files.
#
# SKIP_DOWNLOAD
#     Skip the downloads if the file is already present locally.
#
# SKIP_CHECKSUM_CHECK
#     Skip the checksum verification for downloaded files.

# The control variables for dependencies are on the following general
# form (not all of them are relevant for all dependencies):
#
# <dependency>_URL (e.g. DAISYDIFF_BIN_ARCHIVE_URL)
#     The full URL for the dependency.
#
# <dependency>_URL_BASE (e.g. DAISYDIFF_BIN_ARCHIVE_URL_BASE)
#     The base URL for the dependency. Requires additional dependency
#     specific variables to be specified.
#
# <dependency>_CHECKSUM (e.g. DAISYDIFF_BIN_ARCHIVE_CHECKSUM)
#     The expected checksum of the download file.
#

# The below outlines the details of how the dependencies are
# handled. For each dependency the steps are tried in order and the
# first successful one will be used.
#
# JDK
#     Checksum variables:
#         JDK_ARCHIVE_CHECKSUM: checksum of binary archive
#
#     1. JAVA_HOME
#         The path to the JDK.
#     2a. JDK_ARCHIVE_URL
#         The full URL for the archive.
#     2b. JDK_ARCHIVE_URL_BASE + JDK_VERSION + JDK_BUILD_NUMBER + JDK_FILE
#         The individual URL components used to construct the full URL.
#
# Java Diff Utils
#     Checksum variables:
#         JAVADIFFUTILS_JAR_CHECKSUM: checksum of jar
#         JAVADIFFUTILS_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. JAVADIFFUTILS_JAR + JAVADIFFUTILS_LICENSE
#         The path to java-diff-utils.jar and LICENSE.txt respectively.
#     2a. JAVADIFFUTILS_JAR_URL
#         The full URL for the jar.
#     2b. JAVADIFFUTILS_JAR_URL_BASE + JAVADIFFUTILS_VERSION + JAVADIFFUTILS_FILE
#         The individual URL components used to construct the full URL.
#
# Daisy Diff
#     Checksum variables:
#         DAISYDIFF_BIN_ARCHIVE_CHECKSUM: checksum of binary archive
#         DAISYDIFF_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. DAISYDIFF_JAR + DAISYDIFF_LICENSE
#         The path to daisydiff.jar and LICENSE.txt respectively.
#     2a. DAISYDIFF_JAR_URL
#         The full URL for the jar.
#     2b. DAISYDIFF_JAR_URL_BASE + DAISYDIFF_BIN_VERSION + DAISYDIFF_FILE
#         The individual URL components used to construct the full URL.
#
# Html Cleaner
#     Checksum variables:
#         HTMLCLEANER_JAR_CHECKSUM: checksum of jar
#         HTMLCLEANER_LICENSE_CHECKSUM: checksum of LICENSE file
#
#     1. HTMLCLEANER_JAR + HTMLCLEANER_LICENSE
#         The path to htmlcleaner.jar and licence.txt respectively.
#     2a. HTMLCLEANER_JAR_URL
#         The full URL for the jar.
#     2b. HTMLCLEANER_JAR_URL_BASE + HTMLCLEANER_VERSION + HTMLCLEANER_FILE
#         The individual URL components used to construct the full URL.
#
# JUnit, for running self-tests
#     Checksum variables:
#         JUNIT_JAR_CHECKSUM: checksum of binary archive
#
#     1. JUNIT_JAR + JUNIT_LICENSE
#         The path to junit.jar and LICENSE respectively.
#     2a. JUNIT_JAR_URL
#         The full URL for the jar.
#     2b. JUNIT_JAR_URL_BASE + JUNIT_VERSION + JUNIT_FILE
#         The individual URL components used to construct the full URL.
#
# Some control variables can be overridden by command-line options.
# Use the  --help option for details.

mydir="$(dirname ${BASH_SOURCE[0]})"
log_module="$(basename "${BASH_SOURCE[0]}")"
. "${mydir}/build-support/build-common.sh"

usage() {
    echo "Usage: $0 <options> [ [--] <make-options-and-targets> ]"
    echo "--help"
    echo "      Show this message"
    echo "--jdk /path/to/jdk"
    echo "      Path to JDK; must be JDK 17 or higher"
    echo "--quiet | -q"
    echo "      Reduce the logging output."
    echo "--show-default-versions"
    echo "      Show default versions of external components"
    echo "--show-config-details"
    echo "      Show configuration details"
    echo "--skip-checksum-check"
    echo "      Skip the checksum check for downloaded files."
    echo "--skip-download"
    echo "      Skip downloading files if file already available"
    echo "--skip-make"
    echo "      Skip running 'make' (just download dependencies if needed)"
    echo "--version-numbers file"
    echo "      Provide an alternate file containing dependency version information"
    echo "--"
    echo "      Subsequent arguments are for 'make'"
}

ensure_arg() {
    check_arguments "${FUNCNAME}" 2 $#
    local option="$1"
    local arg_count="$2"
    if [ "$2" -lt "2" ]; then
        echo "The $option option requires an argument"
        exit
    fi
}

process_args() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --help|-h )             HELP=1 ;                                        shift ;;
            --jdk )                 ensure_arg "$1" $# ; JAVA_HOME="$2" ;           shift ; shift ;;
            --quiet|-q )            export QUIET=1 ;                                shift ;;
            --show-config-details ) SHOW_CONFIG_DETAILS=1 ;                         shift ;;
            --show-default-versions ) SHOW_DEFAULT_VERSIONS=1 ;                     shift ;;
            --skip-checksum-check ) export SKIP_CHECKSUM_CHECK=1 ;                  shift ;;
            --skip-download )       export SKIP_DOWNLOAD=1 ;                        shift ;;
            --skip-make )           SKIP_MAKE=1 ;                                   shift ;;
            --version-numbers )     ensure_arg "$1" $# ; VERSION_NUMBERS="$2" ;     shift ; shift ;;
            -- )                    shift ; MAKE_ARGS="$@" ;                        break ;;
            -* )                    error "unknown option: '$1'" ;                  exit 1 ;;
            * )                     MAKE_ARGS="$@" ;                                break ;;
        esac
    done
}

process_args "$@"

if [ -n "${HELP:-}" ]; then
    usage
    exit
fi

. "${VERSION_NUMBERS:-${mydir}/build-support/version-numbers}"

APIDIFF_VERSION="${APIDIFF_VERSION:-}"

DAISYDIFF_BIN_VERSION="${DAISYDIFF_BIN_VERSION:-${DEFAULT_DAISYDIFF_BIN_VERSION}}"
# uncomment or override to download a precompiled jar file for daisydiff
#DAISYDIFF_BIN_ARCHIVE_URL_BASE="${DAISYDIFF_BIN_ARCHIVE_URL_BASE:-${GOOGLE_CODE_URL_BASE}}"
#DAISYDIFF_BIN_ARCHIVE_CHECKSUM="${DAISYDIFF_BIN_ARCHIVE_CHECKSUM:-${DEFAULT_DAISYDIFF_BIN_ARCHIVE_CHECKSUM}}"
DAISYDIFF_LICENSE_VERSION="${DAISYDIFF_LICENSE_VERSION:-${DEFAULT_DAISYDIFF_LICENSE_VERSION:-${DAISYDIFF_BIN_VERSION}}}"
DAISYDIFF_LICENSE_CHECKSUM="${DAISYDIFF_LICENSE_CHECKSUM:-${DEFAULT_DAISYDIFF_LICENSE_CHECKSUM}}"

DAISYDIFF_SRC_VERSION="${DAISYDIFF_SRC_VERSION:-${DEFAULT_DAISYDIFF_SRC_VERSION}}"
DAISYDIFF_SRC_ARCHIVE_URL_BASE="${DAISYDIFF_SRC_ARCHIVE_URL_BASE:-${DAISYDIFF_REPO_URL_BASE}}"
DAISYDIFF_SRC_ARCHIVE_CHECKSUM="${DAISYDIFF_SRC_ARCHIVE_CHECKSUM:-${DEFAULT_DAISYDIFF_SRC_ARCHIVE_CHECKSUM}}"

EQUINOX_VERSION="${EQUINOX_VERSION:-${DEFAULT_EQUINOX_VERSION}}"
EQUINOX_JAR_URL_BASE="${EQUINOX_JAR_URL_BASE:-${MAVEN_REPO_URL_BASE}}"
EQUINOX_JAR_CHECKSUM="${EQUINOX_JAR_CHECKSUM:-${DEFAULT_EQUINOX_JAR_CHECKSUM}}"
EQUINOX_LICENSE_CHECKSUM="${EQUINOX_LICENSE_CHECKSUM:-${DEFAULT_EQUINOX_LICENSE_CHECKSUM}}"

HTMLCLEANER_VERSION="${HTMLCLEANER_VERSION:-${DEFAULT_HTMLCLEANER_VERSION}}"
HTMLCLEANER_JAR_URL_BASE="${HTMLCLEANER_JAR_URL_BASE:-${MAVEN_REPO_URL_BASE}}"
HTMLCLEANER_JAR_CHECKSUM="${HTMLCLEANER_JAR_CHECKSUM:-${DEFAULT_HTMLCLEANER_JAR_CHECKSUM}}"
HTMLCLEANER_LICENSE_CHECKSUM="${HTMLCLEANER_LICENSE_CHECKSUM:-${DEFAULT_HTMLCLEANER_LICENSE_CHECKSUM}}"

JAVADIFFUTILS_VERSION="${JAVADIFFUTILS_VERSION:-${DEFAULT_JAVADIFFUTILS_VERSION}}"
JAVADIFFUTILS_JAR_URL_BASE="${JAVADIFFUTILS_JAR_URL_BASE:-${MAVEN_REPO_URL_BASE}}"
JAVADIFFUTILS_JAR_CHECKSUM="${JAVADIFFUTILS_JAR_CHECKSUM:-${DEFAULT_JAVADIFFUTILS_JAR_CHECKSUM}}"
JAVADIFFUTILS_LICENSE_VERSION="${JAVADIFFUTILS_LICENSE_VERSION:-${DEFAULT_JAVADIFFUTILS_LICENSE_VERSION:-${JAVADIFFUTILS_VERSION}}}"
JAVADIFFUTILS_LICENSE_CHECKSUM="${JAVADIFFUTILS_LICENSE_CHECKSUM:-${DEFAULT_JAVADIFFUTILS_LICENSE_CHECKSUM}}"

JUNIT_VERSION="${JUNIT_VERSION:-${DEFAULT_JUNIT_VERSION}}"
JUNIT_JAR_URL_BASE="${JUNIT_JAR_URL_BASE:-${MAVEN_REPO_URL_BASE}}"
JUNIT_JAR_CHECKSUM="${JUNIT_JAR_CHECKSUM:-${DEFAULT_JUNIT_JAR_CHECKSUM}}"
JUNIT_LICENSE_FILE="${JUNIT_LICENSE_FILE:-${DEFAULT_JUNIT_LICENSE_FILE}}"

if [ "${SHOW_DEFAULT_VERSIONS:-}" != "" ]; then
    find ${mydir} -name version-numbers | \
        xargs cat | \
        grep -v '^#' | \
        grep -E 'DEFAULT.*(_VERSION|_SRC_TAG)' | \
        sort -u
    exit
fi

if [ "${SHOW_CONFIG_DETAILS:-}" != "" ]; then
    ( set -o posix ; set ) | \
        grep -E '^(DAISYDIFF|JAVADIFFUTILS|JUNIT)_[A-Z_]*=' | \
        sort -u
    exit
fi

setup_java_home() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${JAVA_HOME:-}" ]; then
        return
    fi

    if [ -z "${JDK_ARCHIVE_URL:-}" ]; then
        if [ -n "${JDK_ARCHIVE_URL_BASE:-}" ]; then
            if [ -z "${JDK_VERSION:-}" ]; then
                error "JDK_VERSION not set"
                exit 1
            fi
            if [ -z "${JDK_BUILD_NUMBER:-}" ]; then
                error "JDK_BUILD_NUMBER not set"
                exit 1
            fi
            if [ -z "${JDK_FILE:-}" ]; then
                error "JDK_FILE not set"
                exit 1
            fi
            JDK_ARCHIVE_URL="${JDK_ARCHIVE_URL_BASE}/${JDK_VERSION}/${JDK_BUILD_NUMBER}/${JDK_FILE}"
        fi
    fi

    local JDK_DEPS_DIR="${DEPS_DIR}"

    if [ -n "${JDK_ARCHIVE_URL:-}" ]; then
        local JDK_LOCAL_ARCHIVE_FILE="${JDK_DEPS_DIR}/$(basename "${JDK_ARCHIVE_URL}")"
        if [ -n "${JDK_ARCHIVE_CHECKSUM:-}" ]; then
            get_archive "${JDK_ARCHIVE_URL}" "${JDK_LOCAL_ARCHIVE_FILE}" "${JDK_DEPS_DIR}" "${JDK_ARCHIVE_CHECKSUM}"
        else
            get_archive_no_checksum "${JDK_ARCHIVE_URL}" "${JDK_LOCAL_ARCHIVE_FILE}" "${JDK_DEPS_DIR}"
        fi
        local JDK_JAVAC="$(find "${JDK_DEPS_DIR}" -path '*/bin/javac')"
        JAVA_HOME="$(dirname $(dirname "${JDK_JAVAC}"))"
        return
    fi

    error "None of --jdk, JAVA_HOME, JDK_ARCHIVE_URL or JDK_ARCHIVE_URL_BASE are set"
    exit 1
}

sanity_check_java_home() {
    if [ -z "${JAVA_HOME:-}" ]; then
        error "No JAVA_HOME set"
        exit 1
    fi

    if [ ! -d "${JAVA_HOME}" ]; then
        error "'${JAVA_HOME}' is not a directory"
        exit 1
    fi

    if [ ! -x "${JAVA_HOME}/bin/java" ]; then
        error "Could not find an executable binary at '${JAVA_HOME}/bin/java'"
        exit 1
    fi

    local version=$(${JAVA_HOME}/bin/java -version 2>&1)
    local vnum=$(echo "${version}" | \
        grep -i -E '^(java|openjdk)' |
        head -n 1 | \
        sed -e 's/^[^0-9]*//' -e 's/[^0-9].*//' )
    if [ "${vnum:-0}" -lt "17" ]; then
        error "JDK 17 or newer is required to build apidiff"
        exit 1
    fi
}
setup_java_home
sanity_check_java_home
export JAVA_HOME
info "JAVA_HOME: ${JAVA_HOME}"

#----- Daisy Diff -----
setup_daisydiff_jar() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${DAISYDIFF_JAR:-}" ]; then
        return
    fi

    local DAISYDIFF_DEPS_DIR="${DEPS_DIR}/daisydiff"

    if [ -z "${DAISYDIFF_SRC_ARCHIVE_URL:-}" ]; then
        if [ -n "${DAISYDIFF_SRC_ARCHIVE_URL_BASE:-}" ]; then
            DAISYDIFF_SRC_ARCHIVE_URL="${DAISYDIFF_SRC_ARCHIVE_URL_BASE}/archive/refs/tags/release-${DAISYDIFF_SRC_VERSION}.tar.gz"
        fi
    fi

    if [ -n "${DAISYDIFF_SRC_ARCHIVE_URL:-}" ]; then
        local DAISYDIFF_LOCAL_ARCHIVE_FILE="${DAISYDIFF_DEPS_DIR}/$(basename "${DAISYDIFF_SRC_ARCHIVE_URL}")"
        get_archive "${DAISYDIFF_SRC_ARCHIVE_URL}" "${DAISYDIFF_LOCAL_ARCHIVE_FILE}" "${DAISYDIFF_DEPS_DIR}" "${DAISYDIFF_SRC_ARCHIVE_CHECKSUM}"
        DAISYDIFF_SRC=$(cd "${DAISYDIFF_DEPS_DIR}"/*/src; pwd)
        return
    fi

    info "None of DAISYDIFF_JAR, DAISYDIFF_SRC_ARCHIVE_URL, DAISYDIFF_SRC_ARCHIVE_URL_BASE are set"
}

setup_daisydiff_jar
if [ -n "${DAISYDIFF_JAR:-}" ]; then
    info "DAISYDIFF_JAR: ${DAISYDIFF_JAR}"
else
    info "DAISYDIFF_SRC: ${DAISYDIFF_SRC}"
fi

#----- Daisy Diff License -----
setup_daisydiff_license() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${DAISYDIFF_LICENSE:-}" ]; then
        return
    fi

    local DAISYDIFF_LICENSE_DEPS_DIR="${DEPS_DIR}/daisydiff-license"
    DAISYDIFF_LICENSE="${DAISYDIFF_LICENSE_DEPS_DIR}/LICENSE"
    download_and_checksum "https://raw.githubusercontent.com/DaisyDiff/DaisyDiff/${DAISYDIFF_LICENSE_VERSION}/LICENSE.txt" "${DAISYDIFF_LICENSE}" "${DAISYDIFF_LICENSE_CHECKSUM}"
}
setup_daisydiff_license
info "DAISYDIFF_LICENSE: ${DAISYDIFF_LICENSE}"

#----- Eclipse Equinox Common Runtime
setup_equinox_jar() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${EQUINOX_JAR:-}" ]; then
        return
    fi

    if [ -z "${EQUINOX_JAR_URL:-}" ]; then
        if [ -n "${EQUINOX_JAR_URL_BASE:-}" ]; then
            EQUINOX_JAR_URL="${EQUINOX_JAR_URL_BASE}/org/eclipse/equinox/org.eclipse.equinox.common/${EQUINOX_VERSION}/org.eclipse.equinox.common-${EQUINOX_VERSION}.jar"
        fi
    fi

    local EQUINOX_DEPS_DIR="${DEPS_DIR}/equinox"

    if [ -n "${EQUINOX_JAR_URL:-}" ]; then
        EQUINOX_JAR="${EQUINOX_DEPS_DIR}/$(basename "${EQUINOX_JAR_URL}")"
        download_and_checksum "${EQUINOX_JAR_URL}" "${EQUINOX_JAR}" "${EQUINOX_JAR_CHECKSUM}"
        return
    fi

    error "Neither EQUINOX_JAR_URL nor EQUINOX_JAR_URL_BASE is set"
    exit 1
}

if [ -n "${DAISYDIFF_SRC:-}" ]; then
    setup_equinox_jar
    info "EQUINOX_JAR: ${EQUINOX_JAR}"
fi

#----- Eclipse Equinox Common Runtime License -----
setup_equinox_license() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${EQUINOX_LICENSE:-}" ]; then
        return
    fi

    local EQUINOX_LICENSE_DEPS_DIR="${DEPS_DIR}/equinox-license"
    EQUINOX_LICENSE="${EQUINOX_LICENSE_DEPS_DIR}/epl-v10.html"
    download_and_checksum "http://www.eclipse.org/org/documents/epl-v10.html" "${EQUINOX_LICENSE}" "${EQUINOX_LICENSE_CHECKSUM}"

}

if [ -n "${DAISYDIFF_SRC:-}" ]; then
    setup_equinox_license
    info "EQUINOX_LICENSE: ${EQUINOX_LICENSE}"
fi

#----- Html Cleaner
setup_htmlcleaner_jar() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${HTMLCLEANER_JAR:-}" ]; then
        return
    fi

    if [ -z "${HTMLCLEANER_JAR_URL:-}" ]; then
        if [ -n "${HTMLCLEANER_JAR_URL_BASE:-}" ]; then
            HTMLCLEANER_JAR_URL="${HTMLCLEANER_JAR_URL_BASE}/net/sourceforge/htmlcleaner/htmlcleaner/${HTMLCLEANER_VERSION}/htmlcleaner-${HTMLCLEANER_VERSION}.jar"
        fi
    fi

    local HTMLCLEANER_DEPS_DIR="${DEPS_DIR}/htmlcleaner"

    if [ -n "${HTMLCLEANER_JAR_URL:-}" ]; then
        HTMLCLEANER_JAR="${HTMLCLEANER_DEPS_DIR}/$(basename "${HTMLCLEANER_JAR_URL}")"
        download_and_checksum "${HTMLCLEANER_JAR_URL}" "${HTMLCLEANER_JAR}" "${HTMLCLEANER_JAR_CHECKSUM}"
        return
    fi

    error "Neither HTMLCLEANER_JAR_URL nor HTMLCLEANER_JAR_URL_BASE is set"
    exit 1
}
setup_htmlcleaner_jar
info "HTMLCLEANER_JAR: ${HTMLCLEANER_JAR}"

#----- Html Cleaner License -----
setup_htmlcleaner_license() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${HTMLCLEANER_LICENSE:-}" ]; then
        return
    fi

    local HTMLCLEANER_LICENSE_DEPS_DIR="${DEPS_DIR}/htmlcleaner-license"
    HTMLCLEANER_LICENSE="${HTMLCLEANER_LICENSE_DEPS_DIR}/licence.txt"
    download_and_checksum "https://sourceforge.net/p/htmlcleaner/code/HEAD/tree/tags/htmlcleaner-${HTMLCLEANER_VERSION}/licence.txt?format=raw" "${HTMLCLEANER_LICENSE}" "${HTMLCLEANER_LICENSE_CHECKSUM}"

}
setup_htmlcleaner_license
info "HTMLCLEANER_LICENSE: ${HTMLCLEANER_LICENSE}"


#----- Java Diff Utils -----
setup_javadiffutils() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${JAVADIFFUTILS_JAR:-}" ]; then
        return
    fi

    if [ -z "${JAVADIFFUTILS_JAR_URL:-}" ]; then
        if [ -n "${JAVADIFFUTILS_JAR_URL_BASE:-}" ]; then
            JAVADIFFUTILS_JAR_URL="${JAVADIFFUTILS_JAR_URL_BASE}/io/github/java-diff-utils/java-diff-utils/${JAVADIFFUTILS_VERSION}/java-diff-utils-${JAVADIFFUTILS_VERSION}.jar"
        fi
    fi

    local JAVADIFFUTILS_DEPS_DIR="${DEPS_DIR}/java-diff-utils"

    if [ -n "${JAVADIFFUTILS_JAR_URL:-}" ]; then
        JAVADIFFUTILS_JAR="${JAVADIFFUTILS_DEPS_DIR}/$(basename "${JAVADIFFUTILS_JAR_URL}")"
        download_and_checksum "${JAVADIFFUTILS_JAR_URL}" "${JAVADIFFUTILS_JAR}" "${JAVADIFFUTILS_JAR_CHECKSUM}"
        return
    fi

    error "Neither JAVADIFFUTILS_JAR_URL nor JAVADIFFUTILS_JAR_URL_BASE is set"
    exit 1
}
setup_javadiffutils
info "JAVADIFFUTILS_JAR: ${JAVADIFFUTILS_JAR}"

#----- Java Diff Utils License -----
setup_javadiffutils_license() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${JAVADIFFUTILS_LICENSE:-}" ]; then
        return
    fi

    local JAVADIFFUTILS_LICENSE_DEPS_DIR="${DEPS_DIR}/javadiffutils-license"
    JAVADIFFUTILS_LICENSE="${JAVADIFFUTILS_LICENSE_DEPS_DIR}/LICENSE"
    download_and_checksum "https://raw.githubusercontent.com/java-diff-utils/java-diff-utils/java-diff-utils-${JAVADIFFUTILS_LICENSE_VERSION}/LICENSE" "${JAVADIFFUTILS_LICENSE}" "${JAVADIFFUTILS_LICENSE_CHECKSUM}"
}
setup_javadiffutils_license
info "JAVADIFFUTILS_LICENSE: ${JAVADIFFUTILS_LICENSE}"



#----- JUnit -----
setup_junit() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${JUNIT_JAR:-}" ]; then
        return
    fi

    if [ -z "${JUNIT_JAR_URL:-}" ]; then
        if [ -n "${JUNIT_JAR_URL_BASE:-}" ]; then
            JUNIT_JAR_URL="${JUNIT_JAR_URL_BASE}/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"
        fi
    fi

    local JUNIT_DEPS_DIR="${DEPS_DIR}/junit"

    if [ -n "${JUNIT_JAR_URL:-}" ]; then
        JUNIT_JAR="${JUNIT_DEPS_DIR}/$(basename ${JUNIT_JAR_URL})"
        download_and_checksum "${JUNIT_JAR_URL}" "${JUNIT_JAR}" "${JUNIT_JAR_CHECKSUM}"
        return
    fi

    error "None of JUNIT_JAR, JUNIT_JAR_URL or JUNIT_JAR_URL_BASE is set"
    exit 1
}
setup_junit
info "JUNIT_JAR ${JUNIT_JAR}"

#----- JUnit license -----
setup_junit_license() {
    check_arguments "${FUNCNAME}" 0 $#

    if [ -n "${JUNIT_LICENSE:-}" ]; then
        return
    fi

    local JUNIT_LICENSE_DEPS_DIR="${DEPS_DIR}/junit-license"
    "${UNZIP_CMD}" ${UNZIP_OPTIONS} "${JUNIT_JAR}" ${JUNIT_LICENSE_FILE} -d "${JUNIT_LICENSE_DEPS_DIR}"
    JUNIT_LICENSE="${JUNIT_LICENSE_DEPS_DIR}/${JUNIT_LICENSE_FILE}"
}
setup_junit_license
info "JUNIT_LICENSE: ${JUNIT_LICENSE}"

##
# Build number defaults to 0
#
setup_build_info() {
    check_arguments "${FUNCNAME}" 0 $#

    APIDIFF_BUILD_MILESTONE="${APIDIFF_BUILD_MILESTONE:-dev}"
    APIDIFF_BUILD_NUMBER="${APIDIFF_BUILD_NUMBER:-0}"

    if [ -z "${APIDIFF_VERSION_STRING:-}" ]; then
        MILESTONE=""
        if [ -n "${APIDIFF_BUILD_MILESTONE}" ]; then
            MILESTONE="-${APIDIFF_BUILD_MILESTONE}"
        fi
        APIDIFF_VERSION_STRING="${APIDIFF_VERSION}${MILESTONE}+${APIDIFF_BUILD_NUMBER}"
    fi
}
setup_build_info
info "APIDIFF_VERSION: ${APIDIFF_VERSION}"
info "APIDIFF_BUILD_NUMBER: ${APIDIFF_BUILD_NUMBER}"
info "APIDIFF_BUILD_MILESTONE: ${APIDIFF_BUILD_MILESTONE}"

check_file() {
    check_arguments "${FUNCNAME}" 1 $#

    info "Checking $1"
    if [ ! -f "$1" ]; then
        error "Missing: $1"
        exit 1
    fi
}

check_dir() {
    check_arguments "${FUNCNAME}" 1 $#

    info "Checking $1"
    if [ ! -d "$1" ]; then
        error "Missing: $1"
        exit 1
    fi
}

check_dir  "${JAVA_HOME}"
check_file "${JUNIT_JAR}"
check_file "${JAVADIFFUTILS_JAR}"
if [ -n "${JAVADIFFUTILS_LICENSE:-}" ]; then
    check_file "${JAVADIFFUTILS_LICENSE}"
fi
if [ -n "${DAISYDIFF_JAR:-}" ]; then
    check_file "${DAISYDIFF_JAR}"
fi
if [ -n "${DAISYDIFF_SRC:-}" ]; then
    check_dir "${DAISYDIFF_SRC}"
fi
if [ -n "${DAISYDIFF_LICENSE:-}" ]; then
    check_file "${DAISYDIFF_LICENSE}"
fi
if [ -n "${EQUINOX_JAR:-}" ]; then
    check_file "${EQUINOX_JAR}"
fi
if [ -n "${EQUINOX_LICENSE:-}" ]; then
    check_file "${EQUINOX_LICENSE}"
fi
check_file "${HTMLCLEANER_JAR}"
if [ -n "${HTMLCLEANER_LICENSE:-}" ]; then
    check_file "${HTMLCLEANER_LICENSE}"
fi

if [ -n "${SKIP_MAKE:-}" ]; then
    exit
fi


# save make command for possible later reuse, bypassing this script
mkdir -p ${BUILD_DIR}
cat > ${BUILD_DIR}/make.sh << EOF
#!/bin/sh

# Build apidiff
cd "${ROOT}/make"
make BUILDDIR="${BUILD_DIR}"                                  \\
     BUILD_MILESTONE="${APIDIFF_BUILD_MILESTONE}"             \\
     BUILD_NUMBER="${APIDIFF_BUILD_NUMBER}"                   \\
     BUILD_VERSION="${APIDIFF_VERSION}"                       \\
     BUILD_VERSION_STRING="${APIDIFF_VERSION_STRING}"         \\
     DAISYDIFF_JAR="$(mixed_path "${DAISYDIFF_JAR:-}")"       \\
     DAISYDIFF_SRC="$(mixed_path "${DAISYDIFF_SRC:-}")"       \\
     DAISYDIFF_LICENSE="${DAISYDIFF_LICENSE}"                 \\
     EQUINOX_JAR="$(mixed_path "${EQUINOX_JAR:-}")"           \\
     EQUINOX_LICENSE="$(mixed_path "${EQUINOX_LICENSE:-}")"   \\
     HTMLCLEANER_JAR="${HTMLCLEANER_JAR}"                     \\
     HTMLCLEANER_LICENSE="${HTMLCLEANER_LICENSE}"             \\
     JAVADIFFUTILS_JAR="$(mixed_path "${JAVADIFFUTILS_JAR}")" \\
     JAVADIFFUTILS_LICENSE="${JAVADIFFUTILS_LICENSE}"         \\
     JDKHOME="${JAVA_HOME}"                                   \\
     JUNIT_JAR="$(mixed_path "${JUNIT_JAR}")"                 \\
   "\$@"
EOF

sh ${BUILD_DIR}/make.sh ${MAKE_ARGS:-}

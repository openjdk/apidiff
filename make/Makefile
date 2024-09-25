#
# Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
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

TOPDIR = ..

include Defs.gmk

default: build

all: build test

#----------------------------------------------------------------------

include apidiff.gmk
include $(TOPDIR)/test/*/*.gmk
include Rules.gmk

build: check-build-vars $(BUILDFILES)

images: $(VERBOSEZIPFILES)

docs: $(BUILDDIR)/api.jdk.codetools.apidiff.ok

test: check-test-vars $(INITIAL_TESTS) $(TESTS) $(FINAL_TESTS)
	count=`echo $+ | wc -w` ; \
	echo "All ($${count}) selected tests completed successfully"

clean:
	$(RM) $(BUILDDIR)

.NO_PARALLEL: clean

sanity:
	@echo "JDKHOME               = $(JDKHOME)"
	@echo "JUNIT_JAR             = $(JUNIT_JAR)"
	@echo "JAVADIFFUTILS_JAR     = $(JAVADIFFUTILS_JAR)"
	@echo "JAVADIFFUTILS_LICENSE = $(JAVADIFFUTILS_LICENSE)"
	@echo "DAISYDIFF_JAR         = $(DAISYDIFF_JAR)"
	@echo "DAISYDIFF_LICENSE     = $(DAISYDIFF_LICENSE)"
	@echo "HTMLCLEANER_JAR       = $(HTMLCLEANER_JAR)"
	@echo "HTMLCLEANER_LICENSE   = $(HTMLCLEANER_LICENSE)"

check-build-vars:
	@if [ -z "$(JDKHOME)" ]; then \
		echo "JDKHOME not set; must be JDK 17 or later" ; exit 1 ; \
	fi
	@if [ -z "$(DAISYDIFF_JAR)" -a -z "$(DAISYDIFF_SRC)" ]; then \
		echo "DAISYDIFF_JAR or DAISYDIFF_SRC not set" ; exit 1 ; \
	fi
	@if [ -z "$(DAISYDIFF_LICENSE)" ]; then \
		echo "DAISYDIFF_LICENSE not set (will not be included)" ; \
	fi
	@if [ -z "$(HTMLCLEANER_JAR)" ]; then \
		echo "HTMLCLEANER_JAR not set" ; exit 1 ; \
	fi
	@if [ -z "$(HTMLCLEANER_LICENSE)" ]; then \
		echo "HTMLCLEANER_LICENSE not set (will not be included)" ; \
	fi
	@if [ -z "$(JAVADIFFUTILS_JAR)" ]; then \
		echo "JAVADIFFUTILS_JAR not set" ; exit 1 ; \
	fi
	@if [ -z "$(JAVADIFFUTILS_LICENSE)" ]; then \
		echo "JAVADIFFUTILS_LICENSE not set (will not be included)" ; \
	fi

check-test-vars:
	@if [ -z "$(JUNIT_JAR)" ]; then \
		echo "JUNIT_JAR not set" ; exit 1 ; \
	fi

dependencies: check-build-vars check-test-vars

#----------------------------------------------------------------------


.PHONY: default all build test images clean sanity



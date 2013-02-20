#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# $Id: defines.mk,v 1.12 2009/12/03 19:03:59 dknab Exp $
#
#

# Portions Copyrighted 2010-2012 ForgeRock Inc

#
# This makefile defines a number of standard symbols used in by the
# makefiles that build the Agent Pack.
#
# Make sure USERX_ROOT is defined before including this file.

ifndef	DEFINES_INCLUDED
DEFINES_INCLUDED := true

###############
# VERSION INFO
###############

AGENT_MAJOR_VER := 3
AGENT_MINOR_VER := 1
AGENT_MICRO_VER := 0
AGENT_RELEASE_NAME := Xpress
AGENT_VER := $(AGENT_MAJOR_VER).$(AGENT_MINOR_VER).$(AGENT_MICRO_VER)-$(AGENT_RELEASE_NAME)

# Set RELEASE_TYPE to one of the following:
#   empty: for RTM or patch release, ex: agent 3.0 or agent 3.0-01 
#   ER: for Exception Release, ex: agent 3.0-01 ER 1
#   FVB: for Fix Verification Binary, ex: agent 3.0-01 (Escalation 123) 
#        (FVB is only for local workspace, should not be checked in)
# If RELEASE_TYPE is set to ER, update AGENT_ER_VER.
# If RELEASE_TYPE is set to FVB, update AGENT_FVB_MARKER.
#
RELEASE_TYPE =

# For ER set AGENT_ER_VER (ex: AGENT_ER_VER := ER 1)
ifeq ($(RELEASE_TYPE), ER)
AGENT_ER_VER := ER 6
AGENT_ER_TEXT := Exception Release: $(AGENT_ER_VER)
ADD_README := YES
else
ADD_README := NO
endif

# For FVB set AGENT_FVB_MARKER (ex: AGENT_FVB_MARKER := Escalation 123)
ifeq ($(RELEASE_TYPE), FVB)
AGENT_FVB_MARKER := 
AGENT_FVB_TEXT := Fix Verification Binary: $(AGENT_FVB_MARKER)
endif

###############

OS_ARCH := $(shell uname -s)
OS_ARCH_VER := $(shell uname -r)
BUILD_DATE := $(shell date)
SVN_REVISION := $(shell svn info . | grep Revision:)
#BUILD_MACHINE := $(shell uname -n)
BUILD_MACHINE := constable.internal.forgerock.com
MC_ARCH := $(shell uname -m)
CYGWIN_ARCH := CYG32

# this flag is defined for Cygwin running on WOW64 mode in Win64
ifeq ($(strip $(findstring WOW64,$(OS_ARCH))), WOW64)
    CYGWIN_ARCH := WOW64
endif
ifeq ($(strip $(patsubst CYGWIN_NT%, CYGWIN_NT, $(OS_ARCH))), CYGWIN_NT)
    OS_ARCH := WINNT
    OS_IS_CYGWIN := true
endif
ifeq ($(OS_ARCH), Windows_NT)
    OS_ARCH := WINNT
endif
OSMC_ARCH := $(OS_ARCH)


DEBUG :=
COMMA := ,

CP := cp -fp
ECHO := echo
MKDIR := mkdir -p
MV := mv -f
PERL5 := perl
RMDIR := rmdir -p -s
TAR := tar
AR := ar
ARFLAGS := -ru

SRC_DIR := $(USERX_ROOT)
BUILT_DIR := $(USERX_ROOT)/built
DEST_DIR := $(BUILT_DIR)
DEST_BIN_DIR := $(DEST_DIR)/bin
DEST_CLASS_DIR := $(DEST_DIR)/classes
DEST_CONFIG_DIR := $(DEST_DIR)/config
DEST_DOC_DIR := $(DEST_DIR)/docs
DEST_DROP_DIR := $(DEST_DIR)/drop
DEST_EXAMPLES_DIR := $(DEST_DIR)/examples
DEST_SAMPLES_DIR := $(DEST_DIR)/samples
DEST_INC_DIR := $(DEST_DIR)/include
DEST_JAR_DIR := $(DEST_DIR)/archive
DEST_LIB_DIR := $(DEST_DIR)/$(OS_ARCH)/lib
DEST_PACKAGE_DIR := $(DEST_DIR)/$(OS_ARCH)/packages
DEST_PACKAGE_SCRATCH_DIR := $(DEST_DIR)/$(OS_ARCH)/packages.scratch
DEST_TEST_DIR := $(DEST_DIR)/test
DEST_WAR_DIR := $(DEST_DIR)/war
CLASS_PREFIX_DIR := com/iplanet
USERX_CLASS_DIR := $(DEST_CLASS_DIR)/$(CLASS_PREFIX_DIR)
RELTOOLS_DIR := $(USERX_ROOT)/../../../reltools
BASE_PRODUCT_NAME := Sun Java(tm) System

EXTERNAL_DIR := $(USERX_ROOT)/extlib/$(OS_ARCH)
ifeq ($(MC_ARCH), i86pc)
EXTERNAL_DIR := $(USERX_ROOT)/extlib/$(OS_ARCH)_$(MC_ARCH)
endif

#
# Crypt Util Executable
#
ifeq ($(OS_ARCH), WINNT)
CRYPT_EXE := cryptit.exe
else
CRYPT_EXE := crypt_util
endif

#
# The following four symbols are intentionally defined with '=', and not
# ':=', so that individual makefiles can define DEBUG_FLAGS and/or
# INCLUDE_FLAGS as desired.
#
CFLAGS = $(DEBUG_FLAGS) $(INCLUDE_FLAGS) $($@_CFLAGS)
CXXFLAGS = $(DEBUG_FLAGS) $(INCLUDE_FLAGS) $($@_CXXFLAGS)
LDFLAGS = $($@_LDFLAGS)
LDLIBS = $($@_LDLIBS)

#
# The following is intentionally defined with '=', instead of ':=',
# so that the OS-specific settings for versioning the library and the
# local makefile setting of DEBUG_FLAGS are used.
#
MAKE_SHARED_LIB = $(CXX) $(LD_ORIGIN_FLAG) $(LD_MAKE_SHARED_LIB_FLAG) $(DEBUG_FLAGS) \
        $(LDFLAGS) \
	$(LD_VERSION_LIB_FLAG) $(LD_FILTER_SYMS_FLAG) \
	$(filter %.o, $^) $(PLATFORM_SHARED_OBJS) $(LDLIBS) -o $@

ifndef	MAPFILE_EXT
MAPFILE_EXT := .mapfile
endif

ifndef	SO_EXT
SO_EXT := .so
STATIC_EXT := .a
endif


###########
# FOR DSAME
###########

AGENT_JAR_FILE := $(DEST_JAR_DIR)/am_agent.jar
NOTIFYAPI_JAR_FILE := $(DEST_JAR_DIR)/am_notifyapi.jar
ifeq ($(OS_ARCH), Linux)
DSAME_DROP_FILE := common-3.0-0.$(MC_ARCH)
else
DSAME_DROP_FILE := common_3_0.$(OS_ARCH)
endif

include $(USERX_ROOT)/arch/defines_$(OS_ARCH).mk

#
# Now that the OS-specific PATH_SEP has been defined we can initialize
# the Java CLASSPATH.
#
# NOTE: The *CLASSPATH variables need to be set with '=', rather than ':=',
# because the various pieces of the variables are set in components.mk,
# which includes this file before defining those variables.
#
BASECLASSPATH = .$(PATH_SEP)$(JSDK_JAR_FILE)$(PATH_SEP)$(JAXP_JAR_FILES)$(PATH_SEP)$(DEST_CLASS_DIR)$(PATH_SEP)$(SRC_DIR)$(PATH_SEP)$(SETUP_SDK_CLASS_DIR)

ifdef LOCAL_CLASS_PATH
 CLASSPATH = $(BASECLASSPATH)$(PATH_SEP)$(LOCAL_CLASS_PATH)
else
 CLASSPATH = $(BASECLASSPATH)
endif

### Definitions for generating the Agent package file

ifndef RELTOOLS_SUFFIX_ARG
  RELTOOLS_SUFFIX_ARG := -suf .tar
endif

RELTOOLS_FTPNAME_CMD := perl $(RELTOOLS_DIR)/ftpname.pl

ifdef VER
  VERSION_PKG=-ver $(VER)
else
  VERSION_PKG=-ver 2.0
endif

ifdef PRODUCT_MARKET
  ifeq ($(PRODUCT_MARKET), JA)
    INTL=-intl ja
  endif
  ifeq ($(PRODUCT_MARKET), EU)
    INTL=-intl eu
  endif
else
  INTL=-intl us
endif

ifeq ($(BUILD_SECURITY), domestic)
  SEC=-sec domestic
  ifdef FORTEZZA
     SEC=-sec fortezza
  endif
else
  SEC=-sec export
endif

ifneq ($(DEBUG), optimize)
  DBG=-debug full
else
  DBG=-debug optimize
endif

#
# This needs to be defined with '=', instead of ':=', because we want to delay
# the shell invocation until the value is actually needed.
#
RELTOOLS_FTPNAME = $(BUILD_SHIP)/$(shell $(RELTOOLS_FTPNAME_CMD) -name agents $(VERSION_PKG) $(INTL) $(SEC) $(DBG) $(RELTOOLS_SUFFIX_ARG))

###############################################################################

endif

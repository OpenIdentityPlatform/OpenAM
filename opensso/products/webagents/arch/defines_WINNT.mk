#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
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
# $Id: defines_WINNT.mk,v 1.5 2008/09/13 01:11:04 robertis Exp $
# 
#

#
# This makefile defines a number of standard OS-dependent symbols
# used in by the makefiles that build the Agent Pack.
#

PATH_SEP := ;

RELTOOLS_SUFFIX_ARG := -suf .zip

CCG := $(USERX_ROOT)/arch/compiler.ccg
export CCG
COMPILERS_DIR :=

ifdef	OS_IS_CYGWIN
CC := cl
CXX := cl
else
CC := cc
CXX := cc
endif

LINK := link
MT := mt

EXE_EXT := .exe
MAPFILE_EXT := .def
NM := nm
RC := rc
ifdef	OS_IS_CYGWIN
    RMDIR := rmdir -p --ignore-fail-on-non-empty
else
    RMDIR := rmdir -p
endif
SO_EXT := .dll
LIB_EXT := .lib

CFLAGS += -DWINNT -DWIN32
ifdef	OS_IS_CYGWIN

ifneq ($(CYGWIN_ARCH), WOW64)
CXXFLAGS += -DWINNT -DWIN32 -EHsc -W3 -nologo -GF -Gy -GT -G5
else
CXXFLAGS += -DWINNT -D_AMD64_ -EHsc -W3 -nologo -D_CRT_SECURE_NO_WARNINGS
endif

else
CXXFLAGS += -DWINNT -DWIN32
endif

LD_FILTER_SYMS_FLAG = -def:$(filter %$(MAPFILE_EXT),$^)
ifdef	OS_IS_CYGWIN
ifneq ($(CYGWIN_ARCH), WOW64)
LD_MAKE_SHARED_LIB_FLAG := -DLL
else
LD_MAKE_SHARED_LIB_FLAG := -DLL /MACHINE:AMD64 
endif
else
LD_MAKE_SHARED_LIB_FLAG := -dll
endif

# XXX - The following needs be set to the appropriate value.
LD_VERSION_LIB_FLAG :=
PLATFORM_SHARED_OBJS=$(filter %.res, $^)

#
# Give DEBUG_FLAGS a default setting based on the build type
#
#CYGWIN change
ifeq ($(BUILD_DEBUG), full)
ifdef	OS_IS_CYGWIN
    DEBUG_FLAGS := -Zi -DDEBUG -Od -MDd
    LINK_DEBUG_FLAGS := -DEBUG
else
    DEBUG_FLAGS := -g -DDEBUG
endif
endif

ifeq ($(BUILD_DEBUG), optimize)
ifdef	OS_IS_CYGWIN
ifeq ($(CYGWIN_ARCH), WOW64)
    DEBUG_FLAGS := -Zi -O2 -DNDEBUG
else
    DEBUG_FLAGS := -Zi -O2 -DNDEBUG -MD
endif
    LINK_DEBUG_FLAGS := -DEBUG -opt:ref
else
    DEBUG_FLAGS := -O -DNDEBUG
endif
endif

ifndef DEBUG_FLAGS
ifdef	OS_IS_CYGWIN
    DEBUG_FLAGS := -Zi -DDEBUG -Od -MDd
    LINK_DEBUG_FLAGS := -DEBUG
else
    DEBUG_FLAGS := -g -DDEBUG
endif
endif

SHELL_EXEC_EXTENSION := .bat

#
# Give MSDEV_BUILD_TYPE a default setting based on the build type
#
ifeq ($(BUILD_DEBUG), full)
    MSDEV_BUILD_TYPE := Debug
endif
ifeq ($(BUILD_DEBUG), optimize)
    MSDEV_BUILD_TYPE := Release
endif
ifndef MSDEV_BUILD_TYPE
    MSDEV_BUILD_TYPE := Debug
endif

MSDEV_BUILD_CONFIG := "All - $(MSDEV_BUILD_TYPE)"

#CYGWIN change

ifdef	OS_IS_CYGWIN
MAKE_STATIC_LIB = LIB -nodefaultlib -nologo $(filter %.o, $^) -OUT:$@
else
MAKE_STATIC_LIB = LIB -nodefaultlib -nologo $(filter %.o, $^) -OUT:$@
endif

ifdef	OS_IS_CYGWIN
MAKE_SHARED_LIB = $(LINK) $(LD_MAKE_SHARED_LIB_FLAG) -nologo -SUBSYSTEM:WINDOWS  $(LINK_DEBUG_FLAGS) \
	$(LD_ORIGIN_FLAG) $(LDFLAGS) \
	$(LD_VERSION_LIB_FLAG) $(LD_FILTER_SYMS_FLAG) \
	$(filter %.o, $^) $(PLATFORM_SHARED_OBJS) $(LDLIBS) -OUT:$@
endif

INCLUDE_MANIFEST = ${MT} -manifest $(MSCRT_DIR)/Microsoft.VC90.CRT.manifest -outputresource:$@\;2
INCLUDE_MANIFEST_LOCAL = ${MT} -manifest $@.manifest -outputresource:$@\;2

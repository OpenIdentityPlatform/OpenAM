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
# $Id: defines_SunOS.mk,v 1.4 2008/06/25 05:54:25 qcheng Exp $
#
# Portions Copyrighted 2012 ForgeRock AS

#
# This makefile defines a number of standard OS-dependent symbols
# used in by the makefiles that build the Agent Pack.
#

PATH_SEP := :

ECHO := echo
NM := nm -p

#
# C/C++ Compiler related symbols
#
CC := cc
CXX := CC

CFLAGS += -DSOLARIS -mt
CXXFLAGS += -DSOLARIS -mt -library=Cstd -library=Crun

ifeq ($(BUILD_TYPE), 64)
CFLAGS += -m64
CXXFLAGS += -m64 -DSOLARIS_64 
LD_ORIGIN_FLAG := '-R$$ORIGIN' '-R$$ORIGIN/../lib' 
else
LD_ORIGIN_FLAG := '-R$$ORIGIN' '-R$$ORIGIN/../lib' 
endif

LDFLAGS += -mt -norunpath -library=Cstd -library=Crun
	
LD_COMMON_ORIGIN_FLAG := 
LD_FILTER_SYMS_FLAG = -M$(filter %.mapfile, $^)
LD_MAKE_SHARED_LIB_FLAG := -G -i -z nodefaultlib -z ignore -norunpath -R/lib -R/usr/lib
ifeq ($(BUILD_TYPE), 64)
LD_MAKE_SHARED_LIB_FLAG += -m64 -R/lib/64 -R/usr/lib/64
endif
LD_SHARED_FLAG := -Bdynamic
LD_STATIC_FLAG :=
LD_VERSION_LIB_FLAG = -h$@

#
# Give DEBUG_FLAGS a default setting based on the build type
#
ifeq ($(BUILD_DEBUG), full)
  DEBUG_FLAGS := -g -DDEBUG
endif
ifeq ($(BUILD_DEBUG), optimize)
  DEBUG_FLAGS := -xO3 -DNDEBUG
endif
ifndef DEBUG_FLAGS
  DEBUG_FLAGS := -g -xO1 -DDEBUG
endif

SHELL_EXEC_EXTENSION :=

#
# processor name used for ARCH variable in pkginfo 
#
PKG_ARCH := $(shell uname -p)

LN_s := ln -s
TAR := /bin/tar

ifeq ($(MC_ARCH), i86pc)
PIC_FLAG := -KPIC
include $(USERX_ROOT)/arch/defines_SunOS_$(MC_ARCH).mk
else
PIC_FLAG := -xcode=pic32
include $(USERX_ROOT)/arch/defines_SunOS_sparc.mk
endif

PRODUCT_DIR := SUNWam/agents
#
# How to make a System V package
#
# The following is intentionally defined with '=', instead of ':=',
# so that the target dependent values are used.
#
MAKE_STATIC_LIB = $(CXX) $(LD_STATIC_FLAGS) -o $@ $(filter %.o, $^)

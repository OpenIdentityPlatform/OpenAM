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
# $Id: defines_Linux.mk,v 1.5 2008/06/25 05:54:25 qcheng Exp $
# 
#

# "Portions Copyrighted [2010] [ForgeRock AS]"

#
# This makefile defines a number of standard OS-dependent symbols
# used in by the makefiles that build the Agent Pack.
#

PATH_SEP := :

ECHO := echo -e
NM := nm
RMDIR := rmdir -p --ignore-fail-on-non-empty

#
# C/C++ Compiler related symbols
#
COMPILERS_DIR :=
GCC_WARNING_FLAGS := -Wall -Wshadow 
CC := gcc
CXX := g++
GCC_33 := $(shell $(CC) -v 2>&1 | grep version | /bin/awk '{print $$3}')

CFLAGS += -DLINUX -pthread $(GCC_WARNING_FLAGS) -fexceptions
CXXFLAGS += -DLINUX -pthread $(GCC_WARNING_FLAGS) -Woverloaded-virtual -fexceptions
CXX_STD_LIBS := -lstdc++
LDFLAGS += -pthread -lrt
LD_ORIGIN_FLAG := -Xlinker '-R$$ORIGIN'
LD_COMMON_ORIGIN_FLAG := -Xlinker '-R$$ORIGIN/../../lib'
# NOTE: '-z defs' should probably be added to the following definition.
LD_FILTER_SYMS_FLAG = -Xlinker --version-script -Xlinker $(filter %.mapfile, $^)
LD_MAKE_SHARED_LIB_FLAG := -fPIC -shared -rdynamic
LD_SHARED_FLAG := -Wl,-Bdynamic
LD_STATIC_FLAG := -Wl,-Bstatic
LD_VERSION_LIB_FLAG = -Xlinker -h$@
PIC_FLAG := -fPIC

INSTALL_DIR := opt/agents
RPM_DIR :=  $(DEST_PACKAGE_SCRATCH_DIR)/RPMS/$(MC_ARCH)
BUILDROOT := /tmp/$(USER)/agent-buildroot
BUILDROOT_LIB_DIR := $(BUILDROOT)/$(INSTALL_DIR)/lib
BUILDROOT_CONF_DIR := $(BUILDROOT)/$(INSTALL_DIR)/config
BUILDROOT_BIN_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/bin
BUILDROOT_INC_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/include
BUILDROOT_SAMPLES_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/samples
BUILDROOT_RPM_DIR:= $(BUILDROOT)/RPMS/$(MC_ARCH)

#
# Give DEBUG_FLAGS a default setting based on the build type
#
ifeq ($(BUILD_DEBUG), full)
  DEBUG_FLAGS := -g3 -DDEBUG
endif
ifeq ($(BUILD_DEBUG), optimize)
  DEBUG_FLAGS := -O2 -DNDEBUG
endif
ifndef DEBUG_FLAGS
  DEBUG_FLAGS := -g -O1 -DDEBUG
endif

SHELL_EXEC_EXTENSION :=

LN_s := ln -s

ifeq ($(BUILD_TYPE), 64)
CFLAGS += -fPIC
CXXFLAGS += -DLINUX_64
endif

ifeq ($(BUILD_TYPE), 32)
CFLAGS += -fPIC -m32
CXXFLAGS += -DLINUX -m32
endif


#
# the following is the name of the tar ball for dsame drop,.
#
DSAME_DROP_FILE_NAME := common_3_0_$(OS_ARCH)_$(MC_ARCH)

MAKE_STATIC_LIB = $(AR) $(ARFLAGS) $@ $(filter %.o, $^)

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
# $Id: defines_HP-UX.mk,v 1.1 2009/07/22 22:59:06 subbae Exp $
#
#

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
CC := /opt/aCC/bin/aCC
CXX := /opt/aCC/bin/aCC
AR := /usr/bin/ar

#CFLAGS += -DHPUX +DA1.1 +DA2.0 -Ae -c +z -fast
CFLAGS += -DHPUX +DA1.1 +DA2.0 -Ae -c +Z -fast
CXXFLAGS += -DHPUX -c -mt -AA -fast +DA1.1 +DA2.0 +Z +W849
CXX_STD_LIBS := -lstd_v2 -lCsup_v2
LDFLAGS += -mt +DA1.1 +DA2.0
LD_ORIGIN_FLAG := -Wl,'+b/usr/lib/mps'
LD_COMMON_ORIGIN_FLAG := -Wl,'+b/opt/SUNWam/lib'
# NOTE: '-z defs' should probably be added to the following definition.
LD_FILTER_SYMS_FLAG = 
LD_MAKE_SHARED_LIB_FLAG := -b
LD_SHARED_FLAG := 
LD_STATIC_FLAG := -Bstatic
LD_VERSION_LIB_FLAG = 
PIC_FLAG := 
ARFLAGS = -ur

override RMDIR := rmdir -p

#
# Give DEBUG_FLAGS a default setting based on the build type
#
ifeq ($(BUILD_DEBUG), full)
  DEBUG_FLAGS := -g -DDEBUG
endif
ifeq ($(BUILD_DEBUG), optimize)
  DEBUG_FLAGS := -DNDEBUG
endif
ifndef DEBUG_FLAGS
  DEBUG_FLAGS := -g -DDEBUG
endif
SO_EXT = .sl
SHELL_EXEC_EXTENSION :=

#
# NOTE: The JAVA* variables need to be set with '=', rather than ':=',
# because JAVA_HOME variable is an alias for the JDK_DIR variable, which
# is set in components.mk, which includes this file before defining that
# variable.
#
JAVA    = $(JAVA_HOME)/bin/java
JAVAC   = $(JAVA_HOME)/bin/javac 
JAVAH   = $(JAVA_HOME)/bin/javah 
JAVADOC = $(JAVA_HOME)/bin/javadoc
JAR     = $(JAVA_HOME)/bin/jar

LN_s := ln -s
TAR := /tools/ns/bin/tar

#
# How to make a System V package
#
# The following is intentionally defined with '=', instead of ':=',
# so that the target dependent values are used.
#
MAKE_PACKAGE = pkgmk -o -v $(shell grep VERSION $(filter pkginfo%, $^) | cut -f2-3 -d=)$(PACKAGE_REVISION) -d $(DEST_PACKAGE_SCRATCH_DIR) -f $(filter prototype%, $^) PRODUCT_DIR=SUNWam/agents DEF_OWNER=root DEF_GROUP=root def_dir_perm=0755 def_dbg_dir_perm=0777 def_exe_perm=0555 def_write_perm=0600 def_file_perm=0444 def_sgrp_perm=2555 DEF_UMASK=022 ws_dir=$(USERX_ROOT) dest_inc_dir=$(DEST_INC_DIR) dest_lib_dir=$(DEST_LIB_DIR) 

MAKE_STATIC_LIB = $(AR) $(ARFLAGS) $@ $(filter %.o, $^)

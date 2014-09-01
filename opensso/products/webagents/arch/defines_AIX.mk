#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
# $Id: defines_AIX.mk,v 1.1 2009/08/27 21:41:31 subbae Exp $
#
#

PATH_SEP := :

ECHO := echo -e
NM := nm
RMDIR := rmdir -p 
AR := /bin/ar
ARFLAGS = -ru

LD := /bin/ld

#
# C/C++ Compiler related symbols
#
COMPILERS_DIR :=
CC := xlC_r
CXX := xlC_r

CFLAGS += -DAIX 
CXXFLAGS += -DAIX 
LD_MAKE_SHARED_LIB_FLAG := -G -qmkshrobj
LD_SHARED_FLAG := -bdynamic
LD_STATIC_FLAG := -bstatic
PIC_FLAG := -qpic


INSTALL_DIR := agents
BUILDROOT := ../../built
BUILDROOT_LIB_DIR := $(BUILDROOT)/$(INSTALL_DIR)/lib
BUILDROOT_CONF_DIR := $(BUILDROOT)/$(INSTALL_DIR)/config
BUILDROOT_BIN_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/bin
BUILDROOT_INC_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/include
BUILDROOT_SAMPLES_DIR:= $(BUILDROOT)/$(INSTALL_DIR)/samples

#
# Give DEBUG_FLAGS a default setting based on the build type
#
ifeq ($(BUILD_DEBUG), full)
  DEBUG_FLAGS := -g -DDEBUG
endif
ifeq ($(BUILD_DEBUG), optimize)
  #DEBUG_FLAGS := -O3 -qmaxmem=-1 -qstrict -Q
  DEBUG_FLAGS := -O -qmaxmem=-1
endif
ifndef DEBUG_FLAGS
  DEBUG_FLAGS := -g -DDEBUG
endif

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

#
# the following is the name of the tar ball for dsame drop,.
#
MAKE_STATIC_LIB = $(AR) $(ARFLAGS) $@ $(filter %.o, $^)

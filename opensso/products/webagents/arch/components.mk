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
# $Id: components.mk,v 1.16 2009/12/01 21:52:55 subbae Exp $
# 
#
# Portions Copyrighted 2012 ForgeRock Inc
#
# This makefile defines the location of the all of the common components
# used to build the Agent Pack.
#
# Make sure USERX_ROOT is defined before including this file.


ifndef	COMPONENTS_INCLUDED
COMPONENTS_INCLUDED := true

#
# The following is here to insure that 'all' is always the default target.
#
all:

include $(USERX_ROOT)/arch/defines.mk

##########################################################
# Assemble all the bits into the standard set of symbols
##########################################################

##########################################
# Apache 2.0 defines
##########################################
APACHE_DIR = $(EXTERNAL_DIR)/apache
APACHE_INC_DIR = $(APACHE_DIR)/include
APACHE_LIB_DIR = $(APACHE_DIR)/lib
APACHE_MOD_DIR = $(APACHE_DIR)/modules

##########################################
# SJSWS  defines
##########################################
SJSWS_DIR = $(EXTERNAL_DIR)/sjsws
SJSWS_INC_DIR = $(SJSWS_DIR)/include
SJSWS_LIB_DIR = $(SJSWS_DIR)/lib

##########################################
# PROXY40  defines
##########################################
PROXY40_DIR = $(EXTERNAL_DIR)/proxy40
PROXY40_INC_DIR = $(PROXY40_DIR)/include
PROXY40_LIB_DIR = $(PROXY40_DIR)/lib

##########################################
# Apache 2.2 defines
##########################################
APACHE22_DIR = $(EXTERNAL_DIR)/apache22
APACHE22_INC_DIR = $(APACHE22_DIR)/include
APACHE22_LIB_DIR = $(APACHE22_DIR)/lib

##########################################
# Apache 2.4 defines
##########################################
APACHE24_DIR = $(EXTERNAL_DIR)/apache24
APACHE24_INC_DIR = $(APACHE24_DIR)/include
APACHE24_LIB_DIR = $(APACHE24_DIR)/lib

######################################################
# IIS 6.0 Header files
######################################################
IIS6_DIR := $(EXTERNAL_DIR)/iis6
IIS6_INC_DIR := $(IIS6_DIR)/include

ifeq ($(OS_ARCH), WINNT)
ifeq ($(CYGWIN_ARCH), WOW64)
BUILD_TYPE := 64
endif
endif

######################################################
# IIS 7.0 Header files
######################################################
IIS7_DIR := $(EXTERNAL_DIR)/iis7
IIS7_INC_DIR := $(IIS7_DIR)/include

##########################################
# IBM Lotus DOMINO 8.5  defines
##########################################
DOMINO_DIR = $(EXTERNAL_DIR)/domino
DOMINO_INC_DIR = $(DOMINO_DIR)/include
DOMINO_LIB_DIR = $(DOMINO_DIR)/lib

##########################################
# LIBXML defines
##########################################
LIBXML_DIR := $(EXTERNAL_DIR)/libxml2
ifeq ($(BUILD_TYPE), 64)
LIBXML_DIR := $(EXTERNAL_DIR)/libxml2_64
endif
ifeq ($(OS_ARCH), WINNT)
ifeq ($(CYGWIN_ARCH), WOW64)
LIBXML_DIR := $(EXTERNAL_DIR)/libxml2_64
endif
endif
LIBXML_INC_DIR := $(LIBXML_DIR)/include/libxml2
LIBXML_LIB_DIR := $(LIBXML_DIR)/lib

ifndef LIBXML_LIBS
ifeq ($(OS_ARCH), WINNT)
ifdef OS_IS_CYGWIN
LIBXML_LIBS := libxml2.lib
else
LIBXML_LIBS := -llibxml2
endif
else
LIBXML_LIBS := -lxml2
endif
endif


##########################################
# NSPR defines
##########################################

NSPR_DIR := $(EXTERNAL_DIR)/nspr
ifeq ($(BUILD_TYPE), 64)
NSPR_DIR := $(EXTERNAL_DIR)/nspr_64
endif
ifeq ($(OS_ARCH), WINNT)
ifeq ($(CYGWIN_ARCH), WOW64)
NSPR_DIR := $(EXTERNAL_DIR)/nspr_64
endif
endif
NSPR_INC_DIR := $(NSPR_DIR)/include
NSPR_LIB_DIR := $(NSPR_DIR)/lib

ifndef	NSPR_LIBS
ifeq ($(OS_ARCH), WINNT)
ifdef OS_IS_CYGWIN
NSPR_LIBS := libplc4.lib libplds4.lib libnspr4.lib
else
NSPR_LIBS := -llibplc4 -llibplds4 -llibnspr4
endif
else
NSPR_LIBS := -lplc4 -lplds4 -lnspr4
endif
endif

##########################################
# NSS defines
##########################################

NSS_DIR := $(EXTERNAL_DIR)/nss
ifeq ($(BUILD_TYPE), 64)
NSS_DIR := $(EXTERNAL_DIR)/nss_64
endif
ifeq ($(OS_ARCH), WINNT)
ifeq ($(CYGWIN_ARCH), WOW64)
NSS_DIR := $(EXTERNAL_DIR)/nss_64
endif
endif
NSS_BIN_DIR := $(NSS_DIR)/bin
NSS_INC_DIR := $(NSS_DIR)/include
NSS_LIB_DIR := $(NSS_DIR)/lib

ifeq ($(OS_ARCH), WINNT)
ifdef OS_IS_CYGWIN
NSS_DYNAMIC_LIBS := ssl3.lib nss3.lib 
else
NSS_DYNAMIC_LIBS := -lssl3 -lnss3 
endif
else
NSS_DYNAMIC_LIBS := -lssl3 -lnss3 -lnssutil3
endif
NSS_LIBS := $(NSS_DYNAMIC_LIBS)

endif

##########################################
# MSCRT defines
##########################################
ifeq ($(OS_ARCH), WINNT)
ifeq ($(BUILD_TYPE), 64)
MSCRT_DIR := $(EXTERNAL_DIR)/mscrt_64/lib
else
MSCRT_DIR := $(EXTERNAL_DIR)/mscrt/lib
endif
endif
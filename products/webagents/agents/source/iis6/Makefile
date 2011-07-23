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
# $Id: Makefile,v 1.2.2.2 2008/03/21 23:34:38 robertis Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
#

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH),WINNT)

IIS6 := iis6
IIS6_LIB_NAME := am$(IIS6)
IIS6_LIB := $(IIS6_LIB_NAME)$(SO_EXT)

IIS6AUTH := iis6auth
IIS6AUTH_LIB_NAME := am$(IIS6AUTH)
IIS6AUTH_LIB := $(IIS6AUTH_LIB_NAME)$(SO_EXT)

OWAFILTER := owafilter
OWAFILTER_LIB_NAME := am$(OWAFILTER)
ifeq ($(CYGWIN_ARCH), WOW64)
OWAFILTER_LIB_NAME := $(OWAFILTER_LIB_NAME)64
else
OWAFILTER_LIB_NAME := $(OWAFILTER_LIB_NAME)32
endif
OWAFILTER_LIB := $(OWAFILTER_LIB_NAME)$(SO_EXT)

SHAREPOINTFILTER := sharepointfilter
SHAREPOINTFILTER_LIB_NAME := am$(SHAREPOINTFILTER)
ifeq ($(CYGWIN_ARCH), WOW64)
SHAREPOINTFILTER_LIB_NAME := $(SHAREPOINTFILTER_LIB_NAME)64
else
SHAREPOINTFILTER_LIB_NAME := $(SHAREPOINTFILTER_LIB_NAME)32
endif
SHAREPOINTFILTER_LIB := $(SHAREPOINTFILTER_LIB_NAME)$(SO_EXT)

ifneq ($(CYGWIN_ARCH), WOW64)
EXPORTED_LIBS += $(IIS6_LIB) $(IIS6AUTH_LIB) $(OWAFILTER_LIB)  $(SHAREPOINTFILTER_LIB)
else
EXPORTED_LIBS += $(IIS6_LIB) $(OWAFILTER_LIB) $(SHAREPOINTFILTER_LIB)
endif
EXPORTED_OBJS := IisAgent6.o  Iis6auth.o Iis6filter.o

INCLUDE_FLAGS += -I$(DEST_INC_DIR) -I$(NSS_INC_DIR) -I$(NSPR_INC_DIR) -I$(IIS6_INC_DIR)/include
CFLAGS += -DXP_WIN32 -DAM_STATIC_LIB
ifdef   OS_IS_CYGWIN
LDFLAGS += ../../../am/source/am_web.res -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR)
LDLIBS += amsdk.lib advapi32.lib libnspr4.lib libplc4.lib libxml2.lib user32.lib nss3.lib ssl3.lib
else
LDFLAGS += ../../../am/source/am_web.res -L$(DEST_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR)
LDLIBS += -lamsdk -ladvapi32 -llibnspr4 -llibplc4 -llibxml2 -luser32 -lnss3 -lssl3
endif

all: export_libs

$(IIS6_LIB): IisAgent6.o IisAgent6.def
	$(MAKE_SHARED_LIB)

ifneq ($(CYGWIN_ARCH), WOW64)
$(IIS6AUTH_LIB): Iis6auth.o Iis6auth.def
	$(MAKE_SHARED_LIB)

$(OWAFILTER_LIB): Iis6filter.o OwaFilter.def
	$(MAKE_SHARED_LIB)

$(SHAREPOINTFILTER_LIB): Iis6filter.o SharePointFilter.def
	$(MAKE_SHARED_LIB)

else
$(OWAFILTER_LIB): Iis6filter.o OwaFilter.def
	$(MAKE_SHARED_LIB)

$(SHAREPOINTFILTER_LIB): Iis6filter.o SharePointFilter.def
	$(MAKE_SHARED_LIB)
endif

all: $(DEST_PACKAGE_SCRATCH_DIR) $(IIS6_LIB)

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS:%$(SO_EXT)=%*) $(EXPORTED_OBJS)
	$(RM) *.pdb

else
all clean:
	@echo "IIS Agent not supported on $(OS_ARCH)."
endif

include $(USERX_ROOT)/arch/rules.mk


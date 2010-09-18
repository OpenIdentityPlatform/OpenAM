#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
# $Id
#
# Makefile for building IIS7 policy agent module. 
#
#


USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH),WINNT)

IIS7 := iis7
IIS7_LIB_NAME := am$(IIS7)module
IIS7_LIB := $(IIS7_LIB_NAME)$(SO_EXT)


ifneq ($(CYGWIN_ARCH), WOW64)
else
EXPORTED_LIBS += $(IIS7_LIB) 
endif
EXPORTED_OBJS := agent_main.o agent_module.o precomp.o Iis7Agent.o 

DEBUG_FLAGS := -Zi -O2 -DNDEBUG 
INCLUDE_FLAGS += -I$(DEST_INC_DIR) -I$(NSS_INC_DIR) -I$(NSPR_INC_DIR) -I$(IIS7_INC_DIR)
CFLAGS += -DXP_WIN32 -DAM_STATIC_LIB
ifdef   OS_IS_CYGWIN
LDFLAGS += ../../../am/source/am_web.res -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR)
LDLIBS += amsdk.lib advapi32.lib libnspr4.lib libplc4.lib libxml2.lib user32.lib nss3.lib ssl3.lib
else
LDFLAGS += ../../../am/source/am_web.res -L$(DEST_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR)
LDLIBS += -lamsdk -ladvapi32 -llibnspr4 -llibplc4 -llibxml2 -luser32 -lnss3 -lssl3
endif

all: export_libs

$(IIS7_LIB): $(EXPORTED_OBJS) Iis7Agent.def
	$(MAKE_SHARED_LIB)


all: $(DEST_PACKAGE_SCRATCH_DIR) $(IIS7_LIB)

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS:%$(SO_EXT)=%*) $(EXPORTED_OBJS)
	$(RM) $(IIS7_LIB_NAME).pdb $(IIS7_LIB_NAME).lib $(IIS7_LIB_NAME).exp $(IIS7_LIB)

else
all clean:
	@echo "IIS7 Agent not supported on $(OS_ARCH)."
endif

include $(USERX_ROOT)/arch/rules.mk


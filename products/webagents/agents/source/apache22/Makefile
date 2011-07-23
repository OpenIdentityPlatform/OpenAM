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
# $Id: Makefile,v 1.2 2007/08/08 00:39:46 subbae Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
#
# Makefile for building the Apache Policy Agent.
#

# "Portions Copyrighted [2010] [ForgeRock AS]"

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH), WINNT)
APACHE22_LIB_NAME := libamapc22
APACHE22_LIB := $(APACHE22_LIB_NAME)$(SO_EXT)
APACHE22_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common
APACHE22_CFLAGS += -DAPACHE2 -DEAPI -DUSE_EXPAT -DXP_WIN32 -DAM_STATIC_LIB $(PIC_FLAG) -DCORE_PRIVATE $(APACHE22_INCLUDE_FLAGS) -I$(APACHE22_INC_DIR)
ifdef   OS_IS_CYGWIN
LDFLAGS := ../../../am/source/am_web.res -LIBPATH:$(APACHE22_LIB_DIR) -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR)
LDLIBS := libapr-1.lib libapriconv-1.lib libhttpd.lib amsdk.lib libnspr4.lib nss3.lib ssl3.lib libplc4.lib libxml2.lib user32.lib
else
LDFLAGS := ../../../am/source/am_web.res -L$(APACHE22_LIB_DIR) -L$(DEST_LIB_DIR) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) 
LDLIBS := -llibapr-1 -llibapriconv-1 -llibhttpd -lamsdk_static -llibnspr4 -lnss3 -lssl3 -llibplc4 -llibxml2 -luser32
endif

SRCS := apache_agent.c
ifdef   OS_IS_CYGWIN
$(APACHE22_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE22_CFLAGS) -Fo$@ $<
else
$(APACHE22_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE22_CFLAGS) $< $(OUTPUT_OPTION)
endif

APACHE22_OBJS := $(patsubst %.c, $(APACHE22_LIB_NAME).o, $(SRCS))
EXPORTED_LIBS := $(APACHE22_LIB)
OBJS := $(APACHE22_OBJS)
$(APACHE22_LIB): $(APACHE22_OBJS) libamapc.def
	$(MAKE_SHARED_LIB)
	$(CP) $(APACHE22_LIB_NAME).dll $(DEST_LIB_DIR)

all: $(APACHE22_LIB)

clean: clean_libs clean_objs
	$(RM) $(APACHE22_LIB_NAME).o $(APACHE22_LIB_NAME).dll $(APACHE22_LIB_NAME).lib $(APACHE22_LIB_NAME).exp
	$(RM) *.pdb

else
APACHE22_LIB_NAME := libamapc22

APACHE22_LIB := $(APACHE22_LIB_NAME)$(SO_EXT)
	
APACHE22_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common

ifeq ($(OS_ARCH), Linux)
   ifeq ($(OS_ARCH_VER), 2.2.20-compact)
     APACHE22_CFLAGS += -D_FILE_OFFSET_BITS=64
   endif
endif

#ifeq ($(OS_ARCH), Linux)
#     ifeq ($(MC_ARCH), x86_64)
#          APACHE22_CFLAGS += -DX86_64
#      endif
#endif

APACHE22_CFLAGS += -DAPACHE2 -DEAPI -DMOD_PERL -DUSE_EXPAT -DXP_UNIX $(PIC_FLAG) -DCORE_PRIVATE $(APACHE22_INCLUDE_FLAGS) -I$(APACHE22_INC_DIR)

ifeq ($(OS_ARCH), Linux)
APACHE22_CFLAGS += -D_LARGEFILE64_SOURCE
endif

LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG)

SYSTEM_LIBS := -lpthread -ldl -lc -lm
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG)
LDFLAGS += -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR)
$(STD_LIB)_LDLIBS := $(NSS_LIBS) $(NSPR_LIBS)
LDLIBS += -L ../../../am/source
LDLIBS += $(LDFLAGS) $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) $(SYSTEM_LIBS)

ifeq ($(OS_ARCH), Linux)
	ifeq ($(BUILD_TYPE), 32)
	SYSTEM_LIBS += -m32
	endif
endif

ifeq ($(OS_ARCH), SunOS)
LDFLAGS += -L$(DEST_LIB_DIR)
LDLIBS += $(LD_SHARED_FLAG) $(CXX_STD_LIBS) -lc -lm
endif

LDLIBS += -Bstatic -lamsdk

$(APACHE22_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE22_CFLAGS) $< $(OUTPUT_OPTION)

SRCS := apache_agent.c
APACHE22_OBJS := $(patsubst %.c, $(APACHE22_LIB_NAME).o, $(SRCS))
OBJS := $(APACHE22_OBJS)

EXPORTED_LIBS := $(APACHE22_LIB)

$(APACHE22_LIB): $(APACHE22_OBJS) libamapc.mapfile
	$(MAKE_SHARED_LIB)
 
all: export_libs

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS) pkginfo.apache
	$(RM) -r $(DEST_PACKAGE_SCRATCH_DIR)/RPMS
	$(RM) $(APACHE22_LIB_NAME).o
endif

include $(USERX_ROOT)/arch/rules.mk

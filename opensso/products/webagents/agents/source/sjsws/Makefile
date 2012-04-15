#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
# $Id: Makefile,v 1.5 2008/06/25 05:54:14 qcheng Exp $
#
#

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH), WINNT)
ES6_LIB_NAME := ames6
else
ES6_LIB_NAME := libames6
endif

ES6_LIB := $(ES6_LIB_NAME)$(SO_EXT)
LIB_MAPFILE := iws_agent$(MAPFILE_EXT)
CFLAGS += -DAM_STATIC_LIB
ifeq ($(OS_ARCH), WINNT)
CFLAGS += -DXP_WIN32
ES6_CFLAGS += -I$(SJSWS_INC_DIR)
ifdef   OS_IS_CYGWIN
LDFLAGS += -LIBPATH:$(SJSWS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR)
LDLIBS += user32.lib amsdk.lib ns-httpd40.lib libnspr4.lib libplds4.lib libplc4.lib nss3.lib ssl3.lib libxml2.lib
else
LDFLAGS += -L$(SJSWS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(NSS_LIB_DIR) -L$(LIBXML_LIB_DIR)
LDLIBS += -luser32 -lamsdk_static -lns-httpd40 -llibnspr4 -llibplds4 -llibplc4 -lnss3 -lssl3 -llibxml2
endif
endif

ifeq ($(OS_ARCH), SunOS)
CFLAGS += -DXP_UNIX $(PIC_FLAG)
ES6_CFLAGS += -I$(SJSWS_INC_DIR)
LDLIBS += $(LD_STATIC_FLAG) $(CXX_STD_LIBS) $(LD_SHARED_FLAG) -lc -lm
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lc -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2
$(ES6_LIB)_LDLIBS := -Bstatic -lamsdk
endif

ifeq ($(OS_ARCH), HP-UX)
override IWS6.0_VERSION := 20030410E
CFLAGS += -DXP_UNIX -Ae
ES6_CFLAGS += -I$(SJSWS_INC_DIR)
LDLIBS +=-Bdynamic -L$(NSS_LIB_DIR) \
	-L./ -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) \
        -lpthread -lm -lstd -lc -lcl -lnss3 -lssl3 -lnspr4 -lplc4

$(ES6_LIB)_LDLIBS :=
endif

ifeq ($(OS_ARCH), Linux)
CFLAGS += -DXP_UNIX
ES6_CFLAGS += -I$(SJSWS_INC_DIR)
#LDLIBS += $(LD_STATIC_FLAG) $(CXX_STD_LIBS) -lamsdk $(LD_SHARED_FLAG) -lc -lm
LDLIBS += $(LD_STATIC_FLAG) -lamsdk $(LD_SHARED_FLAG) -L/usr/lib/i386-redhat-linux3E/lib/ -lc -lm
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2
	ifeq ($(BUILD_TYPE), 32)
	LDLIBS += -m32
	endif
endif

ES6_CFLAGS += -DWS_VERSION=60000

EXPORTED_LIBS := $(ES6_LIB)

%_$(ES6_LIB_NAME).o: %.c
	$(COMPILE.c) $(ES6_CFLAGS) $< $(OUTPUT_OPTION)

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
%_$(ES6_LIB_NAME).o: %.c
	$(COMPILE.c) $(ES6_CFLAGS) -Fo$@ $< 
endif
endif

SRCS := iws_agent.c
ES6_OBJS := $(patsubst %.c, %_$(ES6_LIB_NAME).o, $(SRCS))
OBJS := $(ES6_OBJS)

INCLUDE_FLAGS += -I$(DEST_INC_DIR)

LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -LIBPATH:$(DEST_LIB_DIR)
endif
endif

all: export_libs

ifeq ($(OS_ARCH), SunOS)
all: $(ES6_LIB) 
endif

ifeq ($(OS_ARCH), WINNT)
all: $(ES6_LIB)
endif

ifeq ($(OS_ARCH), HP-UX)
all: $(ES6_LIB)
endif

$(ES6_LIB): $(ES6_OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)
ifeq ($(OS_ARCH), WINNT)
	$(INCLUDE_MANIFEST)
endif

ifeq ($(OS_ARCH), Linux)
#all: $(EXPORTED_LIBS)
all: $(ES6_LIB)
endif

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS) 

ifeq ($(OS_ARCH),WINNT)
	$(RM) $(EXPORTED_LIBS:.dll=.exp) $(EXPORTED_LIBS:.dll=.lib)
	$(RM) *.pdb
	$(RM) *.manifest
endif

include $(USERX_ROOT)/arch/rules.mk

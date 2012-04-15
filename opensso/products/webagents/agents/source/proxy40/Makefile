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
# $Id: Makefile,v 1.2 2009/02/27 19:26:49 robertis Exp $
#
#

# Portions Copyrighted [2010] [ForgeRock AS]

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

LIB_NAME := libampxy4
ifeq ($(OS_ARCH), WINNT)
LIB_NAME := ampxy4
endif

EXPORTED_LIBS := $(LIB_NAME)$(SO_EXT)
LIB_MAPFILE := $(LIB_NAME)$(MAPFILE_EXT)

OBJS := web_agent.o
DEPENDS := $(OBJS:.o=.d)

ifeq ($(OS_ARCH), SunOS)

CFLAGS += -D__svr4__ -DNET_SSL -DSERVER_BUILD -DDEBUG -DUSE_NSPR_MT \
	-DMCC_HTTPD -DXP_UNIX $(PIC_FLAG)
INCLUDE_FLAGS := -I$(DEST_INC_DIR)  -I$(PROXY40_INC_DIR)
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
LDLIBS += -Bstatic -lamsdk
LDLIBS += $(LD_STATIC_FLAG) $(CXX_STD_LIBS) $(LD_SHARED_FLAG) -lc -lm
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lc -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2

all: depends export_libs

$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)

endif

ifeq ($(OS_ARCH), Linux)
CFLAGS += -DXP_UNIX
     ifeq ($(MC_ARCH), x86_64)
          INCLUDE_FLAGS += -fPIC 
     endif
INCLUDE_FLAGS += -I$(DEST_INC_DIR)  -I$(PROXY40_INC_DIR)
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
LDLIBS += $(LD_STATIC_FLAG) -lamsdk $(LD_SHARED_FLAG) -lc -lm
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lc -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2
        ifeq ($(BUILD_TYPE), 32)
        LDLIBS += -m32
        endif

all: depends export_libs 

$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)


endif

ifeq ($(OS_ARCH), WINNT)
PROXY4_LIB := $(LIB_NAME)$(SO_EXT)
LIB_MAPFILE := proxy4agent$(MAPFILE_EXT)
CFLAGS += -DAM_STATIC_LIB
CFLAGS += -DXP_WIN32
INCLUDE_FLAGS := -I$(DEST_INC_DIR)  -I$(PROXY40_INC_DIR)
LDFLAGS += ../../../am/source/am_web.res -L$(PROXY40_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(NSS_LIB_DIR) -L$(LIBXML_LIB_DIR)
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
LDLIBS += -luser32 -lamsdk_static -lns-httpd40 -llibnspr4 -llibplds4 -llibplc4 -lnss3 -lssl3 -llibxml2
 
all: depends export_libs 

$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	echo "dest inc dir :" $(DEST_INC_DIR)
	echo "proxy dir :" $(PROXY40_INC_DIR)
	$(MAKE_SHARED_LIB)
	$(INCLUDE_MANIFEST)
endif

clean: clean_objs clean_libs
	$(RM) $(EXPORTED_LIBS) 
ifeq ($(OS_ARCH),WINNT)
	$(RM) $(EXPORTED_LIBS:.dll=.exp) $(EXPORTED_LIBS:.dll=.lib)
	$(RM) *.pdb
	$(RM) *.ilk
	$(RM) *.manifest
endif

include $(USERX_ROOT)/arch/rules.mk

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
# $Id: Makefile,v 1.6 2009/12/09 23:01:00 krishna_indigo Exp $
#
#
# Makefile for building the Apache Policy Agent.
#

# Portions Copyrighted 2010-2012 ForgeRock Inc

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

APACHE24_LIB_NAME := libamapc24

APACHE24_LIB := $(APACHE24_LIB_NAME)$(SO_EXT)
	
APACHE24_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common

ifeq ($(OS_ARCH), Linux)
   ifeq ($(OS_ARCH_VER), 2.2.20-compact)
     APACHE24_CFLAGS += -D_FILE_OFFSET_BITS=64
   endif
endif


APACHE24_CFLAGS += -DAPACHE2 -DEAPI -DMOD_PERL -DUSE_EXPAT -DXP_UNIX $(PIC_FLAG) -DCORE_PRIVATE $(APACHE24_INCLUDE_FLAGS) -I$(APACHE24_INC_DIR)

ifeq ($(OS_ARCH), Linux)
APACHE24_CFLAGS += -D_LARGEFILE64_SOURCE
endif

LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG)

ifeq ($(OS_ARCH), HP-UX)
SYSTEM_LIBS := -lpthread -ldld -lc -lm
CFLAGS += -DXP_UNIX -Ae
LDLIBS += -L ../../../am/source
LDLIBS += $(LD_STATIC_FLAG) $(CXX_STD_LIBS)
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lc -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2

else

ifeq ($(OS_ARCH), SunOS)
SYSTEM_LIBS := -lc 
else
SYSTEM_LIBS := -lpthread -ldl -lc -lm
endif

LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG)
LDFLAGS += -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR)
$(STD_LIB)_LDLIBS := $(NSS_LIBS) $(NSPR_LIBS)
LDLIBS += -L ../../../am/source
LDLIBS += $(LDFLAGS) $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) $(SYSTEM_LIBS)

endif

ifeq ($(OS_ARCH), Linux)
        ifeq ($(BUILD_TYPE), 32)
        SYSTEM_LIBS += -m32
        endif
endif

### AIX
ifeq ($(OS_ARCH), AIX)

SYSTEM_LIBS := -lpthread -ldl -lc -lm -liconv
LDFLAGS := $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -brtl -blibpath:/usr/lib:/lib:/usr/lib/mps -L$(APACHE24_LIB_DIR)
LDLIBS := -L ../../am $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) -L${DEST_LIB_DIR} -lamsdk -lapr-1 -laprutil-1 -lexpat
LINKOPTS := -bE:libamapache.exp -bnoentry -bquiet
MAKE_SHARED_LIB := $(CC) -o libamapc24.so libamapc24.o $(LINKOPTS) ${LDFLAGS} -bI:$(APACHE24_DIR)/modules/httpd.exp \
	$(LDLIBS) ${SYSTEM_LIBS}
endif
########

ifeq ($(OS_ARCH), SunOS)
LDFLAGS += -L$(DEST_LIB_DIR)
LDLIBS += $(LD_SHARED_FLAG) $(CXX_STD_LIBS) -lamsdk
else
LDLIBS += -Bstatic -lamsdk
endif

$(APACHE24_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE24_CFLAGS) $< $(OUTPUT_OPTION)

SRCS := apache_agent.c
APACHE24_OBJS := $(patsubst %.c, $(APACHE24_LIB_NAME).o, $(SRCS))
OBJS := $(APACHE24_OBJS)

EXPORTED_LIBS := $(APACHE24_LIB)

$(APACHE24_LIB): $(APACHE24_OBJS) libamapc.mapfile
	$(MAKE_SHARED_LIB)
 
all: export_libs

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS) pkginfo.apache
	$(RM) -r $(DEST_PACKAGE_SCRATCH_DIR)/RPMS
	$(RM) $(APACHE24_LIB_NAME).o

include $(USERX_ROOT)/arch/rules.mk

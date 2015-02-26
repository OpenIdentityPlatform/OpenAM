#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
# $Id: Makefile,v 1.8 2009/08/27 21:41:29 subbae Exp $
#
#
# Makefile for building the Apache Policy Agent.
#

# "Portions Copyrighted [2010] [ForgeRock AS]"

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH), WINNT)
APACHE25_LIB_NAME := libamapc2
APACHE25_LIB := $(APACHE25_LIB_NAME)$(SO_EXT)
APACHE25_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common
APACHE25_CFLAGS += -DAPACHE2 -DEAPI -DUSE_EXPAT -DXP_WIN32 -DAM_STATIC_LIB $(PIC_FLAG) -DCORE_PRIVATE $(APACHE25_INCLUDE_FLAGS) -I$(APACHE_INC_DIR)
ifdef   OS_IS_CYGWIN
LDFLAGS := ../../../am/source/am_web.res -LIBPATH:$(APACHE_LIB_DIR) -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR)
LDLIBS := libapr.lib libapriconv.lib libhttpd.lib amsdk.lib libnspr4.lib nss3.lib ssl3.lib libplc4.lib libxml2.lib user32.lib
else
LDFLAGS := ../../../am/source/am_web.res -L$(APACHE_LIB_DIR) -L$(DEST_LIB_DIR) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L../../built/WINNT/lib
LDLIBS := -llibapr -llibapriconv -llibhttpd -lamsdk_static -llibnspr4 -lnss3 -lssl3 -llibplc4 -llibxml2 -luser32
endif

SRCS := apache_agent.c
$(APACHE25_LIB_NAME).o: apache_agent.c
ifdef   OS_IS_CYGWIN
	$(COMPILE.c) $(APACHE25_CFLAGS) -Fo$@ $< 
else
	$(COMPILE.c) $(APACHE25_CFLAGS) $< $(OUTPUT_OPTION)
endif
$(APACHE25_LIB): APACHE_VERSION:=$(APACHE_25_VERSION)
APACHE25_OBJS := $(patsubst %.c, $(APACHE25_LIB_NAME).o, $(SRCS))
EXPORTED_LIBS := $(APACHE25_LIB)
OBJS := $(APACHE25_OBJS)
$(APACHE25_LIB): $(APACHE25_OBJS) libamapc.def
	$(MAKE_SHARED_LIB)
	$(INCLUDE_MANIFEST)
	$(CP) $(APACHE25_LIB_NAME).dll $(DEST_LIB_DIR)

all: $(APACHE25_LIB)

clean: clean_libs clean_objs
	$(RM) $(APACHE25_LIB_NAME).o $(APACHE25_LIB_NAME).dll $(APACHE25_LIB_NAME).lib $(APACHE25_LIB_NAME).exp
	$(RM) *.pdb
	$(RM) *.manifest

else
# else of WINNT condition

APACHE2_LIB_NAME := libamapc2

APACHE2_LIB := $(APACHE2_LIB_NAME)$(SO_EXT)
	
APACHE2_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common

ifeq ($(OS_ARCH), Linux)
   ifeq ($(OS_ARCH_VER), 2.2.20-compact)
     APACHE2_CFLAGS += -D_FILE_OFFSET_BITS=64
   endif
endif

APACHE2_CFLAGS += -DAPACHE2 -DEAPI -DMOD_PERL -DUSE_EXPAT -DXP_UNIX $(PIC_FLAG) -DCORE_PRIVATE $(APACHE2_INCLUDE_FLAGS) -I$(APACHE_DIR)/include

LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG)

ifeq ($(OS_ARCH), HP-UX)
SYSTEM_LIBS := -lpthread -ldld -lc -lm
CFLAGS += -DXP_UNIX -Ae
LDLIBS += -L ../../../am/source
LDLIBS += $(LD_STATIC_FLAG) $(CXX_STD_LIBS)
LDLIBS += -Bdynamic -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -lc -lnspr4 -lplds4 -lplc4 -lnss3 -lssl3 -lxml2

else

SYSTEM_LIBS := -lpthread -ldl -lc -lm
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
LDFLAGS := $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -brtl -blibpath:/usr/lib:/lib:/usr/lib/mps -L$(APACHE_LIB_DIR)
LDLIBS := -L ../../am $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) -L${DEST_LIB_DIR} -lamsdk -lapr-0 -laprutil-0 -lexpat
LINKOPTS := -bE:libamapache.exp -bnoentry -bquiet
MAKE_SHARED_LIB := $(CC) -o libamapc2.so libamapc2.o $(LINKOPTS) ${LDFLAGS} -bI:$(APACHE_DIR)/modules/httpd.exp \
        $(LDLIBS) ${SYSTEM_LIBS}
endif
########

ifeq ($(OS_ARCH), SunOS)
LDFLAGS += -L$(DEST_LIB_DIR)
LDLIBS += $(LD_SHARED_FLAG) $(CXX_STD_LIBS) -lamsdk
else
LDLIBS += -Bstatic -lamsdk
endif

$(APACHE2_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE2_CFLAGS) $< $(OUTPUT_OPTION)

SRCS := apache_agent.c
APACHE2_OBJS := $(patsubst %.c, $(APACHE2_LIB_NAME).o, $(SRCS))
OBJS := $(APACHE2_OBJS)

EXPORTED_LIBS := $(APACHE2_LIB)

$(APACHE2_LIB): $(APACHE2_OBJS) libamapc.mapfile
	$(MAKE_SHARED_LIB)

# Apache with largefile support
ifeq ($(OS_ARCH), SunOS)
ifneq ($(BUILD_TYPE), 64)

APACHE2_LARGEFILE_LIB_NAME := libamapc2.largefile
APACHE2_LARGEFILE_LIB := $(APACHE2_LARGEFILE_LIB_NAME)$(SO_EXT)
APACHE2_LARGEFILE_CFLAGS := $(APACHE2_CFLAGS) -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64
APACHE2_LARGEFILE_OBJS := $(patsubst %.c, $(APACHE2_LARGEFILE_LIB_NAME).o, $(SRCS))

OBJS += $(APACHE2_LARGEFILE_OBJS)
EXPORTED_LIBS += $(APACHE2_LARGEFILE_LIB)

$(APACHE2_LARGEFILE_LIB_NAME).o: apache_agent.c
	$(COMPILE.c) $(APACHE2_LARGEFILE_CFLAGS) $< $(OUTPUT_OPTION)

$(APACHE2_LARGEFILE_LIB): $(APACHE2_LARGEFILE_OBJS) libamapc.mapfile
	$(MAKE_SHARED_LIB)

endif
endif
# end of largefile support
 
all: export_libs

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS) pkginfo.apache
	$(RM) -r $(DEST_PACKAGE_SCRATCH_DIR)/RPMS
	$(RM) $(APACHE2_LIB_NAME).o
## AIX
ifeq ($(OS_ARCH), AIX)
	$(RM) $((APACHE2_LIB_NAME).so
endif
####

endif

include $(USERX_ROOT)/arch/rules.mk

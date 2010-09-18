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
# $id$ 
#
#

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

LIB_NAME := libamdomino
ifeq ($(OS_ARCH), WINNT)
LIB_NAME := amdomino
endif


EXPORTED_LIBS := $(LIB_NAME)$(SO_EXT)
LIB_MAPFILE := $(LIB_NAME)$(MAPFILE_EXT)

OBJS := domino_agent.o
DEPENDS := $(OBJS:.o=.d)

ifeq ($(OS_ARCH), SunOS)

CFLAGS += -xcg92 -xCC -Xt -xchip=ultra -KPIC $(PIC_FLAG) -DUNIX -D_REENTRANT -DUSE_THREADSAFE_INTERFACES
INCLUDE_FLAGS := -I$(DEST_INC_DIR)  -I$(DOMINO_INC_DIR)
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
SYSTEM_LIBS := -lpthread -ldl -lc -lm
LDFLAGS += -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR)
$(STD_LIB)_LDLIBS := $(NSS_LIBS) $(NSPR_LIBS)
LDLIBS += $(CXX_STD_LIBS) $(LD_SHARED_FLAG) -lc -lm
LDLIBS += $(LDFLAGS) $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) $(SYSTEM_LIBS)
LDLIBS += -Bstatic -lamsdk

all: depends export_libs

$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)

endif

ifeq ($(OS_ARCH), Linux)
# Override optimization flag for IBM Lotus Domino -- on RH AS 2.1 -O2 results in a SEGV
# at run time.
ifeq ($(BUILD_DEBUG), optimize)
DEBUG_FLAGS := -O3 -NDEBUG
endif
CFLAGS += $(PIC_FLAG) -I$(DOMINO_INC_DIR) -DUNIX -D_REENTRANT -DUSE_THREADSAFE_INTERFACES
INCLUDE_FLAGS := -I$(DEST_INC_DIR)  -I$(DOMINO_INC_DIR)
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
SYSTEM_LIBS := -lpthread -ldl -lc -lm
LDFLAGS += -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR)
$(STD_LIB)_LDLIBS := $(NSS_LIBS) $(NSPR_LIBS)
LDLIBS += $(LDFLAGS) $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) $(SYSTEM_LIBS)
LDLIBS += -L../../am
LDLIBS += -Wl,-Bstatic -lamsdk -Wl,-Bdynamic


all: depends export_libs 

$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)


endif


ifeq ($(OS_ARCH), WINNT)

CFLAGS += -Zp -I$(DOMINO_INC_DIR) -I$(DEST_INC_DIR) -DW32 -DWIN32 -DXP_WIN32 -D_WINDOWS -D_REENTRANT -DUSE_THREADSAFE_INTERFACES -DAM_STATIC_LIB
LDFLAGS += ../../../am/source/am_web.res -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(DOMINO_LIB_DIR)/mswin32
LDFLAGS += $(LD_COMMON_ORIGIN_FLAG) $(LD_ORIGIN_FLAG) -L$(DEST_LIB_DIR)
LDLIBS += -lamsdk_static -lnotes -ladvapi32 -lws2_32 -lnss3 -lssl3 -llibnspr4 -llibplc4 -llibxml2 -luser32

all: depends export_libs

$(EXPORTED_LIBS): $(OBJS)  $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)

endif 


### AIX
ifeq ($(OS_ARCH), AIX)
ifeq ($(BUILD_DEBUG), full)
LINKDEBUG := -bnoquiet
endif
EXPORTED_LIBS := $(LIB_NAME).a
CFLAGS +=  $(PIC_FLAG) -I/usr/include -I$(DOMINO_INC_DIR) -I$(DEST_INC_DIR) -DUNIX -DAIX -D_REENTRANT -DUSE_THREADSAFE_INTERFACES 
SYSTEM_LIBS := -lpthread -ldl -lc -lm -liconv
LDFLAGS += -brtl -blibpath:/usr/lib:/lib:/usr/lib/mps -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -L$(LIBXML_LIB_DIR) -L${DOMINO_LIB_DIR}
LDLIBS +=  -L${DEST_LIB_DIR} -lamsdk
LDLIBS += $(NSS_LIBS) $(NSPR_LIBS)
LDLIBS += $(LIBXML_LIBS) -lnotes_r
LINKOPTS := -bE:libamdomino.exp -bnoentry -bquiet

MAKE_SHARED_LIB := $(CC) -o libamdomino.a domino_agent.o $(LINKOPTS) ${LDFLAGS} $(LDLIBS) ${SYSTEM_LIBS}

all: export_libs
$(EXPORTED_LIBS): $(OBJS) $(LIB_MAPFILE)
	$(MAKE_SHARED_LIB)

endif
###

clean: clean_objs clean_libs
	$(RM) $(EXPORTED_LIBS) 
ifeq ($(OS_ARCH),WINNT)
	$(RM) $(EXPORTED_LIBS:.dll=.exp) $(EXPORTED_LIBS:.dll=.lib)
	$(RM) *.pdb
	$(RM) *.ilk
endif

include $(USERX_ROOT)/arch/rules.mk

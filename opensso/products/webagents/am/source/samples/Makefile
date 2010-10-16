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
#
#
USERX_ROOT := ../../..

include $(USERX_ROOT)/arch/components.mk
include $(USERX_ROOT)/arch/rules.mk

ifndef REAL_AM_DIR
    REAL_AM_DIR := ..
endif
ifndef REAL_SRC_DIR
    REAL_SRC_DIR := $(REAL_AM_DIR)/test
endif

vpath %.c $(REAL_SRC_DIR)
vpath %.cpp $(REAL_SRC_DIR)

AM_DIR := ..

ifneq ($(OS_ARCH), AIX)
LIBS = -L$(DEST_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -lamsdk $(LIBXML_LIBS) $(NSPR_LIBS) $(NSS_LIBS)
else
LIBS = -brtl -L$(DEST_LIB_DIR) -L$(LIBXML_LIB_DIR) -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) -lamsdk $(LIBXML_LIBS) $(NSPR_LIBS) $(NSS_LIBS)
endif

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
LIBS = -DEBUG -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) amsdk.lib $(LIBXML_LIBS) $(NSPR_LIBS) $(NSS_LIBS)
endif
endif

MAKE_C_PROGRAM = $(LINK.c) $(OUTPUT_OPTION) $^ $(LIBS)
MAKE_CXX_PROGRAM = $(LINK.cc) $(OUTPUT_OPTION) $^

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
MAKE_C_PROGRAM = $(LINK) $^ $(LIBS)
MAKE_CXX_PROGRAM = $(LINK) $^
endif
endif

ifeq ($(OS_ARCH),  SunOS)
    MAKE_CXX_PROGRAM += '-R$$ORIGIN/$(AM_DIR)'
endif
ifeq ($(OS_ARCH), Linux)
    MAKE_CXX_PROGRAM += -Xlinker '-R$$ORIGIN/$(AM_DIR)'
    ifeq ($(GCC_33), 3.3.2) 
	LIBS += -lgcc_s  
    endif
endif

MAKE_CXX_PROGRAM += $(LIBS)

INCLUDE_FLAGS += -I$(REAL_AM_DIR) -I$(NSPR_INC_DIR)

am_sso_test_OBJS := am_sso_test.o
am_auth_test_OBJS := am_auth_test.o
am_policy_test_OBJS := am_policy_test.o 
am_log_test_OBJS := am_log_test.o 
am_web_agent_test_OBJS := am_web_agent_test.o 

PROGRAM_NAMES := am_auth_test am_sso_test am_policy_test am_log_test \
		am_web_agent_test
PROGRAMS := $(patsubst %,%$(EXE_EXT),$(PROGRAM_NAMES))

OBJS := $(sort $(foreach prog,$(patsubst %,%_OBJS,$(PROGRAM_NAMES)),$($(prog))))

DEPENDS := $(OBJS:.o=.d)

EXPORTED_SAMPLES := \
	README.TXT \
	samples.mk \
	am_auth_test.c \
	am_policy_test.c \
	am_sso_test.c \
	apache_agent.c \
	am_log_test.c \
	am_web_agent_test.c

all: $(PROGRAMS) export_samples

am_auth_test$(EXE_EXT): $(am_auth_test_OBJS)
	$(MAKE_C_PROGRAM)

am_sso_test$(EXE_EXT): $(am_sso_test_OBJS)
	$(MAKE_C_PROGRAM)

am_policy_test$(EXE_EXT): $(am_policy_test_OBJS)
	$(MAKE_C_PROGRAM)

am_log_test$(EXE_EXT): $(am_log_test_OBJS)
	$(MAKE_C_PROGRAM)

am_web_agent_test$(EXE_EXT): $(am_web_agent_test_OBJS)
	$(MAKE_C_PROGRAM)

clean: clean_objs clean_samples
	$(RM) $(PROGRAMS) *.ilk *.pdb *.exe.manifest
ifeq ($(OS_ARCH), WINNT)
	$(RM) $(OBJS)
endif

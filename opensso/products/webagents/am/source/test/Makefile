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
# Makefile for the test directory for the C Policy API library.
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
LIBS = -L$(DEST_LIB_DIR) -L$(LIBXML_LIB_DIR) $(LIBXML_LIBS) -L$(NSS_LIB_DIR) $(NSS_LIBS) -L$(NSPR_LIB_DIR) $(NSPR_LIBS)
else
LIBS = -brtl -L$(DEST_LIB_DIR) -L$(LIBXML_LIB_DIR) $(LIBXML_LIBS) -L$(NSS_LIB_DIR) $(NSS_LIBS) -L$(NSPR_LIB_DIR) $(NSPR_LIBS)
endif

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
LIBS = -LIBPATH:$(DEST_LIB_DIR) -LIBPATH:$(LIBXML_LIB_DIR) -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) $(LIBXML_LIBS) $(NSPR_LIBS) $(NSS_LIBS)
endif
endif

MAKE_C_PROGRAM = $(LINK.c) $(OUTPUT_OPTION) $^ $(LIBS) -lamsdk
MAKE_CXX_PROGRAM = $(LINK.cc) $(OUTPUT_OPTION) $^
MAKE_CXX_PROGRAM += -L$(AM_DIR) $(LIBS) -lamsdk_test

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
MAKE_C_PROGRAM = $(LINK) $^ $(LIBS) amsdk.lib
MAKE_CXX_PROGRAM = $(LINK) $^ $(LIBS) -LIBPATH:$(AM_DIR) amsdk_test.lib
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

INCLUDE_FLAGS += -I$(REAL_AM_DIR) -I$(NSPR_INC_DIR)

map_test_OBJS := map_test.o utilities.o
naming_test_OBJS := naming_test.o utilities.o 
policy_test_OBJS := policy_test.o utilities.o
properties_test_OBJS := properties_test.o utilities.o
xml_test_OBJS := xml_test.o utilities.o
url_test_OBJS := url_test.o
http_test_OBJS := http_test.o
properties_cpp_test_OBJS := properties_cpp.o
entity_ref_test_OBJS := entity_ref_test.o
rem_log_test_OBJS := rem_log_test.o
log_test_OBJS := log_test.o 
utils_test_OBJS := utils_test.o 

ifneq ($(CYGWIN_ARCH), WOW64)
PROGRAM_NAMES := \
	map_test \
	naming_test \
	policy_test \
	properties_test \
	properties_cpp_test \
	xml_test \
	url_test \
	http_test \
	entity_ref_test \
	rem_log_test \
	log_test \
	utils_test
else
PROGRAM_NAMES := \
	map_test
endif

PROGRAMS := $(patsubst %,%$(EXE_EXT),$(PROGRAM_NAMES))

OBJS := $(sort $(foreach prog,$(patsubst %,%_OBJS,$(PROGRAM_NAMES)),$($(prog))))

DEPENDS := $(OBJS:.o=.d)

all: $(PROGRAMS)

map_test$(EXE_EXT): $(map_test_OBJS)
	$(MAKE_C_PROGRAM)

ifneq ($(CYGWIN_ARCH), WOW64)
naming_test$(EXE_EXT): $(naming_test_OBJS)
	$(MAKE_CXX_PROGRAM)

policy_test$(EXE_EXT): $(policy_test_OBJS)
	$(MAKE_CXX_PROGRAM) -L$(NSPR_LIB_DIR) $(NSPR_LIBS)

properties_test$(EXE_EXT): $(properties_test_OBJS)
	$(MAKE_C_PROGRAM)

properties_cpp_test$(EXE_EXT): $(properties_cpp_test_OBJS)
	$(MAKE_CXX_PROGRAM)

xml_test$(EXE_EXT): $(xml_test_OBJS)
	$(MAKE_CXX_PROGRAM)

url_test$(EXE_EXT): $(url_test_OBJS)
	$(MAKE_CXX_PROGRAM)

http_test$(EXE_EXT): $(http_test_OBJS)
	$(MAKE_CXX_PROGRAM)

entity_ref_test$(EXE_EXT): $(entity_ref_test_OBJS)
	$(MAKE_CXX_PROGRAM)

rem_log_test$(EXE_EXT): $(rem_log_test_OBJS)
	$(MAKE_CXX_PROGRAM)

log_test$(EXE_EXT): $(log_test_OBJS)
	$(MAKE_C_PROGRAM)

utils_test$(EXE_EXT): $(utils_test_OBJS)
	$(MAKE_CXX_PROGRAM)
endif

clean: clean_objs 
	$(RM) $(PROGRAMS)
ifeq ($(OS_ARCH), WINNT)
	$(RM) $(OBJS)
endif


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
# $Id
#
# Makefile for building the C Policy API library.
#
#

# "Portions Copyrighted [2010] [ForgeRock AS]"

override BUILD_MODULE=USERX

USERX_ROOT = ../..

include $(USERX_ROOT)/arch/components.mk

ifndef REAL_SRC_DIR
    REAL_SRC_DIR := .
endif

SUBDIRS := samples test

ifeq ($(BUILD_AGENT), iis7)
	DEBUG_FLAGS := -Zi -O2 -DNDEBUG
endif

vpath %.c $(REAL_SRC_DIR)
vpath %.cpp $(REAL_SRC_DIR)
vpath %.h $(REAL_SRC_DIR)
vpath am.def $(REAL_SRC_DIR)
vpath libamsdk.mapfile $(REAL_SRC_DIR)

OBJS := action_decision.o \
	am_auth.o \
	auth_context.o \
	agent_configuration.o \
        agent_profile_service.o \
	agent_config_cache.o \
	am_log.o \
	am_main.o \
	am_map.o \
	am_policy.o \
	am_properties.o \
	am_resource.o \
	am_sso.o \
	am_string_set.o \
	am_utils.o \
	am_web.o \
	auth_svc.o \
	base_service.o \
	connection.o \
	crypt_util.o \
	fqdn_handler.o \
	http.o \
	key_value_map.o \
	log.o \
	log_service.o \
	log_record.o \
	naming_service.o \
	pnotify_handler.o \
	policy_decision.o \
	policy_engine.o \
	policy_entry.o \
	policy_service.o \
	p_cache.o \
	properties.o \
	ref_cnt_ptr.o \
	server_info.o \
	service.o \
	service_info.o \
	session_info.o \
	sso_token.o \
	session_service.o \
        sso_token_service.o \
	string_util.o \
	thread_pool.o \
	tree.o \
	url.o \
	utils.o \
	xml_attribute.o \
	xml_element.o \
	xml_tree.o

ifeq ($(OS_ARCH), WINNT)
OBJS += am_web.res
endif

DEPENDS := $(OBJS:.o=.d)

EXPORTED_HDRS := \
	am.h \
	am_auth.h \
	am_log.h \
	am_map.h \
	am_notify.h \
	am_policy.h \
	am_properties.h \
	am_sso.h \
	am_string_set.h \
	am_types.h \
	am_utils.h \
	am_web.h

ifeq ($(OS_ARCH), WINNT)
STD_LIB_NAME := amsdk
STD_STATIC_LIB_NAME := amsdk_static
STD_LIB := $(STD_LIB_NAME)$(SO_EXT)
STD_STATIC_LIB := $(STD_STATIC_LIB_NAME)$(LIB_EXT)
else
STD_LIB_NAME := libamsdk
STD_LIB := $(STD_LIB_NAME)$(SO_EXT).$(AGENT_MAJOR_VER)
STD_STATIC_LIB := $(STD_LIB_NAME)$(STATIC_EXT)
endif

STD_LIB_MAP_FILE := $(STD_LIB_NAME)$(MAPFILE_EXT)
TEST_LIB_NAME := $(STD_LIB_NAME)_test
TEST_LIB := $(TEST_LIB_NAME)$(SO_EXT)
TEST_LIB_MAP_FILE := $(TEST_LIB_NAME)$(MAPFILE_EXT)

EXPORTED_LIBS := $(STD_LIB)
EXPORTED_STATIC_LIBS := $(STD_STATIC_LIB)
ifeq ($(OS_ARCH), WINNT)
EXPORTED_LIBS += $(STD_LIB_NAME).lib
endif

$(TEST_LIB_MAP_FILE): $(OBJS) $(STD_LIB_MAP_FILE)
	$(NM) -ng $(filter %.o, $^) | \
		egrep 'smi|Utils' | \
		awk 'NF == 3 && $$2 != "U" { printf "\t\t%s;\n", $$3 }' | \
		egrep -v '^		(\?\?_[EG]|\$$)' | \
		sort | uniq > $@.tmp
ifeq ($(OS_ARCH), WINNT)
	cat $(filter %$(MAPFILE_EXT), $^) $@.tmp > $@ || $(RM) $@
else
	( sed -e '/local:/,/\*;/d' < $(filter %$(MAPFILE_EXT), $^) ; $(ECHO) "\nSUNWprivate_1.0 {\n 	global:" ; cat $@.tmp ; $(ECHO) "\n\tlocal:\n\t\t*;\n};" ) > $@ || $(RM) $@
endif
	$(RM) $@.tmp


ifeq ($(OS_ARCH), SunOS)
SYSTEM_LIBS := $(LD_STATIC_FLAG) $(CXX_STD_LIBS) $(LD_SHARED_FLAG) \
	       -lnls -lpthread -ldl -lc -lm
endif

ifeq ($(OS_ARCH), HP-UX)
SYSTEM_LIBS := -Wl,+s $(LD_STATIC_FLAG) $(CXX_STD_LIBS) $(LD_SHARED_FLAG) \
	       -lpthread -ldld -lc -lm
endif

ifeq ($(OS_ARCH), Linux)
SYSTEM_LIBS := -lpthread -ldl
	ifeq ($(BUILD_TYPE), 32)
	SYSTEM_LIBS += -m32
	endif
endif

INCLUDE_FLAGS := -I$(REAL_SRC_DIR) -I$(NSPR_INC_DIR) -I$(NSS_INC_DIR) -I$(LIBXML_INC_DIR)

LDFLAGS += -L$(NSS_LIB_DIR) -L$(NSPR_LIB_DIR) \
	-L$(LIBXML_LIB_DIR)

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
LDFLAGS += -LIBPATH:$(NSS_LIB_DIR) -LIBPATH:$(NSPR_LIB_DIR) \
	-LIBPATH:$(LIBXML_LIB_DIR)
endif
endif

$(STD_LIB)_LDLIBS := $(NSS_DYNAMIC_LIBS) $(NSPR_LIBS)

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
$(STD_LIB)_LDLIBS += user32.lib
else
$(STD_LIB)_LDLIBS += -luser32
endif
endif

$(TEST_LIB)_LDLIBS := $($(STD_LIB)_LDLIBS)
ifeq ($(OS_ARCH), SunOS)

$(STD_LIB)_LDFLAGS := -z defs
LD_STATIC_FLAGS += -xar
endif

LDLIBS += $(LIBXML_LIBS) $($(STD_LIB)_LDLIBS) $(SYSTEM_LIBS)

CXXFLAGS += $(PIC_FLAG) -DAM_BUILDING_LIB -D_CHAR_IS_UNSIGNED
LD_STATIC_FLAGS += $(PIC_FLAG) -DAM_BUILDING_LIB

CFLAGS += -DAM_BUILDING_LIB

include $(USERX_ROOT)/arch/rules.mk

ifneq ($(OS_ARCH), AIX)
all: version.h $(USERX_ROOT)/.version depends export_headers export_static_libs export_libs $(TEST_LIB) softlink_so $(DEST_BIN_DIR)/$(CRYPT_EXE) all_subdirs
else
all: version.h $(USERX_ROOT)/.version export_headers export_static_libs export_libs $(TEST_LIB) softlink_so $(DEST_BIN_DIR)/$(CRYPT_EXE) all_subdirs
endif

$(EXPORTED_LIBS): $(OBJS) $(STD_LIB_MAP_FILE)
	$(MAKE_SHARED_LIB)

$(STD_STATIC_LIB): $(OBJS) $(STD_LIB_MAP_FILE)
	$(MAKE_STATIC_LIB)
          
$(TEST_LIB): $(OBJS) $(TEST_LIB_MAP_FILE)
	$(MAKE_SHARED_LIB)

version.h: version.template
	sed -e 's%BUILD_DATE%$(BUILD_DATE)%g' \
	    -e 's%BUILD_MACHINE%$(BUILD_MACHINE)%g' \
	    -e 's%SVN_REVISION%$(SVN_REVISION)%g' \
	    -e 's%AGENT_VER%$(AGENT_VER)%g' \
	    -e 's%AGENT_ER_VER%$(AGENT_ER_VER)%g' \
	    -e 's%AGENT_FVB_MARKER%$(AGENT_FVB_MARKER)%g' $< > $@

$(USERX_ROOT)/.version: $(USERX_ROOT)/version.template
	sed -e 's%BUILD_DATE%$(BUILD_DATE)%g' \
	    -e 's%BUILD_MACHINE%$(BUILD_MACHINE)%g' \
	    -e 's%SVN_REVISION%$(SVN_REVISION)%g' \
	    -e 's%AGENT_VER%$(AGENT_VER)%g' \
	    -e 's%AGENT_ER_TEXT%$(AGENT_ER_TEXT)%g' \
	    -e 's%AGENT_FVB_TEXT%$(AGENT_FVB_TEXT)%g' $< > $@
ifeq ($(ADD_README), YES)
	echo "copy release notes" > $(USERX_ROOT)/.copyReleaseNotes
endif

softlink_so:
ifeq ($(OS_ARCH), WINNT)
# nothing
else
	@set -e; (cd $(DEST_LIB_DIR); \
	$(RM) $(STD_LIB_NAME)$(SO_EXT); \
	ln -s $(STD_LIB) $(STD_LIB_NAME)$(SO_EXT); \
	)
endif
ifeq ($(OS_ARCH), SunOS)
	@set -e; (cd $(DEST_LIB_DIR); \
	)
endif

clean: clean_headers clean_libs clean_objs clean_subdirs
	$(RM) version.h
	$(RM) $(DEST_LIB_DIR)/$(STD_LIB_NAME)$(SO_EXT)
	$(RM) $(EXPORTED_LIBS) $(EXPORTED_STATIC_LIBS) $(TEST_LIB) $(TEST_LIB_MAP_FILE) $(CRYPT_EXE)
	$(RM) $(DEST_BIN_DIR)/$(CRYPT_EXE)
	$(RM) $(DEST_LIB_DIR)/$(STD_LIB_NAME)$(SO_EXT)
	$(RM) $(DEST_LIB_DIR)/$(STD_LIB_NAME)$(STATIC_EXT)
ifeq ($(OS_ARCH), WINNT)
	$(RM) $(STD_LIB_NAME).exp $(TEST_LIB_NAME).exp $(TEST_LIB_NAME).lib
	$(RM) *.pdb
	$(RM) crypt_util_program.o
endif

$(DEST_BIN_DIR)/$(CRYPT_EXE): $(DEST_BIN_DIR)
ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
	$(CC) -DWINNT -Fe$(CRYPT_EXE) -Focrypt_util_program.o crypt_util.c
	${MT} -manifest $(MSCRT_DIR)/Microsoft.VC90.CRT.manifest -outputresource:$(CRYPT_EXE)\;1
else
	$(CC) -DWINNT -c crypt_util.c -o crypt_util_program.o
	$(CC) -o $(CRYPT_EXE) crypt_util_program.o
	${MT} -manifest $(MSCRT_DIR)/Microsoft.VC90.CRT.manifest -outputresource:$(CRYPT_EXE)\;1
endif
else
ifeq ($(BUILD_TYPE), 32)
	$(CC) -m32 -c crypt_util.c -o crypt_util_program.o
	$(CC) -m32 -o $(CRYPT_EXE) crypt_util_program.o
else
	$(CC) -c crypt_util.c -o crypt_util_program.o
	$(CC) -o $(CRYPT_EXE) crypt_util_program.o
endif
endif
	$(CP) $(CRYPT_EXE) $(DEST_BIN_DIR)



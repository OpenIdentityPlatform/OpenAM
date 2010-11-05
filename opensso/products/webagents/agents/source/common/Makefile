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
# Makefile file for building the Sun ONE Identity Server Agent Toolkit
#
#

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

ifeq ($(OS_ARCH), SunOS)
SUBDIRS += resources
endif

ifeq ($(OS_ARCH), Linux)
SUBDIRS += resources
endif

PACKAGE_REVISION := ,REV=$(shell date +%Y.%m.%d.%H.%M)

all: all_subdirs 

PKG_DIR := $(DEST_PACKAGE_SCRATCH_DIR)/SUNWamcom
EXTRA_PACKAGE_FILES := \
	$(DEST_INC_DIR)/am.h \
	$(DEST_INC_DIR)/am_auth.h \
	$(DEST_INC_DIR)/am_log.h \
	$(DEST_INC_DIR)/am_map.h \
	$(DEST_INC_DIR)/am_notify.h \
	$(DEST_INC_DIR)/am_policy.h \
	$(DEST_INC_DIR)/am_properties.h \
	$(DEST_INC_DIR)/am_sso.h \
	$(DEST_INC_DIR)/am_string_set.h \
	$(DEST_INC_DIR)/am_types.h \
	$(DEST_INC_DIR)/am_utils.h \
	$(DEST_INC_DIR)/am_web.h

EXTRA_PACKAGE_FILES += \
	$(DEST_SAMPLES_DIR)/README.TXT \
	$(DEST_SAMPLES_DIR)/am_auth_test.c \
	$(DEST_SAMPLES_DIR)/am_policy_test.c \
	$(DEST_SAMPLES_DIR)/am_sso_test.c \
	$(DEST_SAMPLES_DIR)/am_log_test.c \
	$(DEST_SAMPLES_DIR)/apache_agent.c \
	$(DEST_SAMPLES_DIR)/am_web_agent_test.c \
	$(DEST_SAMPLES_DIR)/samples.mk

EXTRA_PACKAGE_FILES += \
	$(DEST_BIN_DIR)/$(CRYPT_EXE)

# libxml_license.txt
EXTRA_PACKAGE_FILES += \
	THIRD_PARTY_LICENSE.TXT

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
EXTRA_PACKAGE_FILES += \
	$(patsubst %.lib, $(NSPR_LIB_DIR)/%$(SO_EXT), $(NSPR_LIBS)) \
	$(patsubst %.lib, $(NSPR_LIB_DIR)/%$(LIB_EXT), $(NSPR_LIBS)) \
	$(patsubst %.lib, $(NSS_LIB_DIR)/%$(SO_EXT), $(NSS_DYNAMIC_LIBS)) \
	$(patsubst %.lib, $(NSS_LIB_DIR)/%$(LIB_EXT), $(NSS_DYNAMIC_LIBS))
else
EXTRA_PACKAGE_FILES += \
	$(patsubst -l%, $(NSPR_LIB_DIR)/%$(SO_EXT), $(NSPR_LIBS)) \
	$(patsubst -l%, $(NSPR_LIB_DIR)/%$(LIB_EXT), $(NSPR_LIBS)) \
	$(patsubst -l%, $(NSS_LIB_DIR)/%$(SO_EXT), $(NSS_DYNAMIC_LIBS)) \
	$(patsubst -l%, $(NSS_LIB_DIR)/%$(LIB_EXT), $(NSS_DYNAMIC_LIBS))
endif
else
ifneq ($(OS_ARCH), SunOS)
EXTRA_PACKAGE_FILES += \
	$(patsubst -l%, $(NSPR_LIB_DIR)/lib%$(SO_EXT), $(NSPR_LIBS)) \
	$(patsubst -l%, $(NSS_LIB_DIR)/lib%$(SO_EXT), $(NSS_DYNAMIC_LIBS))
endif
endif

ifeq ($(OS_ARCH), SunOS)
ZIP_DIR:=$(DEST_PACKAGE_SCRATCH_DIR)/is_csdk
ZIP_SUB_DIRS:=lib include config samples bin
EXTRA_PACKAGE_FILES += prototype $(USERX_ROOT)/am/source/prototype \
	copyright pkginfo request preinstall postinstall preremove \
	comlib \
	$(LIBXML_LIB_DIR)/libxml2.so.2 \
	$(NSPR_LIB_DIR)/libnspr4.so \
	$(NSPR_LIB_DIR)/libplc4.so \
	$(NSPR_LIB_DIR)/libplds4.so \
	$(NSS_LIB_DIR)/libnss3.so \
	$(NSS_LIB_DIR)/libssl3.so \
	$(DEST_LIB_DIR)/libamsdk.so.$(AGENT_MAJOR_VER)

all: $(DEST_PACKAGE_SCRATCH_DIR) $(DEST_PACKAGE_SCRATCH_DIR)/$(ZIP_DIR)

$(DEST_PACKAGE_SCRATCH_DIR)/$(ZIP_DIR): $(DEST_PACKAGE_SCRATCH_DIR) $(EXPORTED_HEADERS) $(EXPORTED_LIBS) $(EXTRA_PACKAGE_FILES)
	$(RM) -r $(ZIP_DIR)
	$(MKDIR) $(patsubst %, $(ZIP_DIR)/%, $(ZIP_SUB_DIRS))
	$(CP) $(filter %$(SO_EXT).$(AGENT_MAJOR_VER), $^) $(ZIP_DIR)/lib
	$(CP) $(LIBXML_LIB_DIR)/libxml2.so.2 $(ZIP_DIR)/lib
	$(CP) -r $(NSPR_LIB_DIR)/* $(ZIP_DIR)/lib
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentBootstrap.template $(ZIP_DIR)/config/OpenSSOAgentBootstrap.template.orig
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentConfiguration.template $(ZIP_DIR)/config/OpenSSOAgentConfiguration.template.orig
	$(CP) $(USERX_ROOT)/am/source/crypt_util $(ZIP_DIR)/bin
	$(CP) $(filter %.h, $^) $(ZIP_DIR)/include
	$(CP) $(filter %THIRD_PARTY_LICENSE.TXT, $^) $(ZIP_DIR)
	$(CP) $(filter %README.TXT, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.c, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.mk, $^) $(ZIP_DIR)/samples/Makefile
endif

ifeq ($(OS_ARCH), WINNT)
ZIP_FILE := $(PKG_DIR).zip
ZIP_DIRS := bin config include samples
EXTRA_PACKAGE_FILES += $(DEST_LIB_DIR)/amsdk.dll $(DEST_LIB_DIR)/amsdk.lib \
	$(LIBXML_LIB_DIR)/libxml2.dll $(LIBXML_LIB_DIR)/libxml2.lib \
	$(NSS_BIN_DIR)/certutil.exe $(NSS_LIB_DIR)/softokn3.dll \
	$(NSS_LIB_DIR)/softokn3.chk $(NSS_LIB_DIR)/freebl3.chk \
	$(NSS_LIB_DIR)/freebl3.dll $(NSS_LIB_DIR)/jss4.dll \
	$(NSS_LIB_DIR)/nssckbi.dll $(NSS_LIB_DIR)/nssdbm3.dll \
	$(NSS_LIB_DIR)/nssdbm3.chk $(NSS_LIB_DIR)/nssutil3.dll \
	$(NSS_LIB_DIR)/smime3.dll $(NSS_LIB_DIR)/sqlite3.dll \
	$(NSS_LIB_DIR)/nssutil3.lib $(NSS_LIB_DIR)/smime3.lib 

$(ZIP_FILE): $(DEST_PACKAGE_SCRATCH_DIR) $(EXPORTED_HEADERS) $(EXPORTED_LIBS) $(EXTRA_PACKAGE_FILES)
	$(RM) -r $(PKG_DIR)
	$(MKDIR) $(patsubst %, $(PKG_DIR)/%, $(ZIP_DIRS))
	$(CP) $(filter %.exe, $^) $(PKG_DIR)/bin
	$(CP) $(filter %$(SO_EXT), $^) $(PKG_DIR)/bin
	$(CP) $(filter %.chk, $^) $(PKG_DIR)/bin
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentBootstrap.template $(PKG_DIR)/config/OpenSSOAgentBootstrap.template.orig
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentConfiguration.template $(PKG_DIR)/config/OpenSSOAgentConfiguration.template.orig
	$(CP) $(filter %.h, $^) $(PKG_DIR)/include
	$(CP) $(filter %THIRD_PARTY_LICENSE.TXT, $^) $(PKG_DIR)
	$(CP) $(filter %.lib, $^) $(PKG_DIR)/bin
	$(CP) $(filter %README.TXT, $^) $(PKG_DIR)/samples
	$(CP) $(filter %.c, $^) $(PKG_DIR)/samples
	$(CP) $(filter %.mk, $^) $(PKG_DIR)/samples/Makefile
	cd $(PKG_DIR) ; zip -r ../$(notdir $@) $(ZIP_DIRS)

all: $(ZIP_FILE)
endif

ifeq ($(OS_ARCH), HP-UX)
ZIP_FILE := $(ZIP_DIR).zip
ZIP_DIR := $(PKG_DIR)/is_csdk
ZIP_SUB_DIRS := bin config debug include lib samples
EXTRA_PACKAGE_FILES += $(DEST_LIB_DIR)/libamsdk.sl \
	$(LIBXML_LIB_DIR)/libxml2.sl $(NSS_BIN_DIR)/certutil

$(ZIP_FILE): $(DEST_PACKAGE_SCRATCH_DIR) $(EXPORTED_HEADERS) $(EXPORTED_LIBS) $(EXTRA_PACKAGE_FILES)
	$(RM) -rf $(ZIP_DIR)
	$(MKDIR) $(patsubst %, $(ZIP_DIR)/%, $(ZIP_SUB_DIRS))
	$(CP) $(USERX_ROOT)/am/source/crypt_util $(ZIP_DIR)/bin
	$(CP) comlib_HP-UX $(ZIP_DIR)/lib/comlib
	$(CP) $(filter %$(SO_EXT), $^) $(ZIP_DIR)/lib
	$(CP) THIRD_PARTY_LICENSE.TXT $(ZIP_DIR)
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentBootstrap.template $(ZIP_DIR)/config/OpenSSOAgentBootstrap.template.orig
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentConfiguration.template $(ZIP_DIR)/config/OpenSSOAgentConfiguration.template.orig
	$(CP) $(filter %.h, $^) $(ZIP_DIR)/include
	$(CP) $(filter %README.TXT, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.c, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.mk, $^) $(ZIP_DIR)/samples/Makefile
	cd $(ZIP_DIR); zip -r ../$(notdir $@) *

all: $(ZIP_FILE)
endif

ifeq ($(OS_ARCH), AIX)
ZIP_FILE := $(ZIP_DIR).zip
ZIP_DIR := $(PKG_DIR)/is_csdk
ZIP_SUB_DIRS := bin config debug include lib samples
EXTRA_PACKAGE_FILES += $(DEST_LIB_DIR)/libamsdk.so \
	$(LIBXML_LIB_DIR)/libxml2.so.2 $(NSS_BIN_DIR)/certutil

$(ZIP_FILE): $(DEST_PACKAGE_SCRATCH_DIR) $(EXPORTED_HEADERS) $(EXPORTED_LIBS) $(EXTRA_PACKAGE_FILES)
	$(RM) -rf $(ZIP_DIR)
	$(MKDIR) $(patsubst %, $(ZIP_DIR)/%, $(ZIP_SUB_DIRS))
	$(CP) $(USERX_ROOT)/am/source/crypt_util $(ZIP_DIR)/bin
	$(CP) comlib_AIX $(ZIP_DIR)/lib/comlib
	$(CP) $(filter %$(SO_EXT), $^) $(ZIP_DIR)/lib
	$(CP) THIRD_PARTY_LICENSE.TXT $(ZIP_DIR)
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentBootstrap.template $(ZIP_DIR)/config/OpenSSOAgentBootstrap.template.orig
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentConfiguration.template $(ZIP_DIR)/config/OpenSSOAgentConfiguration.template.orig
	$(CP) $(filter %.h, $^) $(ZIP_DIR)/include
	$(CP) $(filter %README.TXT, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.c, $^) $(ZIP_DIR)/samples
	$(CP) $(filter %.mk, $^) $(ZIP_DIR)/samples/Makefile
	cd $(ZIP_DIR); zip -r ../$(notdir $@) *

all: $(ZIP_FILE)
endif

RPM := $(RPM_DIR)/$(DSAME_DROP_FILE).rpm

BUILDROOT_SAMPLES_DIR := $(BUILDROOT)/$(INSTALL_DIR)/samples

ifeq ($(OS_ARCH), Linux)
all: $(RPM)
endif

$(RPM): 
	$(MKDIR) $(RPM_DIR)
	$(RM) -r $(BUILDROOT)
	$(MKDIR) $(BUILDROOT_LIB_DIR)
	$(MKDIR) $(BUILDROOT_CONF_DIR)
	$(MKDIR) $(BUILDROOT_BIN_DIR)
	$(MKDIR) $(BUILDROOT_INC_DIR)
	$(MKDIR) $(BUILDROOT_SAMPLES_DIR)
	$(MKDIR) $(BUILDROOT_RPM_DIR)
	$(CP) $(DEST_LIB_DIR)/libamsdk.so.$(AGENT_MAJOR_VER) $(BUILDROOT_LIB_DIR)
	$(CP)  $(NSPR_LIB_DIR)/* $(BUILDROOT_LIB_DIR)
	$(CP) THIRD_PARTY_LICENSE.TXT $(BUILDROOT_LIB_DIR)/..
	$(CP) $(LIBXML_LIB_DIR)/libxml2.so.2 $(BUILDROOT_LIB_DIR)
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentBootstrap.template $(BUILDROOT_CONF_DIR)/OpenSSOAgentBootstrap.template.orig
	$(CP) $(USERX_ROOT)/conf/OpenSSOAgentConfiguration.template $(BUILDROOT_CONF_DIR)/OpenSSOAgentConfiguration.template.orig
	$(CP) $(USERX_ROOT)/am/source/crypt_util $(BUILDROOT_BIN_DIR)
	$(CP) comlib_linux $(BUILDROOT_LIB_DIR)
	$(CP) $(DEST_SAMPLES_DIR)/README.TXT $(BUILDROOT_SAMPLES_DIR)
	$(CP) $(DEST_SAMPLES_DIR)/*.c $(BUILDROOT_SAMPLES_DIR)
	$(CP) $(DEST_SAMPLES_DIR)/samples.mk $(BUILDROOT_SAMPLES_DIR)/Makefile
	$(CP) $(DEST_INC_DIR)/*.h $(BUILDROOT_INC_DIR)
	if [ -f $(HOME)/.rpmmacros ] ; then mv $(HOME)/.rpmmacros $(HOME)/.rpmmacros.agent ; fi
	echo "%_topdir $(BUILDROOT)" > $(HOME)/.rpmmacros
	rpmbuild -bb --buildroot $(BUILDROOT) --target $(MC_ARCH) common-3.0-0.spec
	$(RM) $(HOME)/.rpmmacros
	if [ -f $(HOME)/.rpmmacros.agent ] ; then mv $(HOME)/.rpmmacros.agent $(HOME)/.rpmmacros ; fi
	$(MV) $(BUILDROOT_RPM_DIR)/$(DSAME_DROP_FILE).rpm $(RPM_DIR)
	$(MV) $(BUILDROOT)/opt/agents $(DEST_PACKAGE_SCRATCH_DIR)/common
	$(RM) -r $(BUILDROOT)

$(PKG_TIMESTAMP): $(EXTRA_PACKAGE_FILES)
	$(MAKE_PACKAGE) libxml_lib_dir=$(LIBXML_LIB_DIR) \
                dest_drop_dir=$(DEST_DROP_DIR)
	touch $@

pkginfo: pkginfo.template
	sed -e 's;%%ARCH%%;$(PKG_ARCH);g' -e 's;%%BASE_PRODUCT_NAME%%;$(BASE_PRODUCT_NAME);g' -e '/^#/d' $< > $@

clean: clean_subdirs
	$(RM) -r $(PKG_DIR) pkginfo

include $(USERX_ROOT)/arch/rules.mk

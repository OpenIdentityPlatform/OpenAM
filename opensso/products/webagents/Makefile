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
# Top level makefile for the Agent Pack.
#
#

override BUILD_MODULE := USERX_ROOT
USERX_ROOT := .
include $(USERX_ROOT)/arch/components.mk

#
# Make sure that the local copies of the components are populated before
# doing the main part of the build.
#

SUBDIRS := am/source agents/source

ifeq ($(OS_ARCH), WINNT)
   AM_LIB_PREFIX :=
   NSS_LIB_PREFIX :=
   NSS_EXE_SUFFIX := .exe
   LIB_XML_SUFFIX :=
else
   AM_LIB_PREFIX := lib
   NSS_LIB_PREFIX := lib
   NSS_EXE_SUFFIX :=
   LIB_XML_SUFFIX := .2
endif

all: all_subdirs $(DEST_DROP_DIR)/$(DSAME_DROP_FILE) 

clean: clean_subdirs
	$(RM) .version
	$(RM) .copyReleaseNotes
	$(RM) -r $(DEST_DIR)

drop: $(DEST_DROP_DIR)/$(DSAME_DROP_FILE)

$(DEST_DROP_DIR)/$(DSAME_DROP_FILE): $(DEST_PACKAGE_DIR) 
	$(ECHO) Making Target $(DSAME_DROP_FILE)
	$(MKDIR) $(DEST_DROP_DIR)
ifeq ($(OS_ARCH), WINNT)
	cd $(DEST_PACKAGE_SCRATCH_DIR)/SUNWamcom; \
	zip -r ../../../../$@.zip *
else
ifeq ($(OS_ARCH), Linux)
	# DSAME_DROP_FILE on linux takes the form of rpms which makes 
	# the name inconsistent with other platforms. 
	# release wants consistent name for all platforms, so use 
	# DSAME_DROP_FILE_NAME instead of just DSAME_DROP_FILE for Linux. 
	cd $(DEST_PACKAGE_SCRATCH_DIR)/common; \
	$(TAR) cvf ../../../../$(DEST_DROP_DIR)/$(DSAME_DROP_FILE_NAME).tar *; \
	gzip -f ../../../../$(DEST_DROP_DIR)/$(DSAME_DROP_FILE_NAME).tar
else
ifeq ($(OS_ARCH), HP-UX)
	cd $(DEST_PACKAGE_SCRATCH_DIR)/SUNWamcom; \
	$(TAR) cvf ../../../../$@.tar *; \
	gzip -f ../../../../$@.tar
else
ifeq ($(OS_ARCH), AIX)
	cd $(DEST_PACKAGE_SCRATCH_DIR)/SUNWamcom; \
	zip -r ../../../../$@.zip *
else

	cd $(DEST_PACKAGE_SCRATCH_DIR); \
	$(TAR) cvf ../../../$@.tar is_csdk; \
	gzip -f ../../../$@.tar
endif
endif
endif
endif

make_zip:
ifdef BUILD_SHIP
ifeq ($(OS_ARCH), WINNT)
	$(CP) $(DEST_DROP_DIR)/$(DSAME_DROP_FILE).zip $(BUILD_SHIP)
else
ifeq ($(OS_ARCH), Linux)
	$(CP) $(DEST_DROP_DIR)/$(DSAME_DROP_FILE_NAME).tar.gz $(BUILD_SHIP)
else
	$(CP) $(DEST_DROP_DIR)/$(DSAME_DROP_FILE).tar.gz $(BUILD_SHIP)
endif
endif
	$(CP) $(DEST_PACKAGE_DIR)/agent_$(OS_ARCH)* $(BUILD_SHIP)
endif

include $(USERX_ROOT)/arch/rules.mk

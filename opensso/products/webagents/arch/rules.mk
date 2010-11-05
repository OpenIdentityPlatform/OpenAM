#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
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
# $Id: rules.mk,v 1.3 2008/06/25 05:54:25 qcheng Exp $
# 
#

#
# This makefile defines the standard rules used to build the Agent Pack.
#
# Make sure USERX_ROOT is defined before including this file.

ifndef	RULES_INCLUDED
RULES_INCLUDED := true

include $(USERX_ROOT)/arch/defines.mk

.PHONY: all clean depends

internal_DEST_DIRS := \
	$(DEST_BIN_DIR) \
	$(DEST_CLASS_DIR) \
	$(DEST_CONFIG_DIR) \
	$(DEST_DOC_DIR) \
	$(DEST_DROP_DIR) \
	$(DEST_EXAMPLES_DIR) \
	$(DEST_INC_DIR) \
	$(DEST_LIB_DIR) \
	$(DEST_SAMPLES_DIR) \
	$(DEST_PACKAGE_DIR) \
	$(DEST_PACKAGE_SCRATCH_DIR) \
	$(DEST_TEST_DIR)

include $(USERX_ROOT)/arch/rules_$(OS_ARCH).mk

$(internal_DEST_DIRS):
	$(MKDIR) $@

#
# Provide this for backward compatibility.
#
subdirs: all_subdirs
cleansubdirs: clean_subdirs

%_subdirs:
ifeq	($(strip $(SUBDIRS)),)
	@echo "There are no SUBDIRS to process."
else
	@set -e; for i in $(SUBDIRS); do \
		$(MAKE) -C $$i $*; \
	done
endif

$(DEST_LIB_DIR)/% $(DEST_INC_DIR)/%: %
	$(CP) $< $@

clean_headers:
	if [ -d $(DEST_INC_DIR) ]; then \
	    (cd $(DEST_INC_DIR) ; $(RM) $(EXPORTED_HDRS)) ; \
	    $(RMDIR) $(DEST_INC_DIR) || exit 0 ; \
	fi

clean_libs:
	if [ -d $(DEST_LIB_DIR) ]; then \
	    (cd $(DEST_LIB_DIR) ; $(RM) $(EXPORTED_LIBS)) ; \
	    $(RMDIR) $(DEST_LIB_DIR) || exit 0 ; \
	fi

clean_samples:
	if [ -d $(DEST_SAMPLES_DIR) ]; then \
	    (cd $(DEST_SAMPLES_DIR) ; $(RM) $(EXPORTED_SAMPLES)) ; \
	    $(RMDIR) $(DEST_SAMPLES_DIR) || exit 0 ; \
	fi

export_headers: $(DEST_INC_DIR) \
		$(patsubst %, $(DEST_INC_DIR)/%, $(EXPORTED_HDRS))

export_libs: $(DEST_LIB_DIR) $(patsubst %, $(DEST_LIB_DIR)/%, $(EXPORTED_LIBS))
export_static_libs: $(DEST_LIB_DIR) $(patsubst %, $(DEST_LIB_DIR)/%, $(EXPORTED_STATIC_LIBS))

export_samples: $(DEST_SAMPLES_DIR) 
		$(CP) $(EXPORTED_SAMPLES) $(DEST_SAMPLES_DIR)

copyProperties: $(DEST_TEST_DIR)
	$(CP) *.properties $(DEST_TEST_DIR)

copyLocaleFiles: $(DEST_CONFIG_DIR)/locale $(DEST_CLASS_DIR) $(LOCALE_FILES)
	$(CP) $(LOCALE_FILES) $(DEST_CONFIG_DIR)/locale
	$(CP) $(LOCALE_FILES) $(DEST_CLASS_DIR)

depends: $(DEPENDS)

endif

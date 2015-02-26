#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# http://forgerock.org/license/CDDLv1.0.html
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at http://forgerock.org/license/CDDLv1.0.html
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#

USERX_ROOT = ../../..

include $(USERX_ROOT)/arch/components.mk

VARNISH_LIB_NAME := libvmod_am
VARNISH_LIB := $(VARNISH_LIB_NAME)$(SO_EXT)
ifeq ($(BUILD_TYPE), 32)
VARNISH_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common -I../../../extlib/$(OSMC_ARCH)/varnish_32/include/
VARNISH_LDFLAGS := -L../../../extlib/$(OSMC_ARCH)/varnish_32/lib/
VARNISH_CFLAGS := -m32 -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE
else
VARNISH_INCLUDE_FLAGS := -I$(DEST_INC_DIR) -I../common -I../../../extlib/$(OSMC_ARCH)/varnish_64/include/
VARNISH_LDFLAGS := -L../../../extlib/$(OSMC_ARCH)/varnish_64/lib/
VARNISH_CFLAGS := -m64
endif

VARNISH_CFLAGS += -DVARNISH303

%.o: %.c
	$(COMPILE.c) $(VARNISH_INCLUDE_FLAGS) $(VARNISH_CFLAGS) $< $(OUTPUT_OPTION)

VARNISH_SRC := $(wildcard *.c)
OBJS := $(VARNISH_SRC:.c=.o)

EXPORTED_LIBS := $(VARNISH_LIB)

ifeq ($(OS_ARCH), SunOS)
VARNISH_LINK := ld -R'$$ORIGIN/vmod_am_lib' -R'$$ORIGIN' -G -i -z origin -z ignore -lc -lm -lsocket -lnsl -ldl -h $(VARNISH_LIB)
ifeq ($(BUILD_TYPE), 64)
VARNISH_LINK += -64
VARNISH_CFLAGS += -KPIC
endif
else
VARNISH_LINK := ${CC} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,$(VARNISH_LIB) -Wl,-rpath,'$$ORIGIN/vmod_am_lib' -Wl,-rpath,'$$ORIGIN'
ifeq ($(BUILD_TYPE), 64)
VARNISH_LINK += -m64
VARNISH_CFLAGS += -fPIC
endif
endif

$(VARNISH_LIB): $(OBJS)
	${VARNISH_LINK} $(OBJS) -o $(VARNISH_LIB) $(DEST_LIB_DIR)/libamsdk.so $(VARNISH_LDFLAGS) -lapr-1
 
all: export_libs

clean: clean_libs clean_objs
	$(RM) $(EXPORTED_LIBS)
	$(RM) -r $(DEST_PACKAGE_SCRATCH_DIR)/RPMS

include $(USERX_ROOT)/arch/rules.mk
	
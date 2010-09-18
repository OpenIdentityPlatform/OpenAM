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
# $Id: rules_WINNT.mk,v 1.4 2008/06/25 05:54:27 qcheng Exp $
# 
#

#
# This makefile defines the Windows NT/2000 specific rules needed to build
# the Agent Pack.
#

%.cpp %.d:

ifdef   OS_IS_CYGWIN
%.o: %.cpp
	$(COMPILE.cc) -Fo$@ $< 
%.o: %.c
	$(COMPILE.cc) -Fo$@ $< 
else
%.o: %.cpp
	$(COMPILE.cc) $< $(OUTPUT_OPTION)
endif

%.res: %.rc
	$(RC) -fo$@ $<

#
# We currently do not have any means of generating dependency information
# on Windows so make sure this variable has an empty value.
#
DEPENDS :=

#
# Build/clean up OS/compiler specific junk. 
#
.PHONY: build_objs build_workspace clean_objs clean_workspace

ifdef WORKSPACE_NAME
clean_workspace:
	msdev $(WORKSPACE_NAME) /MAKE $(MSDEV_BUILD_CONFIG) /CLEAN
	if [ -d $(MSDEV_BUILD_TYPE) ] ; then \
	    $(RMDIR) $(MSDEV_BUILD_TYPE) || exit 0 ; \
	fi
	$(RM) $(WORKSPACE_NAME:.dsw=.ncb $(WORKSPACE_NAME:.dsw=.plg)) $(WORKSPACE_NAME:.dsw=.opt)

clean_objs: clean_workspace

build_workspace:
	msdev $(WORKSPACE_NAME) /MAKE $(MSDEV_BUILD_CONFIG)

build_objs: build_workspace
endif

clean_objs:
ifneq ($(strip $(OBJS) $(DEPENDS)),)
	$(RM) $(OBJS) $(DEPENDS)
endif

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
# $Id: rules_SunOS.mk,v 1.3 2008/06/25 05:54:25 qcheng Exp $
# 
#

#
# This makefile defines the Solaris-specific rules needed to build
# the Agent Pack.
#

%.cpp %.cxx %.d:

%.o: %.cpp
	$(COMPILE.cc) $< $(OUTPUT_OPTION)

%.o: %.cxx
	$(COMPILE.cc) $< $(OUTPUT_OPTION)

%.d: %.c
	set -e; $(COMPILE.c) -xM1 $< | sort | uniq \
		| sed 's;\($*\.o\)[ :]*;\1 $@ : ;' > $@; [ -s $@ ] || $(RM) $@

%.d: %.cpp
	set -e; $(COMPILE.cc) -xM1 $< | sort | uniq \
		| sed 's;\($*\.o\)[ :]*;\1 $@ : ;' > $@; [ -s $@ ] || $(RM) $@

%.d: %.cxx
	set -e; $(COMPILE.cc) -xM1 $< | sort | uniq \
		| sed 's;\($*\.o\)[ :]*;\1 $@ : ;' > $@; [ -s $@ ] || $(RM) $@

#
# Clean up OS/compiler specific junk. 
#
clean_objs:
	$(RM) -r SunWS_cache
	$(RM) $(OBJS) $(DEPENDS)

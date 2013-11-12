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

ifndef	LINUX_MK_INCLUDED
LINUX_MK_INCLUDED := true

CC = gcc
CXX = g++

CFLAGS += -fPIC -pthread -D_REENTRANT -DLINUX -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE

ifdef DEBUG
 CFLAGS += -g3 -fno-inline -O0 -DDEBUG -Wall
else
 CFLAGS += -O2 -DNDEBUG
endif

ifdef 64
 CFLAGS += -DLINUX_64
endif

LDFLAGS += -lm -lpthread -lxml2 -lrt -ldl

libamsdk: $(SDKOBJS) $(COBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libamsdk.so.3 $(LDFLAGS) $(SDKOBJS) $(COBJS) -o am/source/libamsdk.so.3 $(EXT_LIBS)

libamsdk_static: $(SDKOBJS) $(COBJS)
	@echo "[*** Creating "$@" static library ***]"
	ar -rcuS am/source/libamsdk.a $(SDKOBJS) $(COBJS)
	ranlib am/source/libamsdk.a

	@echo "[*** Creating crypt utility ***]"
	${CC} $(CFLAGS) -UAM_BUILDING_LIB -o am/source/crypt_util am/source/crypt_util.c

apache20i: $(APA20OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libamapc2.so $(LDFLAGS) $(APA20OBJS) -o agents/source/apache/libamapc2.so am/source/libamsdk.a $(EXT_LIBS)
 
apache22i: $(APA22OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libamapc22.so $(LDFLAGS) $(APA22OBJS) -o agents/source/apache22/libamapc22.so am/source/libamsdk.a $(EXT_LIBS)

apache24i: $(APA24OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libamapc24.so $(LDFLAGS) $(APA24OBJS) -o agents/source/apache24/libamapc24.so am/source/libamsdk.a $(EXT_LIBS)

varnishi: $(VRNSHOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libvmod_am.so $(LDFLAGS) -Wl,-rpath,'$$ORIGIN/../lib' -Wl,-rpath,'$$ORIGIN' -z origin $(VRNSHOBJS) -o agents/source/varnish/libvmod_am.so am/source/libamsdk.a $(EXT_LIBS) -Lextlib/$(OS_ARCH)$(OS_MARCH)/varnish$(ARCH)/lib -lapr-1

oiwsi: $(OIWSOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libames6.so $(LDFLAGS) $(OIWSOBJS) -o agents/source/sjsws/libames6.so am/source/libamsdk.a $(EXT_LIBS)

oipsi: $(OPSOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -Wl,-export-dynamic -fPIC -Wl,-soname,libampxy4.so $(LDFLAGS) $(OPSOBJS) -o agents/source/sjsws/libampxy4.so am/source/libamsdk.a $(EXT_LIBS)

dominoi: $(DOMINOOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -shared -z muldefs -Wl,-export-dynamic -fPIC -Wl,-soname,libamdomino.so $(LDFLAGS) $(DOMINOOBJS) -o agents/source/domino/libamdomino.so am/source/libamsdk.a $(EXT_LIBS) extlib/$(OS_ARCH)$(OS_MARCH)/domino/lib/notes0.o extlib/$(OS_ARCH)$(OS_MARCH)/domino/lib/notesai0.o

endif

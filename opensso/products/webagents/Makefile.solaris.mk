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

ifndef	SOLARIS_MK_INCLUDED
SOLARIS_MK_INCLUDED := true
	
CC = suncc
CXX = sunCC

CFLAGS  += -mt -D_REENTRANT -DSOLARIS -D_POSIX_PTHREAD_SEMANTICS

ifdef DEBUG
 CFLAGS += -xO0 -DDEBUG
else
 CFLAGS += -xO3 -DNDEBUG
endif

ifeq ($(OS_MARCH), i86pc)
 OS_MARCH = _i86pc
 CFLAGS += -KPIC
else
 OS_MARCH =
 CFLAGS += -xcode=pic32 
endif

ifdef 64
 LDFLAGS += -m64
endif

LDFLAGS += -G -i -z ignore -z lazyload -z nodefs -z combreloc -lc -lm -lsocket -lnsl -ldl -lrt -lxml2

libamsdk: $(SDKOBJS) $(COBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} -h libamsdk.so.3 $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(SDKOBJS) $(COBJS) -o am/source/libamsdk.so.3 -M am/source/libamsdk.mapfile $(EXT_LIBS)

libamsdk_static: $(SDKOBJS) $(COBJS)
	@echo "[*** Creating "$@" static library ***]"
	ar -rcu am/source/libamsdk.a $(SDKOBJS) $(COBJS)
	ranlib am/source/libamsdk.a

	@echo "[*** Creating crypt utility ***]"
	${CC} $(CFLAGS) -UAM_BUILDING_LIB -o am/source/crypt_util am/source/crypt_util.c

apache20i: $(APA20OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libamapc2.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(APA20OBJS) -o agents/source/apache/libamapc2.so -M agents/source/apache/libamapc.mapfile am/source/libamsdk.a $(EXT_LIBS)

apache22i: $(APA22OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libamapc22.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(APA22OBJS) -o agents/source/apache22/libamapc22.so -M agents/source/apache22/libamapc.mapfile am/source/libamsdk.a $(EXT_LIBS)

apache24i: $(APA24OBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libamapc24.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(APA24OBJS) -o agents/source/apache24/libamapc24.so -M agents/source/apache24/libamapc.mapfile am/source/libamsdk.a $(EXT_LIBS)

ifeq ($(OS_MARCH), i86pc)
varnishi: $(VRNSHOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libvmod_am.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(VRNSHOBJS) -R'$$ORIGIN/vmod_am_lib' -R'$$ORIGIN' -z origin -o agents/source/varnish/libvmod_am.so am/source/libamsdk.a $(EXT_LIBS) -Lextlib/$(OS_ARCH)$(OS_MARCH)/varnish$(ARCH)/lib -lapr-1
else
varnishi:
	@echo "[*** Creating "$@" shared library ***]"
	@echo "Varnish is not supported on SPARC"
endif

oiwsi: $(OIWSOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libames6.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(OIWSOBJS) -o agents/source/sjsws/libames6.so -M agents/source/sjsws/iws_agent.mapfile am/source/libamsdk.a $(EXT_LIBS)

oipsi: $(OPSOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libampxy4.so $(LDFLAGS) -norunpath -library=Cstd -library=Crun -Wl,-C $(OPSOBJS) -o agents/source/proxy40/libampxy4.so -M agents/source/proxy40/libampxy4.mapfile am/source/libamsdk.a $(EXT_LIBS)

ifneq ($(OS_MARCH), i86pc)
dominoi: $(DOMINOOBJS)
	@echo "[*** Creating "$@" shared library ***]"
	${CXX} $(CFLAGS) -h libamdomino.so $(LDFLAGS) -z muldefs -norunpath -library=Cstd -library=Crun -Wl,-C $(DOMINOOBJS) -o agents/source/domino/libamdomino.so am/source/libamsdk.a $(EXT_LIBS) extlib/$(OS_ARCH)$(OS_MARCH)/domino/lib/notes0.o extlib/$(OS_ARCH)$(OS_MARCH)/domino/lib/notesai0.o
else
dominoi:
	@echo "[*** Creating "$@" shared library ***]"
	@echo "Domino is not supported on Solaris x86"
endif

endif

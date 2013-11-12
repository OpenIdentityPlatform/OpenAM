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

# make options:
#  64=1 builds 64bit binary
#  DEBUG=1 builds debug version of binary

AGENT_MAJOR_VER := 4
AGENT_MINOR_VER := 0
AGENT_MICRO_VER := 0
AGENT_RELEASE_NAME := SNAPSHOT

AGENT_VER := $(AGENT_MAJOR_VER).$(AGENT_MINOR_VER).$(AGENT_MICRO_VER)
ifdef $(AGENT_RELEASE_NAME)
 AGENT_VER := $(AGENT_VER)-$(AGENT_RELEASE_NAME)
endif

OS_ARCH := $(shell uname -s)
OS_MARCH = $(shell uname -m)
BUILD_DATE := $(shell date +"%b %d %Y %T")
SVN_REVISION := $(shell svn info . | grep Revision:)
BUILD_MACHINE := constable.internal.forgerock.com
BUILD_DATE_JS := $(shell date +%Y%m%d)

CFLAGS = -g -Iam/source/

ifdef 64
 CFLAGS += -m64
 LDFLAGS = 
 ARCH = _64
else
 CFLAGS += -m32 -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE
 LDFLAGS = 
 ARCH = 
endif

ifeq ($(OS_ARCH), Linux)
 OS_MARCH =
endif

CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/libxml2$(ARCH)/include/libxml2 -Iextlib/$(OS_ARCH)$(OS_MARCH)/pcre$(ARCH)/include/
EXT_LIBS = extlib/$(OS_ARCH)$(OS_MARCH)/pcre$(ARCH)/lib/libpcre.a

WIN_SRC=am/source/connection_win.cpp am/source/xml_attribute_win.cpp am/source/xml_element_win.cpp am/source/xml_tree_win.cpp

SDKSRCS=$(filter-out $(WIN_SRC), $(wildcard am/source/*.cpp))
SDKOBJS=$(SDKSRCS:.cpp=.o)
CSRCS=$(wildcard am/source/*.c)
COBJS=$(CSRCS:.c=.o)
APA20SRCS=$(wildcard agents/source/apache/*.c)
APA20OBJS=$(APA20SRCS:.c=.o)
APA22SRCS=$(wildcard agents/source/apache22/*.c)
APA22OBJS=$(APA22SRCS:.c=.o)
APA24SRCS=$(wildcard agents/source/apache24/*.c)
APA24OBJS=$(APA24SRCS:.c=.o)
OIWSSRCS=$(wildcard agents/source/sjsws/*.c)
OIWSOBJS=$(OIWSSRCS:.c=.o)
OPSSRCS=$(wildcard agents/source/proxy40/*.c)
OPSOBJS=$(OPSSRCS:.c=.o)
VRNSHSRCS=$(wildcard agents/source/varnish/*.c)
VRNSHOBJS=$(VRNSHSRCS:.c=.o)
DOMINOSRCS=$(wildcard agents/source/domino/*.c)
DOMINOOBJS=$(DOMINOSRCS:.c=.o)
	
$(APA20OBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/apache/include -DAPACHE2 -DEAPI -DXP_UNIX -DCORE_PRIVATE
$(APA22OBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/apache22/include -DAPACHE2 -DEAPI -DXP_UNIX -DCORE_PRIVATE
$(APA24OBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/apache24/include -DAPACHE2 -DEAPI -DXP_UNIX -DCORE_PRIVATE
$(VRNSHOBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/varnish$(ARCH)/include -Iagents/source/varnish -DVARNISH303
$(OIWSOBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/sjsws/include -DXP_UNIX
$(OPSOBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/proxy40/include -DXP_UNIX
$(DOMINOOBJS): CFLAGS += -Iextlib/$(OS_ARCH)$(OS_MARCH)/domino/include -DUNIX -D_REENTRANT -DUSE_THREADSAFE_INTERFACES
	
ifeq ($(OS_ARCH), Linux)
 include Makefile.linux.mk
else
 include Makefile.solaris.mk
endif

%.o: %.cpp
	@echo "[*** Compiling "$<" ***]"
	$(CXX) -c $(CFLAGS) $< -o $@

%.o: %.c
	@echo "[*** Compiling "$<" ***]"
	$(CC) -c $(CFLAGS)  $< -o $@


version:
	@echo "[***** Updating version.h *****]"
	rm -fr am/source/version.h
	sed -e 's%BUILD_MACHINE%$(BUILD_MACHINE)%g' \
	   -e 's%SVN_REVISION%$(SVN_REVISION)%g' \
	   -e 's%AGENT_VER%$(AGENT_VER)%g' \
	   -e 's%AGENT_ER_VER%%g' \
           -e 's%AGENT_FVB_MARKER%%g' < am/source/version.template > am/source/version.h
	@echo "[***** Updating .version *****]"
	rm -fr .version
	sed -e 's%BUILD_DATE%$(BUILD_DATE)%g' \
	   -e 's%BUILD_MACHINE%$(BUILD_MACHINE)%g' \
	   -e 's%SVN_REVISION%$(SVN_REVISION)%g' \
	   -e 's%AGENT_VER%$(AGENT_VER)%g' \
	   -e 's%AGENT_ER_TEXT%%g' \
           -e 's%AGENT_FVB_TEXT%%g' < version.template > .version

all: version libamsdk_static libamsdk apache20i apache22i apache24i varnishi oiwsi oipsi dominoi

apache: version libamsdk_static libamsdk apache20i
apache22: version libamsdk_static libamsdk apache22i
apache24: version libamsdk_static libamsdk apache24i
sjsws: version libamsdk_static libamsdk oiwsi
proxy40: version libamsdk_static libamsdk oipsi
varnish: version libamsdk_static libamsdk varnishi
domino: version libamsdk_static libamsdk dominoi
	
clean:
	rm -fr built/dist built/apache built/apache22 built/apache24 built/proxy40 built/sjsws built/varnish am/source/version.h am/source/*.o agents/source/domino/*.so agents/source/domino/*.o agents/source/apache22/*.o agents/source/apache/*.o agents/source/proxy40/*.so agents/source/proxy40/*.o agents/source/sjsws/*.so agents/source/sjsws/*.o agents/source/apache24/*.o agents/source/apache/*.so agents/source/apache22/*.so agents/source/apache24/*.so agents/source/varnish/*.so agents/source/varnish/*.o am/source/*.so am/source/*.so.3 am/source/*.a am/source/crypt_util

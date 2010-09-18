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
# Makefile for building sample agent
#

OS_ARCH := $(shell uname -s)

ifeq ($(strip $(findstring WOW64,$(OS_ARCH))), WOW64)
    CYGWIN_ARCH := WOW64
endif
ifeq ($(strip $(patsubst CYGWIN_NT%, CYGWIN_NT, $(OS_ARCH))), CYGWIN_NT)
    OS_ARCH := WINNT
    OS_IS_CYGWIN := true
endif
ifeq ($(OS_ARCH), Windows_NT)
    OS_ARCH := WINNT
endif


ifndef AM_INCLUDE_DIR
	AM_INCLUDE_DIR := ../include
endif
ifndef AM_LIB_DIR
ifeq ($(OS_ARCH), WINNT)
	AM_LIB_DIR := ../bin
else
	AM_LIB_DIR := ../lib
endif
endif

#
# Default targets
#
PROGRAMS_NAMES = am_policy_test am_auth_test am_sso_test am_log_test
PROGRAMS := $(patsubst %,%$(EXE_EXT),$(PROGRAM_NAMES))

#
# Source Files
#
SRCS = am_policy_test.c am_auth_test.c am_sso_test.c am_log_test.c

#
# Object Files
#
ifeq ($(OS_ARCH), WINNT)
OBJS = $(patsubst %.c, %.obj, $(filter %.c,$(SRCS)))
else
OBJS = $(patsubst %.c, %.o, $(filter %.c,$(SRCS)))
endif

#
# Compiler to be used
#
ifeq ($(OS_ARCH), Linux)
CC = g++
else
ifeq ($(OS_ARCH), AIX)
CC = xlC_r
else
ifeq ($(OS_ARCH), HP-UX)
CC = aCC
else
CC = cc
endif
endif
endif
ifdef   OS_IS_CYGWIN
CC=cl
LINK=cl
endif

#
# Libs to be used
#
ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
LIBS = amsdk.lib libxml2.lib ssl3.lib nss3.lib libplc4.lib libplds4.lib libnspr4.lib
else
LIBS = -lamsdk -llibxml2 -lssl3 -lnss3 -llibplc4 -llibplds4 -llibnspr4
endif
else
ifeq ($(OS_ARCH), Linux)
LIBS = -lamsdk -lxml2 -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4
else
ifeq ($(OS_ARCH), HP-UX)
LIBS = -lamsdk -lxml2 -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4
else
ifeq ($(OS_ARCH), AIX)
LIBS = -lamsdk -lxml2 -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4
else
ifeq ($(BUILD_VERSION), 64)
LIBS = -lamsdk -lxml2 \
	-L /usr/lib/mps/64 -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4
else
LIBS = -lamsdk -lxml2 \
	-L /usr/lib/mps -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4 \
	-L /usr/ucblib -lucb
endif
endif
endif
endif
endif

#
# Compiler flags
#
ifeq ($(OS_ARCH), HP-UX)
CFLAGS = -I$(AM_INCLUDE_DIR) -DHPUX
else
ifeq ($(OS_ARCH), AIX)
CFLAGS = -I$(AM_INCLUDE_DIR) -DAIX
else
CFLAGS = -I$(AM_INCLUDE_DIR)
endif
endif
LDFLAGS = -L$(AM_LIB_DIR) $(LIBS) 
ifeq ($(OS_ARCH), WINNT)
CFLAGS += -DWINNT
ifdef   OS_IS_CYGWIN
	LDFLAGS = -LIBPATH:$(AM_LIB_DIR) $(LIBS) 
endif
else
ifeq ($(OS_ARCH), Linux)
CFLAGS += -g -Wall -DLINUX
else
ifeq ($(BUILD_VERSION), 64)
CFLAGS += -g -xO3 -DNDEBUG -DSOLARIS -mt -KPIC -fast -xarch=generic64
else
CFLAGS += -g
endif
endif
endif

#
# Make C programs
#
MAKE_C_PROGRAM = $(LINK.c) $(OUTPUT_OPTION) $^ 

ifeq ($(OS_ARCH), WINNT)
ifdef   OS_IS_CYGWIN
MAKE_C_PROGRAM = $(LINK) $(LDFLAGS) $^ $(LIBS)
endif
endif

ifeq ($(OS_ARCH), WINNT)
EXE_EXT := .exe
OBJ_EXT := obj
else
OBJ_EXT := o
endif

PROGRAM_NAMES := am_auth_test am_sso_test am_policy_test am_log_test
PROGRAMS := $(patsubst %,%$(EXE_EXT),$(PROGRAM_NAMES))

all: $(PROGRAMS)

ifeq ($(OS_ARCH), WINNT)
%.obj: %.c
	$(CC) $(CFLAGS) -c -o $@ $*.c
else
%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $*.c
endif

am_auth_test$(EXE_EXT): am_auth_test.$(OBJ_EXT)
	$(MAKE_C_PROGRAM)

am_sso_test$(EXE_EXT): am_sso_test.$(OBJ_EXT)
	$(MAKE_C_PROGRAM)

am_policy_test$(EXE_EXT): am_policy_test.$(OBJ_EXT)
	$(MAKE_C_PROGRAM)

am_log_test$(EXE_EXT): am_log_test.$(OBJ_EXT)
	$(MAKE_C_PROGRAM)

am_web_agent_test$(EXE_EXT): am_web_agent_test.$(OBJ_EXT)
	$(MAKE_C_PROGRAM)

#
# Clean target
#
clean:
	rm -f $(OBJS) $(PROGRAMS)


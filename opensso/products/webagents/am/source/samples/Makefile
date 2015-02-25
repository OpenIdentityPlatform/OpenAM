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
# Portions Copyrighted 2013 ForgeRock Inc

# make options:
#  64=1 builds 64bit binary
#  DEBUG=1 builds debug version of binary

OS_ARCH := $(shell uname -s)
OS_MARCH = $(shell uname -m)
DIR := ${CURDIR}
LIBS := -L${DIR}/../lib -lamsdk
CFLAGS := -I../include

ifeq ($(OS_ARCH),  SunOS)
    CC := suncc
    LIBS += '-R$$ORIGIN/../lib'
    ifeq ($(OS_MARCH), i86pc)
     CFLAGS += -KPIC
    else
     CFLAGS += -xcode=pic32 
    endif
    CFLAGS  += -mt -D_REENTRANT -DSOLARIS -D_POSIX_PTHREAD_SEMANTICS
    ifdef DEBUG
     CFLAGS += -g -xO0 -DDEBUG
    else
     CFLAGS += -g -xO3 -DNDEBUG
    endif
endif
ifeq ($(OS_ARCH), Linux)
    CC := gcc
    LIBS += -Xlinker '-R$$ORIGIN/../lib'
    CFLAGS += -fPIC -pthread -D_REENTRANT -DLINUX -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE
    ifdef 64
     CFLAGS += -DLINUX_64
    endif
    ifdef DEBUG
     CFLAGS += -g3 -fno-inline -O0 -DDEBUG -Wall
    else
     CFLAGS += -g -O2 -DNDEBUG
    endif
endif

ifdef 64
 CFLAGS += -m64
endif

%.o: %.c
	@echo "[*** Compiling "$<" ***]"
	$(CC) -c -o $@ $< $(CFLAGS)
	
am_sso_test_OBJS := am_sso_test.o
am_auth_test_OBJS := am_auth_test.o
am_policy_test_OBJS := am_policy_test.o
am_log_test_OBJS := am_log_test.o
am_web_agent_test_OBJS := am_web_agent_test.o

all: am_auth_test am_sso_test am_policy_test am_log_test am_web_agent_test

am_auth_test: $(am_auth_test_OBJS)
	if [ ! -f "${DIR}/../lib/libamsdk.so" ]; then ln -s ${DIR}/../lib/libamsdk.so.3 ${DIR}/../lib/libamsdk.so;  fi
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

am_sso_test: $(am_sso_test_OBJS)
	if [ ! -f "${DIR}/../lib/libamsdk.so" ]; then ln -s ${DIR}/../lib/libamsdk.so.3 ${DIR}/../lib/libamsdk.so;  fi
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

am_policy_test: $(am_policy_test_OBJS)
	if [ ! -f "${DIR}/../lib/libamsdk.so" ]; then ln -s ${DIR}/../lib/libamsdk.so.3 ${DIR}/../lib/libamsdk.so;  fi
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

am_log_test: $(am_log_test_OBJS)
	if [ ! -f "${DIR}/../lib/libamsdk.so" ]; then ln -s ${DIR}/../lib/libamsdk.so.3 ${DIR}/../lib/libamsdk.so;  fi
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

am_web_agent_test: $(am_web_agent_test_OBJS)
	if [ ! -f "${DIR}/../lib/libamsdk.so" ]; then ln -s ${DIR}/../lib/libamsdk.so.3 ${DIR}/../lib/libamsdk.so;  fi
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

clean:
	rm -rf am_auth_test am_sso_test am_policy_test am_web_agent_test *.o
	

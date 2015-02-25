#
# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2014 ForgeRock AS.
#

AGENT_VER = 
BUILD_MACHINE = constable.internal.forgerock.com

UNAME = $(shell uname)

ifeq ($(UNAME), Linux)
CC = gcc
CFLAGS = -fPIC -fno-common -D_REENTRANT -DAM_BUILDING_LIB -DLINUX_64 -m64
LDFLAGS = -m64 -Wl,-rpath,'$$ORIGIN/../lib' -Wl,-rpath,'$$ORIGIN' -rdynamic -z origin -lpthread -lc -lm -lrt -ldl
OPT = -O2
ifdef DEBUG
 CFLAGS += -g3 -fno-inline -O0 -DDEBUG -Wall
else
 CFLAGS += -g -O2 -DNDEBUG
endif
endif

CFLAGS += -Iinclude -I../../extlib/Linux/ncurses_Linux_64/include/ncurses -I../../extlib/Linux/ncurses_Linux_64/include
LDFLAGS += -L../../extlib/Linux/ncurses_Linux_64/lib -lmenu -lform -lncurses

SVN_REVISION = $(shell svn info . | grep Revision:)
ifeq ($(strip $(SVN_REVISION)),)
SVN_REVISION = Revision:
endif
	
SOURCES = $(wildcard source/*.c) $(wildcard ../../am/source/crypt_util.c)
OBJECTS = $(SOURCES:.c=.o)

.c.o:
	$(CC) -c $(OPT) $(CFLAGS) $< -o $@

all: clean version agentadmin
	
version:
	@echo "*********** Updating version.h ***********"
	sed -e 's%BUILD_MACHINE%$(BUILD_MACHINE)%g' \
	   -e 's%SVN_REVISION%$(SVN_REVISION)%g' \
	   -e 's%AGENT_VER%$(AGENT_VER)%g' < include/version.template > include/version.h

agentadmin: $(OBJECTS)
	@echo "*********** Creating Agent Installer ***********"
	$(CC) $(SHARED) $(OBJECTS) -o agentadmin $(LDFLAGS)

clean:
	rm -rf include/version.h source/*.o agentadmin ../../am/source/crypt_util.o

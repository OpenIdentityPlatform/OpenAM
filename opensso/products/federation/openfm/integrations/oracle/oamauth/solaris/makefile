#
# Makefile for the sample Oracle NetPoint Authentication Plugin
#
# Please customize to your environment
#
# Note: this makefile expects to be in 
#      /INSTALLDIR/sdk/authentication/samples/general/
#

CC = /tools/ns/bin/gcc-2.95.2
RM = rm -f
AUTHN_INCLUDE_DIR =include

OBJS = authn_api.o
DSO  = authn_api.so

CC_FLAGS = -I$(AUTHN_INCLUDE_DIR) -c -KPIC

GCC_FLAGS = -I$(AUTHN_INCLUDE_DIR) -c -fPIC -Wall 
LIBS =  -lamsdk -lxml2 \
	-L /usr/lib/mps -lssl3 -lnss3 -lplc4 -lplds4 -lnspr4 \
	-L /usr/ucblib -lucb

CFLAGS=     $(GCC_FLAGS)

LDFLAGS = -L lib $(LIBS)

$(DSO): $(OBJS) 
	$(CC) -G $(OBJS) $(LDFLAGS) -o $(DSO)

$(OBJS): authn_api.c
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(OBJS) authn_api.c


clean:
	$(RM) $(OBJS) $(DSO)

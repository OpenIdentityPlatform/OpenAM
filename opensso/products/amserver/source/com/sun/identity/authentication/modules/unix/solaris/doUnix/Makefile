# Copyright © 2005 Sun Microsystems, Inc.  All rights reserved.
#
# Sun Microsystems, Inc. has intellectual property rights relating to
# technology embodied in the product that is described in this document.
# In particular, and without limitation, these intellectual property rights
# may include one or more of the U.S. patents listed at
# http://www.sun.com/patents and one or more additional patents or pending
# patent applications in the U.S. and in other countries.
#
# U.S. Government Rights - Commercial software.  Government users are subject
# to the Sun Microsystems, Inc. standard license agreement and applicable
# provisions of the FAR and its supplements.
#
# Use is subject to license terms.
#
# This distribution may include materials developed by third parties.Sun,
# Sun Microsystems and  the Sun logo are trademarks or registered trademarks
# of Sun Microsystems, Inc. in the U.S. and other countries.  
#
# Copyright © 2005 Sun Microsystems, Inc. Tous droits réservés.
# Sun Microsystems, Inc. détient les droits de propriété intellectuels relatifs
# à la technologie incorporée dans le produit qui est décrit dans ce document.
# En particulier, et ce sans limitation, ces droits de propriété
# intellectuelle peuvent inclure un ou plus des brevets américains listés
# à l'adresse http://www.sun.com/patents et un ou les brevets supplémentaires
# ou les applications de brevet en attente aux Etats - Unis et dans les
# autres pays.
#
# L'utilisation est soumise aux termes du contrat de licence.
#
# Cette distribution peut comprendre des composants développés par des
# tierces parties.
#
# Sun,  Sun Microsystems et  le logo Sun sont des marques de fabrique ou des
# marques déposées de Sun Microsystems, Inc. aux Etats-Unis et dans
# d'autres pays.
# hpux-dev - added targets for HP-UX
USERX_ROOT = ../../../../../../../..
include $(USERX_ROOT)/arch/components.mk
include $(USERX_ROOT)/arch/defines.mk
include $(USERX_ROOT)/arch/rules.mk


CFLAGS = -c
ifeq ($(OS_ARCH), Linux)
    CFLAG = -DLINUX
    LDLIBFLAGS =  -lnsl -lpam -lcrypt
    OBJS  = debug_utils.o sr_subrs.o unixyp_passwd.o
else
#####hpux-dev#####
ifeq ($(OS_ARCH), HP-UX)
    CFLAG = -DHPUX +DAportable
    LDLIBFLAGS = -lsec -lnsl -lpam -lcrypt
    OBJS  = debug_utils.o sr_subrs.o unixyp_passwd.o doUnix.o
else
    CFLAG = -DSOLARIS
    LDLIBFLAGS = -lpam -lsocket -lnsl -lpam -lbsm
    OBJS  = debug_utils.o sr_subrs.o unixyp_passwd.o doUnix.o
endif
endif

EXES = doUnix
ifeq ($(OS_ARCH), SunOS)
MSGFILE = dounix_msgs.po
endif


all:		$(EXES)

doUnix: $(OBJS) doUnix.o
	$(CC) -I. $(CFLAG) -o doUnix doUnix.c $(OBJS) $(LDLIBFLAGS)
ifeq ($(OS_ARCH), SunOS)
	msgfmt -v -o am_auth_unix_keys.mo $(MSGFILE)
endif

.c.o:
	$(CC) $(CFLAGS) $(CFLAG) $<

clean:
		$(RM) $(OBJS) $(EXES) am_auth_unix_keys.mo

install:
	@test -d ${ARCHROOTDIR} || \
		(mkdir ${ARCHROOTDIR})
	@test -d ${PKGROOTDIR} || \
		(mkdir ${PKGROOTDIR})
	@test -d ${PKGPRODROOTDIR} || \
		(mkdir ${PKGPRODROOTDIR})
	@test -d ${PRODROOTDIR} || \
		(mkdir ${PRODROOTDIR})
	@test -d ${PRODSBINDIR} || \
		(mkdir ${PRODSBINDIR})
	@echo

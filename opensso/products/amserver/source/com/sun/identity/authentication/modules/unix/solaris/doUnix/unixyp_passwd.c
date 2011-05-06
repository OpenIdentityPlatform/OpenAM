/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: unixyp_passwd.c,v 1.2 2008/09/04 23:34:55 bigfatrat Exp $
 *
 */


/*
 * unixyp_passwd.c: simple program for hashing a clear text password
 * 	and comparing it to its "known" hashed value
 *
 * Usage:   unixyp_passwd (userid, clear_passwd)
 * Returns: 0  if good password
 *          !0 if password mismatch or can't find username
 */


#include <sys/types.h>
#include <stdio.h>
#include <string.h>
#include <sys/signal.h>
#include <stdlib.h>
#include <shadow.h>
#include <errno.h>

#ifdef LINUX
#include <pthread.h>
#define _XOPEN_SOURCE 
#include <unistd.h>
#else
#ifdef HPUX		/* hpux-dev */
#include <pthread.h>
#include <unistd.h>
#else
#include <thread.h>
#include <synch.h>
#endif
#endif


#define MAX_STRING_LEN 256
//hpux-dev
#if defined(LINUX) || defined(HPUX)
    static pthread_mutex_t lock =PTHREAD_MUTEX_INITIALIZER;
#else
    static mutex_t lock = DEFAULTMUTEX;
#endif

#ifdef HPUX		/* hpux-dev */
    static pthread_mutex_t lock_spnam = PTHREAD_MUTEX_INITIALIZER;
#endif

#ifndef HPUX		/* hpux-dev */
char *crypt(char *pw,char *salt);
#endif

int check_password(char *pw,char *e_pw) {

	char *cpw;
	int result = -1;
	#if defined(LINUX) || defined(HPUX)		/* hpux-dev */
	    pthread_mutex_lock(&lock);
	#else
	    mutex_lock(&lock);
	#endif
	cpw = crypt(pw,e_pw);
	result = strncmp(cpw,e_pw,MAX_STRING_LEN);
	#if defined(LINUX) || defined(HPUX)		/* hpux-dev */
	    pthread_mutex_unlock(&lock);
	#else
	    mutex_unlock (&lock);
	#endif
	return result;
}

get_enc_passwd (char *usernamep, char *encpwdp)
{
	struct spwd spwd, *spwdp;
	struct spwd tmpbuf;
	int buflen;
	extern int errno;

	#ifdef LINUX
	    getspnam_r(usernamep, &spwd, (char *)&tmpbuf, sizeof(struct spwd),&spwdp);
	#else 
	#ifdef HPUX		/* hpux-dev */
	   pthread_mutex_lock(&lock_spnam);
	   spwdp = getspnam(usernamep);
	   pthread_mutex_unlock(&lock_spnam);
	#else 
	    spwdp = getspnam_r(usernamep, &spwd, (char *)&tmpbuf, sizeof(struct spwd));
	#endif
	#endif

	if (spwdp != NULL) {
		strcpy (encpwdp, spwd.sp_pwdp);
		return (0);
	} else {
		*encpwdp = 0x00;
		return (-1);
	}
}

int
check_unix_passwd (char *usernamep, char *clear_passwordp)
{
	char username[MAX_STRING_LEN];
	char enc_passwd[MAX_STRING_LEN];
	int i;

	if (usernamep == NULL || clear_passwordp == NULL || *clear_passwordp == 0x00) {
		return(-1);
	}

	i = get_enc_passwd (usernamep, enc_passwd);
	if (i) {	/* can't find user in database */
		return (-2);
	}

	i = check_password (clear_passwordp, enc_passwd);
	return (i);
}


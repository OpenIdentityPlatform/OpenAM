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
 * $Id: debug_utils.c,v 1.2 2008/09/04 23:34:55 bigfatrat Exp $
 *
 */


#include <stdio.h>
#include <sys/time.h>
#include <time.h>

#if defined(LINUX) || defined(HPUX)
#define DEBUG_FILE "/var/opt/sun/identity/debug/unix_client.debug"
#else
#define DEBUG_FILE "/var/opt/SUNWam/debug/unix_client.debug"
#endif

#if defined(LINUX ) || defined(HPUX)
#include <sys/types.h>
#endif

char
get_ascii (char c)
{
	char cc = '*';

	if ((c >= ' ') && (c <= '~'))
		cc = c;
	return (cc);
}

void
hexdump (FILE *fp, u_char *buf, int len)
{
	int i, j = 0, k;
	u_char uc;
	char interp[16];

	if (fp) {
		fprintf (fp, "    ");
	} else {
		printf ("    ");
	}
	for (i = 0; i < len; i++) {
		uc = *buf;
		buf++;
		if (fp) {
			fprintf (fp, "%02x ", uc);
		} else {
			printf ("%02x ", uc);
		}
		interp[j] = (char)uc;
		if (++j >= 16) {
			if (fp) {
				fprintf (fp, "\t");
			} else {
				printf ("\t");
			}
			for (j = 0; j < 16; j++) {
				if (fp) {
					fprintf (fp, "%c", get_ascii(interp[j]));
				} else {
					printf ("%c", get_ascii(interp[j]));
				}
			}
			if (fp) {
				fprintf (fp, "\n    ");
			} else {
				printf ("\n    ");
			}
			j = 0;
		}
	}
	if (j) {
		i = 16-j;
		for (k = 0; k < i; k++) {
			if (fp) {
				fprintf (fp, "   ");
			} else {
				printf ("   ");
			}
		}
		if (fp) {
			fprintf (fp, "\t");
		} else {
			printf ("\t");
		}
		for (k = 0; k < j; k++) {
			if (fp) {
				fprintf (fp, "%c", get_ascii(interp[k]));
			} else {
				printf ("%c", get_ascii(interp[k]));
			}
		}
	}
	if (fp) {
		fprintf (fp, "\n");
	} else {
		printf ("\n");
	}
}


void
get_time_str (char *cp, int maxsz)
{
	struct tm *tm;
	struct timeval time;

	#ifdef SOLARIS
	char *nullp = NULL;

	if (gettimeofday (&time, nullp))
		printf ("gettimeofday error%c", 10);
        #endif
	#if defined(LINUX) || defined(HPUX)
	struct timezone *nullp = NULL;

	if (gettimeofday (&time, nullp))
		printf ("gettimeofday error%c", 10);
        #endif
	tm = localtime (&time.tv_sec);
	strftime (cp, maxsz, "%x %X", tm);
}

/*
 *  if DEBUG_FILE exists, then add trace stuff to it,
 *  otherwise, just return.
 */

void
debug_trace(char *stuff)
{
	FILE *foo;
	char datetime[18];

	foo = fopen(DEBUG_FILE, "r");
	if (!foo) return;
	fclose(foo);

	get_time_str (datetime, sizeof(datetime));
	foo = fopen(DEBUG_FILE, "a");
	fprintf(foo, "%s: %s", datetime, stuff);
	fflush(foo);
	fclose(foo);
	return;
}

void
debug_trace_dump(char *stuff, char *bufp, int buflen)
{
	FILE *foo;
	char datetime[18];

	foo = fopen(DEBUG_FILE, "r");
	if (!foo) return;
	fclose(foo);

	get_time_str (datetime, sizeof(datetime));
	foo = fopen(DEBUG_FILE, "a");
	fprintf(foo, "%s: %s", datetime, stuff);
	fprintf(foo, "\ndata:\n");
	hexdump (foo, (u_char *)bufp, buflen);
	fflush(foo);
	fclose(foo);
	return;
}


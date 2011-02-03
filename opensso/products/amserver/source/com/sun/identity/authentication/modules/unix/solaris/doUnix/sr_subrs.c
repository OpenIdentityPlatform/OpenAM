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
 * $Id: sr_subrs.c,v 1.2 2008/09/04 23:34:55 bigfatrat Exp $
 *
 */


#include <stdarg.h>
#include <errno.h>
#include <limits.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/types.h>
//#####hpux-dev#####
#ifndef HPUX
#include <sys/select.h>
#endif
#include <sys/socket.h>
//#####hpux-dev#####
#ifdef HPUX
#include <syslog.h>
#else
#include <sys/syslog.h>
#endif
#include <netdb.h>
#include <fmtmsg.h>
#include <sys/resource.h>
#include <sys/time.h>
#include "sr_unix.h"
#include "sr_extern.h"

#define HACK 1
#define MAX_FD 256

int auth_rqt_fd = -1;
u_short listen_port = DOUNIX_LISTEN_PORT;
int number_ipaddrs = 0;
char *accept_ipaddrs;
int open_auth_listen_port(u_short lport);
/*
 *  ripped off from EFS 2.0
 */

void
a_syslog (int prio, const char *fmt, ...) {
	FILE *fP;
	int prim = prio & LOG_PRIMASK;
	va_list val;
	int i;
	char tmpbuf[256];

	switch (prim) {
		case LOG_DEBUG:
			if (verbose <= 1)
				return;		/* DEBUG only if verbose > 1 */
			break;
		case LOG_INFO:
			if (verbose == 0)
				return;		/* INFO only if verbose */
			break;
	}
	va_start(val, fmt);
	if (prim <= LOG_NOTICE) {
		fP = stderr;
		vsprintf(tmpbuf, fmt, val);
		i = fmtmsg (MM_SOFT|MM_APPL|MM_CONSOLE|MM_NRECOV,
			"SUNWamsvc:amunixd", MM_ERROR, tmpbuf, "Restart Access Manager", "No documentation");
	} else
		fP = stdout;
	vfprintf(fP, fmt, val);
	va_end(val);
	fputc('\n', fP);
}

/*
 *  ripped off from EFS 2.0
 */

int
get_args (int argc, char **argvP) {
	int o;
	int addrcnt = 0;
	char *cp;
	while ((o = getopt(argc, argvP, "vc:i:a:")) != EOF) {
		switch (o) {
		case 'v':		/* verbose */
			++verbose;
			break;
		case 'c':		/* config listening port number */
			listen_port = (u_short)strtoul(optarg, (char **)0, 0);
			break;
		case 'i':
			if (strlen(optarg) == 0) {
			    sprintf (debug_buf, "%s: no number of IP addresses specified\n",progname);
			    debug_trace (debug_buf);
			    printf ("%s", debug_buf);
			    a_syslog(AUTH_FAC|LOG_ERR, debug_buf);
			    return -22;
			} 
			number_ipaddrs = (int)strtoul(optarg, (char **)0, 0);
			if (errno) {
			    sprintf (debug_buf,
				"%s: invalid number of IP addresses specified: %s\n", progname,optarg);
			    debug_trace (debug_buf);
			    printf ("%s", debug_buf);
			    a_syslog(AUTH_FAC|LOG_ERR, debug_buf);
			    return -23;
			}
			/*
			 *  max chars for ipaddr ("111.111.111.111") is 15;
			 *  allocate one more byte for terminating null.
			 */

			if ((accept_ipaddrs = (char *)calloc(number_ipaddrs, 16)) == (char *)0) {
			    a_syslog(AUTH_FAC|LOG_ERR
		  		, "%s: unable to allocate %d IP Address elements"
		  		, progname, number_ipaddrs);
			    return -22;
			}
			break;
		case 'a':
			if (strlen(optarg) == 0) {
			    sprintf (debug_buf, "%s: no IP address specified\n",progname);
			    debug_trace (debug_buf);
			    printf ("%s", debug_buf); 
			    a_syslog(AUTH_FAC|LOG_ERR, debug_buf);
			    return -24;
			}
			if ((number_ipaddrs <= 0) || (addrcnt >= number_ipaddrs)) {
			    a_syslog(AUTH_FAC|LOG_ERR,
			        "Usage: %s [-v] [-c portnum] [-i <N> -a ipaddr1 ... -a ipaddrN>]",
			        progname);
			    return -20;
			}
			if (strlen(optarg) > 15) {	/* "111.111.111.111" */
			    sprintf (debug_buf, "%s: invalid IP address length specified: %s\n", progname,optarg);
			    debug_trace (debug_buf);
			    a_syslog(AUTH_FAC|LOG_ERR, debug_buf);
			    return -21;
			}
			cp = &accept_ipaddrs[16*addrcnt++];
			strcpy (cp, optarg);
			break;
		default:
			a_syslog(AUTH_FAC|LOG_ERR,
			    "Usage: %s [-v] [-c portnum] [-i <N> -a ipaddr1 ... -a ipaddrN>]",
			    progname);
			return -2;
		}
	}

	if (number_ipaddrs > addrcnt) {
	    if (verbose) {
		sprintf (debug_buf,
		    "%s: %d allowed ipaddrs indicated, %d supplied; adjust to %d\n",
		    progname,number_ipaddrs, addrcnt, addrcnt);
		debug_trace (debug_buf);
	    }
	    number_ipaddrs = addrcnt;
	}

/*	if (number_ipaddrs > 0) { */
/*	    for (o = 0; o < number_ipaddrs; o++) { */
/*		cp = &accept_ipaddrs[16*o]; */
/*		printf ("IP address[%d] = %s\n", o, cp); */
/*	    } */
/*	} */

	if (verbose) {
		sprintf (debug_buf, "%s: verbose, config listening port = %u\n",
			progname, listen_port);
		debug_trace (debug_buf);
		a_syslog(AUTH_FAC|LOG_INFO, "%s", debug_buf);
		if (number_ipaddrs > 0) {
		    for (o = 0; o < number_ipaddrs; o++) {
			cp = &accept_ipaddrs[16*o];
			sprintf (debug_buf, "IP address[%d] = %s\n", o, cp);
			debug_trace (debug_buf);
		    }
		}
	}
	return 0;
}

int
sr_readline (char *buf, size_t bufsiz) {
	char *cP;
	int c, ign = 0, readmore, i;
	u_int len = bufsiz - 1;
	extern void sr_server_chld ();

	memset (buf, 0, bufsiz);

	cP = buf;
	do {
		readmore = 0;
		i = read(0, cP, bufsiz);
		if (i == -1 && errno == EINTR) {
			continue;
		} else if (i == 0) {
			return EOF;
		}
	} while ((i == -1 && errno == EINTR) || readmore);

	/*
	 *  trim any trailing <cr><lf>
	 */
	
	if ((cP[i-1] == 0x0a) || (cP[i-1] == 0x0d)) {
		cP[i-1] = 0x00;
		if ((cP[i-2] == 0x0a) || (cP[i-2] == 0x0d)) {
			cP[i-2] = 0x00;
		}
	}
	return (strlen(cP));
}


/* this is in the child - we only give each session so long to do its
   business and leave - otherwise ... */
void
sr_server_alarm (int sig) {
	sprintf (debug_buf, "sr_server_alarm received signal %d\n", sig);
	debug_trace (debug_buf);
	terminate = 1;
}

void
sr_server_sighup (int sig) {
	sprintf (debug_buf, "sr_server_sighup received signal %d\n", sig);
	debug_trace (debug_buf);
	return;
}

void
sr_server_sigpipe (int sig) {
	sprintf (debug_buf, "sr_server_sigpipe received signal %d\n", sig);
	debug_trace (debug_buf);
	return;
}

void
sr_server_chld (int sig) {
	unsigned u;
	pid_t pid = wait((int *)0);

	sprintf (debug_buf, "sr_server_chld received signal %d\n", sig);
	debug_trace (debug_buf);
	for (u = 0; u < n_procs; ++u) {
		if (procs[u] == pid) {
			procs[u] = 0;
			break;
		}
	}
}

void
sr_server_term (int sig) {
	sprintf (debug_buf, "sr_server_term received signal %d\n", sig);
	debug_trace (debug_buf);
	if (sig != SIGHUP)
		terminate = 1;
}

void
sr_sig_default (int sig) {
	sprintf (debug_buf, "sr_sig_default received signal %d\n", sig);
	debug_trace (debug_buf);
	return;
}

/*
 *  Note: 'lport' is listening port, in host order
 *
 *  Note also: this function forks - it only returns in the child
 *  processes - the parent stays in here, listening for connections
 */

int
allowed_ipaddress (char *hostipp) {
    int i, i2, i3;
    char *cp;

    i3 = strlen (hostipp);
/*    printf ("allowed_ipaddress: hostip addr = %s, len = %d\n", hostipp, i3); */
    for (i = 0; i < number_ipaddrs; i++) {
	cp = &accept_ipaddrs[16*i];
	i2 = strlen(cp);
/*	printf ("allowed_ipaddress: checking i2=%d, cp=%s, hostipp=%s\n",
		i2, cp, hostipp); */
	if ((i3 == i2) && (strcmp(cp, hostipp) == 0)) {
	    return (1);
	}
    }
    return (0);
}

int
sr_server () {
	int f, ls = -1, n, on, s, i, rlim_cur, i2;
	u_int u;
	char *what;
	char *cp;
	char rmtaddr[16];
	struct sockaddr_in sin;
	struct rlimit rl, *rlp = &rl;
	fd_set readfds, exceptfds;
	short si;
	extern int config_fd;
	(void) signal(SIGHUP, sr_server_term);
	(void) signal(SIGINT, sr_server_term);
	(void) signal(SIGTERM, sr_server_term);
	(void) sigset(SIGCHLD, sr_server_chld);
	(void) sigset(SIGPIPE, sr_server_sigpipe);

	ls = auth_rqt_fd;

	if ((procs = (int *)calloc(n_procs, sizeof(int))) == (int *)0) {
		a_syslog(AUTH_FAC|LOG_ERR
		  , "%s: unable to allocate %u proc state elements"
		  , progname, n_procs);

                a_syslog(AUTH_FAC|LOG_ERR, "%s: server socket %s failed: %s"
                  , progname, what, strerror(errno));
                if (ls != -1)
                        close(ls);
                return -2;

	}

	for (u = 0; u < n_procs; ++u) {
		procs[u] = 0;
	}

	for (f = 3; f < sysconf(_SC_OPEN_MAX); ++f)
		if ((f != ls) && (f != config_fd))
			close(f);
	close(0);
	(void) open("/dev/null", 0);
	(void) dup2(2, 1);	/* stdout */
	(void) setsid();

	getrlimit(RLIMIT_NOFILE, rlp);
	rlim_cur = (rl.rlim_cur >= FD_SETSIZE) ? FD_SETSIZE-1 : rl.rlim_cur;

	if (verbose) {
		sprintf (debug_buf, "sr_server rl.rlim_cur = %d, rlim_cur = %d\n", rl.rlim_cur, rlim_cur);
		debug_trace (debug_buf);
	}

	for (;;) {
		/* await connection */
		FD_ZERO(&readfds);
		FD_ZERO(&exceptfds);
		FD_SET (ls, &readfds);
		FD_SET (config_fd, &readfds);

		si = select (rlim_cur, &readfds, 0, &exceptfds, 0);
		if (verbose) {
			sprintf (debug_buf, "sr_server select returns with %d\n", si);
			debug_trace (debug_buf);
		}
		if (si == -1) {
			/*
			 *  get an interrupt when the child finishes, and when kill issued.
			 *  check to see if terminate has been set.
			 */
			if (verbose) {
				sprintf (debug_buf, "sr_server select returns -1, errno = %d\n", errno);
				debug_trace (debug_buf);
			}
			if (terminate) {
				if (verbose) {
					debug_trace ("SIGTERM, or SIGINT received, exiting\n");
				}
				break;
			}
			continue;
		} else if (si > MAX_FD) {
			if (verbose) {
				sprintf (debug_buf, "sr_server select got fd (256 max) = %d\n", si);
				debug_trace (debug_buf);
			}
			break;
		}

		if (FD_ISSET(ls, &readfds)) {
			if (verbose) {
				debug_trace ("sr_server info: got fd == ls\n");
			}
		} else if (FD_ISSET(config_fd, &readfds)) {
			if (verbose) {
				debug_trace ("sr_server info: got fd == config_fd\n");
			}
			i = do_subsequent_config();
			if (i) {
				if (verbose) {
					sprintf (debug_buf, "do_subsequent_config returned %d\n", i);
					debug_trace (debug_buf);
				}
				/*
				 *  what to do about this?
				 */
			}
			for (f = 3; f < sysconf(_SC_OPEN_MAX); ++f)
				if ((f != ls) && (f != config_fd))
					close(f);
			close(0);
			(void) open("/dev/null", 0);
			(void) dup2(2, 1);	/* stdout */
			(void) setsid();
			continue;
		} else {
			if (verbose) {
				sprintf (debug_buf, "sr_server: what? got fd == 0x%08x, ls = %d\n",
					readfds, ls);
				debug_trace (debug_buf);
			}
			continue;
		}

		n = sizeof sin;
		if ((s = accept(ls, (struct sockaddr *)&sin, &n)) == -1) {
			if (terminate)
				break;
			if (errno) {
				if (verbose) {
					sprintf (debug_buf, "accept errno=%d\n", errno);
					debug_trace(debug_buf);
				}
			}
			if (errno != EINTR) {
				a_syslog(AUTH_FAC|LOG_ERR
				  , "%s: unable to accept socket: %s"
				  , progname, strerror(errno));
				break;
			}
			sleep(1);	/* give it a rest */
			continue;
		}
/*
printf("newconn: fd==%d, peeraddr==%s,%u\n", s, inet_ntoa(sin.sin_addr)
  , ntohs(sin.sin_port));
*/
		/*
		 *  make sure it's a local connection
		 */

#define CHECK_LOCALHOST 1

#ifdef CHECK_LOCALHOST
		memset (rmtaddr, 0, 16);
		cp = inet_ntoa(sin.sin_addr);
		/*
		 *  length of addr shouldn't be more than 15!
		 */
		if ((i2 = strlen(cp)) > 15) {
		    i2 = 15;
		}
		strncpy (rmtaddr, cp, i2);

		if (strcmp(inet_ntoa(sin.sin_addr), "127.0.0.1") != 0) {
		    /*
		     *  see if this address is in the list of "allowed" ones
		     */
			
		    if (!allowed_ipaddress(rmtaddr)) {
			/*
			 *  don't allow remote connections, since communication is in the clear
			 */
			a_syslog(AUTH_FAC|LOG_ERR, "%s: attempted connection from %s",
				progname, inet_ntoa(sin.sin_addr));
			close(s);
			continue;
		    }
		}
#else
		if (verbose) {
			sprintf (debug_buf, "Connection from %s\n", inet_ntoa(sin.sin_addr));
			debug_trace (debug_buf);
		}
#endif


		/* find a spot for this */
		for (u = 0; u < n_procs; ++u) {
			if (procs[u] == 0) {
				goto found_courtney;
			}
		}

/*		a_syslog(AUTH_FAC|LOG_ERR, "%s: unable to allocate slot for new connection",
	  		progname); */

/*		if (verbose) {
			sprintf(debug_buf, "%s: unable to allocate slot for new connection\n", progname);
			debug_trace (debug_buf);
			sprintf (debug_buf,
				"%s: procs[0]=%d, [1]=%d, [2]=%d, [3]=%d, [4]=%d\n",
				progname, procs[0], procs[1], procs[2], procs[3], procs[4]);
			debug_trace (debug_buf);
		} */

		/*
		 *  check status of children in "process" table
		 */

		for (u = 0; u < n_procs; ++u) {
			if (procs[u]) {
				errno = 0;
				i = kill (procs[u], SIGHUP);
				if (i) {
					if (errno == ESRCH) {
						procs[u] = 0;

						if (verbose) {
							sprintf (debug_buf,
								"%s: process %d actually was INactive\n",
								progname, u);
							debug_trace (debug_buf);
						}
						goto found_courtney;
					} else {
						if (verbose) {
							sprintf (debug_buf,
								"%s: killing process 0x%8x returned errno=%d\n",
								progname, u, errno);
							debug_trace (debug_buf);
						}
					}
				} else {	/* process is busy */
					if (verbose) {
						sprintf (debug_buf,
							"%s: process %d actually was ACTIVE\n",
							progname, u);
						debug_trace (debug_buf);
					}
				}
			}
		}

		if (verbose) {
			sprintf (debug_buf, "closing socket %d\n", s);
			debug_trace (debug_buf);
		}
		close (s);
		continue;

		/* found a hole for this one */
found_courtney:;
		switch (procs[u] = fork()) {
		case -1:		/* trouble */
			a_syslog(AUTH_FAC|LOG_ERR
			  , "%s: unable to fork new process: %s"
			  , progname, strerror(errno));
			procs[u] = 0;
			continue;
		case 0:			/* child */
			(void) sigset(SIGHUP, sr_server_sighup);
			(void) signal(SIGINT, SIG_DFL);
			(void) signal(SIGTERM, SIG_DFL);
			(void) signal(SIGCHLD, SIG_DFL);
			(void) signal(SIGALRM, sr_server_alarm);
			(void) signal(SIGPIPE, sr_server_sigpipe);

			(void) dup2(s, 0);	/* stdin */
			(void) dup2(s, 1);	/* stdout */
			close(ls);
			close(s);
			(void) alarm(stimeout*60);	/* you got 'stimeout' minutes ... */
			return 0;
		}
		/* parent */
		close(s);
	}

	/* gets here if select sez we're suppose to die */
	/* terminate all the child processes */
	(void) kill(0, SIGTERM);
	return (-2);
}


int
sr_termio (char *outstrP, char *inbufP, int inbuflen)
{
	int len;

	sr_termout(outstrP);
	len = sr_readline(inbufP, inbuflen);
	return len;
}

sr_termout (register char *outstrP) {
	register char c;
	static char lastc = 0;
	int i;

	while ((c = *outstrP++) != '\0') {
		if (lastc == '\r')
			putchar('\0');
		putchar(c);
		lastc = c;
	}
	fflush(stdout);
}

sr_raw_termout (register char *outstrP) {
	register char c;

	while ((c = *outstrP++) != '\0') {
		putchar(c);
	}
	fflush(stdout);
}

int
get_config_info()
{
	int f, ls = -1, on, n, num_procs, s, stat, rlim_cur, i2;
	char *cp;
	char rmtaddr[16];
	struct sockaddr_in sin;
	char input[64+1];
	struct rlimit rl, *rlp = &rl;
	fd_set readfds, writefds, exceptfds;
	short si;
	int not_configed;
	extern int config_fd;

	if ((ls = socket(PF_INET, SOCK_STREAM, 0)) == -1) {
		if (verbose) {
			sprintf (debug_buf, "get_config_info: socket create failed.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: config socket create failed: %s"
			, progname, strerror(errno));
		return (-1);
	}

	on = 1;
	if (setsockopt(ls, SOL_SOCKET, SO_REUSEADDR, (char *)&on, sizeof on) == -1) {
		if (verbose) {
			sprintf (debug_buf, "get_config_info: setsockopt failed.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: config setsockopt failed: %s"
			, progname, strerror(errno));
		return (-2);
	}
	memset(&sin, 0, sizeof sin);
	sin.sin_family = AF_INET;
	sin.sin_port = htons(listen_port);
	if (bind(ls, (struct sockaddr *)&sin, sizeof sin) == -1) {
		if (verbose) {
			sprintf (debug_buf, "get_config_info: bind failed.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: config bind failed: %s"
			, progname, strerror(errno));
		return (-3);
	}
	num_procs = 1;
	if (listen(ls, num_procs) == -1) {
		if (verbose) {
			sprintf (debug_buf, "get_config_info: listen failed.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: config listen failed: %s"
			, progname, strerror(errno));
		return (-4);
	}

	for (f = 3; f < sysconf(_SC_OPEN_MAX); ++f)
		if (f != ls)
			close(f);
	close(0);
	(void) open("/dev/null", 0);
	(void) dup2(2, 1);	/* stdout */

	if (verbose) {
		sprintf (debug_buf, "%s: listening on port %u for config info.\n",
			progname, listen_port);
		debug_trace (debug_buf);
	}

	getrlimit(RLIMIT_NOFILE, rlp);
	rlim_cur = (rl.rlim_cur >= FD_SETSIZE) ? FD_SETSIZE-1 : rl.rlim_cur;
	if (verbose) {
		sprintf (debug_buf, "get_config_info rl.rlim_cur = %d, rlim_cur = %d\n",
			rl.rlim_cur, rlim_cur);
		debug_trace (debug_buf);
	}

	/*
	 *  if the config process fails, stay in waiting-to-be-config'ed state.
	 *  might cause other problems, but...  also, there are some errors that
	 *  will still cause termination of the process.  for one, don't want to
	 *  get caught in a loop.
	 */

#ifdef BE_SENSITIVE
	for (;;) {
#else
	not_configed = 1;
	while (not_configed) {
#endif
		FD_ZERO(&readfds);
		FD_ZERO(&exceptfds);
		FD_SET (ls, &readfds);
		FD_SET (ls, &exceptfds);
		(void) signal(SIGPIPE, sr_server_sigpipe);
		si = select (rlim_cur, &readfds, 0, &exceptfds, 0);
		if (verbose) {
			sprintf (debug_buf,
				"get_config_info select returns with %d, errno=%d, rd=%d, err=%d\n",
				si, errno, FD_ISSET(ls, &readfds), FD_ISSET (ls, &exceptfds));
			debug_trace (debug_buf);
		}
		if (si == -1) {
/*			sprintf (debug_buf, "get_config_info select returns -1, errno = %d\n", errno); */
/*			debug_trace (debug_buf); */
			if (terminate) {
				if (verbose) {
					debug_trace ("SIGTERM, or SIGINT received, exiting\n");
				}
				break;
			}
			continue;
		} else if (si > MAX_FD) {
			if (verbose) {
				sprintf (debug_buf, "get_config_info select got fd (256 max) = %d\n", si);
				debug_trace (debug_buf);
			}
			break;
		}

		if (FD_ISSET(ls, &readfds)) {
			if (verbose) {
				debug_trace ("get_config info: got fd == ls\n");
			}
		} else {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: what? got fd == 0x%08x, ls = %d\n",
					readfds, ls);
				debug_trace (debug_buf);
			}
		}

		n = sizeof sin;
		if ((s = accept(ls, (struct sockaddr *)&sin, &n)) == -1) {
			if (errno != EINTR) {
				if (verbose) {
					sprintf (debug_buf, "get_config_info: unable to accept socket: %s.\n",
						strerror(errno));
					debug_trace (debug_buf);
					a_syslog(AUTH_FAC|LOG_ERR
			  			, "%s: unable to accept new connection: %s"
			  			, progname, strerror(errno));
					return (-5);
				}
			}
			sleep(1);
			continue;
		}

		/*
		 *  make sure it's a local connection
		 */

#ifdef CHECK_LOCALHOST
		memset (rmtaddr, 0, 16);
		cp = inet_ntoa(sin.sin_addr);
		/*
		 *  length of addr shouldn't be more than 15!
		 */
		if ((i2 = strlen(cp)) > 15) {
		    i2 = 15;
		}
		strncpy (rmtaddr, cp, i2);

		if (strcmp(inet_ntoa(sin.sin_addr), "127.0.0.1") != 0) {
		    /*
		     *  see if this address is in the list of "allowed" ones
		     */
			
		    if (!allowed_ipaddress(rmtaddr)) {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: attempted connection from %s.\n",
					inet_ntoa(sin.sin_addr));
				debug_trace (debug_buf);
			}
			close (s);
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: attempted connection from %s"
				, progname, inet_ntoa(sin.sin_addr));
			return (-6);
		    }
		}
#else
		if (verbose) {
			sprintf (debug_buf, "get_config_info: Connection from %s\n", inet_ntoa(sin.sin_addr));
			debug_trace (debug_buf);
		}
#endif
		
		(void) dup2(s,0);
		(void) dup2(s,1);

		sprintf (debug_buf, "Enter Unix Helper Listen Port [%u]:  ",
			DEFAULT_UNIXHELPER_PORT);
		stat = sr_termio (debug_buf, input, sizeof(input));
		
		if (stat == EOF) {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: Error reading config: listen port.\n");
				debug_trace (debug_buf);
			}
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Error reading listen port", progname);
#ifdef BE_SENSITIVE
			return (-7);
#else
			close(s);
			continue;
#endif
		}

		if (stat == 0) {
			lport = DEFAULT_UNIXHELPER_PORT;
			if (verbose) {
				sprintf (debug_buf, "get_config_info: using default=%u as listen port.\n",
					DEFAULT_UNIXHELPER_PORT);
				debug_trace (debug_buf);
			}
		} else {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: received %s as listen port.\n",
					input);
				debug_trace (debug_buf);
			}
			errno = 0;
			lport = (u_short)strtoul(input, (char **)0, 0);
			if (errno) {
				sprintf (debug_buf,
					"get_config_info: Invalid listen port number (%s) specified.\n",
					input);
				if (verbose) {
					debug_trace (debug_buf);
				}
				sr_termout (debug_buf);
				a_syslog(AUTH_FAC|LOG_ERR
					, "%s: Invalid listen port (%s)", progname, input);
#ifdef BE_SENSITIVE
				return (-8);
#else
				close(s);
				continue;
#endif
			}
		}

		auth_rqt_fd = open_auth_listen_port(lport);
		if (auth_rqt_fd < 0) {
			sprintf (debug_buf,
				"get_config_info: Could not open listen port (%d) specified.\n",
				lport);
			if (verbose) {
				debug_trace (debug_buf);
			}
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Could not open listen port (%d)", progname, lport);
		}

		sprintf (debug_buf, "Enter Unix Helper Session Timeout [%d]:  ",
			DEFAULT_UNIX_TIMEOUT);
		stat = sr_termio (debug_buf, input, sizeof(input));
		
		if (stat == EOF) {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: Error reading config: session timeout.\n");
				debug_trace (debug_buf);
			}
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Error reading session timeout", progname);
#ifdef BE_SENSITIVE
			return (-9);
#else
			close(s);
			continue;
#endif
		}

		if (stat == 0) {
			stimeout = DEFAULT_UNIX_TIMEOUT;
			if (verbose) {
				sprintf (debug_buf, "get_config_info: using default=%d as session timeout.\n",
					DEFAULT_UNIX_TIMEOUT);
				debug_trace (debug_buf);
			}
		} else {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: received %s as session timeout.\n",
					input);
				debug_trace (debug_buf);
			}

			errno = 0;
			stimeout = atoi(input);
			if (errno) {
				sprintf (debug_buf,
					"get_config_info: Invalid session timeout (%s) specified.\n",
					input);
				if (verbose) {
					debug_trace (debug_buf);
				}
				sr_termout (debug_buf);
				a_syslog(AUTH_FAC|LOG_ERR
					, "%s: Invalid session timeout (%s)", progname, input);
#ifdef BE_SENSITIVE
				return (-10);
#else
				close(s);
				continue;
#endif
			}
		}

		sprintf (debug_buf, "Enter Unix Helper Max Sessions [%d]:  ",
			DEFAULT_UNIX_MAX_PROCS);
		stat = sr_termio (debug_buf, input, sizeof(input));
		
		if (stat == EOF) {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: Error reading config: max sessions.\n");
				debug_trace (debug_buf);
			}
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Error reading max sessions", progname);
#ifdef BE_SENSITIVE
			return (-11);
#else
			close(s);
			continue;
#endif
		}
		if (stat == 0) {
			n_procs = DEFAULT_UNIX_MAX_PROCS;
			if (verbose) {
				sprintf (debug_buf, "get_config_info: using default=%d as max sessions.\n",
					DEFAULT_UNIX_MAX_PROCS);
				debug_trace (debug_buf);
			}
		} else {
			if (verbose) {
				sprintf (debug_buf, "get_config_info: received %s as max sessions.\n",
					input);
				debug_trace (debug_buf);
			}

			errno = 0;
			n_procs = (u_int)strtoul(input, (char **)0, 0);
			if (errno) {
				sprintf (debug_buf,
					"get_config_info: Invalid max sessions (%s) specified.\n",
					input);
				if (verbose) {
					debug_trace (debug_buf);
				}
				sr_termout (debug_buf);
				a_syslog(AUTH_FAC|LOG_ERR
					, "%s: Invalid max sessions (%s)", progname, input);
#ifdef BE_SENSITIVE
				return (-12);
#else
				close(s);
				continue;
#endif
			}
		}

		sprintf (debug_buf, "get_config_info: %s configured successfully\n",progname);
		if (verbose) {
			debug_trace (debug_buf);
		}
		sr_termout (debug_buf);

		
		close (s);

/*		close (ls); */
		config_fd = ls;
		not_configed = 0;
	}
	return (0);
}

int
do_subsequent_config()
{
	int i, n, s, stat, i2;
	struct sockaddr_in sin;
	char input[64+1];
	struct rlimit rl, *rlp = &rl;
	fd_set readfds, writefds, exceptfds;
	short si;
	extern int config_fd;
	u_short usi;
	u_int ui;
	char *cp;
	char rmtaddr[16];

	n = sizeof sin;
	memset(&sin, 0, n);
	if ((s = accept(config_fd, (struct sockaddr *)&sin, &n)) == -1) {
		if (errno != EINTR) {
			if (verbose) {
				sprintf (debug_buf, "do_subsequent_config: unable to accept socket: %s.\n",
					strerror(errno));
				debug_trace (debug_buf);
				a_syslog(AUTH_FAC|LOG_ERR
		  			, "%s: unable to accept new connection: %s"
		  			, progname, strerror(errno));
				return (-5);
			}
		}
		sleep(1);
	}

	/*
	 *  make sure it's a local connection
	 */

#ifdef CHECK_LOCALHOST
	memset (rmtaddr, 0, 16);
	cp = inet_ntoa(sin.sin_addr);
	/*
	 *  length of addr shouldn't be more than 15!
	 */
	if ((i2 = strlen(cp)) > 15) {
	    i2 = 15;
	}
	strncpy (rmtaddr, cp, i2);

	if (strcmp(inet_ntoa(sin.sin_addr), "127.0.0.1") != 0) {
	    /*
	     *  see if this address is in the list of "allowed" ones
	     */

	    if (!allowed_ipaddress(rmtaddr)) {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: attempted connection from %s.\n",
				inet_ntoa(sin.sin_addr));
			debug_trace (debug_buf);
		}
		close (s);
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: attempted connection from %s"
			, progname, inet_ntoa(sin.sin_addr));
		return (-6);
	    }
	}
#else
	if (verbose) {
		sprintf (debug_buf, "do_subsequent_config: Connection from %s\n",
			inet_ntoa(sin.sin_addr));
		debug_trace (debug_buf);
	}
#endif
		
	(void) dup2(s,0);
	(void) dup2(s,1);

	if (verbose) {
		sprintf (debug_buf,
			"Subsequent configuration request; lport=%d, stimeout=%d, n_procs=%d\n",
			lport, stimeout, n_procs);
		debug_trace (debug_buf);
	}

	sprintf (debug_buf, "Enter Unix Helper Listen Port [%u]:  ",
		DEFAULT_UNIXHELPER_PORT);
	stat = sr_termio (debug_buf, input, sizeof(input));
		
	if (stat == EOF) {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: Error reading config: listen port.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: Error reading listen port", progname);
		return (-7);
	}

	if (stat == 0) {
		usi = DEFAULT_UNIXHELPER_PORT;
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: using default=%u as listen port.\n",
				DEFAULT_UNIXHELPER_PORT);
			debug_trace (debug_buf);
		}
	} else {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: received %s as listen port.\n",
				input);
			debug_trace (debug_buf);
		}
		errno = 0;
		usi = (u_short)strtoul(input, (char **)0, 0);
		if (errno) {
			sprintf (debug_buf,
				"do_subsequent_config: Invalid listen port number (%s) specified.\n",
				input);
			if (verbose) {
				debug_trace (debug_buf);
			}
			sr_termout (debug_buf);
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Invalid listen port (%s)", progname, input);
			return (-8);
		}
	}

	if (usi != lport) {
		sprintf (debug_buf,
			"do_subsequent_config: original lport (%d) remains, not new (%d)\n",
			lport, usi);
		debug_trace (debug_buf);
	}

	sprintf (debug_buf, "Enter Unix Helper Session Timeout [%d]:  ",
		DEFAULT_UNIX_TIMEOUT);
	stat = sr_termio (debug_buf, input, sizeof(input));
		
	if (stat == EOF) {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: Error reading config: session timeout.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: Error reading session timeout", progname);
		return (-7);
	}

	if (stat == 0) {
		i = DEFAULT_UNIX_TIMEOUT;
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: using default=%d as session timeout.\n",
				DEFAULT_UNIX_TIMEOUT);
			debug_trace (debug_buf);
		}
	} else {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: received %s as session timeout.\n",
				input);
			debug_trace (debug_buf);
		}

		errno = 0;
		i = atoi(input);
		if (errno) {
			sprintf (debug_buf,
				"do_subsequent_config: Invalid session timeout (%s) specified.\n",
				input);
			if (verbose) {
				debug_trace (debug_buf);
			}
			sr_termout (debug_buf);
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Invalid session timeout (%s)", progname, input);
			return (-8);
		}
	}

	if (i != stimeout) {
		sprintf (debug_buf,
			"do_subsequent_config: original session timeout (%d) remains, not new (%d)\n",
			stimeout, i);
		debug_trace (debug_buf);
	}

	sprintf (debug_buf, "Enter Unix Helper Max Sessions [%d]:  ",
		DEFAULT_UNIX_MAX_PROCS);
	stat = sr_termio (debug_buf, input, sizeof(input));
		
	if (stat == EOF) {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: Error reading config: max sessions.\n");
			debug_trace (debug_buf);
		}
		a_syslog(AUTH_FAC|LOG_ERR
			, "%s: Error reading max sessions", progname);
		return (-7);
	}
	if (stat == 0) {
		ui = DEFAULT_UNIX_MAX_PROCS;
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: using default=%d as max sessions.\n",
				DEFAULT_UNIX_MAX_PROCS);
			debug_trace (debug_buf);
		}
	} else {
		if (verbose) {
			sprintf (debug_buf, "do_subsequent_config: received %s as max sessions.\n",
				input);
			debug_trace (debug_buf);
		}

		errno = 0;
		ui = (u_int)strtoul(input, (char **)0, 0);
		if (errno) {
			sprintf (debug_buf,
				"do_subsequent_config: Invalid max sessions (%s) specified.\n",
				input);
			if (verbose) {
				debug_trace (debug_buf);
			}
			sr_termout (debug_buf);
			a_syslog(AUTH_FAC|LOG_ERR
				, "%s: Invalid max sessions (%s)", progname, input);
			return (-8);
		}
	}

	if (ui != n_procs) {
		sprintf (debug_buf,
			"do_subsequent_config: original max sessions (%d) remains, not new (%d)\n",
			n_procs, ui);
		debug_trace (debug_buf);
	}

	sprintf (debug_buf, "get_config_info: %s configured successfully\n",progname);
	if (verbose) {
		debug_trace (debug_buf);
	}
	sprintf (debug_buf, "get_config_info: %s configured successfully\n",progname);
	sr_termout (debug_buf);

	i = close (s);
	return (0);
}


int
open_auth_listen_port(u_short lport)
{
	int ls = -1, on;
	struct sockaddr_in sin;

	if ((ls = socket(PF_INET, SOCK_STREAM, 0)) == -1) {
                if (verbose) {
                        sprintf (debug_buf, "%s: open_auth_listen_port:couldn't open socket\n", progname);
                        debug_trace (debug_buf);
                        a_syslog(AUTH_FAC|LOG_ERR, "%s: open_auth_listen_port:couldn't open socket", progname);
                }
		exit (3);
	}
	on = 1;
	if (setsockopt(ls, SOL_SOCKET, SO_REUSEADDR, (char *)&on, sizeof on) == -1) {
                if (verbose) {
                        sprintf (debug_buf, "%s: open_auth_listen_port:couldn't setsockopt\n", progname);
                        debug_trace (debug_buf);
                        a_syslog(AUTH_FAC|LOG_ERR, "%s: open_auth_listen_port:couldn't setsockopt", progname);
                }
		close (ls);
		exit (4);
	}
	memset (&sin, 0, sizeof (sin));
	sin.sin_family = AF_INET;
	sin.sin_port = htons(lport);
	if (bind(ls, (struct sockaddr *)&sin, sizeof sin) == -1) {
        	if (verbose) {
                	sprintf (debug_buf, "%s: open_auth_listen_port:couldn't bind port\n", progname);
                	debug_trace (debug_buf);
                	a_syslog(AUTH_FAC|LOG_ERR, "%s: open_auth_listen_port:couldn't bind port", progname);
        	}
		close (ls);
		exit (5);
	}
	if (listen(ls, n_procs) == -1) {
                if (verbose) {
                        sprintf (debug_buf, "%s: open_auth_listen_port:couldn't bind port\n", progname);
                        debug_trace (debug_buf);
                        a_syslog(AUTH_FAC|LOG_ERR, "%s: open_auth_listen_port:couldn't bind port", progname);
                }
                close (ls);
		exit (6);
	}
	return (ls);
}

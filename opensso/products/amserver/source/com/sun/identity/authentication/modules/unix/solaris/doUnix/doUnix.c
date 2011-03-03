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
 * $Id: doUnix.c,v 1.2 2008/09/04 23:34:55 bigfatrat Exp $
 *
 */


/*
 *  changes made for Webtop
 *
 *  client invokes doUnix by opening a socket to localhost:7946.
 *  port 7946 is the default port number if another value isn't supplied
 *  on the commandline.  sysadmin can find the "configured" value
 *  in the "unix.port=" property in /etc/opt/SUNWstnr/platform.conf.
 *
 *  the other parameter is the "session" timeout value (in minutes).
 *  the default is 5 minutes.
 *
 *  the interface to doUnix is cleartext through stdin.  that's why
 *  only localhost connections are permitted to this service.
 */

/* hpux-dev: added pre-processor defs HPUX for HP-UX port */
#include <stdio.h>
#include <errno.h>
#include <sys/param.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
//#####hpux-dev#####
#ifdef HPUX
#include <syslog.h>
#else
#include <sys/syslog.h>
#endif

#include <netdb.h>
#include "sr_unix.h"
#include <security/pam_appl.h>
#include <pwd.h>
#include <string.h>     /* for strdup() */

#include <sys/param.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <signal.h>

#include <pwd.h>
#include <shadow.h>
#include <utmpx.h>
#include <unistd.h>
#include <string.h>
#include <locale.h>

#ifdef SOLARIS
#include <sys/systeminfo.h>
#include <bsm/audit.h>
#include <bsm/libbsm.h>
#include <bsm/audit_uevents.h>
#include <bsm/audit_record.h>
int audit_login_save_machine();
void audit_login_record();
void audit_login_session_setup();
int selected();
void get_terminal_id();
int get_machine(uint32_t *, uint32_t *);
int	sav_rflag;
int	sav_hflag;
char	sav_name[512];
uid_t	sav_uid;
gid_t	sav_gid;
dev_t	sav_port;
uint32_t	sav_machine[4];
uint32_t	sav_iptype;
char	sav_host[512];
#endif

char this_host[512];
char *progname = "amunixd";
/*
 *  globals
 */

u_short lport = DEFAULT_UNIXHELPER_PORT;        /* default server listening port */
u_int n_procs = 5;
int stimeout = 5;       /* session timeout default 5 minutes */
int *procs;
int terminate = 0;
int auth_fac = LOG_LOCAL7;
u_long verbose = 0;

char debug_buf[256];
char username[64];      /* used for login userid */
char password[64];      /* used for login password */
char service[64];
char clientaddr[16];    /* IP address; 4 groups of 3 digits, 3 dots */
int int_ipaddr[4];
int config_fd = 0;
char* login_passwd=&password[0];
#define CANCELED_LOGIN          -2

int pam_status;

int login_conv(int num_msg, struct pam_message **msg,
                          struct pam_response **response, void *appdata_ptr);

static struct pam_conv pam_conv = { login_conv , NULL };

#ifdef SOLARIS
static char *dounixdomainname = "am_auth_unix_keys";
static char *dmncp;

uint32_t
combine (int i1, int i2, int i3, int i4) {
    u_char uc1, uc2;
    uint32_t uint;
    uint32_t uint2;

    uint = (uint32_t)(i4 & 0xff);
    uint2 = (uint32_t)((i3 & 0xff) << 8);
    uint += uint2;
    uint2 = (uint32_t)((i2 & 0xff) << 16);
    uint += uint2;
    uint2 = (uint32_t)((i1 & 0xff) << 24);
    uint += uint2;
    if (verbose) {
	sprintf (debug_buf, "combine got 0x%02x/0x%02x/0x%02x/0x%02x; returning 0x%08x\n",
	    i1, i2, i3, i4, uint);
	debug_trace(debug_buf);
    }
    return (uint);
}


int
audit_login_save_host(host)
	char *host;
{
	int rv;

	if (cannot_audit(0)) {
		return (0);
	}
	(void) strncpy(sav_host, host, 511);
	sav_host[511] = '\0';
	rv = audit_login_save_machine();
	return (rv);
}


int
audit_login_save_machine()
{
	int rv;

	if (cannot_audit(0)) {
		return (0);
	}
	rv = get_machine(&sav_machine[0], &sav_iptype);

	return (rv);
}

int
audit_login_save_pw(useridp)
	char *useridp;
{
    struct passwd passwd, *pwdp;
    char cbuf[250];

    pwdp = &passwd;
    pwdp = getpwnam_r(useridp, pwdp, cbuf, sizeof(cbuf));

    if (cannot_audit(0)) {
	return (0);
    }
    if (pwdp == NULL) {
	sav_name[0] = '\0';
	sav_uid = -1;
	sav_gid = -1;
    } else {
	(void) strncpy(sav_name, pwdp->pw_name, 511);
	sav_name[511] = '\0';
	sav_uid = pwdp->pw_uid;
	sav_gid = pwdp->pw_gid;
    }
    return (0);
}


int
audit_login_bad_pw(char *msgp)
{
    if (cannot_audit(0)) {
	return (0);
    }
    audit_login_session_setup();
    audit_login_record(4, msgp, AUE_login, -1);
    return (0);
}


int
audit_login_success(char *msgp)
{
	if (cannot_audit(0)) {
		return (0);
	}
	audit_login_session_setup();
	audit_login_record(0, msgp, AUE_login, 0);
	return (0);
}

void
audit_login_record(typ, string, event_no, rtn_code)
int	typ;
char	*string;
au_event_t event_no;
int	rtn_code;
{
	int		ad, rc;
	uid_t		uid;
	gid_t		gid;
	pid_t		pid;
	au_tid_addr_t	tid;

	uid = sav_uid;
	gid = sav_gid;
	pid = getpid();

	get_terminal_id(&tid);

	rc = rtn_code;

	event_no = AUE_telnet;	/* not rlogin, so... */

	if (!selected(sav_name, uid, event_no, rc)) {
	    return;
	}

	ad = au_open();

	au_write(ad, au_to_subject_ex(uid, uid, gid, uid, gid, pid, pid, &tid));
	au_write(ad, au_to_text(string));
#ifdef _LP64
	au_write(ad, au_to_return64(typ, (int64_t)rc));
#else
	au_write(ad, au_to_return32(typ, (int32_t)rc));
#endif

	rc = au_close(ad, AU_TO_WRITE, event_no);
	if (rc < 0) {
		perror("audit");
	}
}

void
audit_login_session_setup()
{
	int	rc;
	struct auditinfo_addr info;
	au_mask_t mask;
	struct auditinfo_addr now;

	info.ai_auid = sav_uid;
	info.ai_asid = getpid();
	mask.am_success = 0;
	mask.am_failure = 0;

	au_user_mask(sav_name, &mask);

	info.ai_mask.am_success  = mask.am_success;
	info.ai_mask.am_failure  = mask.am_failure;

	/* see if terminal id already set */
	if (getaudit_addr(&now, sizeof(now)) < 0) {
		perror("getaudit");
	}

	/* XXX perhaps we should abort if audit characteristics already set */
	if (now.ai_termid.at_type) {
		info.ai_termid = now.ai_termid;
		/* update terminal ID with real values */
		sav_port   = now.ai_termid.at_port;
		sav_iptype = now.ai_termid.at_type;

		/*
		 *  this part changed.  note that it's only IPv4 capable...
		 */

		sav_machine[0] = combine(int_ipaddr[0], int_ipaddr[1],
					int_ipaddr[2], int_ipaddr[3]);
		sav_machine[1] = 0;
		sav_machine[2] = 0;
		sav_machine[3] = 0;
	} else
		get_terminal_id(&(info.ai_termid));

	rc = setaudit_addr(&info, sizeof(info));
	if (rc < 0) {
		perror("setaudit");
	}
}

void get_terminal_id(tid)
au_tid_addr_t *tid;
{
	tid->at_port = sav_port;
	tid->at_type = sav_iptype;
	tid->at_addr[0] = sav_machine[0];
	tid->at_addr[1] = sav_machine[1];
	tid->at_addr[2] = sav_machine[2];
	tid->at_addr[3] = sav_machine[3];
}

int
get_machine(uint32_t *buf, uint32_t *iptype)
{
	int	rc;
	char	hostname[256];
	struct hostent *hostent;
	int stat;

	if (sav_rflag || sav_hflag) {
		stat = aug_get_machine(sav_host, buf, iptype);
	} else {
		rc = sysinfo(SI_HOSTNAME, hostname, 256);
		if (rc < 0) {
			perror("sysinfo");
			return (0);
		}
		stat = aug_get_machine(hostname, buf, iptype);
	}
	return (stat);
}


int
selected(nam, uid, event, sf)
char	*nam;
uid_t uid;
au_event_t event;
int	sf;
{
	int	rc, sorf;
	char	naflags[512];
	struct au_mask mask;

	mask.am_success = mask.am_failure = 0;
	if (uid < 0) {
		rc = getacna(naflags, 256); /* get non-attrib flags */
		if (rc == 0)
			getauditflagsbin(naflags, &mask);
	} else {
		rc = au_user_mask(nam, &mask);
	}

	if (sf == 0) {
		sorf = AU_PRS_SUCCESS;
	} else {
		sorf = AU_PRS_FAILURE;
	}
	rc = au_preselect(event, &mask, sorf, AU_PRS_REREAD);
	return (rc);
}
#endif
int
check_ipaddr (char *bufp) {
    char *cp, *cp1, *cp2, *cp_end;
    int stat;
    char ipaddr[4][4];
    int i, j;

    cp1 = bufp;
    cp_end = bufp + strlen(bufp)-1;

    for (j = 0; j < 3; j++) {
	memset (ipaddr[j], 0, 4);
	cp2 = strchr(cp1, (int)'.');
	if (cp2 == NULL) {
	    if (verbose) {
		sprintf (debug_buf, "check_ipaddr: no 'dot' found in %s!\n", bufp);
		debug_trace(debug_buf);
	    }
	    return (1);
	}
	cp = strncpy(ipaddr[j], cp1, cp2-cp1);
	for (i = 0; i < strlen(ipaddr[j]); i++) {
	    if (!isdigit((int)ipaddr[j][i])) {
		if (verbose) {
		    sprintf (debug_buf, "check_ipaddr: found non-digit in ipaddr[%d]\n", j);
		    debug_trace(debug_buf);
		}
		return (2);
	    }
	}
	cp1 = cp2+1;
	if (cp1 > cp_end) {
	    if (verbose) {
		sprintf (debug_buf, "check_ipaddr: insufficient input (%s)\n", bufp);
		debug_trace(debug_buf);
	    }
	    return (3);
	}
    }

    memset (ipaddr[3], 0, 4);
    cp2 = strchr(cp1, (int)'.');
    if (cp2 != NULL) {
	/*
	 * shouldn't be another "dot"... maybe IPv6?
	 */
	if (verbose) {
	    sprintf (debug_buf, "check_ipaddr: extra dot? (remainder=%s)\n", cp1);
	    debug_trace(debug_buf);
	}
	return (4);
    }
    if (strlen(cp1) > 3) {
	/*
	 * shouldn't be more than 3 digits long
	 */
	if (verbose) {
	    sprintf (debug_buf, "check_ipaddr: last field too long (remainder=%s)\n", cp1);
	    debug_trace(debug_buf);
	}
	return (5);
    }

    cp = strncpy(ipaddr[3], cp1, strlen(cp1));
    for (i = 0; i < strlen(ipaddr[3]); i++) {
    	if (!isdigit((int)ipaddr[3][i])) {
	    if (verbose) {
		sprintf (debug_buf, "check_ipaddr: found non-digit in ipaddr[3] (%s)\n",
		    ipaddr[3]);
		debug_trace(debug_buf);
	    }
	    return (6);
    	}
    }

    for (j = 0; j < 4; j++) {
	errno = 0;
	i = atoi(ipaddr[j]);
	if (errno) {
	    if (verbose) {
		sprintf (debug_buf, "check_ipaddr: paddr[%d] = %s is not integer\n",
		    j, ipaddr[j]);
		debug_trace(debug_buf);
	    }
	    return (7);
	}
	int_ipaddr[j] = i;
    }

    return (0);
}


int
login_proc ()
{
	int stat, ret;

	pam_handle_t *pamh;
	char* service_module=&service[0];
	char* loginId = &username[0];
	char auditbuf[256];
	char *cp1, *cp2;
	int i;
	int error;
	
	sprintf(debug_buf,"getting service name\n");
	debug_trace(debug_buf);
	stat = sr_termio ("Enter Service Name :", service, sizeof(service)); 
	sprintf(debug_buf,"Service modules is : %s\n",service_module);
	debug_trace (debug_buf);
	sprintf(debug_buf,"stat is : %d ",stat);
	if ((stat == EOF) || (stat == 0)) {
		return (CANCELED_LOGIN);
	}

	stat = sr_termio ("Enter Unix login:  ", username, sizeof(username)); 
	sprintf(debug_buf,"Unix login is  : %s\n", loginId);
		debug_trace (debug_buf);
	if ((stat == EOF) || (stat == 0)) {
		return (CANCELED_LOGIN);
	}

	memset(login_passwd, 0, sizeof(password));
	stat = sr_termio ("Enter password:  ", password, sizeof(password));
	if (stat == EOF) {
		sprintf (debug_buf, "%s: error reading password for login '%s'",
			progname, loginId);
		debug_trace (debug_buf);
		return (CANCELED_LOGIN);
	}

	if (verbose) {
	    sprintf(debug_buf,"User name is : %s\n" , loginId);
	    debug_trace(debug_buf);
	}

	memset(clientaddr, 0, sizeof(clientaddr));
	stat = sr_termio ("Enter Client IP Address:  ", clientaddr, sizeof(clientaddr)-1);
	if (stat == EOF) {
		sprintf (debug_buf, "%s: error reading Client IP address for login '%s'",
			progname, loginId);
		debug_trace (debug_buf);
		return (CANCELED_LOGIN);
	}

	if (verbose) {
	    sprintf (debug_buf, "received Client IP address = %s\n", clientaddr);
	    debug_trace (debug_buf);
	}

	stat = check_ipaddr(clientaddr);
	if (stat) {
	    sprintf (debug_buf, "%s: error in Client IP address (%s) for login '%s'",
		progname, clientaddr, loginId);
	    debug_trace (debug_buf);
	    return (CANCELED_LOGIN);
	}


        pam_start(service_module,loginId,&pam_conv,&pamh);

	#ifdef LINUX
	    pam_set_item(pamh,PAM_AUTHTOK,login_passwd) ;
	#else
	    if (pam_set_item(pamh,PAM_AUTHTOK,login_passwd) != PAM_SUCCESS) {
		sprintf(debug_buf,"unable to start authentication for %s",loginId);
		debug_trace(debug_buf);
		return (CANCELED_LOGIN); 
	    }
	#endif

	pam_status = pam_authenticate(pamh,0);	
		
	if (verbose) {
		sprintf (debug_buf, "%s: user '%s': login_proc returning %d.\n",
			progname, username, pam_status);
		debug_trace (debug_buf);
	}

	/*
	 *  set up bsm auditing stuff
	 *  
	 */

	#ifdef SOLARIS
	    i = audit_login_save_pw(username);
	#endif

	i = gethostname(this_host, sizeof(this_host));
	if (i == 0) {
	#ifdef SOLARIS
	    i = audit_login_save_host(this_host);
	    sprintf (auditbuf, dgettext(dmncp, "useridstring"), username);
	#endif
	} else {
	    if (verbose) {
		sprintf (debug_buf, "gethostname error; errno = %d", errno);
		debug_trace (debug_buf);
	    }
	}

        if (pam_status == PAM_SUCCESS) {
	    /*
	     *  check validity of user account
	     */
	    if ((error = pam_acct_mgmt(pamh, 0)) != PAM_SUCCESS) {
		pam_status = error;
		switch (error) {
		    case PAM_ACCT_EXPIRED:
			#ifdef SOLARIS
			    strcat (auditbuf, dgettext(dmncp, "unsuccessful_exp"));
			    i = audit_login_bad_pw(auditbuf);
			#endif
			if (verbose) {
			    sprintf (debug_buf, "user %s auth unsuccessful; expired account.",
				username);
			    debug_trace (debug_buf);
			}
			break;
		    case PAM_NEW_AUTHTOK_REQD:
			#ifdef SOLARIS
			    strcat (auditbuf, dgettext(dmncp, "unsuccessful_newtok"));
			    i = audit_login_bad_pw(auditbuf);
			#endif
			if (verbose) {
			    sprintf (debug_buf, "user %s auth unsuccessful; new auth token required.",
				username);
			    debug_trace (debug_buf);
			}
			break;
		    default:
			#ifdef SOLARIS
			    strcat (auditbuf, dgettext(dmncp, "unsuccessful"));
			    i = audit_login_bad_pw(auditbuf);
			#endif
			break;	/* hpux-dev: b'cos HP-UX compilation fails */
		}
	    } else {
			#ifdef SOLARIS
		            strcat (auditbuf, dgettext(dmncp, "successful"));
			    i = audit_login_success(auditbuf);
			#endif
	    }
	} else {
	    #ifdef SOLARIS
	        strcat (auditbuf, dgettext(dmncp, "unsuccessful"));
		i = audit_login_bad_pw(auditbuf);
	    #endif
	}

	return (pam_status);
}


main (int argc, char **argvP) {
	int ret, i;
	char inbuf[128];
	char inbuf2[128];
	char buf[128];
	char prompt[256];
	char reply[4];
	char *c, *p;
	int stat, retcode;

        #ifdef SOLARIS
	dmncp = textdomain (dounixdomainname);
        #endif

	/*
	 *  process arguments - sets a few globals
	 */

	if (stat = get_args(argc, argvP))
		return stat;

	if (verbose) {
		sprintf (debug_buf, "%s: version %s\n", progname, DOUNIX_VERSION);
		debug_trace (debug_buf);
	}

	/*
	 *  now time to wait for connection that provides the rest of the
	 *  startup and runtime configuration information.
	 */
	
	i = get_config_info();

	if (i) {
		sprintf (debug_buf, "%s: Error %d getting startup configuration information\n", progname, i);
		debug_trace (debug_buf);
		exit (-10);
	} else {
		if (verbose) {
			sprintf (debug_buf, "%s: now listening on port %u,\n", progname, lport);
			debug_trace (debug_buf);
			sprintf (debug_buf, "%s: session timeout is %d minutes,\n", progname, stimeout);
			debug_trace (debug_buf);
			sprintf (debug_buf, "%s: max concurrent sessions = %u.\n", progname, n_procs);
			debug_trace (debug_buf);
		}
	}
	
	/*
	 *  time to fork
	 */

	if (stat = sr_server())
		return stat;

	sprintf(debug_buf,"Calling login process Bina\n");
	debug_trace(debug_buf);
	pam_status = login_proc ();

	sprintf(debug_buf,"pamstattus is : %d\n",pam_status);
	debug_trace(debug_buf);

        switch (pam_status)
        {
        case PAM_SUCCESS:
	  sprintf(prompt,"pam_status: %d PAM_SUCCESS \n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication passed");
          break;
        case PAM_AUTH_ERR:
	  sprintf(prompt,"pam_status: %d PAM_AUTH_ERR\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
        case PAM_CRED_INSUFFICIENT:
	  sprintf(prompt,"ret: %d PAM_CRED_INSUFFICIENT\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
        case PAM_AUTHINFO_UNAVAIL:
	  sprintf(prompt,"ret: %d PAM_AUTHINFO_UNAVAIL\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
        case PAM_USER_UNKNOWN:
	  sprintf(prompt,"ret: %d PAM_USER_UNKNOWN\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
        case PAM_MAXTRIES:
	  sprintf(prompt,"ret: %d PAM_MAXTRIES\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
        case PAM_OPEN_ERR:
	  sprintf(prompt,"ret: %d PAM_OPEN_ERR\n", pam_status);
		debug_trace(prompt);
          sr_termout("Authentication Failed");
          break;
	case PAM_ACCT_EXPIRED:
	  sprintf(prompt,"ret: %d PAM_ACCT_EXPIRED\n", pam_status);
	  debug_trace(prompt);
          sr_termout("Authentication Failed");
	  break;
	case PAM_NEW_AUTHTOK_REQD:
	  sprintf(prompt,"ret: %d PAM_NEW_AUTHTOK_REQD\n", pam_status);
	  debug_trace(prompt);
          sr_termout("Authentication Failed - Password Expired");
	  break;
        default:
                sprintf(debug_buf,"ret: %d UNKNOWN ERROR\n", pam_status);
		debug_trace(debug_buf);
          	sr_termout("Authentication Failed");

        }

	exit(0);
}


/* eof */


int login_conv(int num_msg,struct pam_message **msg,struct pam_response **response,void *appdata_ptr ) {
    int i;
    *response = (struct pam_response *)calloc(num_msg,sizeof (struct pam_response));
    for (i=0; i<num_msg; i++) {
        if (msg[i]->msg_style == PAM_PROMPT_ECHO_ON ||
            msg[i]->msg_style == PAM_PROMPT_ECHO_OFF)
             (*response)[i].resp=strdup(login_passwd);
        else
            printf("%s\n", msg[i]->msg);
    }
    return 0;
}

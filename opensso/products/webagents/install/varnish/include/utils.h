/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
#ifndef UTILS_H
#define	UTILS_H

#include <stdarg.h>

#define TIMESTAMPLONG timestamplong()

#define	BADCH	(int)'?'
#define	BADARG	(int)':'
#define	EMSG	""

extern char log_path[4096];

void am_free(void *);
void am_vfree(int count, ...);
void am_trim(char *);
int am_whitespace(char *);
size_t am_bin_path(char*, size_t);
int am_file_exists(const char *);
int am_file_writeable(const char *);
int am_make_path(const char *, int, int);
int am_cleanup_conf(const char *, const char *);
int am_setup_conf(const char *, const char *);
int am_read_password(const char *path, char *p);
char* am_read_file(char *filename);
char *am_url_encode(const char *str);

typedef struct am_conf* am_conf_p;

typedef struct am_conf {
    char *name;
    char *path;
    char *webpath;
    am_conf_p next;
} am_conf_t;

am_conf_p am_search_conf(am_conf_p, const char *);
void am_free_conf(am_conf_p);
int am_read_instances(const char *, am_conf_p *);
int am_delete_file(const char *fn);
int am_delete_directory(const char *);

typedef struct url {
    char ssl;
    char error;
    char proto[6];
    char host[128];
    char uri[2048];
    int port;
} url_t;

url_t URL(const char *);

int am_create_agent_dir(const char *path, char **, char **);

void LOG(char *, ...);

int am_file_link(const char *from, const char *to);

void am_b64encode(char *source, char *b64destination);
char *am_random_key();

char *timestamplong();
int encrypt_base64(const char *, char *, const char*);

int validate_am_host(url_t *c);
int validate_agent(url_t *c, const char *agentid, const char *pass);

#endif

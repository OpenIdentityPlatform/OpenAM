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
#ifdef linux
#define _XOPEN_SOURCE 500
#define __USE_XOPEN_EXTENDED
#define _GNU_SOURCE
#endif

#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <stddef.h>
#include <time.h>
#include <ctype.h>
#include <stdarg.h>
#include <unistd.h>
#include <ftw.h>
#include <errno.h>
#include <stdint.h>
#include <limits.h>

#include "utils.h"
#include "net.h"

#define LINESZ 1024

#define URI_HTTP "%5[HTPShtps]"
#define URI_HOST "%255[-_.abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789]"
#define URI_PORT "%6d"
#define URI_PATH "%[-_.!~*'();/?:@&=+$,%#abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789]"
#define HD1 URI_HTTP "://" URI_HOST ":" URI_PORT "/" URI_PATH
#define HD2 URI_HTTP "://" URI_HOST "/" URI_PATH
#define HD3 URI_HTTP "://" URI_HOST ":" URI_PORT
#define HD4 URI_HTTP "://" URI_HOST

static char b64[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

void am_free(void *p) {
    if (p != NULL)
        free(p);
    p = NULL;
}

void am_vfree(int count, ...) {
    int i;
    va_list vl;
    va_start(vl, count);
    for (i = 0; i < count; i++) {
        void *p = va_arg(vl, void *);
        am_free(p);
        p = NULL;
    }
    va_end(vl);
}

void am_trim(char *a) {
    char *b = a;
    while (isspace(*b)) ++b;
    while (*b) *a++ = *b++;
    *a = '\0';
    while (isspace(*--a)) *a = '\0';
}

int am_whitespace(char *s) {
    int n = -1;
    if (s != NULL) {
        char *p;
        for (n = 0, p = s; *p != '\0'; p++) {
            if (isspace(*p)) {
                n++;
            }
        }
    }
    return n;
}

url_t URL(const char *u) {
    url_t uu;
    int port = 0;
    uu.error = 1;
    if (u == NULL || u[0] == '\0') return uu;
    while (u) {
        if (sscanf(u, HD1, uu.proto, uu.host, &port, uu.uri) == 4) {
            break;
        } else if (sscanf(u, HD2, uu.proto, uu.host, uu.uri) == 3) {
            break;
        } else if (sscanf(u, HD3, uu.proto, uu.host, &port) == 3) {
            break;
        } else if (sscanf(u, HD4, uu.proto, uu.host) == 2) {
            break;
        } else {
            LOG("URL() error parsing %s", u);
            return uu;
        }
    }
    uu.port = port;
    uu.error = 0;
    if (strcasecmp(uu.proto, "https") == 0) {
        uu.ssl = 1;
    } else {
        uu.ssl = 0;
    }
    if (strcasecmp(uu.proto, "https") == 0 && (uu.port == 80 || uu.port == 0)) {
        uu.port = 443;
    } else if (strcasecmp(uu.proto, "http") == 0 && uu.port == 0) {
        uu.port = 80;
    } else {
        uu.port = abs(uu.port);
    }
    return uu;
}

int am_file_exists(const char *fn) {
    struct stat sb;
    if (stat(fn, &sb) == 0) {
        if (S_ISREG(sb.st_mode) || S_ISDIR(sb.st_mode) || S_ISLNK(sb.st_mode)) {
            return 1;
        }
    }
    return 0;
}

int am_file_writeable(const char *fn) {
    if (access(fn, R_OK | W_OK) == 0) {
        return 1;
    }
    return 0;
}

int am_delete_file(const char *fn) {
    struct stat sb;
    if (stat(fn, &sb) == 0) {
        if (S_ISREG(sb.st_mode) || S_ISLNK(sb.st_mode)) {
            return unlink(fn);
        } else if (S_ISDIR(sb.st_mode)) {
            return am_delete_directory(fn);
        }
    }
    return -1;
}

int am_file_link(const char *from, const char *to) {
    if (!am_file_exists(from)) return -1;
    if (am_file_exists(to)) {
        if (unlink(to) != 0) {
            LOG("am_file_link() remove failed %s (%d)", to, errno);
            return -1;
        }
    }
    return symlink(from, to);
}

int am_make_path(const char *path, int nmode, int parent_mode) {
    int oumask;
    struct stat sb;
    char *p, *npath;
    if (stat(path, &sb) == 0) {
        if (S_ISDIR(sb.st_mode) == 0) return 1;
        if (chmod(path, nmode)) return 1;
        return 0;
    }
    oumask = umask(0);
    npath = strdup(path);
    p = npath;
    while (*p == '/') p++;
    while ((p = strchr(p, '/'))) {
        *p = '\0';
        if (stat(npath, &sb) != 0) {
            if (mkdir(npath, parent_mode)) {
                umask(oumask);
                am_free(npath);
                return 1;
            }
        } else if (S_ISDIR(sb.st_mode) == 0) {
            umask(oumask);
            am_free(npath);
            return 1;
        }
        *p++ = '/';
        while (*p == '/') p++;
    }
    if (stat(npath, &sb) && mkdir(npath, nmode)) {
        umask(oumask);
        am_free(npath);
        return 1;
    }
    umask(oumask);
    am_free(npath);
    return 0;
}

size_t am_bin_path(char* buffer, size_t len) {
    char* path_end;
    int r = readlink(
#ifdef __sun
            "/proc/self/path/a.out"
#elif __linux__
            "/proc/self/exe"
#endif
            , buffer, len);
    if (r <= 0) {
        fprintf(stderr, "readlink error %d\n", errno);
        return -1;
    }
    path_end = strrchr(buffer, '/');
    if (path_end == NULL)
        return -1;
    ++path_end;
    *path_end = '\0';
    return (size_t) (path_end - buffer);
}

am_conf_p am_search_conf(am_conf_p p, const char *name) {
    am_conf_p tmp;
    tmp = p;
    while (tmp != NULL) {
        if (strcasecmp((const char*) tmp->name, name) == 0) {
            break;
        }
        tmp = tmp->next;
    }
    return tmp;
}

void am_free_conf(am_conf_p ptr) {
    am_conf_p temp;
    while (ptr != NULL) {
        temp = ptr->next;
        am_free(ptr->name);
        am_free(ptr->path);
        am_free(ptr->webpath);
        ptr = temp;
    }
    am_free(ptr);
}

static am_conf_p am_add_conf(am_conf_p p, const char *name, const char *val, const char *wval) {
    if (p == NULL) {
        p = (am_conf_p) malloc(sizeof (am_conf_t));
        p->name = strdup(name);
        p->path = strdup(val);
        p->webpath = strdup(wval);
        p->next = NULL;
    } else {
        p->next = am_add_conf(p->next, name, val, wval);
    }
    return (p);
}

int am_read_instances(const char *path, am_conf_p *inst) {
    char *insp = NULL;
    int ret = 0;
    char buff[LINESZ * 3];
    char a[LINESZ], b[LINESZ], c[LINESZ];
    asprintf(&insp, "%s/.agents", path);
    FILE *fin = fopen(insp, "r");
    if (fin != NULL) {
        while (fgets(buff, (LINESZ * 3), fin)) {
            if (buff[0] == '#' || buff[0] == '\n') {
                continue;
            }
            if (sscanf(buff, "%s %s %s", a, b, c) == 3) {
                *inst = am_add_conf(*inst, a, b, c);
                ret++;
            }
        }
        fclose(fin);
    }
    am_free(insp);
    return ret;
}

int am_read_password(const char *path, char *p) {
    char buff[64];
    int ret = 0;
    if (am_file_exists(path)) {
        FILE *fin = fopen(path, "r");
        if (fin != NULL) {
            if (fgets(buff, 64, fin) != NULL) {
                am_trim(buff);
                sprintf(p, buff);
                ret = 1;
            }
            fclose(fin);
        }
    }
    return ret;
}

char* am_read_file(char *filename) {
    char *buffer = NULL;
    int string_size, read_size;
    FILE *file = fopen(filename, "r");
    if (file != NULL) {
        fseek(file, 0, SEEK_END);
        string_size = ftell(file);
        rewind(file);
        buffer = (char*) malloc(string_size + 1);
        read_size = fread(buffer, sizeof (char), string_size, file);
        buffer[string_size] = '\0';
        if (string_size != read_size) {
            free(buffer);
            buffer = NULL;
        }
    }
    return buffer;
}

int am_setup_conf(const char *pth, const char *confline) {
    FILE *fin = fopen(pth, "a");
    if (fin != NULL) {
        if (confline != NULL)
            fprintf(fin, "%s\n", confline);
        fclose(fin);
        return 0;
    }
    return -1;
}

int am_cleanup_conf(const char *pth, const char *key) {
    int ret;
    char *p1 = NULL;
    char buff[LINESZ * 3];
    FILE *fout, *fin = fopen(pth, "r");
    if (fin != NULL) {
        asprintf(&p1, "%s_edit", pth);
        fout = fopen(p1, "w");
        if (fout != NULL) {
            while (fgets(buff, LINESZ * 3, fin)) {
                if (strstr(buff, key) == NULL) {
                    fputs(buff, fout);
                }
            }
            fclose(fout);
        }
        fclose(fin);
        ret = rename(p1, pth);
        am_free(p1);
    }
    return ret;
}

static int _rm_(const char *path, const struct stat *s, int flag, struct FTW *f) {
    int status;
    int (*rm_func)(const char *);
    switch (flag) {
        default: rm_func = unlink;
            break;
        case FTW_DP: rm_func = rmdir;
    }
    status = rm_func(path);
    return status;
}

int am_delete_directory(const char *path) {
    if (nftw(path, _rm_, 32, FTW_DEPTH)) {
        return -1;
    }
    return 0;
}

int am_alphasort(const struct dirent **_a, const struct dirent **_b) {
    struct dirent **a = (struct dirent **) _a;
    struct dirent **b = (struct dirent **) _b;
    return strcoll((*a)->d_name, (*b)->d_name);
}

int am_file_filter(const struct dirent *_a) {
    return (strncasecmp(_a->d_name, "agent_", 6) == 0);
}

int am_scandir(const char *dirname, struct dirent ***ret_namelist, int (*select)(const struct dirent *), int (*compar)(const struct dirent **, const struct dirent **)) {
    int i, len;
    int used, allocated;
    DIR *dir;
    struct dirent *ent, *ent2, *dirbuf;
    struct dirent **namelist = NULL;
    if ((dir = opendir(dirname)) == NULL)
        return -1;
    used = 0;
    allocated = 2;
    namelist = malloc(allocated * sizeof (struct dirent *));
    if (!namelist)
        goto error;
    dirbuf = malloc(sizeof (struct dirent) + 255 + 1);
    while (readdir_r(dir, dirbuf, &ent) == 0 && ent) {
        if (strcmp(ent->d_name, ".") == 0 || strcmp(ent->d_name, "..") == 0)
            continue;
        if (select != NULL && !select(ent))
            continue;
        len = offsetof(struct dirent, d_name) + strlen(ent->d_name) + 1;
        if ((ent2 = malloc(len)) == NULL)
            return -1;
        if (used >= allocated) {
            allocated *= 2;
            namelist = realloc(namelist, allocated * sizeof (struct dirent *));
            if (!namelist)
                goto error;
        }
        memcpy(ent2, ent, len);
        namelist[used++] = ent2;
    }
    am_free(dirbuf);
    closedir(dir);
    if (compar)
        qsort(namelist, used, sizeof (struct dirent *),
            (int (*)(const void *, const void *)) compar);

    *ret_namelist = namelist;
    return used;
error:
    if (namelist) {
        for (i = 0; i < used; i++)
            am_free(namelist[i]);
        am_free(namelist);
    }
    return -1;
}

int am_create_agent_dir(const char *path, char **created_name, char **created_name_simple) {
    struct dirent **instlist;
    int idx = 0;
    char *p0 = NULL;
    int i, n, ret = -1;
    if ((n = am_scandir(path, &instlist, am_file_filter, am_alphasort)) <= 0) {
        asprintf(created_name, "%s/agent_1", path);
        asprintf(created_name_simple, "agent_1");
        am_make_path(*created_name, 0755, 0755);
        asprintf(&p0, "%s/agent_1/config", path);
        am_make_path(p0, 0755, 0755);
        am_free(p0);
        p0 = NULL;
        asprintf(&p0, "%s/agent_1/logs/debug", path);
        am_make_path(p0, 0755, 0755);
        am_free(p0);
        p0 = NULL;
        asprintf(&p0, "%s/agent_1/logs/audit", path);
        am_make_path(p0, 0755, 0755);
        am_free(p0);
        p0 = NULL;
        ret = 0;
    } else {
        for (i = 0; i < n; i++) {
            if (i == n - 1) {
                char *id = strstr(instlist[i]->d_name, "_");
                if (id != NULL && (idx = atoi(id + 1)) > 0) {
                    asprintf(created_name, "%s/agent_%d", path, idx + 1);
                    asprintf(created_name_simple, "agent_%d", idx + 1);
                    am_make_path(*created_name, 0755, 0755);
                    asprintf(&p0, "%s/agent_%d/config", path, idx + 1);
                    am_make_path(p0, 0755, 0755);
                    am_free(p0);
                    p0 = NULL;
                    asprintf(&p0, "%s/agent_%d/logs/debug", path, idx + 1);
                    am_make_path(p0, 0755, 0755);
                    am_free(p0);
                    p0 = NULL;
                    asprintf(&p0, "%s/agent_%d/logs/audit", path, idx + 1);
                    am_make_path(p0, 0755, 0755);
                    am_free(p0);
                    p0 = NULL;
                    ret = 0;
                }
            }
            am_free(instlist[i]);
        }
        am_free(instlist);
    }
    return ret;
}

char *timestamp() {
    int offset;
    struct timespec ts;
    unsigned short msec = 0;
    char time_string[20];
    static char time_string_tz[40];
    struct tm now;
    clock_gettime(CLOCK_REALTIME, &ts);
    msec = ts.tv_nsec / 1000000;
    localtime_r(&ts.tv_sec, &now);
    offset = (-(int) timezone);
    if (now.tm_isdst) offset += 3600;
    strftime(time_string, sizeof (time_string), "%Y-%m-%d %H:%M:%S", &now);
    snprintf(time_string_tz, sizeof (time_string_tz), "%s.%03d %+03d%02d", time_string, msec, (int) (offset / 3600),
            (int) ((abs((int) offset) / 60) % 60));
    return time_string_tz;
}

char *timestamplong() {
    static char time_string_tz[30];
    char time_string[20];
    struct timespec ts;
    unsigned short msec = 0;
    struct tm now;
    clock_gettime(CLOCK_REALTIME, &ts);
    msec = ts.tv_nsec / 1000000;
    localtime_r(&ts.tv_sec, &now);
    strftime(time_string, sizeof (time_string), "%Y%m%d%H%M%S", &now);
    snprintf(time_string_tz, sizeof (time_string_tz), "%s%d", time_string, msec);
    return time_string_tz;
}

void LOG(char *fmt, ...) {
    char *temp = NULL;
    int size = 0;
    va_list ap;
    va_start(ap, fmt);
    size = vasprintf(&temp, fmt, ap);
    va_end(ap);
    if (size > 0) {
        if (log_path != NULL && log_path[0] != '\n') {
            FILE *fout = fopen(log_path, "a");
            if (fout != NULL) {
                fprintf(fout, "%s   \t%s\r\n", timestamp(), temp);
                fclose(fout);
            }
        }
    }
    am_free(temp);
}

static inline void encodeblock(unsigned char in[], char b64str[], int len) {
    unsigned char out[5];
    out[0] = b64[ in[0] >> 2 ];
    out[1] = b64[ ((in[0] & 0x03) << 4) | ((in[1] & 0xf0) >> 4) ];
    out[2] = (unsigned char) (len > 1 ? b64[ ((in[1] & 0x0f) << 2) |
            ((in[2] & 0xc0) >> 6) ] : '=');
    out[3] = (unsigned char) (len > 2 ? b64[ in[2] & 0x3f ] : '=');
    out[4] = '\0';
    strncat(b64str, (const char *) out, sizeof (out));
}

void am_b64encode(char *source, char *b64destination) {
    unsigned char in[3];
    int i, len = 0;
    int j = 0;
    b64destination[0] = '\0';
    while (source[j]) {
        len = 0;
        for (i = 0; i < 3; i++) {
            in[i] = (unsigned char) source[j];
            if (source[j]) {
                len++;
                j++;
            } else in[i] = 0;
        }
        if (len) {
            encodeblock(in, b64destination, len);
        }
    }
}

char *am_random_key() {
    int i;
    srand(time(0));
    static char k[25];
    memset(k, 0, 25);
    static const char alphanum[] =
            "0123456789"
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz";
    for (i = 0; i < 24; ++i) {
        k[i] = alphanum[rand() % (sizeof (alphanum) - 1)];
    }
    k[24] = 0;
    return k;
}

static char *http_read_write(url_t *c, const char *data, const size_t size) {
    sock_t s;
    char *out = NULL;
    s = net_connect(c->host, c->port, 8);
    if (s != -1) {
        if (c->ssl == 1) {
            tls_t *t = tls_initialize(s, 0, NULL, NULL);
            if (t) {
                ssize_t l = tls_write(t, data, size);
                if (l > 0) {
                    l = tls_read(t, &out);
                }
                tls_free(t);
            }
        } else {
            ssize_t l = net_write(s, data, size);
            if (l > 0) {
                l = net_read(s, &out);
            }
        }
        net_close(s);
    }
    return out;
}

int validate_am_host(url_t *c) {
    char *t = NULL, *o = NULL;
    int ok = 0;
    if (c) {
        size_t l = asprintf(&t, "GET /%s/UI/Login HTTP/1.0\r\n"
                "Host: %s:%d\r\n"
                "User-Agent: ForgeRock Web Policy Agent\r\n"
                "Accept: */*\r\n"
                "Connection: close\r\n\r\n", c->uri, c->host, c->port);
        if (t) {
            o = http_read_write(c, t, l);
            if (o) {
                LOG("validate_am_host() response:\n%s", o);
                if (strstr(o, "AMAuthCookie") != NULL) {
                    ok = 1;
                } else {
                    ok = 0;
                }
                am_free(o);
            }
            am_free(t);
        }
    }
    return ok == 1 ? 0 : -1;
}

static int agent_logout(url_t *c, const char *token) {
    char *t = NULL, *o = NULL;
    int ok = 0;
    if (c && token) {
        size_t l = asprintf(&t, "GET /%s/identity/logout?subjectid=%s HTTP/1.0\r\n"
                "Host: %s:%d\r\n"
                "User-Agent: ForgeRock Web Policy Agent\r\n"
                "Accept: */*\r\n"
                "Connection: close\r\n\r\n", c->uri, token, c->host, c->port);
        if (t) {
            o = http_read_write(c, t, l);
            if (o) {
                am_free(o);
            }
            am_free(t);
        }
    }
    return ok == 1 ? 0 : -1;
}

int validate_agent(url_t *c, const char *agentid, const char *pass) {
    char *t = NULL, *o = NULL, *a = NULL, *b = NULL, *ae = NULL, *pe = NULL;
    char buffer_out[2048];
    size_t i, l;
    int ok = 0;
    if (c && agentid && pass) {
        ae = am_url_encode(agentid);
        pe = am_url_encode(pass);
        if (!ae || !pe) {
            LOG("validate_agent() url_encode error");
            return -1;
        }
        i = asprintf(&a, "username=%s&password=%s&uri=realm%%3D/%%26module%%3DApplication", ae, pe);
        if (a) {
            l = asprintf(&t, "POST /%s/identity/authenticate HTTP/1.0\r\n"
                    "Host: %s:%d\r\n"
                    "User-Agent: ForgeRock Web Policy Agent\r\n"
                    "Content-Language: UTF-8\r\n"
                    "Connection: close\r\n"
                    "Content-Type: application/x-www-form-urlencoded\r\n"
                    "Content-Length: %d\r\n\r\n"
                    "%s\r\n",
                    c->uri, c->host, c->port, i, a);
            if (t) {
                LOG("validate_agent() request:\n%s", t);
                o = http_read_write(c, t, l);
                if (o) {
                    LOG("validate_agent() response:\n%s", o);
                    if ((b = strstr(o, "token.id=")) != NULL &&
                            sscanf(b, "token.id=%s", buffer_out) == 1) {
                        ok = 1;
                        agent_logout(c, buffer_out);
                    } else {
                        ok = 0;
                    }
                    am_free(o);
                }
                am_free(t);
            }
            am_free(a);
        }
    }
    return ok == 1 ? 0 : -1;
}

static inline void encode(unsigned char *s, char *enc, char *tb) {
    for (; *s; s++) {
        if (tb[*s]) sprintf(enc, "%c", tb[*s]);
        else sprintf(enc, "%%%02X", *s);
        while (*++enc);
    }
}

char *am_url_encode(const char *str) {
    char rfc3986[256] = {0};
    char *enc = NULL;
    int i;
    if (str != NULL) {
        enc = (char *) malloc(strlen(str) * 3 + 1);
        for (i = 0; i < 256; i++) {
            rfc3986[i] = isalnum(i) || i == '~' || i == '-' || i == '.' || i == '_'
                    ? i : 0;
        }
        encode((unsigned char *) str, enc, rfc3986);
    }
    return enc;
}

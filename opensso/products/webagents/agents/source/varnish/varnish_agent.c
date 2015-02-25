/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <stdio.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <stdarg.h>
#include <unistd.h>

#include <vrt.h>
#include <cache.h>
#include <vct.h>
#include <vcc_if.h>

#include <am_web.h>

#define	MAGIC_STR		"sunpostpreserve"
#define	POST_PRESERVE_URI	"/dummypost/"MAGIC_STR
#define DEBUG_FILE              "/tmp/vmod_am.log"
#ifdef DEBUG
#define LOG_E(...)         log_file(__VA_ARGS__)
#else
#define LOG_E(...)         {}
#endif
#define THREAD_ID             (void *)(uintptr_t)pthread_self()
#define HTTP_HEADER_UNSET       1
#define HTTP_HEADER_SET         0
#define HTTP_HEADER_MAX_LEN     256
#define bprintf(buf, fmt, ...)						\
	do {								\
		assert(snprintf(buf, sizeof buf, fmt, __VA_ARGS__)	\
		    < sizeof buf);					\
	} while (0)

struct request {
    unsigned int xid;
    void *value;
    VTAILQ_ENTRY(request) list;
};

typedef struct {
    unsigned int magic;
#define VMOD_AM_R_MAGIC 0x54F43E2F
    VTAILQ_HEAD(, request) vars;
} request_list_t;

struct header {
    int type;
    int unset;
    enum gethdr_e where;
    char *name;
    char *value;
    VTAILQ_ENTRY(header) list;
};

typedef struct {
    unsigned int magic;
#define VMOD_AM_H_MAGIC 0x53F43E2F
    VTAILQ_HEAD(, header) vars;
} header_list_t;

typedef enum {
    OK = 0,
    DONE,
} ret_status;

typedef struct {
    struct sess *s;
    int status;
    int inauth;
    unsigned int key;
    header_list_t *headers;
    char *body;
    char *notes;
    unsigned int xid;
} request_data_t;

static pthread_mutex_t init_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t thread_once = PTHREAD_ONCE_INIT;
static pthread_key_t thread_key;
static int n_init = 0;

static void log_file(const char *format, ...) {
    FILE *fin = fopen(DEBUG_FILE, "a+");
    if (fin != NULL) {
        if (format != NULL) {
            struct timespec ts;
            struct tm now;
            unsigned short msec = 0;
            char tz[8];
            char time_string[25];
            char time_string_tz[50];
            clock_gettime(CLOCK_REALTIME, &ts);
            msec = ts.tv_nsec / 1000000;
            localtime_r(&ts.tv_sec, &now);
            strftime(time_string, sizeof (time_string), "%Y-%m-%d %H:%M:%S", &now);
            strftime(tz, sizeof (tz), "%z", &now);
            snprintf(time_string_tz, sizeof (time_string_tz), "%s.%03d %s   ", time_string, msec, tz);
            va_list args;
            va_start(args, format);
            fprintf(fin, time_string_tz);
            vfprintf(fin, format, args);
            fprintf(fin, "\n");
            va_end(args);
        }
        fclose(fin);
    }
}

static void fini_am(void *priv) {
    LOG_E("fini_am() %d", n_init);
    assert(priv == &n_init);
    AZ(pthread_mutex_lock(&init_mutex));
    assert(n_init > 0);
    n_init--;
    if (n_init == 0) {
        am_web_cleanup();
        am_shutdown_nss();
    }
    AZ(pthread_mutex_unlock(&init_mutex));
}

static void delete_key(void *k) {
    LOG_E("vmod_delete_worker_key() (%p)", THREAD_ID);
    free(k);
}

static void make_key() {
    pthread_key_create(&thread_key, delete_key);
    LOG_E("vmod_make_worker_key()");
}

request_data_t *get_request_data(struct sess *sp) {
    request_list_t *rl = NULL;
    request_data_t *r = NULL;

    if (sp == NULL) {
        LOG_E("get_request_data() ***ERROR*** sess is NULL (%p)", THREAD_ID);
        return NULL;
    }

    if ((rl = pthread_getspecific(thread_key)) == NULL) {
        struct request *rld;

        LOG_E("get_request_data() ***CREATE*** key %u (%p)", sp->xid, THREAD_ID);

        rl = (request_list_t *) malloc(sizeof (request_list_t));
        assert(rl != NULL);
        pthread_setspecific(thread_key, rl);

        rl->magic = VMOD_AM_R_MAGIC;
        VTAILQ_INIT(&rl->vars);

        rld = (struct request *) malloc(sizeof (struct request));
        assert(rld != NULL);

        rld->xid = sp->xid;

        r = (request_data_t *) WS_Alloc(sp->ws, sizeof (request_data_t));
        assert(r != NULL);

        r->s = sp;
        r->status = 0;
        r->inauth = 0;
        r->xid = rld->xid;
        r->body = NULL;
        r->notes = NULL;
        r->headers = (header_list_t *) WS_Alloc(sp->ws, sizeof (header_list_t));
        assert(r->headers != NULL);

        r->headers->magic = VMOD_AM_H_MAGIC;
        VTAILQ_INIT(&r->headers->vars);

        rld->value = r;
        VTAILQ_INSERT_TAIL(&rl->vars, rld, list);
    } else {
        struct request *v = NULL, *rld = NULL, *v_tmp = NULL;

        LOG_E("get_request_data() ***FETCH*** key %u (%p)", sp->xid, THREAD_ID);

        if (rl == NULL || sp == NULL) {
            LOG_E("get_request_data() ***FETCH FAILED*** key %u (%p)", sp ? sp->xid : 0, THREAD_ID);
            return NULL;
        }

        VTAILQ_FOREACH_SAFE(v, &rl->vars, list, v_tmp) {
            if (v != NULL && sp != NULL && v->xid == sp->xid) {
                LOG_E("get_request_data() ***FOUND*** key %u (%p)", sp->xid, THREAD_ID);
                return (request_data_t *) v->value;
            }
        }

        LOG_E("get_request_data() ***UPDATE*** key %u (%p)", sp->xid, THREAD_ID);

        rld = (struct request *) malloc(sizeof (struct request));
        assert(rld != NULL);

        rld->xid = sp->xid;

        r = (request_data_t *) WS_Alloc(sp->ws, sizeof (request_data_t));
        assert(r != NULL);

        r->s = sp;
        r->status = 0;
        r->inauth = 0;
        r->xid = rld->xid;
        r->body = NULL;
        r->notes = NULL;
        r->headers = (header_list_t *) WS_Alloc(sp->ws, sizeof (header_list_t));
        assert(r->headers != NULL);

        r->headers->magic = VMOD_AM_H_MAGIC;
        VTAILQ_INIT(&r->headers->vars);

        rld->value = r;
        VTAILQ_INSERT_TAIL(&rl->vars, rld, list);
    }

    return r;
}

static void am_add_header(request_data_t *r, const char *name, const char *value, int type, int unset, enum gethdr_e where) {
    struct header *h;
    if (r != NULL && r->headers != NULL && name != NULL) {
        size_t nl = strlen(name);
        h = (struct header *) WS_Alloc(r->s->ws, sizeof (struct header));
        assert(h != NULL);
        h->type = type;
        h->unset = unset;
        h->where = where;
        h->name = WS_Dup(r->s->ws, name);
        assert(h->name != NULL);
        h->name[nl] = '\0';
        /*http_SetHeader requires both h->name as (varnish) header and h->value as header + value*/
        if (value != NULL) {
            size_t hl = strlen(name + 1) + strlen(value) + 1;
            char *hv = WS_Alloc(r->s->ws, hl + 1);
            assert(hv != NULL);
            strcpy(hv, name + 1);
            strcat(hv, " ");
            strcat(hv, value);
            hv[hl] = '\0';
            h->value = hv;
        } else {
            h->value = NULL;
        }
        VTAILQ_INSERT_TAIL(&r->headers->vars, h, list);
    }
}

static struct http *get_sess_http(struct sess *sp, enum gethdr_e where) {
    switch (where) {
        case HDR_REQ:
            return sp->http;
        case HDR_RESP:
            return sp->wrk->resp;
        case HDR_OBJ:
            return sp->obj->http;
        case HDR_BEREQ:
            return sp->wrk->bereq;
        case HDR_BERESP:
            return sp->wrk->beresp;
        default:
            return NULL;
    }
}

static void am_custom_response(request_data_t *r, int status, char *data) {
    r->status = status;
    r->body = data != NULL ? WS_Dup(r->s->ws, data) : NULL;
}

static void send_badrequest(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 400, "400 Bad Request");
}

static void send_deny(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 403, "403 Forbidden");
}

static void send_notfound(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 404, "404 Not Found");
}

static void send_ok(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 200, "OK");
}

static void send_error(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 500, "500 Internal Server Error");
}

static void send_error_not_implemented(request_data_t *r, int type) {
    am_add_header(r, "\015Content-Type:", "text/plain", type, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 501, "501 Not Implemented");
}

static void send_redirect(request_data_t *r, char *location) {
    am_add_header(r, "\011Location:", location, DONE, HTTP_HEADER_SET, HDR_OBJ);
    am_custom_response(r, 302, NULL);
}

int init_am(struct vmod_priv *priv, const struct VCL_conf *conf) {
    remove(DEBUG_FILE);
    LOG_E("init_am %d", n_init);
    pthread_once(&thread_once, make_key);
    priv->priv = &n_init;
    priv->free = fini_am;
    AZ(pthread_mutex_lock(&init_mutex));
    if (n_init == 0) {
        /* First use - setup global state */
    }
    n_init++;
    AZ(pthread_mutex_unlock(&init_mutex));
    return 0;
}

void vmod_init(struct sess *sp, struct vmod_priv *priv, const char *agent_bootstrap_file, const char *agent_config_file) {
    LOG_E("vmod_init %d", n_init);
    AZ(pthread_mutex_lock(&init_mutex));
    if (n_init == 1) {
        if (agent_bootstrap_file == NULL || agent_config_file == NULL ||
                access(agent_bootstrap_file, R_OK) != 0 || access(agent_config_file, R_OK) != 0 ||
                am_web_init(agent_bootstrap_file, agent_config_file) != AM_SUCCESS) {
            fprintf(stderr, "am_web_init failed. can't access bootstrap|configuration file.\n");
            LOG_E("am_web_init failed. can't access bootstrap|configuration file.");
        } else {
            am_status_t status = AM_FAILURE;
            boolean_t init = B_FALSE;
            if ((status = am_agent_init(&init)) != AM_SUCCESS) {
                const char *status_s = am_status_to_string(status);
                fprintf(stderr, "am_agent_init failed: %s (%d)\n", status_s ? status_s : "N/A", status);
                LOG_E("am_agent_init failed: %s (%d)", status_s ? status_s : "N/A", status);
            } else {
                char **v = (char **) malloc(4 * sizeof (char *));
                if (v != NULL) {
                    am_agent_version(v);
                    fprintf(stderr, "am_agent_init OpenAM WPA/%s success\n", v[0]);
                    LOG_E("am_agent_init OpenAM WPA/%s success", v[0]);
                    free(v);
                } else {
                    LOG_E("am_agent_init success");
                }
            }
        }
    }
    n_init++;
    AZ(pthread_mutex_unlock(&init_mutex));
}

void vmod_request_cleanup(struct sess *sp, struct vmod_priv *priv) {
    struct header *v, *v1;
    struct request *vr, *vr1;
    request_list_t *rl = pthread_getspecific(thread_key);
    request_data_t *r = get_request_data(sp);

    assert(r != NULL);
    assert(rl != NULL);

    LOG_E("vmod_request_cleanup() %u %u", sp->xid, r->xid);

    /*clean up request data headers*/
    VTAILQ_FOREACH_SAFE(v, &r->headers->vars, list, v1) {
        VTAILQ_REMOVE(&r->headers->vars, v, list);
    }

    r->headers->magic = VMOD_AM_H_MAGIC;
    VTAILQ_INIT(&r->headers->vars);
    r->xid = 0;

    /*remove request key from thread-local request list*/
    VTAILQ_FOREACH_SAFE(vr, &rl->vars, list, vr1) {
        if (vr != NULL && vr->xid == sp->xid) {
            LOG_E("vmod_request_cleanup() ***REMOVING*** key %u (%p)", sp->xid, THREAD_ID);
            VTAILQ_REMOVE(&rl->vars, vr, list);
            free(vr);
            break;
        }
    }
}

void vmod_cleanup(struct sess *sp, struct vmod_priv *priv) {
    LOG_E("vmod_cleanup");
    am_agent_cleanup();
    am_web_cleanup();
}

void vmod_done(struct sess *sp, struct vmod_priv *priv) {
    struct header *v, *v1;
    request_data_t *r = get_request_data(sp);
    assert(r != NULL);
    if (r->inauth == 0) return;
    if (sp->xid == r->xid) {

        LOG_E("vmod_done() %u (%d)", r->xid, sp->err_code);

        if (sp->err_code == 801) {
            r->body = NULL;
            r->status = 404;
            send_notfound(r, DONE);
        }

        VTAILQ_FOREACH_SAFE(v, &r->headers->vars, list, v1) {
            if (v != NULL && v->type == DONE) {
                struct http *hp = get_sess_http(sp, v->where);
                assert(hp != NULL);
                if (v->value != NULL) {
                    LOG_E("vmod_done [%s]", v->value);
                    http_SetHeader(sp->wrk, sp->fd, hp, v->value);
                }
            }
        }

        if (r->body != NULL && strlen(r->body) > 0) {
            VRT_synth_page(sp, 0, r->body, vrt_magic_string_end);
        }
    } else {
        LOG_E("vmod_done() error: xid %u does not match %u", sp->xid, r->xid);
    }
}

void vmod_ok(struct sess *sp, struct vmod_priv *priv) {
    struct header *v, *v1;
    int status;
    request_data_t *r = get_request_data(sp);
    assert(r != NULL);
    if (r->inauth == 1) {
        if (sp->xid == r->xid) {

            status = r->status;
            if (status < 100 || status > 999) {
                status = 503;
            }
            if (status == 200 && sp->wrk->resp->status != 200
                    && sp->wrk->resp->status != 800 && sp->wrk->resp->status != 801) {
                status = sp->wrk->resp->status;
            }

            LOG_E("vmod_ok() %u (%d)", sp->xid, status);

            http_PutStatus(sp->wrk->resp, status);
            http_PutResponse(sp->wrk, sp->fd, sp->wrk->resp, http_StatusMessage(status));

            VTAILQ_FOREACH_SAFE(v, &r->headers->vars, list, v1) {
                if (v != NULL && v->type == OK) {
                    struct http *hp = get_sess_http(sp, v->where);
                    assert(hp != NULL);
                    if (v->value == NULL) {
                        /*should not happen within v->type == OK*/
                        http_Unset(hp, v->name);
                    } else {
                        LOG_E("vmod_ok [%s]", v->value);
                        if (v->unset) {
                            http_Unset(hp, v->name);
                        }
                        http_SetHeader(sp->wrk, sp->fd, hp, v->value);
                    }
                }
            }
        } else {
            http_PutStatus(sp->wrk->resp, 403);
            http_PutResponse(sp->wrk, sp->fd, sp->wrk->resp, http_StatusMessage(403));
            LOG_E("vmod_ok() error: xid %u does not match %u", sp->xid, r->xid);
        }
    }
    vmod_request_cleanup(sp, priv);
}

static am_status_t content_read(void **args, char **body) {
    request_data_t *r;
    const char *thisfunc = "content_read()";
    am_status_t status = AM_FAILURE;
    size_t total_read = 0;
    int bytes_read;
#define BUF_LENGTH 8192
    char buf[BUF_LENGTH];

    if (!args || !(r = args[0]) || !r->s || r->s->magic != SESS_MAGIC || !r->s->wrk) {
        am_web_log_error("%s: invalid arguments", thisfunc);
        return AM_INVALID_ARGUMENT;
    } else {
        char *cl_ptr = VRT_GetHdr(r->s, HDR_REQ, "\017Content-Length:");
        size_t content_length = cl_ptr ? strtoul(cl_ptr, NULL, 10) : 0;
        if (content_length <= 0 || errno == ERANGE) {
            am_web_log_warning("%s: post data is empty", thisfunc);
            return AM_NOT_FOUND;
        } else {
            size_t content_length_hdr = content_length;
            *body = (char*) WS_Alloc(r->s->ws, content_length + 1);
            if (*body == NULL) {
                am_web_log_error("%s: memory allocation failure", thisfunc);
                return AM_FAILURE;
            } else {
                am_web_log_debug("%s: content-length is %d bytes", thisfunc, content_length);
            }
            while (content_length) {
                bytes_read = content_length > BUF_LENGTH ? BUF_LENGTH : content_length;
#ifdef VARNISH302
                bytes_read = HTC_Read(r->s->htc, buf, bytes_read);
#else
                bytes_read = HTC_Read(r->s->wrk, r->s->htc, buf, bytes_read);
#endif
                if (bytes_read <= 0) {
                    *body = NULL;
                    am_web_log_error("%s: HTC_Read failure (%d)", thisfunc, errno);
                    return AM_FAILURE;
                }

                content_length -= bytes_read;
                memcpy((*body) + total_read, buf, bytes_read);
                total_read += bytes_read;
            }

            if (total_read == content_length_hdr) {
                (*body)[total_read] = '\0';
                status = AM_SUCCESS;
            } else {
                am_web_log_warning("%s: post data read %d does not correspond to content length %d",
                        thisfunc, total_read, content_length_hdr);
                *body = NULL;
                return AM_FAILURE;
            }
        }
    }

    am_web_log_debug("%s: %d bytes", thisfunc, total_read);
    return status;
}

static const char* get_req_header(request_data_t* r, const char* key) {
    char h[HTTP_HEADER_MAX_LEN];
    if (key != NULL && r != NULL && r->s != NULL) {
        memset(&h[0], 0x00, sizeof (h));
        bprintf(h, "%c%s:", (unsigned) strlen(key) + 1, key);
        return VRT_GetHdr(r->s, HDR_REQ, h);
    } else {
        am_web_log_error("get_req_header(): invalid arguments");
    }
    return NULL;
}

static am_status_t set_header_in_request(void **args, const char *key, const char *value) {
    am_status_t status = AM_SUCCESS;
    request_data_t * r = (request_data_t *) args[0];
    if (!r || !key) {
        am_web_log_error("set_header_in_request(): invalid arguments");
        status = AM_INVALID_ARGUMENT;
    } else {
        char h[HTTP_HEADER_MAX_LEN];
        memset(&h[0], 0x00, sizeof (h));
        bprintf(h, "%c%s:", (unsigned) strlen(key) + 1, key);
        am_add_header(r, h,
                value != NULL && *value != '\0' ? value : "", OK,
                HTTP_HEADER_UNSET, HDR_REQ);
    }
    return status;
}

static am_status_t set_cookie(const char *header, void **args) {
    am_status_t status = AM_INVALID_ARGUMENT;
    const char* set_cookie_h = "\013Set-Cookie:";
    char *currentCookies;
    if (header && args) {
        request_data_t *r = (request_data_t *) args[0];
        if (!r || !r->s) {
            am_web_log_error("set_cookie(): invalid arguments");
        } else {
            am_add_header(r, set_cookie_h, header, OK, HTTP_HEADER_SET, HDR_RESP);
            if ((currentCookies = (char *) get_req_header(r, "Cookie")) == NULL) {
                set_header_in_request(args, "Cookie", header);
            } else {
                size_t hl = strlen(currentCookies) + strlen(header);
                char *h = WS_Alloc(r->s->ws, hl + 3);
                assert(h != NULL);
                strcpy(h, header);
                strcat(h, "; ");
                strcat(h, currentCookies);
                set_header_in_request(args, "Cookie", h);
            }
            status = AM_SUCCESS;
        }
    }
    return status;
}

static am_status_t add_header_in_response(void **args, const char *key, const char *values) {
    request_data_t* r = NULL;
    am_status_t status = AM_SUCCESS;
    if (args == NULL || (r = (request_data_t *) args[0]) == NULL || key == NULL) {
        am_web_log_error("add_header_in_response(): invalid arguments");
        status = AM_INVALID_ARGUMENT;
    } else {
        if (values == NULL) {
            status = set_cookie(key, args);
        } else {
            char h[HTTP_HEADER_MAX_LEN];
            memset(&h[0], 0x00, sizeof (h));
            bprintf(h, "%c%s:", (unsigned) strlen(key) + 1, key);
            am_add_header(r, h,
                    values, OK,
                    HTTP_HEADER_SET, HDR_RESP);
        }
    }
    return status;
}

static am_status_t set_user(void **args, const char *user) {
    /*not implemented/supported on Varnish*/
    return AM_SUCCESS;
}

static am_status_t set_method(void **args, am_web_req_method_t method) {
    request_data_t *rec;
    struct sess* sp;
    am_status_t status = AM_SUCCESS;
    if (!args || !(rec = (request_data_t *) args[0]) ||
            !(sp = rec->s) || !sp->http) {
        am_web_log_error("set_method(): invalid arguments");
        status = AM_INVALID_ARGUMENT;
    } else {
        char hdr[HTTP_HEADER_MAX_LEN];
        memset(&hdr[0], 0x00, sizeof (hdr));
        snprintf(hdr, sizeof (hdr), "%s", am_web_method_num_to_str(method));
        http_SetH(sp->http, HTTP_HDR_REQ, hdr);
    }
    return status;
}

static am_status_t render_result(void **args, am_web_result_t http_result, char *data) {
    const char *thisfunc = "render_result()";
    request_data_t* rec;
    am_status_t status = AM_SUCCESS;
    int *ret = NULL;
    int len = 0;
    char clen[13];

    if (!args || !(rec = (request_data_t *) args[0]) || !(ret = (int *) args[1]) ||
            ((http_result == AM_WEB_RESULT_OK_DONE || http_result == AM_WEB_RESULT_REDIRECT) &&
            (!data || *data == '\0'))) {
        am_web_log_error("%s: invalid arguments", thisfunc);
        status = AM_INVALID_ARGUMENT;
    } else {
        switch (http_result) {
            case AM_WEB_RESULT_OK:
                am_custom_response(rec, 200, NULL);
                *ret = OK;
                break;
            case AM_WEB_RESULT_OK_DONE:
                if (data && ((len = strlen(data)) > 0)) {
                    memset(&clen[0], 0x00, sizeof (clen));
                    snprintf(clen, sizeof (clen), "%d", len);
                    am_add_header(rec, "\015Content-Type:", "text/html", DONE, HTTP_HEADER_SET, HDR_OBJ);
                    am_add_header(rec, "\017Content-Length:", clen, DONE, HTTP_HEADER_SET, HDR_OBJ);
                    am_custom_response(rec, 200, data);
                    *ret = DONE;
                } else {
                    am_custom_response(rec, 200, NULL);
                    *ret = OK;
                }
                break;
            case AM_WEB_RESULT_REDIRECT:
                send_redirect(rec, data);
                *ret = DONE;
                break;
            case AM_WEB_RESULT_FORBIDDEN:
                send_deny(rec, DONE);
                *ret = DONE;
                break;
            case AM_WEB_RESULT_ERROR:
                send_error(rec, DONE);
                *ret = DONE;
                break;
            case AM_WEB_RESULT_NOT_IMPLEMENTED:
                send_error_not_implemented(rec, DONE);
                *ret = DONE;
                break;
            default:
                am_web_log_error("%s: unrecognized process result %d", thisfunc, http_result);
                send_error(rec, DONE);
                *ret = DONE;
                break;
        }
    }
    return status;
}

static am_web_req_method_t get_method_num(request_data_t *sp) {
    const char *thisfunc = "get_method_num()";
    am_web_req_method_t method_num = AM_WEB_REQUEST_UNKNOWN;
    if (!sp || !sp->s || !sp->s->http) {
        am_web_log_error("%s: invalid arguments", thisfunc);
    } else {
        method_num = am_web_method_str_to_num(http_GetReq(sp->s->http));
        am_web_log_debug("%s: Method string is %s", thisfunc, http_GetReq(sp->s->http));
        am_web_log_debug("%s: Varnish method number corresponds to %s method",
                thisfunc, am_web_method_num_to_str(method_num));
    }
    return method_num;
}

static am_status_t update_post_data_for_request(void **args, const char *key, const char *acturl, const char *value, const unsigned long postcacheentry_life) {
    const char *thisfunc = "update_post_data_for_request()";
    am_web_postcache_data_t post_data;
    void *agent_config;
    am_status_t status = AM_SUCCESS;
    agent_config = am_web_get_agent_configuration();
    if (!agent_config || !key || !acturl) {
        am_web_log_error("%s: invalid arguments", thisfunc);
        if (agent_config) am_web_delete_agent_configuration(agent_config);
        return AM_INVALID_ARGUMENT;
    } else {
        post_data.value = (char *) value;
        post_data.url = (char *) acturl;
        am_web_log_debug("%s: Register POST data key :%s", thisfunc, key);
        if (am_web_postcache_insert(key, &post_data, agent_config) == B_FALSE) {
            am_web_log_error("Register POST data insert into hash table failed: %s", key);
            status = AM_FAILURE;
        }
    }
    am_web_delete_agent_configuration(agent_config);
    return status;
}

static am_status_t check_for_post_data(void **args, const char *requestURL, char **page, const unsigned long postcacheentry_life) {
    const char *thisfunc = "check_for_post_data()";
    const char *post_data_query = NULL;
    am_web_postcache_data_t get_data = {NULL, NULL};
    const char *actionurl = NULL;
    const char *postdata_cache = NULL;
    am_status_t status = AM_SUCCESS;
    am_status_t status_tmp = AM_SUCCESS;
    void *agent_config = NULL;
    char *buffer_page = NULL;
    char *stickySessionValue = NULL;
    char *stickySessionPos = NULL;
    char *temp_uri = NULL;
    *page = NULL;

    agent_config = am_web_get_agent_configuration();

    if (agent_config == NULL) {
        am_web_log_error("%s: unable to get agent configuration", thisfunc);
        return AM_FAILURE;
    }

    if (requestURL == NULL) {
        status = AM_INVALID_ARGUMENT;
    }
    // Check if magic URI is present in the URL
    if (status == AM_SUCCESS) {
        status = AM_NOT_FOUND;
        post_data_query = strstr(requestURL, POST_PRESERVE_URI);
        if (post_data_query != NULL) {
            post_data_query += strlen(POST_PRESERVE_URI);
            status = AM_SUCCESS;
            // Check if a query parameter for the  sticky session has been
            // added to the dummy URL. Remove it if it is the case.
            status_tmp = am_web_get_postdata_preserve_URL_parameter(&stickySessionValue, agent_config);
            if (status_tmp == AM_SUCCESS) {
                stickySessionPos = strstr((char *) post_data_query, stickySessionValue);
                if (stickySessionPos != NULL) {
                    size_t len = strlen(post_data_query) - strlen(stickySessionPos) - 1;
                    temp_uri = (char *) malloc(len + 1);
                    memset(temp_uri, 0, len + 1);
                    strncpy(temp_uri, post_data_query, len);
                    post_data_query = temp_uri;
                }
            }
        }
    }
    // If magic uri present search for corresponding value in hashtable
    if ((status == AM_SUCCESS) && (post_data_query != NULL) && (strlen(post_data_query) > 0)) {
        am_web_log_debug("%s: POST Magic Query Value: %s", thisfunc, post_data_query);
        status = AM_NOT_FOUND;
        if (am_web_postcache_lookup(post_data_query, &get_data,
                agent_config) == B_TRUE) {
            postdata_cache = get_data.value;
            actionurl = get_data.url;
            am_web_log_debug("%s: POST hashtable actionurl: %s", thisfunc, actionurl);
            // Create the post page
            buffer_page = am_web_create_post_page(post_data_query,
                    postdata_cache, actionurl, agent_config);
            *page = strdup(buffer_page);
            if (*page == NULL) {
                am_web_log_error("%s: Not enough memory to allocate page");
                status = AM_NO_MEMORY;
            } else {
                status = AM_SUCCESS;
            }
            am_web_postcache_data_cleanup(&get_data);
            if (buffer_page != NULL) {
                am_web_free_memory(buffer_page);
            }
        } else {
            am_web_log_error("%s: Found magic URI (%s) but entry is not in POST hash table", thisfunc, post_data_query);
            status = AM_FAILURE;
        }
    }
    if (temp_uri != NULL) {
        free(temp_uri);
    }
    if (stickySessionValue != NULL) {
        am_web_free_memory(stickySessionValue);
    }
    am_web_delete_agent_configuration(agent_config);
    return status;
}

static const char *get_query_string(request_data_t *r, const char *url) {
    char *ptr;
    if (r && url) {
        ptr = strstr(url, "?");
        if (ptr) {
            return ptr + 1;
        }
    }
    return "";
}

unsigned vmod_authenticate(struct sess *sp, struct vmod_priv *priv, const char *req_method, const char *proto, const char *host, int port, const char *uri_p, struct sockaddr_storage * cip) {
    const char thisfunc[] = "vmod_authenticate()";
    void *agent_config;
    am_status_t status = AM_FAILURE;
    char *url;
    size_t url_len;
    request_data_t *r;
    int ret = OK;
    void *args[] = {NULL, (void*) &ret};
    char client_ip[INET6_ADDRSTRLEN];
    am_web_req_method_t method;
    am_web_request_params_t req_params;
    am_web_request_func_t req_func;
    const char *clientIP_hdr_name = NULL;
    char *clientIP_hdr = NULL;
    char *clientIP = NULL;
    const char *clientHostname_hdr_name = NULL;
    char *clientHostname_hdr = NULL;
    char *clientHostname = NULL;
    const char *uri = uri_p == NULL ? "/" : uri_p;

    memset((void *) & req_params, 0, sizeof (req_params));
    memset((void *) & req_func, 0, sizeof (req_func));

    LOG_E("vmod_authenticate %u\n", sp->xid);

    r = get_request_data(sp);
    assert(r != NULL);
    r->inauth = 1;

    if (!sp || sp->magic != SESS_MAGIC || !sp->wrk) {
        send_deny(r, DONE);
        return 0;
    }

    if (host == NULL) {
        send_badrequest(r, DONE);
        return 0;
    }

    args[0] = r;
    agent_config = am_web_get_agent_configuration();

    if (!agent_config) {
        send_deny(r, DONE);
        return 0;
    }

    am_web_log_debug("Begin process %s request, proto: %s, host: %s, port: %d, uri: %s", req_method, proto, host, port, uri);

    if (!proto) {
        url_len = strlen(host) + strlen(uri) + 8;
        url = WS_Alloc(sp->ws, url_len + 1);
        if (url) {
            snprintf(url, url_len, "http://%s%s", host, uri);
        }
    } else {
        url_len = strlen(proto) + strlen(host) + strlen(uri) + 4;
        url = WS_Alloc(sp->ws, url_len + 1);
        if (url) {
            snprintf(url, url_len, "%s://%s%s", proto, host, uri);
        }
    }

    if (!url) {
        am_web_log_error("%s: memory allocation error", thisfunc);
        status = AM_FAILURE;
    } else {
        am_web_log_debug("%s: request url: %s", thisfunc, url);
    }

    method = get_method_num(r);
    if (method == AM_WEB_REQUEST_UNKNOWN) {
        am_web_log_error("%s: Request method is unknown.", thisfunc);
        status = AM_FAILURE;
    } else {
        status = AM_SUCCESS;
    }

    if (status == AM_SUCCESS) {
        if (B_TRUE == am_web_is_notification(url, agent_config)) {
            char* data = NULL;
            status = content_read(args, &data);
            if (status == AM_SUCCESS && data != NULL && strlen(data) > 0) {
                am_web_handle_notification(data, strlen(data), agent_config);
                am_web_delete_agent_configuration(agent_config);
                am_web_log_debug("%s: received notification message, sending HTTP-200 response", thisfunc);
                send_ok(r, DONE);
                return 0;
            } else {
                am_web_log_error("%s: content_read for notification failed, %s", thisfunc, am_status_to_string(status));
                am_web_delete_agent_configuration(agent_config);
                send_deny(r, DONE);
                return 0;
            }
        }
    }

    if (status == AM_SUCCESS) {
        int vs = am_web_validate_url(agent_config, url);
        if (vs != -1) {
            if (vs == 1) {
                am_web_log_debug("%s: Request URL validation succeeded", thisfunc);
                status = AM_SUCCESS;
            } else {
                am_web_log_error("%s: Request URL validation failed. Returning Access Denied error (HTTP403)", thisfunc);
                status = AM_FAILURE;
                am_web_delete_agent_configuration(agent_config);
                send_deny(r, DONE);
                return 0;
            }
        }
    }

    if (status == AM_SUCCESS) {
        /* get the client IP address header set by the proxy, if there is one */
        clientIP_hdr_name = am_web_get_client_ip_header_name(agent_config);
        if (clientIP_hdr_name != NULL) {
            clientIP_hdr = (char *) get_req_header(r, clientIP_hdr_name);
        }
        /* get the client host name header set by the proxy, if there is one */
        clientHostname_hdr_name =
                am_web_get_client_hostname_header_name(agent_config);
        if (clientHostname_hdr_name != NULL) {
            clientHostname_hdr = (char *) get_req_header(r, clientHostname_hdr_name);
        }
        /* if the client IP and host name headers contain more than one
         * value, take the first value */
        if ((clientIP_hdr != NULL && strlen(clientIP_hdr) > 0) ||
                (clientHostname_hdr != NULL && strlen(clientHostname_hdr) > 0)) {
            status = am_web_get_client_ip_host(clientIP_hdr, clientHostname_hdr,
                    &clientIP, &clientHostname);
        }
    }

    if (status == AM_SUCCESS) {
        if (clientIP == NULL) {
            if (cip != NULL && cip->ss_family == AF_INET) {
                struct sockaddr_in *sai = (struct sockaddr_in *) cip;
                if (inet_ntop(AF_INET, &sai->sin_addr, client_ip, sizeof (client_ip)) == NULL) {
                    am_web_log_error("%s: Could not get the remote host IPv4 (error: %d)", thisfunc, errno);
                    status = AM_FAILURE;
                } else {
                    am_web_log_debug("%s: client host IPv4: %s", thisfunc, client_ip);
                    req_params.client_ip = client_ip;
                }
            } else if (cip != NULL && cip->ss_family == AF_INET6) {
                struct sockaddr_in6 *sai = (struct sockaddr_in6 *) cip;
                if (inet_ntop(AF_INET6, &sai->sin6_addr, client_ip, sizeof (client_ip)) == NULL) {
                    am_web_log_error("%s: Could not get the remote host IPv6 (error: %d)", thisfunc, errno);
                    status = AM_FAILURE;
                } else {
                    am_web_log_debug("%s: client host IPv6: %s", thisfunc, client_ip);
                    req_params.client_ip = client_ip;
                }
            } else {
                am_web_log_error("%s: Could not get the remote host IP (invalid address family)", thisfunc);
                status = AM_FAILURE;
            }
        } else {
            req_params.client_ip = clientIP;
        }
        if ((req_params.client_ip == NULL) || (strlen(req_params.client_ip) == 0)) {
            am_web_log_error("%s: Could not get the remote host IP", thisfunc);
            status = AM_FAILURE;
        }
    }

    if (status == AM_SUCCESS) {
        req_params.client_hostname = clientHostname;
        req_params.url = (char *) url;
        req_params.query = (char *) get_query_string(r, url);
        req_params.method = method;
        req_params.path_info = ""; // N/A in Varnish
        req_params.cookie_header_val = (char *) get_req_header(r, "Cookie");
        req_params.content_type = (char *) get_req_header(r, "Content-Type");
        req_func.get_post_data.func = content_read;
        req_func.get_post_data.args = args;
        req_func.free_post_data.func = NULL;
        req_func.free_post_data.args = NULL;
        req_func.set_user.func = set_user;
        req_func.set_user.args = args;
        req_func.set_method.func = set_method;
        req_func.set_method.args = args;
        req_func.set_header_in_request.func = set_header_in_request;
        req_func.set_header_in_request.args = args;
        req_func.add_header_in_response.func = add_header_in_response;
        req_func.add_header_in_response.args = args;
#ifdef CDSSO_REPOST_URL
        /*not yet supported on Varnish*/
        req_func.set_notes_in_request.func = set_notes_in_request;
        req_func.set_notes_in_request.args = args;
#endif
        req_func.render_result.func = render_result;
        req_func.render_result.args = args;
        req_func.reg_postdata.func = update_post_data_for_request;
        req_func.reg_postdata.args = args;
        req_func.check_postdata.func = check_for_post_data;
        req_func.check_postdata.args = args;

        am_web_process_request(&req_params, &req_func, &status, agent_config);

        if (status != AM_SUCCESS) {
            am_web_log_error("%s: error from am_web_process_request: %s", thisfunc, am_status_to_string(status));
        }
    }

    if (clientIP != NULL) {
        am_web_free_memory(clientIP);
    }
    if (clientHostname != NULL) {
        am_web_free_memory(clientHostname);
    }

    am_web_delete_agent_configuration(agent_config);

    if (status != AM_SUCCESS) {
        am_web_log_error("%s: error encountered rendering result: %s", thisfunc, am_status_to_string(status));
        send_error(r, DONE);
        return 0;
    }

    if (0 == ret && r->status == 200) {
        struct header *v;

        VTAILQ_FOREACH(v, &r->headers->vars, list) {
            if (v != NULL && v->type == OK &&
                    v->where == HDR_REQ) {
                struct http *hp = sp->http;
                assert(hp != NULL);
                if (v->value == NULL) {
                    http_Unset(hp, v->name);
                } else {
                    LOG_E("vmod_authenticate [%s]", v->value);
                    if (v->unset) {
                        http_Unset(hp, v->name);
                    }
                    http_SetHeader(sp->wrk, sp->fd, hp, v->value);
                }
            }
        }
    }

    return (0 == ret);
}

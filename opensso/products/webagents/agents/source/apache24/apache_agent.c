/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: apache_agent.c,v 1.1 2011/04/26 15:13:00 dknab Exp $
 */

/*
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */

#include <limits.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <httpd.h>
#include <http_config.h>
#include <http_core.h>
#include <http_protocol.h>
#include <http_request.h>
#include <http_main.h>
#include <http_log.h>
#include <ap_mpm.h>
#include <apr.h>
#include <apr_strings.h>
#include <apr_general.h>
#include <apr_version.h>
#ifdef _MSC_VER
#include <windows.h>
#include <process.h>
#include <winbase.h>
typedef int pid_t;
#define	getpid	_getpid
#define stricmp _stricmp
#define strnicmp _strnicmp
#ifndef strcasecmp
#define strcasecmp stricmp
#endif
#ifndef strncasecmp
#define strncasecmp strnicmp
#endif
#define snprintf _snprintf
#define strdup _strdup
#define strtok_r strtok_s
#else
#include <sys/mman.h>
#include <sys/stat.h> 
#include <fcntl.h>
#include <pthread.h>
#include <unistd.h>
#include <stddef.h>
#endif
#include "am_web.h"

#define OpenAM                 "OpenAM"
#define	MAGIC_STR		"sunpostpreserve"
#define	POST_PRESERVE_URI	"/dummypost/"MAGIC_STR
#define QUEUE_SIZE              512
#define MESSAGE_SIZE            8192
#define PDP_KEY_SIZE            256
#define PDP_URL_SIZE            4096
#define AM_DATA_KEY             "__AM_DATA_KEY__"

module AP_MODULE_DECLARE_DATA dsame_module;

#ifdef APLOG_USE_MODULE
APLOG_USE_MODULE(dsame);
#endif 

#define LOG_S(l,s,...) \
	ap_log_error(APLOG_MARK,l|APLOG_NOERRNO,0,s, "%s", apr_psprintf((s)->process->pool, __VA_ARGS__))

#ifdef _MSC_VER
#define AM_LOCK(x) WaitForSingleObject((x), INFINITE)
#define AM_UNLOCK(x) ReleaseMutex((x))
#define AM_DATA_SMEM "Global\\AM_SHARED_DATA_%d"
#define AM_PDP_DATA_SMEM "Global\\AM_PDP_SHARED_DATA_%d"
#define AM_DATA_SLCK "Global\\AM_SHARED_DATA_LOCK_%d"
#define AM_PDP_DATA_SLCK "Global\\AM_PDP_SHARED_DATA_LOCK_%d"
#else
#define AM_LOCK(x) pthread_mutex_lock(&(x))
#define AM_UNLOCK(x) pthread_mutex_unlock(&(x));
#define AM_DATA_SMEM "%sAM_SHARED_DATA_%d"
#define AM_PDP_DATA_SMEM "%sAM_PDP_SHARED_DATA_%d"
#endif

static am_status_t set_cookie(const char *, void **);

typedef struct {
    char *properties_file;
    char *bootstrap_file;
    char *dir;
    int instance_id;
} agent_server_config;

typedef struct {
    void *data;
    void *pdp_data;
#ifdef _MSC_VER
    HANDLE data_hdl;
    HANDLE acc;
    HANDLE pdp_data_hdl;
    HANDLE pdp_acc;
#else
    size_t data_sz;
    size_t pdp_data_sz;
    pthread_mutex_t acc;
    pthread_mutexattr_t acc_attr;
    pthread_mutex_t pdp_acc;
    pthread_mutexattr_t pdp_acc_attr;
#endif
} am_data_t;

typedef struct {
    volatile unsigned int read;
    pid_t id;
#ifdef _MSC_VER
    HANDLE thr;
    HANDLE thr_exit;
#else
    pthread_t thr;
    pthread_mutex_t thr_exit;
#endif
} am_notification_read_t;

typedef struct {
    unsigned long long count;
    unsigned char data[QUEUE_SIZE][MESSAGE_SIZE];
    size_t data_sz;
    volatile unsigned int write;
    size_t read_sz;
    am_notification_read_t read[1];
} am_notification_data_t;

typedef struct {
    unsigned long long count;
    volatile unsigned int write;
    size_t data_sz;

    struct pdp_data {
        apr_time_t created;
        char key[PDP_KEY_SIZE];
        char url[PDP_URL_SIZE];
        char value[MESSAGE_SIZE];
    } data[QUEUE_SIZE];
} am_pdp_data_t;

static void post_notification(char *value, server_rec *s) {
    int slen, idx, i;
    unsigned int wli;
    am_data_t *md = NULL;
    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);
    if (value == NULL || (slen = strlen(value)) == 0 || slen > MESSAGE_SIZE) {
        am_web_log_error("post_notification() notification message is empty or exceeds MESSAGE_SIZE [%d] size [%d]",
                MESSAGE_SIZE, slen);
        return;
    }
    am_web_log_debug("post_notification() notification message size: %d bytes", slen);
    if (md && md->data != NULL) {
        am_notification_data_t *d = (am_notification_data_t *) md->data;

        /* the writer must not write over parts which readers have not read yet */
        idx = 0;
        for (i = 0; i < d->read_sz; i++) {
            if (d->read[i].id > 0 &&
                    ((d->write + 1) % d->data_sz) == d->read[i].read) {
                idx = 1;
            }
        }
        if (idx) {
            am_web_log_warning("post_notification() unable to handle notification request, processing queue is full [%d]",
                    QUEUE_SIZE);
            return;
        }

        AM_LOCK(md->acc);
        memset(d->data[d->write], 0x00, MESSAGE_SIZE);
        memcpy(d->data[d->write], value, slen);
        d->write = (d->write + 1) % d->data_sz;
        d->count++;
        wli = d->write;
        AM_UNLOCK(md->acc);
        am_web_log_debug("post_notification() succeeded [%d]", wli);
    } else {
        am_web_log_error("post_notification() shared memory failed");
    }
}

static am_status_t find_post_data(char *id, am_web_postcache_data_t *pd, const unsigned long postcacheentry_life, server_rec *s) {
    am_status_t status = AM_FAILURE;
    am_data_t *md = NULL;
    int i;
    apr_time_t now = apr_time_now();
    if (postcacheentry_life >= 1L) {
        now -= apr_time_from_sec(postcacheentry_life * 60);
    } else {
        am_web_log_warning("find_post_data(): invalid com.sun.identity.agents.config.postcache.entry.lifetime value, defaulting to 3 minutes");
        now -= apr_time_from_sec(180);
    }
    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);
    if (md && md->pdp_data != NULL) {
        am_pdp_data_t *d = (am_pdp_data_t *) md->pdp_data;

        for (i = 0; i < d->data_sz; i++) {
            if (id != NULL && strcmp(d->data[i].key, id) == 0) {
                if (d->data[i].created < now) {
                    am_web_log_warning("find_post_data(): entry [%s] is obsolete", id);
                    status = AM_FAILURE;
                } else {
                    status = AM_SUCCESS;
                }
                break;
            }
        }

        if (status == AM_SUCCESS) {
            AM_LOCK(md->pdp_acc);
            if (id != NULL && strcmp(d->data[i].key, id) == 0) {
                am_web_log_debug("find_post_data() %d: %s", i, d->data[i].key);
                pd->url = strdup(d->data[i].url);
                pd->value = strdup(d->data[i].value);
            }
            AM_UNLOCK(md->pdp_acc);
            am_web_log_debug("find_post_data() succeeded");
        }
    } else {
        am_web_log_error("find_post_data() shared memory failed");
    }
    return status;
}

static void pdp_dump(server_rec *s) {
    am_data_t *md = NULL;
    int i;
    char date[APR_RFC822_DATE_LEN];
    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);
    if (md && md->pdp_data != NULL) {
        am_pdp_data_t *d = (am_pdp_data_t *) md->pdp_data;
        AM_LOCK(md->pdp_acc);
        am_web_log_debug("pdp_dump() write: %d", d->write);
        for (i = 0; i < d->data_sz; i++) {
            apr_rfc822_date(date, d->data[i].created);
            am_web_log_debug("pdp_dump() [%d] created: %s, key: %s, url: %s, data: %s",
                    i, date, d->data[i].key, d->data[i].url, d->data[i].value);
        }
        AM_UNLOCK(md->pdp_acc);
    } else {
        am_web_log_debug("pdp_dump() shared memory failed");
    }
}

static am_status_t update_post_data_for_request(void **args, const char *key, const char *acturl, const char *value, const unsigned long postcacheentry_life) {
    const char *thisfunc = "update_post_data_for_request()";
    am_status_t status = AM_FAILURE;
    request_rec *r = NULL;
    apr_time_t now;
    am_data_t *md = NULL;
    size_t klen, ulen, vlen;
    struct pdp_data pdp;

    am_web_log_debug("%s: updating post data cache for key: %s", thisfunc, key);
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL || acturl == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    } else {

        if (key == NULL || acturl == NULL || value == NULL
                || (klen = strlen(key)) > PDP_KEY_SIZE
                || (ulen = strlen(acturl)) > PDP_URL_SIZE
                || (vlen = strlen(value)) > MESSAGE_SIZE) {
            am_web_log_warning("%s: key, action_url or value is empty or their values exceed MAX limits [%d],[%d],[%d]", thisfunc, klen, ulen, vlen);
            return status;
        }

        apr_pool_userdata_get((void *) &md, AM_DATA_KEY, r->server->process->pool);
        if (md && md->pdp_data != NULL) {
            am_pdp_data_t *d = (am_pdp_data_t *) md->pdp_data;
            am_web_log_debug("%s: key size [%d], url size [%d], value size [%d]", thisfunc, klen, ulen, vlen);
            now = apr_time_now();
            if (postcacheentry_life >= 1L) {
                now -= apr_time_from_sec(postcacheentry_life * 60);
            } else {
                am_web_log_warning("%s: invalid com.sun.identity.agents.config.postcache.entry.lifetime value, defaulting to 3 minutes", thisfunc);
                now -= apr_time_from_sec(180);
            }

            memset(&pdp, 0x00, sizeof (struct pdp_data));
            memcpy(pdp.key, key, klen);
            pdp.key[klen] = '\0';
            memcpy(pdp.url, acturl, ulen);
            pdp.url[ulen] = '\0';
            memcpy(pdp.value, value, vlen);
            pdp.value[vlen] = '\0';
            pdp.created = apr_time_now();

            AM_LOCK(md->pdp_acc);
            memcpy(&d->data[d->write], &pdp, sizeof (struct pdp_data));
            d->write = (d->write + 1) % d->data_sz;
            d->count++;
            AM_UNLOCK(md->pdp_acc);
            am_web_log_debug("update_post_data_for_request() succeeded");
            status = AM_SUCCESS;
        }
#ifdef DEBUG
        pdp_dump(r->server);
#endif
    }
    return status;
}

static const char *am_set_string_slot(cmd_parms *cmd, void *dummy, const char *arg) {
    char *error_str = NULL;
    int offset = (int) (long) cmd->info;
    agent_server_config *fsc = ap_get_module_config(cmd->server->module_config, &dsame_module);
    *(const char **) ((char *) fsc + offset) = arg;
    if (*arg == '\0') {
        error_str = apr_psprintf(cmd->pool,
                "Invalid value for directive %s, expected string",
                cmd->directive->directive);
    }
    return error_str;
}

static const char *am_set_int_slot(cmd_parms *cmd, void *dummy, const char *arg) {
    char *endptr;
    char *error_str = NULL;
    int offset = (int) (long) cmd->info;
    agent_server_config *fsc = ap_get_module_config(cmd->server->module_config, &dsame_module);
    *(int *) ((char *) fsc + offset) = strtol(arg, &endptr, 10);
    if ((*arg == '\0') || (*endptr != '\0')) {
        error_str = apr_psprintf(cmd->pool,
                "Invalid value for directive %s, expected integer",
                cmd->directive->directive);
    }
    return error_str;
}

static const command_rec agent_auth_cmds[] = {
    AP_INIT_TAKE1("Agent_Config_File", am_set_string_slot, (void *) APR_OFFSETOF(agent_server_config, properties_file), RSRC_CONF,
    "Full path of the Agent configuration file"),
    AP_INIT_TAKE1("Agent_Bootstrap_File", am_set_string_slot, (void *) APR_OFFSETOF(agent_server_config, bootstrap_file), RSRC_CONF,
    "Full path of the Agent bootstrap file"),
    AP_INIT_TAKE1("Agent_Instance_Id", am_set_int_slot, (void *) APR_OFFSETOF(agent_server_config, instance_id), RSRC_CONF,
    "Agent Instance Id"), {
        NULL
    }
};

#ifndef _MSC_VER

static int need_quit(pthread_mutex_t *mtx) {
    switch (pthread_mutex_trylock(mtx)) {
        case 0:
            pthread_mutex_unlock(mtx);
            return 1;
        case EBUSY:
            return 0;
    }
    return 1;
}
#endif

static void *notification_listener(void *arg) {
    server_rec *s = (server_rec *) arg;
    am_data_t *md = NULL;
    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);
    if (md && md->data != NULL) {
        am_notification_data_t *d = (am_notification_data_t *) md->data;
        pid_t pid = getpid();
        int i = 0;
        unsigned char *data = (unsigned char *) apr_pcalloc(s->process->pool, MESSAGE_SIZE + 1);
        if (data == NULL) {
            LOG_S(APLOG_ERR, s, "notification_listener() memory failure");
            return NULL;
        }

        for (i = 0; i < d->read_sz; i++) {
            if (d->read[i].id == pid) {
                break;
            }
        }

#ifdef _MSC_VER
        while (WaitForSingleObject(d->read[i].thr_exit, 0) == WAIT_TIMEOUT) {
            if (d->read[i].read == d->write) {
                SleepEx(250, FALSE); /* 1/4 sec */
#else
        while (!need_quit(&d->read[i].thr_exit)) {
            if (d->read[i].read == d->write) {
                usleep(250000); /* 1/4 sec */
#endif
            } else {
                void *agent_config = am_web_get_agent_configuration();
                if (agent_config != NULL) {
                    memcpy(data, d->data[d->read[i].read], MESSAGE_SIZE);
                    data[MESSAGE_SIZE] = 0;
                    am_web_log_debug("notification_listener() processing [%d][%d]...", i, d->read[i].read);
                    am_web_handle_notification((const char *) data, strlen((const char *) data), agent_config);
                    d->read[i].read = (d->read[i].read + 1) % d->data_sz;
                    am_web_delete_agent_configuration(agent_config);
                } else {
                    am_web_log_error("notification_listener() failed to fetch agent configuration [%d][%d]", i, d->read[i].read);
                }
            }
        }
    } else {
        LOG_S(APLOG_ERR, s, "notification_listener() shared memory failed");
    }
    am_web_log_info("notification_listener() shutting down");
    return NULL;
}

static void *agent_create_server_config(apr_pool_t *p, server_rec *s) {
    char *tmpdir = NULL;
    agent_server_config *cfg = apr_pcalloc(p, sizeof (agent_server_config));
    if (cfg != NULL) {
        if (apr_temp_dir_get((const char**) &tmpdir, p) != APR_SUCCESS) {
            LOG_S(APLOG_ERR, s, "Web Policy Agent failed to locate temporary storage directory");
        }
        cfg->dir = "/";
        cfg->instance_id = 0;
    }
    return (void *) cfg;
}

static apr_status_t agent_cleanup(void *arg) {
    /* main process cleanup */

#ifndef _MSC_VER

    am_data_t *d = NULL;
    char shm_nname[256];
    char shm_pname[256];
    server_rec *s = (server_rec *) arg;
    agent_server_config *c = ap_get_module_config(s->module_config, &dsame_module);
    LOG_S(APLOG_DEBUG, s, "agent_cleanup() instance: %d", c->instance_id);
    apr_pool_userdata_get((void *) &d, AM_DATA_KEY, s->process->pool);
    if (d != NULL) {
        am_notification_data_t *nd = (am_notification_data_t *) d->data;
        am_pdp_data_t *pd = (am_pdp_data_t *) d->pdp_data;

        snprintf(shm_nname, sizeof (shm_nname), AM_DATA_SMEM, c->dir, c->instance_id);
        snprintf(shm_pname, sizeof (shm_pname), AM_PDP_DATA_SMEM, c->dir, c->instance_id);

        if (nd != NULL && pd != NULL) {
            LOG_S(APLOG_DEBUG, s, "agent_cleanup() processed %ld notification and %ld post-data messages", nd->count, pd->count);
        }

        pthread_mutexattr_destroy(&d->acc_attr);
        pthread_mutex_destroy(&d->acc);
        if (munmap(d->data, d->data_sz) == -1) {
            LOG_S(APLOG_ERR, s, "agent_cleanup() am_notification_data_t munmap failed (%d)", errno);
        }
        if (shm_unlink(shm_nname) == -1) {
            LOG_S(APLOG_ERR, s, "agent_cleanup() am_notification_data_t shm_unlink failed (%d)", errno);
        }

        pthread_mutexattr_destroy(&d->pdp_acc_attr);
        pthread_mutex_destroy(&d->pdp_acc);
        if (munmap(d->pdp_data, d->pdp_data_sz) == -1) {
            LOG_S(APLOG_ERR, s, "agent_cleanup() am_pdp_data_t munmap failed (%d)", errno);
        }
        if (shm_unlink(shm_pname) == -1) {
            LOG_S(APLOG_ERR, s, "agent_cleanup() am_pdp_data_t shm_unlink failed (%d)", errno);
        }
    }

    am_web_cleanup();
    am_shutdown_nss();

#endif
    return APR_SUCCESS;
}

static int agent_init(apr_pool_t *pconf, apr_pool_t *plog, apr_pool_t *ptemp, server_rec *s) {
    /* 
     * main process init
     * not used/supported in mpm_winnt environments
     */
    char **v = NULL;

#ifndef _MSC_VER 

    void *data;
    char shm_nname[256];
    char shm_pname[256];
    apr_status_t rv;
    int max_threads = 0, max_procs = 0, is_threaded = 0, is_forked = 0, max_clients;
    const char *data_key = "agent_init_data_key";
    size_t sz, pdp_sz;
    agent_server_config *c = ap_get_module_config(s->module_config, &dsame_module);
    am_data_t *sd;

    apr_pool_userdata_get(&data, data_key, s->process->pool);
    if (!data) {
        /* module has already been initialized */
        apr_pool_userdata_set((const void *) 1, data_key, apr_pool_cleanup_null, s->process->pool);
        return OK;
    }

    LOG_S(APLOG_DEBUG, s, "agent_init() instance: %d", c->instance_id);

    snprintf(shm_nname, sizeof (shm_nname), AM_DATA_SMEM, c->dir, c->instance_id);
    snprintf(shm_pname, sizeof (shm_pname), AM_PDP_DATA_SMEM, c->dir, c->instance_id);

    ap_mpm_query(AP_MPMQ_IS_THREADED, &is_threaded);
    if (is_threaded != AP_MPMQ_NOT_SUPPORTED) {
        ap_mpm_query(AP_MPMQ_MAX_THREADS, &max_threads);
    }
    ap_mpm_query(AP_MPMQ_IS_FORKED, &is_forked);
    if (is_forked != AP_MPMQ_NOT_SUPPORTED) {
        ap_mpm_query(AP_MPMQ_MAX_DAEMON_USED, &max_procs);
        if (max_procs == -1) {
            ap_mpm_query(AP_MPMQ_MAX_DAEMONS, &max_procs);
        }
    }
    max_clients = (((max_threads <= 0) ? 1 : max_threads) *
            ((max_procs <= 0) ? 1 : max_procs));
    LOG_S(APLOG_DEBUG, s, "agent_init() max_procs: %d", max_procs);

    sz = APR_ALIGN_DEFAULT(sizeof (am_notification_data_t) + (sizeof (am_notification_read_t) * max_procs));
    pdp_sz = APR_ALIGN_DEFAULT(sizeof (am_pdp_data_t));

    LOG_S(APLOG_DEBUG, s, "agent_init() am_notification_data_t size: %ld, am_pdp_data_t size: %ld", sz, pdp_sz);

    sd = apr_pcalloc(s->process->pool, sizeof (am_data_t));
    memset(sd, 0, sizeof (am_data_t));

    shm_unlink(shm_nname);
    shm_unlink(shm_pname);

    int id = shm_open(shm_nname, (O_CREAT | O_EXCL | O_RDWR), (S_IRUSR | S_IWUSR));
    if (id != -1) {
        if (ftruncate(id, sz) == -1) {
            LOG_S(APLOG_ERR, s, "agent_init() am_notification_data_t ftruncate failed");
        }
    } else {
        if (errno == EEXIST) {
            id = shm_open(shm_nname, O_RDWR, (S_IRUSR | S_IWUSR));
            if (id == -1) {
                LOG_S(APLOG_ERR, s, "agent_init() am_notification_data_t shm_open failed (%d)", errno);
            }
        } else {
            LOG_S(APLOG_ERR, s, "agent_init() am_notification_data_t shm_open (create) failed (%d)", errno);
        }
    }

    int id_pdp = shm_open(shm_pname, (O_CREAT | O_EXCL | O_RDWR), (S_IRUSR | S_IWUSR));
    if (id_pdp != -1) {
        if (ftruncate(id_pdp, pdp_sz) == -1) {
            LOG_S(APLOG_ERR, s, "agent_init() am_pdp_data_t ftruncate failed");
        }
    } else {
        if (errno == EEXIST) {
            id_pdp = shm_open(shm_pname, O_RDWR, (S_IRUSR | S_IWUSR));
            if (id_pdp == -1) {
                LOG_S(APLOG_ERR, s, "agent_init() am_pdp_data_t shm_open failed (%d)", errno);
            }
        } else {
            LOG_S(APLOG_ERR, s, "agent_init() am_pdp_data_t shm_open (create) failed (%d)", errno);
        }
    }

    if (id != -1) {
        am_notification_data_t *d = (am_notification_data_t *) mmap(NULL, sz, (PROT_READ | PROT_WRITE), MAP_SHARED, id, 0);
        if (d == MAP_FAILED) {
            LOG_S(APLOG_ERR, s, "agent_init() am_notification_data_t mmap failed (%d)", errno);
        } else {
            int i;
            memset(d, 0x00, sz);
            d->read_sz = max_procs;
            am_notification_read_t rt[d->read_sz];
            memcpy(d->read, rt, sizeof (am_notification_read_t) * d->read_sz);
            for (i = 0; i < d->read_sz; i++) {
                d->read[i].read = 0;
                d->read[i].id = 0;
            }
            d->count = 0;
            d->write = 0;
            d->data_sz = QUEUE_SIZE;

            sd->data = d;
            sd->data_sz = sz;

            pthread_mutexattr_init(&sd->acc_attr);
            pthread_mutexattr_setpshared(&sd->acc_attr, PTHREAD_PROCESS_SHARED);
            pthread_mutex_init(&sd->acc, &sd->acc_attr);
        }
        close(id);
    }

    if (id_pdp != -1) {
        am_pdp_data_t *d = (am_pdp_data_t *) mmap(NULL, pdp_sz, (PROT_READ | PROT_WRITE), MAP_SHARED, id_pdp, 0);
        if (d == MAP_FAILED) {
            LOG_S(APLOG_ERR, s, "agent_init() am_pdp_data_t mmap failed (%d)", errno);
        } else {
            memset(d, 0x00, pdp_sz);
            d->data_sz = QUEUE_SIZE;
            d->count = 0;
            d->write = 0;

            sd->pdp_data = d;
            sd->pdp_data_sz = pdp_sz;

            pthread_mutexattr_init(&sd->pdp_acc_attr);
            pthread_mutexattr_setpshared(&sd->pdp_acc_attr, PTHREAD_PROCESS_SHARED);
            pthread_mutex_init(&sd->pdp_acc, &sd->pdp_acc_attr);
        }
        close(id_pdp);
    }

    rv = apr_pool_userdata_set((void *) sd, AM_DATA_KEY, apr_pool_cleanup_null, s->process->pool);
    if (rv == APR_SUCCESS) {
        LOG_S(APLOG_DEBUG, s, "agent_init() initialization succeeded");
    } else {
        LOG_S(APLOG_ERR, s, "agent_init() initialization failed");
    }

    apr_pool_cleanup_register(pconf, s, agent_cleanup, apr_pool_cleanup_null);

    if (am_web_init(c->bootstrap_file, c->properties_file) != AM_SUCCESS) {
        LOG_S(APLOG_ERR, s, "agent_init() am_web_init failed");
        return HTTP_INTERNAL_SERVER_ERROR;
    }

#endif
    v = (char **) apr_pcalloc(pconf, 4 * sizeof (char *));
    if (v != NULL) {
        am_agent_version(v);
        ap_add_version_component(pconf, apr_psprintf(pconf, "OpenAM WPA/%s", v[0]));
    }
    return OK;
}

static apr_status_t agent_worker_cleanup(void *arg) {
    /* worker process cleanup */
    server_rec *s = (server_rec *) arg;
    am_data_t *md = NULL;
    am_web_log_debug("agent_worker_cleanup()");
    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);
    if (md && md->data != NULL) {
        am_notification_data_t *d = (am_notification_data_t *) md->data;
        pid_t pid = getpid();
        int i = 0;
        for (i = 0; i < d->read_sz; i++) {
            if (d->read[i].id == pid) {
                d->read[i].id = 0;
                break;
            }
        }
        am_web_log_debug("agent_worker_cleanup() pid: %d, notification reader id: %d", pid, i);
#ifdef _MSC_VER
        SetEvent(d->read[i].thr_exit);
        WaitForSingleObject(d->read[i].thr, INFINITE);
        CloseHandle(d->read[i].thr);
        CloseHandle(d->read[i].thr_exit);

        am_web_log_debug("agent_worker_cleanup() processed %ld notification messages", d->count);

        CloseHandle(md->acc);
        UnmapViewOfFile(md->data);
        CloseHandle(md->data_hdl);

        CloseHandle(md->pdp_acc);
        UnmapViewOfFile(md->pdp_data);
        CloseHandle(md->pdp_data_hdl);

#else
        pthread_mutex_unlock(&d->read[i].thr_exit);
        pthread_join(d->read[i].thr, NULL);
        pthread_mutex_destroy(&d->read[i].thr_exit);
#endif
    }
    am_agent_cleanup();
    return APR_SUCCESS;
}

#ifdef _MSC_VER

static void agent_worker_pre_init(apr_pool_t *pool_ptr, server_rec *s) {
    /* worker process pre-init on mpm_winnt platforms */
    char shm_nname[MAX_PATH];
    char shm_pname[MAX_PATH];
    char acc_nname[MAX_PATH];
    char acc_pname[MAX_PATH];
    apr_status_t rv;
    DWORD sz, pdp_sz;
    am_data_t *sd;
    am_notification_data_t *d;
    am_pdp_data_t *pd;
    HANDLE id, pdp_id, acc, pdp_acc;
    agent_server_config *c = ap_get_module_config(s->module_config, &dsame_module);

    LOG_S(APLOG_DEBUG, s, "agent_worker_pre_init()");

    snprintf(shm_nname, sizeof (shm_nname), AM_DATA_SMEM, c->instance_id);
    snprintf(shm_pname, sizeof (shm_pname), AM_PDP_DATA_SMEM, c->instance_id);
    snprintf(acc_nname, sizeof (acc_nname), AM_DATA_SLCK, c->instance_id);
    snprintf(acc_pname, sizeof (acc_pname), AM_PDP_DATA_SLCK, c->instance_id);

#define MPM_WINNT_MAXPROC 1

    sz = sizeof (am_notification_data_t) + (sizeof (am_notification_read_t) * MPM_WINNT_MAXPROC);
    pdp_sz = sizeof (am_pdp_data_t);

    LOG_S(APLOG_DEBUG, s, "agent_worker_pre_init() am_notification_data_t size: %ld, am_pdp_data_t size: %ld", sz, pdp_sz);

    sd = apr_pcalloc(s->process->pool, sizeof (am_data_t));
    memset(sd, 0, sizeof (am_data_t));

    id = CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, sz, shm_nname);
    if (id != NULL) {
        d = (am_notification_data_t *) MapViewOfFile(id, FILE_MAP_ALL_ACCESS, 0, 0, sz);
        if (d != NULL) {
            int i;
            am_notification_read_t rt[MPM_WINNT_MAXPROC];
            memset(d, 0x00, sz);
            d->read_sz = MPM_WINNT_MAXPROC;
            memcpy(d->read, rt, sizeof (am_notification_read_t) * d->read_sz);
            for (i = 0; i < d->read_sz; i++) {
                d->read[i].read = 0;
                d->read[i].id = 0;
            }
            d->count = 0;
            d->write = 0;
            d->data_sz = QUEUE_SIZE;

            sd->data = d;
            sd->data_hdl = id;

            acc = CreateMutex(NULL, FALSE, acc_nname);
            if (acc == NULL && GetLastError() == ERROR_ACCESS_DENIED) {
                acc = OpenMutex(SYNCHRONIZE, FALSE, acc_nname);
            }
            sd->acc = acc;
            if (!sd->acc) {
                LOG_S(APLOG_ERR, s, "agent_worker_pre_init() mutex initialization failed");
            }
        } else {
            LOG_S(APLOG_ERR, s, "agent_worker_pre_init() shared memory map failed");
        }
    } else {
        LOG_S(APLOG_ERR, s, "agent_worker_pre_init() shared memory open failed");
    }

    pdp_id = CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, pdp_sz, shm_pname);
    if (pdp_id != NULL) {
        pd = (am_pdp_data_t *) MapViewOfFile(pdp_id, FILE_MAP_ALL_ACCESS, 0, 0, pdp_sz);
        if (pd != NULL) {
            memset(pd, 0x00, pdp_sz);
            pd->data_sz = QUEUE_SIZE;
            pd->count = 0;
            pd->write = 0;

            sd->pdp_data = pd;
            sd->pdp_data_hdl = pdp_id;

            pdp_acc = CreateMutex(NULL, FALSE, acc_pname);
            if (pdp_acc == NULL && GetLastError() == ERROR_ACCESS_DENIED) {
                pdp_acc = OpenMutex(SYNCHRONIZE, FALSE, acc_pname);
            }
            sd->pdp_acc = pdp_acc;
            if (!sd->pdp_acc) {
                LOG_S(APLOG_ERR, s, "agent_worker_pre_init() pdp mutex initialization failed");
            }
        } else {
            LOG_S(APLOG_ERR, s, "agent_worker_pre_init() pdp shared memory map failed");
        }
    } else {
        LOG_S(APLOG_ERR, s, "agent_worker_pre_init() pdp shared memory open failed");
    }

    rv = apr_pool_userdata_set((void *) sd, AM_DATA_KEY, apr_pool_cleanup_null, s->process->pool);
    if (rv == APR_SUCCESS) {
        LOG_S(APLOG_DEBUG, s, "agent_worker_pre_init() initialization succeeded");
    } else {
        LOG_S(APLOG_ERR, s, "agent_worker_pre_init() initialization failed");
    }

    if (am_web_init(c->bootstrap_file, c->properties_file) != AM_SUCCESS) {
        LOG_S(APLOG_ERR, s, "agent_worker_pre_init() am_web_init failed");
    }
}

#endif

static void agent_worker_init(apr_pool_t *pool_ptr, server_rec *s) {
    /* worker process init */
    boolean_t init = B_FALSE;
    int rv = 0;
    am_data_t *md = NULL;

#ifdef _MSC_VER
    agent_worker_pre_init(pool_ptr, s);
#endif

    am_web_log_debug("agent_worker_init()");

    apr_pool_userdata_get((void *) &md, AM_DATA_KEY, s->process->pool);

#ifdef _MSC_VER
    if (md == NULL) {
        return;
    }
#endif

    if (md && md->data != NULL && md->pdp_data != NULL) {
        int i = 0;
        am_notification_data_t *d = (am_notification_data_t *) md->data;

        if (am_agent_init(&init) != AM_SUCCESS) {
            am_web_log_error("agent_worker_init() am_agent_init failed");
        }

        /*register worker process*/
        AM_LOCK(md->acc);
        for (i = 0; i < d->read_sz; i++) {
            if (d->read[i].id == 0) {
                d->read[i].id = getpid();
                break;
            }
        }
        AM_UNLOCK(md->acc);

#ifdef _MSC_VER

        d->read[i].thr_exit = CreateEvent(0, FALSE, FALSE, 0);
        if (d->read[i].thr_exit == NULL) {
            am_web_log_error("agent_worker_init() event create failed (%d)", GetLastError());
        } else {
            d->read[i].thr = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) notification_listener, (void *) s, 0, NULL);
            if (d->read[i].thr == NULL) {
                am_web_log_error("agent_worker_init() thread create failed (%d)", GetLastError());
            }
        }
#else
        rv = pthread_mutex_init(&d->read[i].thr_exit, NULL);
        if (rv != 0) {
            am_web_log_error("agent_worker_init() mutex create failed (%d)", rv);
        } else {
            pthread_mutex_lock(&d->read[i].thr_exit);
            if ((rv = pthread_create(&d->read[i].thr, NULL, notification_listener, (void *) s)) != 0) {
                am_web_log_error("agent_worker_init() thread create failed (%d)", rv);
            }
        }
#endif

        am_web_log_debug("agent_worker_init() am_notification_data_t open succeeded");

    } else {
        am_web_log_error("agent_worker_init() shared memory failed");
    }

    /*apr_pool_pre_cleanup_register(pool_ptr, s, agent_worker_cleanup);*/
    apr_pool_cleanup_register(pool_ptr, s, agent_worker_cleanup, apr_pool_cleanup_null);
}

static am_status_t render_result(void **args, am_web_result_t http_result, char *data) {
    request_rec *r = NULL;
    const char *thisfunc = "render_result()";
    int *apache_ret = NULL;
    am_status_t sts = AM_SUCCESS;
    int len = 0;
    char *url = NULL;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL,
            (apache_ret = (int *) args[1]) == NULL ||
            ((http_result == AM_WEB_RESULT_OK_DONE ||
            http_result == AM_WEB_RESULT_REDIRECT) &&
            (data == NULL || *data == '\0'))) {
        am_web_log_error("%s: invalid arguments received.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // only redirect and OK-DONE need special handling.
        // ok, forbidden and internal error can just set in the result.
        switch (http_result) {
            case AM_WEB_RESULT_OK:
                *apache_ret = OK;
                break;
            case AM_WEB_RESULT_OK_DONE:
                if (data && ((len = strlen(data)) > 0)) {
                    ap_set_content_type(r, "text/html");
                    ap_set_content_length(r, len);
                    ap_rwrite(data, len, r);
                    ap_rflush(r);
                    *apache_ret = DONE;
                } else {
                    *apache_ret = OK;
                }
                break;
            case AM_WEB_RESULT_REDIRECT:
                if ((url = (char*) apr_table_get(r->notes, "CDSSO_REPOST_URL"))) {
                    r->method = "GET";
                    r->method_number = M_GET;
                    apr_table_unset(r->notes, "CDSSO_REPOST_URL");
                    ap_internal_redirect(url, r);
                    *apache_ret = DONE;
                } else {
                    ap_custom_response(r, HTTP_MOVED_TEMPORARILY, data);
                    *apache_ret = HTTP_MOVED_TEMPORARILY;
                }
                break;
            case AM_WEB_RESULT_FORBIDDEN:
                *apache_ret = HTTP_FORBIDDEN;
                break;
            case AM_WEB_RESULT_ERROR:
                *apache_ret = HTTP_INTERNAL_SERVER_ERROR;
                break;
            case AM_WEB_RESULT_NOT_IMPLEMENTED:
                *apache_ret = HTTP_NOT_IMPLEMENTED;
                break;
            default:
                am_web_log_error("%s: Unrecognized process result %d.", thisfunc, http_result);
                *apache_ret = HTTP_INTERNAL_SERVER_ERROR;
                break;
        }
        sts = AM_SUCCESS;
    }
    return sts;
}

/**
 * Get and normalize request URL. Trailing forward slashes are not
 * recognized as part of a resource name.
 */
static am_status_t get_request_url(request_rec *r, char **requestURL) {
    const char *thisfunc = "get_request_url()";
    am_status_t status = AM_SUCCESS;
    *requestURL = ap_construct_url(r->pool, r->unparsed_uri, r);
    if (*requestURL == NULL) {
        status = AM_FAILURE;
    } else {
        char *url = *requestURL;
#ifdef OPENAM_2969
        am_web_log_debug("%s: request url before normalization: %s", thisfunc, url);
        /*find the end of url string*/
        while (url && *url) ++url;
        for (--url; *requestURL < url; --url) {
            if (*url == '/') {
                /*erase (all) trailing slashes*/
                *url = 0;
            } else break;
        }
#endif
    }
    am_web_log_debug("%s: returning request url: %s", thisfunc, *requestURL);
    return status;
}

static am_status_t content_read(void **args, char **rbuf) {
    const char *thisfunc = "content_read()";
    request_rec *r = NULL;
    int rc = 0;
    int rsize = 0, len_read = 0, rpos = 0;
    int sts = AM_FAILURE;
    const char *new_clen_val = NULL;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL || rbuf == NULL) {
        am_web_log_error("%s: invalid arguments passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else if ((rc = ap_setup_client_block(r, REQUEST_CHUNKED_ERROR)) != OK) {
        am_web_log_error("%s: error setup client block: %d", thisfunc, rc);
        sts = AM_FAILURE;
    } else if (ap_should_client_block(r)) {
        char argsbuffer[HUGE_STRING_LEN];
        long length = r->remaining;
        *rbuf = apr_pcalloc(r->pool, length + 1);
        while ((len_read = ap_get_client_block(r, argsbuffer, sizeof (argsbuffer))) > 0) {
            if ((rpos + len_read) > length) {
                rsize = length - rpos;
            } else {
                rsize = len_read;
            }
            memcpy((char*) * rbuf + rpos, argsbuffer, rsize);
            rpos = rpos + rsize;
        }
        am_web_log_debug("%s: Read %d bytes", thisfunc, rpos);
        sts = AM_SUCCESS;
    } else {
        *rbuf = NULL;
        am_web_log_warning("%s: post data is empty", thisfunc);
        sts = AM_NOT_FOUND;
    }

    // Remove the content length since the body has been read.
    // If the content length is not reset, servlet containers think
    // the request is a POST.
    if (sts == AM_SUCCESS) {
        r->clength = 0;
        apr_table_unset(r->headers_in, "Content-Length");
        new_clen_val = apr_table_get(r->headers_in, "Content-Length");
        am_web_log_debug("content_read(): New value "
                "of content length after reset: %s",
                new_clen_val ? "(NULL)" : new_clen_val);
    }
    return sts;
}

static am_status_t set_header_in_request(void **args, const char *key, const char *values) {
    const char *thisfunc = "set_header_in_request()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        // remove all instances of the header first.
        apr_table_unset(r->headers_in, key);
        if (values != NULL && *values != '\0') {
            apr_table_set(r->headers_in, key, values);
        }
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t add_header_in_response(void **args, const char *key, const char *values) {
    const char *thisfunc = "add_header_in_response()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        if (values == NULL) {
            /*value is empty, sdk is setting a cookie in response*/
            sts = set_cookie(key, args);
        } else {
            /* Apache keeps two separate server response header tables in the request 
             * record—one for normal response headers and one for error headers. 
             * The difference between them is that the error headers are sent to 
             * the client even (not only) on an error response (REDIRECT is one of them)
             */
            apr_table_add(r->err_headers_out, key, values);
            sts = AM_SUCCESS;
        }
    }
    return sts;
}

static am_status_t set_notes_in_request(void **args, const char *key, const char *values) {
    const char *thisfunc = "set_notes_in_request()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        apr_table_unset(r->notes, key);
        if (values != NULL && *values != '\0') {
            apr_table_set(r->notes, key, values);
        }
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t set_method(void **args, am_web_req_method_t method) {
    const char *thisfunc = "set_method()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL) {
        am_web_log_error("%s: invalid argument passed", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        r->method = am_web_method_num_to_str(method);
        r->method_number = ap_method_number_of(r->method);
        am_web_log_debug("%s: %s (%d)", thisfunc, r->method, r->method_number);
        if (r->method_number == M_INVALID) {
            sts = AM_INVALID_ARGUMENT;
            am_web_log_error("%s: invalid method [%s] passed", thisfunc, r->method);
        } else sts = AM_SUCCESS;
    }
    return sts;
}

static am_status_t set_user(void **args, const char *user) {
    const char *thisfunc = "set_user()";
    request_rec *r = NULL;
    am_status_t sts = AM_SUCCESS;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        sts = AM_INVALID_ARGUMENT;
    } else {
        if (user == NULL) {
            user = "";
        }
        r->user = apr_pstrdup(r->pool, user);
        r->ap_auth_type = apr_pstrdup(r->pool, OpenAM);
        am_web_log_debug("%s: user set to %s", thisfunc, user);
        sts = AM_SUCCESS;
    }
    return sts;
}

static am_web_req_method_t get_method_num(request_rec *r) {
    const char *thisfunc = "get_method_num()";
    am_web_req_method_t method_num = AM_WEB_REQUEST_UNKNOWN;
    const char *mthd = ap_method_name_of(r->pool, r->method_number);
    am_web_log_debug("%s: %s (%s, %d)", thisfunc, r->method, mthd, r->method_number);
    if (r->method_number == M_GET && r->header_only > 0) {
        method_num = AM_WEB_REQUEST_HEAD;
    } else {
        method_num = am_web_method_str_to_num(mthd);
    }
    am_web_log_debug("%s: number corresponds to %s method",
            thisfunc, am_web_method_num_to_str(method_num));
    // Check if method number and method string correspond
    if (method_num == AM_WEB_REQUEST_UNKNOWN) {
        // If method string is not null, set the correct method number
        if (r->method != NULL && *(r->method) != '\0') {
            method_num = am_web_method_str_to_num(r->method);
            r->method_number = ap_method_number_of(r->method);
            am_web_log_debug("%s: set method number to correspond to %s method (%d)",
                    thisfunc, r->method, r->method_number);
        }
    } else if (strcasecmp(r->method, am_web_method_num_to_str(method_num))
            && (method_num != AM_WEB_REQUEST_INVALID)) {
        // If the method number and the method string do not match,
        // correct the method string. But if the method number is invalid
        // the method string needs to be preserved in case Apache is
        // used as a proxy (in front of Exchange Server for instance)
        r->method = am_web_method_num_to_str(method_num);
        am_web_log_debug("%s: set method to %s", thisfunc, r->method);
    }
    return method_num;
}

/**
 * Deny the access in case the agent is found uninitialized
 */
static int do_deny(request_rec *r, am_status_t status) {
    int retVal = HTTP_FORBIDDEN;
    /* Set the return code 403 Forbidden */
    r->content_type = "text/plain";
    ap_custom_response(r, HTTP_FORBIDDEN,
            "403 Forbidden");
    am_web_log_info("do_deny() Status code= %s.",
            am_status_to_string(status));
    return retVal;
}

static am_status_t set_cookie(const char *header, void **args) {
    am_status_t ret = AM_INVALID_ARGUMENT;
    char *currentCookies;
    if (header != NULL && args != NULL) {
        request_rec *rq = (request_rec *) args[0];
        if (rq == NULL) {
            am_web_log_error("in set_cookie: Invalid Request structure");
        } else {
            apr_table_add(rq->err_headers_out, "Set-Cookie", header);
            if ((currentCookies = (char *) apr_table_get(rq->headers_in, "Cookie")) == NULL)
                apr_table_add(rq->headers_in, "Cookie", header);
            else
                apr_table_set(rq->headers_in, "Cookie", (apr_pstrcat(rq->pool, header, ";", currentCookies, NULL)));
            ret = AM_SUCCESS;
        }
    }
    return ret;
}

static am_status_t check_for_post_data(void **args, const char *requestURL, char **page, const unsigned long postcacheentry_life) {
    const char *thisfunc = "check_for_post_data()";
    request_rec *r;
    const char *post_data_query = NULL;
    am_web_postcache_data_t get_data = {NULL, NULL};
    const char *actionurl = NULL;
    const char *postdata_cache = NULL;
    am_status_t status = AM_SUCCESS;
    am_status_t status_tmp = AM_SUCCESS;
    char* buffer_page = NULL;
    char *stickySessionValue = NULL;
    char *stickySessionPos = NULL;
    char *temp_uri = NULL;
    void *agent_config = am_web_get_agent_configuration();
    *page = NULL;

    if (args == NULL || (r = (request_rec *) args[0]) == NULL || requestURL == NULL || agent_config == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
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
            status_tmp = am_web_get_postdata_preserve_URL_parameter
                    (&stickySessionValue, agent_config);
            if (status_tmp == AM_SUCCESS) {
                stickySessionPos = strstr(post_data_query, stickySessionValue);
                if (stickySessionPos != NULL) {
                    size_t len = strlen(post_data_query) -
                            strlen(stickySessionPos) - 1;
                    temp_uri = malloc(len + 1);
                    memset(temp_uri, 0, len + 1);
                    strncpy(temp_uri, post_data_query, len);
                    post_data_query = temp_uri;
                }
            }
        }
    }
    // If magic uri present search for corresponding value in shared cache
    if ((status == AM_SUCCESS) && (post_data_query != NULL) &&
            (strlen(post_data_query) > 0)) {
        am_web_log_debug("%s: POST Magic Query Value: %s", thisfunc, post_data_query);
#ifdef DEBUG
        pdp_dump(r->server);
#endif
        status = AM_NOT_FOUND;
        if ((status = find_post_data((char*) post_data_query, &get_data, postcacheentry_life, r->server)) == AM_SUCCESS) {
            postdata_cache = get_data.value;
            actionurl = get_data.url;
            am_web_log_debug("%s: POST cache actionurl: %s",
                    thisfunc, actionurl);
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
            am_web_log_error("%s: Found magic URI (%s) but entry is not in POST"
                    " hash table", thisfunc, post_data_query);
            status = AM_FAILURE;
        }
    }
    if (temp_uri != NULL) {
        free(temp_uri);
        temp_uri = NULL;
    }
    if (stickySessionValue != NULL) {
        am_web_free_memory(stickySessionValue);
        stickySessionValue = NULL;
    }
    am_web_delete_agent_configuration(agent_config);
    return status;
}

/**
 * Grant access depending on policy and session evaluation
 */
int agent_check_access(request_rec *r) {
    const char *thisfunc = "agent_check_access()";
    am_status_t status = AM_SUCCESS;
    int ret = OK;
    char *url = NULL;
    void *args[] = {(void *) r, (void *) & ret};
    const char *clientIP_hdr_name = NULL;
    char *clientIP_hdr = NULL;
    char *clientIP = NULL;
    const char *clientHostname_hdr_name = NULL;
    char *clientHostname_hdr = NULL;
    char *clientHostname = NULL;
    am_web_req_method_t method;
    am_web_request_params_t req_params;
    am_web_request_func_t req_func;
    void* agent_config = NULL;

    memset((void *) & req_params, 0, sizeof (req_params));
    memset((void *) & req_func, 0, sizeof (req_func));

    if (r == NULL || r->connection == NULL) {
        return HTTP_INTERNAL_SERVER_ERROR;
    }

    // Get agent configuration instance
    if ((agent_config = am_web_get_agent_configuration()) == NULL) {
        ap_log_rerror(APLOG_MARK, APLOG_CRIT | APLOG_NOERRNO, 0, r, "%s: failed to fetch agent configuration instance", thisfunc);
        status = AM_FAILURE;
        ret = do_deny(r, status);
    }

    am_web_log_info("%s: starting...", thisfunc);

    // Get the request URL
    if (status == AM_SUCCESS) {
        status = get_request_url(r, &url);
        if (status == AM_SUCCESS) {
            int vs = am_web_validate_url(agent_config, url);
            if (vs != -1) {
                if (vs == 1) {
                    am_web_log_debug("%s: Request URL validation succeeded", thisfunc);
                    status = AM_SUCCESS;
                } else {
                    am_web_log_error("%s: Request URL validation failed. Returning Access Denied error (HTTP403)", thisfunc);
                    status = AM_FAILURE;
                    ret = do_deny(r, status);
                }
            }
        }
    }

    // Get the request method
    if (status == AM_SUCCESS) {
        method = get_method_num(r);
        if (method == AM_WEB_REQUEST_UNKNOWN) {
            am_web_log_error("%s: Request method is unknown.", thisfunc);
            status = AM_FAILURE;
        }
    }

    // Check notification URL
    if (status == AM_SUCCESS) {
        if (B_TRUE == am_web_is_notification(url, agent_config)) {
            char* data = NULL;
            status = content_read((void*) &r, &data);
            if (status == AM_SUCCESS) {
                post_notification(data, r->server);
                /*notification is received, respond with HTTP200 and OK in response body*/
                ap_set_content_type(r, "text/html");
                ap_set_content_length(r, 2);
                ap_rwrite("OK", 2, r);
                ap_rflush(r);
                /*data is allocated on apr pool, will be released together with a pool*/
                am_web_delete_agent_configuration(agent_config);
                return DONE;
            } else {
                am_web_log_error("%s: content_read for notification failed, %s", thisfunc, am_status_to_string(status));
            }
        }
    }

    // If there is a proxy in front of the agent, the user can set in the
    // properties file the name of the headers that the proxy uses to set
    // the real client IP and host name. In that case the agent needs
    // to use the value of these headers to process the request
    if (status == AM_SUCCESS) {
        // Get the client IP address header set by the proxy, if there is one
        clientIP_hdr_name = am_web_get_client_ip_header_name(agent_config);
        if (clientIP_hdr_name != NULL) {
            clientIP_hdr = (char *) apr_table_get(r->headers_in,
                    clientIP_hdr_name);
        }
        // Get the client host name header set by the proxy, if there is one
        clientHostname_hdr_name =
                am_web_get_client_hostname_header_name(agent_config);
        if (clientHostname_hdr_name != NULL) {
            clientHostname_hdr = (char *) apr_table_get(r->headers_in,
                    clientHostname_hdr_name);
        }
        // If the client IP and host name headers contain more than one
        // value, take the first value.
        if ((clientIP_hdr != NULL && strlen(clientIP_hdr) > 0) ||
                (clientHostname_hdr != NULL && strlen(clientHostname_hdr) > 0)) {
            status = am_web_get_client_ip_host(clientIP_hdr,
                    clientHostname_hdr,
                    &clientIP, &clientHostname);
        }
    }

    // Set the client ip in the request parameters structure
    if (status == AM_SUCCESS) {
        if (clientIP == NULL) {
#ifdef APACHE24
            req_params.client_ip = (char *) r->connection->client_ip;
#else
            req_params.client_ip = (char *) r->connection->remote_ip;
#endif
        } else {
            req_params.client_ip = clientIP;
        }
        if ((req_params.client_ip == NULL) ||
                (strlen(req_params.client_ip) == 0)) {
            am_web_log_error("%s: Could not get the remote IP.", thisfunc);
            status = AM_FAILURE;
        }
    }

    // Process the request
    if (status == AM_SUCCESS) {
        req_params.client_hostname = clientHostname;
        req_params.url = url;
        req_params.query = r->args;
        req_params.method = method;
        req_params.path_info = r->path_info;
        req_params.cookie_header_val =
                (char *) apr_table_get(r->headers_in, "Cookie");
        req_params.content_type = (char *) apr_table_get(r->headers_in, "Content-Type");
        req_func.get_post_data.func = content_read;
        req_func.get_post_data.args = args;
        // no free_post_data
        req_func.set_user.func = set_user;
        req_func.set_user.args = args;
        req_func.set_method.func = set_method;
        req_func.set_method.args = args;
        req_func.set_header_in_request.func = set_header_in_request;
        req_func.set_header_in_request.args = args;
        req_func.add_header_in_response.func = add_header_in_response;
        req_func.add_header_in_response.args = args;
        req_func.set_notes_in_request.func = set_notes_in_request;
        req_func.set_notes_in_request.args = args;
        req_func.render_result.func = render_result;
        req_func.render_result.args = args;
        // post data preservation (create shared cache table entry)
        req_func.reg_postdata.func = update_post_data_for_request;
        req_func.reg_postdata.args = args;
        req_func.check_postdata.func = check_for_post_data;
        req_func.check_postdata.args = args;

        (void) am_web_process_request(&req_params, &req_func,
                &status, agent_config);

        if (status != AM_SUCCESS) {
            am_web_log_error("%s: error encountered rendering result: %s", thisfunc, am_status_to_string(status));
        }
    }
    // Cleaning
    if (clientIP != NULL) {
        am_web_free_memory(clientIP);
    }
    if (clientHostname != NULL) {
        am_web_free_memory(clientHostname);
    }
    am_web_delete_agent_configuration(agent_config);
    // Failure handling
    if (status == AM_FAILURE) {
        if (ret == OK) {
            ret = HTTP_INTERNAL_SERVER_ERROR;
        }
    }
    return ret;
}

static void agent_register_hooks(apr_pool_t *p) {
    ap_hook_access_checker(agent_check_access, NULL, NULL, APR_HOOK_MIDDLE);
    /*main agent init, called once per server lifecycle*/
    ap_hook_post_config(agent_init, NULL, NULL, APR_HOOK_MIDDLE);
    /*agent worker init, called upon new server child process creation*/
    ap_hook_child_init(agent_worker_init, NULL, NULL, APR_HOOK_MIDDLE);
}

module AP_MODULE_DECLARE_DATA dsame_module = {
    STANDARD20_MODULE_STUFF,
    NULL,
    NULL,
    agent_create_server_config,
    NULL,
    agent_auth_cmds,
    agent_register_hooks
};

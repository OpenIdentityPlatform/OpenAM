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
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */

#include <limits.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

#if defined(LINUX)
#include <dlfcn.h>
#endif
#include <httpd.h>
#include <http_config.h>
#include <http_core.h>
#include <http_protocol.h>
#include <http_request.h>
#include <http_main.h>
#include <http_log.h>
#include <apr.h>
#include <apr_strings.h>
#include <apr_general.h>
#include <apr_shm.h>
#include <apr_global_mutex.h>
#include <apr_version.h>
#ifdef _MSC_VER
#include <process.h>
#include <windows.h>
#include <winbase.h>
#define	getpid	_getpid
#else
#include <unistd.h>
#endif
#ifdef AP_NEED_SET_MUTEX_PERMS
#include <unixd.h>
#endif 

#include "am_web.h"

#define DSAME                   "DSAME"
#define OpenSSO                 "OpenSSO"
#define	MAGIC_STR		"sunpostpreserve"
#define	POST_PRESERVE_URI	"/dummypost/"MAGIC_STR
#define MAX_NOTIF_MSG_SIZE      8192
#define MAX_PDP_KEY_SIZE        256
#define MAX_PDP_URL_SIZE        4096
#define MAX_PDP_VALUE_SIZE      8192

module AP_MODULE_DECLARE_DATA dsame_module;

#ifdef APLOG_USE_MODULE
APLOG_USE_MODULE(dsame);
#endif 

static apr_status_t pre_cleanup_dsame(void *data);
static apr_status_t cleanup_dsame(void *data);
static am_status_t set_cookie(const char *header, void **args);

typedef void (*sighandler_t)(int);
boolean_t agentInitialized = B_FALSE;
/* This is used to hold SIGTERM while agent is cleaning up
 * and released when done.
 */
static int sigterm_delivered = 0;
/* Notification listener thread sleep interval, in sec.
 * Values equal to 0 will shut down listener thread.
 */
static int am_watchdog_interval = 0;

/* Mutex variable */
static apr_thread_mutex_t *init_mutex = NULL;
/* Notification listener thread handle */
static apr_thread_t *notification_listener_t = NULL;

typedef struct am_notification_list_item_t {
    pid_t pid;
    int read;
    char value[MAX_NOTIF_MSG_SIZE];
} am_notification_list_item_t;

typedef struct am_post_data_list_item_t {
    char key[MAX_PDP_KEY_SIZE];
    char action_url[MAX_PDP_URL_SIZE];
    char value[MAX_PDP_VALUE_SIZE];
    apr_time_t created;
} am_post_data_list_item_t;

typedef struct {
    char *properties_file;
    char *bootstrap_file;
    char *notification_lockfile;
    char *postdata_lockfile;
    int max_pid_count;
    int max_pdp_count;
    apr_shm_t *notification_cache;
    apr_global_mutex_t *notification_lock;
    apr_shm_t *pdp_cache;
    apr_global_mutex_t *pdp_lock;
} agent_server_config;

static int get_global_lock(apr_global_mutex_t * mutex) {
    apr_status_t rs;
    int i;
    for (i = 0; i < 10; i++) {
        rs = apr_global_mutex_trylock(mutex);
        if (APR_STATUS_IS_EBUSY(rs)) {
            apr_sleep(10);
        } else if (rs == APR_SUCCESS) {
            return 1;
        } else if (APR_STATUS_IS_ENOTIMPL(rs)) {
            rs = apr_global_mutex_lock(mutex);
            if (rs == APR_SUCCESS) {
                return 1;
            } else {
                /*could not acquire hard mutex_lock*/
                return 0;
            }
        } else {
            /*unknown return status from mutex_trylock*/
            return 0;
        }
    }
    /*timed out mutex_trylock*/
    return 0;
}

static void register_process(pid_t pid, server_rec *s) {
    am_notification_list_item_t *table;
    int i;
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    if (get_global_lock(scfg->notification_lock) != 0) {
        table = apr_shm_baseaddr_get(scfg->notification_cache);
        if (table != NULL) {
            for (i = 0; i < scfg->max_pid_count; i++) {
                if (table[i].pid == 0) {
                    table[i].pid = pid;
                    table[i].read = 1;
                    table[i].value[0] = '\0';
                    break;
                }
            }
        } else {
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, s, "Failed to register notification listener for process id [%d] (shared memory error)", pid);
        }
        apr_global_mutex_unlock(scfg->notification_lock);
    }
}

static void deregister_process(pid_t pid, server_rec *s) {
    am_notification_list_item_t *table;
    int i;
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    if (get_global_lock(scfg->notification_lock) != 0) {
        table = apr_shm_baseaddr_get(scfg->notification_cache);
        if (table != NULL) {
            for (i = 0; i < scfg->max_pid_count; i++) {
                if (table[i].pid == pid) {
                    table[i].pid = 0;
                    table[i].read = 1;
                    table[i].value[0] = '\0';
                    break;
                }
            }
        } else {
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, s, "Failed to deregister notification listener for process id [%d] (shared memory error)", pid);
        }
        apr_global_mutex_unlock(scfg->notification_lock);
    }
}

static void post_notification(char *value, server_rec *s) {
    am_notification_list_item_t *table;
    int slen, i;
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    if (value == NULL || (slen = strlen(value)) == 0 || slen > MAX_NOTIF_MSG_SIZE) {
        am_web_log_max_debug("Notification message is empty or exceeds MAX_NOTIF_MSG_SIZE size [%d]", slen);
        return;
    }
    am_web_log_max_debug("Notification message size [%d]", slen);
    if (get_global_lock(scfg->notification_lock) != 0) {
        table = apr_shm_baseaddr_get(scfg->notification_cache);
        if (table != NULL) {
            for (i = 0; i < scfg->max_pid_count; i++) {
                if (table[i].pid > 0) {
                    table[i].read = 0;
                    memcpy(table[i].value, value, slen);
                    table[i].value[slen] = '\0';
                }
            }
        } else {
            am_web_log_warning("Failed to register notification message (shared memory error)");
        }
        apr_global_mutex_unlock(scfg->notification_lock);
    }
}

static void listall_post_data(server_rec *s) {
    am_post_data_list_item_t *table;
    int i;
    char datestring[APR_RFC822_DATE_LEN];
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    if (get_global_lock(scfg->pdp_lock) != 0) {
        table = apr_shm_baseaddr_get(scfg->pdp_cache);
        if (table != NULL) {
            for (i = 0; i < scfg->max_pdp_count; i++) {
                if (table[i].created > 0) {
                    am_web_log_max_debug("PDP=========[ %d ]===========", i);
                    am_web_log_max_debug("PDP-KEY: %s", table[i].key);
                    apr_rfc822_date(datestring, table[i].created);
                    am_web_log_max_debug("PDP-TIMESTAMP: %s", datestring);
                    am_web_log_max_debug("PDP-ACTIONURL: %s", table[i].action_url);
                    am_web_log_max_debug("PDP-VALUE: %s", table[i].value);
                    am_web_log_max_debug("PDP========================");
                }
            }
        } else {
            ap_log_error(APLOG_MARK, APLOG_ERR, 0, s, "Failed to list post data (shared memory error)");
        }
        apr_global_mutex_unlock(scfg->pdp_lock);
    }
}

static am_status_t find_post_data(char *id, am_web_postcache_data_t *pd, const unsigned long postcacheentry_life, server_rec *s) {
    am_post_data_list_item_t *table;
    int i;
    apr_time_t now;
    am_status_t status = AM_FAILURE;
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    if (get_global_lock(scfg->pdp_lock) != 0) {
        table = apr_shm_baseaddr_get(scfg->pdp_cache);
        if (table != NULL) {
            now = apr_time_now();
            if (postcacheentry_life >= 1L) {
                now -= apr_time_from_sec(postcacheentry_life * 60);
            } else {
                am_web_log_warning("find_post_data(): invalid com.sun.identity.agents.config.postcache.entry.lifetime value, defaulting to 3 minutes");
                now -= apr_time_from_sec(180);
            }
            for (i = 0; i < scfg->max_pdp_count; i++) {
                if (id != NULL && table[i].key != NULL
                        && table[i].action_url != NULL
                        && table[i].value != NULL
                        && strcmp(table[i].key, id) == 0) {
                    if (table[i].created < now) {
                        am_web_log_warning("find_post_data(): entry [%s] is obsolete", id);
                        status = AM_FAILURE;
                    } else {
                        pd->url = strdup(table[i].action_url);
                        pd->value = strdup(table[i].value);
                        status = AM_SUCCESS;
                    }
                    table[i].key[0] = '\0';
                    table[i].action_url[0] = '\0';
                    table[i].value[0] = '\0';
                    table[i].created = 0;
                    break;
                }
            }
        } else {
            am_web_log_warning("Failed to find post data for a key [%s] (shared memory error)", id);
        }
        apr_global_mutex_unlock(scfg->pdp_lock);
    }
    return status;
}

static am_status_t update_post_data_for_request(void **args, const char *key, const char *acturl, const char *value, const unsigned long postcacheentry_life) {
    const char *thisfunc = "update_post_data_for_request()";
    am_status_t status = AM_FAILURE;
    request_rec *r;
    int i, klen, ulen, vlen;
    am_post_data_list_item_t *table;
    apr_time_t now;
    agent_server_config *scfg;
    am_web_log_max_debug("%s: updating post data cache for key: %s", thisfunc, key);
    r = NULL;
    if (args == NULL || (r = (request_rec *) args[0]) == NULL ||
            key == NULL || acturl == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    } else {
        if (key == NULL || acturl == NULL || value == NULL
                || (klen = strlen(key)) > MAX_PDP_KEY_SIZE
                || (ulen = strlen(acturl)) > MAX_PDP_URL_SIZE
                || (vlen = strlen(value)) > MAX_PDP_VALUE_SIZE) {
            am_web_log_warning("%s: key, action_url or value is empty or their values exceed MAX limits [%d],[%d],[%d]", thisfunc, klen, ulen, vlen);
            return status;
        }
        scfg = ap_get_module_config(r->server->module_config, &dsame_module);
        am_web_log_max_debug("%s: key size [%d], url size [%d], value size [%d]", thisfunc, klen, ulen, vlen);
        now = apr_time_now();
        if (postcacheentry_life >= 1L) {
            now -= apr_time_from_sec(postcacheentry_life * 60);
        } else {
            am_web_log_warning("%s: invalid com.sun.identity.agents.config.postcache.entry.lifetime value, defaulting to 3 minutes", thisfunc);
            now -= apr_time_from_sec(180);
        }
        if (get_global_lock(scfg->pdp_lock) != 0) {
            table = apr_shm_baseaddr_get(scfg->pdp_cache);
            if (table != NULL) {
                for (i = 0; i < scfg->max_pdp_count; i++) {
                    if (table[i].created == 0 || table[i].created < now) {
                        memcpy(table[i].key, key, klen);
                        table[i].key[klen] = '\0';
                        memcpy(table[i].action_url, acturl, ulen);
                        table[i].action_url[ulen] = '\0';
                        memcpy(table[i].value, value, vlen);
                        table[i].value[vlen] = '\0';
                        table[i].created = apr_time_now();
                        status = AM_SUCCESS;
                        break;
                    }
                }
            } else {
                am_web_log_warning("%s: failed to update post data cache (shared memory error)", thisfunc);
            }
            apr_global_mutex_unlock(scfg->pdp_lock);
        }
    }
    if (am_web_is_max_debug_on()) {
        listall_post_data(r->server);
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

static const command_rec dsame_auth_cmds[] = {
    AP_INIT_TAKE1("Agent_Config_File", am_set_string_slot, (void *) APR_OFFSETOF(agent_server_config, properties_file), RSRC_CONF,
    "Full path of the Agent configuration file"),
    AP_INIT_TAKE1("Agent_Bootstrap_File", am_set_string_slot, (void *) APR_OFFSETOF(agent_server_config, bootstrap_file), RSRC_CONF,
    "Full path of the Agent bootstrap file"),
    AP_INIT_TAKE1("Agent_Max_PID_Count", am_set_int_slot, (void *) APR_OFFSETOF(agent_server_config, max_pid_count), RSRC_CONF,
    "Agent notification module max pid count"),
    AP_INIT_TAKE1("Agent_Max_PDP_Count", am_set_int_slot, (void *) APR_OFFSETOF(agent_server_config, max_pdp_count), RSRC_CONF,
    "Agent PDP module max active cache entry count"), {
        NULL
    }
};

static void * APR_THREAD_FUNC notification_listener(apr_thread_t *t, void *data) {
    server_rec *s = (server_rec *) data;
    agent_server_config *scfg = ap_get_module_config(s->module_config, &dsame_module);
    am_notification_list_item_t *table;
    pid_t pid = 0;
    int len, i;
    for (;;) {
        if (am_watchdog_interval <= 0)
            break;
        if (agentInitialized == B_TRUE && (pid = getpid()) > 0) {
            if (get_global_lock(scfg->notification_lock) != 0) {
                table = apr_shm_baseaddr_get(scfg->notification_cache);
                for (i = 0; i < scfg->max_pid_count; i++) {
                    if (table[i].pid == pid && table[i].read == 0) {
                        table[i].read = 1;
                        len = strlen(table[i].value);
                        am_web_log_max_debug("Notification listener pid: %d, size: %d, value: %s", pid, len, (table[i].value ? table[i].value : "NULL"));
                        am_web_handle_notification(table[i].value, len, am_web_get_agent_configuration());
                        table[i].value[len] = '\0';
                        break;
                    }
                }
                apr_global_mutex_unlock(scfg->notification_lock);
            }
        }
        apr_sleep(apr_time_from_sec(am_watchdog_interval));
    }
    am_web_log_info("Shutting down policy web agent notification listener thread for pid: %d", pid);
    deregister_process(pid, s);
    return NULL;
}

static void *dsame_create_server_config(apr_pool_t *p, server_rec *s) {
    char *tmpdir = NULL;
    agent_server_config *cfg = apr_pcalloc(p, sizeof (agent_server_config));
    if (apr_temp_dir_get((const char**) &tmpdir, p) != APR_SUCCESS) {
        ap_log_error(__FILE__, __LINE__, APLOG_MODULE_INDEX, APLOG_ALERT, 0, s,
                "Policy web agent failed to locate temporary storage directory");
    }
    /*default values*/
    ((agent_server_config *) cfg)->notification_lockfile = apr_pstrcat(p, tmpdir,
#ifdef _MSC_VER
            "\\AMNotifLock",
#else
            "/AMNotifLock",
#endif
            NULL);
    ((agent_server_config *) cfg)->postdata_lockfile = apr_pstrcat(p, tmpdir,
#ifdef _MSC_VER
            "\\AMPostLock",
#else
            "/AMPostLock",
#endif
            NULL);
    ((agent_server_config *) cfg)->max_pid_count = 256;
    ((agent_server_config *) cfg)->max_pdp_count = 256;
    return (void *) cfg;
}

/*
 * This routine is called by the Apache server when the module is first
 * loaded.  It handles loading all of the shared libraries that are needed
 * to instantiate the DSAME Policy Agent.  If all of the libraries can
 * successfully be loaded, then the routine looks up the two entry points
 * in the actual policy agent: am_web_init and dsame_check_access.  The
 * first routine is invoked directly and an error is logged if it returns
 * an error.  The second routine is inserted into the module interface
 * table for use by Apache during request processing.
 */
static int init_dsame(apr_pool_t *pconf, apr_pool_t *plog, apr_pool_t *ptemp, server_rec *server_ptr) {
    void *lib_handle;
    int i, ret = OK;
    am_status_t status = AM_SUCCESS;
    apr_status_t rv;
    apr_size_t shm_size, pd_shm_size;
    void *data; /* These two help ensure that we only init once. */
    const char *data_key = "init_dsame";
    agent_server_config *scfg;
    am_notification_list_item_t *notification_list;
    am_post_data_list_item_t *post_data_list;
    apr_version_t version;
    apr_version(&version);

    if (!(version.major >= 1 && version.minor >= 3 && version.patch >= 0)) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, 0, server_ptr,
                "Policy web agent initialization failed (need APR library version 1.3.0 or newer)");
        return HTTP_INTERNAL_SERVER_ERROR;
    }

#ifdef _MSC_VER
    if (!getenv("AP_PARENT_PID")) {
        return OK;
    }
#endif
    /*
     * The following checks if this routine has been called before.
     * This is necessary because the parent process gets initialized
     * a couple of times as the server starts up, and we don't want
     * to create any more mutexes and shared memory segments than
     * we're actually going to use.
     */
    apr_pool_userdata_get(&data, data_key, server_ptr->process->pool);
    if (!data) {
        apr_pool_userdata_set((const void *) 1, data_key, apr_pool_cleanup_null, server_ptr->process->pool);
        return OK;
    }

#if defined(LINUX) 					
    lib_handle = dlopen("libamapc24.so", RTLD_LAZY);
    if (!lib_handle) {
        fprintf(stderr, "Error during dlopen(): %s\n", dlerror());
        exit(1);
    }
#endif

    scfg = ap_get_module_config(server_ptr->module_config, &dsame_module);

    /* If the shared memory/lock file already exists then delete it.  Otherwise we are
     * going to run into problems creating the shared memory.
     */
    if (scfg->notification_lockfile)
        apr_file_remove(scfg->notification_lockfile, pconf);
    if (scfg->postdata_lockfile)
        apr_file_remove(scfg->postdata_lockfile, pconf);

    status = am_web_init(scfg->bootstrap_file, scfg->properties_file);

    if (status == AM_SUCCESS) {
        am_web_log_debug("Process initialization result:%s", am_status_to_string(status));

        ap_add_version_component(pconf, "DSAME/3.0");

        if ((apr_thread_mutex_create(&init_mutex, APR_THREAD_MUTEX_UNNESTED,
                pconf)) != APR_SUCCESS) {
            ap_log_error(__FILE__, __LINE__, APLOG_MODULE_INDEX, APLOG_ALERT, 0, server_ptr,
                    "Policy web agent configuration failed: %s",
                    am_status_to_string(status));
            ret = HTTP_BAD_REQUEST;
        }

        rv = apr_global_mutex_create(&(scfg->notification_lock), scfg->notification_lockfile, APR_LOCK_DEFAULT, pconf);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to create "
                    "policy web agent notification global mutex file '%s'",
                    scfg->notification_lockfile);
            return HTTP_INTERNAL_SERVER_ERROR;
        }

#ifdef AP_NEED_SET_MUTEX_PERMS
        rv = ap_unixd_set_global_mutex_perms(scfg->notification_lock);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr,
                    "Policy web agent could not set permissions "
                    "on notifications global lock; check User and Group directives");
            return HTTP_INTERNAL_SERVER_ERROR;
        }
#endif 
        rv = apr_global_mutex_create(&(scfg->pdp_lock), scfg->postdata_lockfile, APR_LOCK_DEFAULT, pconf);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to create "
                    "policy web agent postdata global mutex file '%s'",
                    scfg->postdata_lockfile);
            return HTTP_INTERNAL_SERVER_ERROR;
        }

#ifdef AP_NEED_SET_MUTEX_PERMS
        rv = ap_unixd_set_global_mutex_perms(scfg->pdp_lock);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr,
                    "Policy web agent could not set permissions "
                    "on PDP global lock; check User and Group directives");
            return HTTP_INTERNAL_SERVER_ERROR;
        }
#endif 
        shm_size = APR_ALIGN_DEFAULT(sizeof (am_notification_list_item_t) * scfg->max_pid_count);
        pd_shm_size = APR_ALIGN_DEFAULT(sizeof (am_post_data_list_item_t) * scfg->max_pdp_count);

        rv = apr_shm_create(&(scfg->notification_cache), shm_size, NULL, pconf);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to create "
                    "policy web agent notification anonymous shared segment");
            return HTTP_INTERNAL_SERVER_ERROR;
        }

        rv = apr_shm_create(&(scfg->pdp_cache), pd_shm_size, NULL, pconf);
        if (rv != APR_SUCCESS) {
            ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to create "
                    "policy web agent postdata anonymous shared segment");
            return HTTP_INTERNAL_SERVER_ERROR;
        }

        notification_list = apr_shm_baseaddr_get(scfg->notification_cache);
        for (i = 0; i < scfg->max_pid_count; i++) {
            notification_list[i].pid = 0;
            notification_list[i].read = 1;
            notification_list[i].value[0] = '\0';
        }

        post_data_list = apr_shm_baseaddr_get(scfg->pdp_cache);
        for (i = 0; i < scfg->max_pdp_count; i++) {
            post_data_list[i].key[0] = '\0';
            post_data_list[i].action_url[0] = '\0';
            post_data_list[i].value[0] = '\0';
            post_data_list[i].created = 0;
        }

        ap_log_error(__FILE__, __LINE__, APLOG_MODULE_INDEX, APLOG_NOTICE, 0, server_ptr,
                "Policy web agent shared memory configuration: notif_shm_size[%d], pdp_shm_size[%d], max_pid_count[%d], max_pdp_count[%d]",
                shm_size, pd_shm_size, scfg->max_pid_count, scfg->max_pdp_count);
    } else {
        ap_log_error(APLOG_MARK, APLOG_CRIT, 0, server_ptr, "Failed to initialize policy web agent");
        ret = HTTP_INTERNAL_SERVER_ERROR;
    }
    return ret;
}

/*
 * Called in the child_init hook, this function is needed to
 * register the pre_cleanup_dsame and cleanup_dsame routines to be called upon the child's exit
 * as well as initialize global mutexes, attach to shared memory segment and register
 * process id with a notification listener
 */
static void child_init_dsame(apr_pool_t *pool_ptr, server_rec *server_ptr) {
    apr_status_t rv;
    agent_server_config *scfg = ap_get_module_config(server_ptr->module_config, &dsame_module);

    /*register callback - shut down notification listener thread before apr pool cleanup*/
    apr_pool_pre_cleanup_register(pool_ptr, NULL, pre_cleanup_dsame);
    /*register callback - clean up apr pool and release shared memory, shut down amsdk backend*/
    apr_pool_cleanup_register(pool_ptr, server_ptr, cleanup_dsame, apr_pool_cleanup_null);

    rv = apr_global_mutex_child_init(&(scfg->notification_lock), scfg->notification_lockfile, pool_ptr);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to attach to "
                "policy web agent notification global mutex file '%s'",
                scfg->notification_lockfile);
        return;
    }

    rv = apr_global_mutex_child_init(&(scfg->pdp_lock), scfg->postdata_lockfile, pool_ptr);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to attach to "
                "policy web agent postdata global mutex file '%s'",
                scfg->postdata_lockfile);
        return;
    }

    register_process(getpid(), server_ptr);

    /*all is set, setup notification thread watchdog interval and start listener thread*/
    am_watchdog_interval = 1; //1 sec
    rv = apr_thread_create(&notification_listener_t, NULL, notification_listener, (void *) server_ptr, pool_ptr);
    if (rv != APR_SUCCESS) {
        ap_log_error(APLOG_MARK, APLOG_CRIT, rv, server_ptr, "Failed to create "
                "policy web agent notification changes listener");
        return;
    }
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
 * gets request URL
 */
static am_status_t get_request_url(request_rec *r, char **requestURL) {
    const char *thisfunc = "get_request_url()";
    am_status_t status = AM_SUCCESS;
    *requestURL = ap_construct_url(r->pool, r->unparsed_uri, r);
    if (*requestURL == NULL) {
        status = AM_FAILURE;
    }
    am_web_log_debug("%s: Returning request URL = %s", thisfunc, *requestURL);
    return status;
}

/**
 * gets content if this is notification.
 */
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
    }

    // Remove the content length since the body has been read.
    // If the content length is not reset, servlet containers think
    // the request is a POST.
    if (sts == AM_SUCCESS) {
        r->clength = 0;
        apr_table_unset(r->headers_in, "Content-Length");
        new_clen_val = apr_table_get(r->headers_in, "Content-Length");
        am_web_log_max_debug("content_read(): New value "
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
        r->ap_auth_type = apr_pstrdup(r->pool, OpenSSO);
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
            "Access denied as Agent profile not"
            " found in Access Manager.");
    am_web_log_info("do_deny() Status code= %s.",
            am_status_to_string(status));
    return retVal;
}

/**
 * Send HTTP_INTERNAL_SERVER_ERROR in case of an error
 */
static int send_error(request_rec *r) {
    int retVal = HTTP_INTERNAL_SERVER_ERROR;
    r->content_type = "text/plain";
    ap_custom_response(r, HTTP_INTERNAL_SERVER_ERROR,
            "Server encountered an error while processing"
            " request to/from OpenAM.");
    am_web_log_debug("send_error() HTTP_INTERNAL_SERVER_ERROR");
    return retVal;
}

static char *get_cookie(request_rec *r, const char *cookie_name) {
    char *raw_cookie_start = NULL, *raw_cookie_end, *cookie;
    char *raw_cookie = (char*) apr_table_get(r->headers_in, "Cookie");
    if (!(raw_cookie)) return 0;
    do {
        if (!(raw_cookie = strstr(raw_cookie, cookie_name))) return NULL;
        raw_cookie_start = raw_cookie;
        if (!(raw_cookie = strchr(raw_cookie, '='))) return NULL;
    } while (strncmp(cookie_name, raw_cookie_start, raw_cookie - raw_cookie_start) != 0);
    raw_cookie++; //skip '='
    if (!((raw_cookie_end = strchr(raw_cookie, ';')) || (raw_cookie_end = strchr(raw_cookie, '\0')))) return NULL;
    if (!(cookie = apr_pstrndup(r->pool, raw_cookie, raw_cookie_end - raw_cookie))) return NULL;
    if (!(ap_unescape_url(cookie) == 0)) return NULL;
    return cookie;
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

/**
 * This function is invoked to initialize the agent
 * during the first request.
 */
void init_at_request() {
    am_status_t status;
    status = am_agent_init(&agentInitialized);
    if (status != AM_SUCCESS) {
        am_web_log_debug("Initialization of the agent failed: "
                "status = %s (%d)", am_status_to_string(status), status);
    }
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
    void *agent_config = NULL;
    *page = NULL;
    agent_config = am_web_get_agent_configuration();

    if (args == NULL || (r = (request_rec *) args[0]) == NULL || requestURL == NULL) {
        am_web_log_error("%s: invalid argument passed.", thisfunc);
        status = AM_INVALID_ARGUMENT;
    }
    // Check if magic URI is present in the URL
    if (status == AM_SUCCESS) {
        post_data_query = strstr(requestURL, POST_PRESERVE_URI);
        if (post_data_query != NULL) {
            post_data_query += strlen(POST_PRESERVE_URI);
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
        if (am_web_is_max_debug_on()) {
            listall_post_data(r->server);
        }
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
    return status;
}

/**
 * Grant access depending on policy and session evaluation
 */
int dsame_check_access(request_rec *r) {
    const char *thisfunc = "dsame_check_access()";
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

    /**
     * Initialize the agent during first request
     * Should not be repeated during subsequent requests.
     */
    if (agentInitialized != B_TRUE) {
        apr_thread_mutex_lock(init_mutex);
        if (agentInitialized != B_TRUE) {
            (void) init_at_request();
            if (agentInitialized != B_TRUE) {
                status = AM_FAILURE;
                ret = do_deny(r, status);
            }
        }
        apr_thread_mutex_unlock(init_mutex);
    }
    if (status == AM_SUCCESS) {
        // Get the agent config
        agent_config = am_web_get_agent_configuration();
        // Check request
        if (r == NULL) {
            am_web_log_error("%s: Request to http server is NULL", thisfunc);
            status = AM_FAILURE;
        }
    }
    am_web_log_info("%s: starting...", thisfunc);

    if (agent_config == NULL || agentInitialized != B_TRUE) {
        status = AM_FAILURE;
        ret = do_deny(r, status);
    }

    if (status == AM_SUCCESS) {
        if (r->connection == NULL) {
            am_web_log_error("%s: Request connection is NULL.", thisfunc);
            status = AM_FAILURE;
        }
    }
    // Get the request URL
    if (status == AM_SUCCESS) {
        status = get_request_url(r, &url);
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
            req_params.client_ip = (char *) r->connection->client_ip;
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

static void sigterm_handler(int sig) {
    // remember that a SIGTERM was delivered so we can raise it later.
    sigterm_delivered = 1;
}

static apr_status_t pre_cleanup_dsame(void *data) {
    apr_status_t ret = APR_SUCCESS;
    am_watchdog_interval = 0;
    if (notification_listener_t) {
        apr_thread_join(&ret, notification_listener_t);
    }
    return APR_SUCCESS;
}

static apr_status_t cleanup_dsame(void *data) {
    /*
     * Apache calls the cleanup func then sends a SIGTERM before
     * the routine finishes. so hold SIGTERM to let destroy agent session
     * complete and release it after.
     * The signal() interface (ANSI C) is chosen to hold the signal instead of
     * sigaction() or other signal handling interfaces since it seems
     * to work best across platforms.
     */
    agent_server_config *scfg = ap_get_module_config(((server_rec *) data)->module_config, &dsame_module);
    sighandler_t prev_handler = signal(SIGTERM, sigterm_handler);

    am_web_log_info("Cleaning up web agent..");
    if (init_mutex) {
        apr_thread_mutex_destroy(init_mutex);
        am_web_log_info("Destroyed mutex...");
        init_mutex = NULL;
    }

    if (scfg->notification_cache) {
        apr_shm_destroy(scfg->notification_cache);
        am_web_log_info("Destroyed shared memory...");
        scfg->notification_cache = NULL;
    }

    if (scfg->notification_lock) {
        apr_global_mutex_destroy(scfg->notification_lock);
        am_web_log_info("Destroyed shared memory global mutex...");
        scfg->notification_lock = NULL;
    }

    if (scfg->pdp_cache) {
        apr_shm_destroy(scfg->pdp_cache);
        am_web_log_info("Destroyed shared memory...");
        scfg->pdp_cache = NULL;
    }

    if (scfg->pdp_lock) {
        apr_global_mutex_destroy(scfg->pdp_lock);
        am_web_log_info("Destroyed shared memory global mutex...");
        scfg->pdp_lock = NULL;
    }

    (void) am_web_cleanup();

    // release SIGTERM
    (void) signal(SIGTERM, prev_handler);
    if (sigterm_delivered) {

        raise(SIGTERM);
    }
    return APR_SUCCESS;
}

static apr_status_t shutdownNSS(void *data) {
    am_status_t status = am_shutdown_nss();
    if (status != AM_SUCCESS) {
        am_web_log_error("shutdownNSS(): Failed to shutdown NSS.");
    }
    return OK;
}

static void dsame_register_hooks(apr_pool_t *p) {
    ap_hook_access_checker(dsame_check_access, NULL, NULL, APR_HOOK_MIDDLE);
    /*main agent init, called once per server lifecycle*/
    ap_hook_post_config(init_dsame, NULL, NULL, APR_HOOK_LAST);
    /*NSS shutdown hook*/
    apr_pool_cleanup_register(p, NULL, shutdownNSS, apr_pool_cleanup_null);
    /*agent child init, called upon new server child process creation*/
    ap_hook_child_init(child_init_dsame, NULL, NULL, APR_HOOK_LAST);
}

/*
 * Interface table used by Apache 2.x to interact with this module.
 */
module AP_MODULE_DECLARE_DATA dsame_module = {
    STANDARD20_MODULE_STUFF,
    NULL, /* per-directory config creator */
    NULL, /* dir config merger */
    dsame_create_server_config, /* server config creator */
    NULL, /* server config merger */
    dsame_auth_cmds, /* command table */
    dsame_register_hooks /* set up other request processing hooks */
};

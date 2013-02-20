/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

#ifdef _MSC_VER
#include <windows.h>
#define sleep(x) SleepEx(x * 1000, FALSE)
#define THREAD HANDLE
typedef CRITICAL_SECTION MUTEX;
#define THREAD_WAIT(thread) WaitForSingleObject(thread, INFINITE)
#define THREAD_CREATE(thread,func,arg) ((thread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)func, arg, 0, NULL)) == NULL ? -1 : 0)
#define MUTEX_CREATE(mutex) InitializeCriticalSection(&(mutex))
#define MUTEX_DELETE(mutex) DeleteCriticalSection(&(mutex))
#define MUTEX_LOCK(mutex) EnterCriticalSection(&(mutex))
#define MUTEX_TRY_LOCK(mutex) (TryEnterCriticalSection(&(mutex)) != 0)
#define MUTEX_UNLOCK(mutex) LeaveCriticalSection(&(mutex))
#else
#define _XOPEN_SOURCE 500
#include <pthread.h>
#include <signal.h>
#include <sys/time.h>
#include <fcntl.h>
#include <unistd.h>

#define THREAD pthread_t
typedef pthread_mutex_t MUTEX;
#define THREAD_WAIT(thread) pthread_join(thread, NULL);
#define THREAD_CREATE(thread,func,arg) pthread_create(&thread, NULL, func, arg)
#define MUTEX_CREATE(mutex) pthread_mutex_init(&(mutex), NULL)
#define MUTEX_TRY_LOCK(mutex) pthread_mutex_trylock(&(mutex))
#define MUTEX_LOCK(mutex) pthread_mutex_lock(&(mutex))
#define MUTEX_UNLOCK(mutex) pthread_mutex_unlock(&(mutex))
#define MUTEX_DELETE(mutex) pthread_mutex_destroy(&(mutex))

#if defined(__sun) && defined(__SunOS_5_10)
#include <port.h>
int port;
typedef void (timer_callback) (void *);
#else
typedef void (timer_callback) (union sigval);
#endif

#endif
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "am_types.h"

typedef struct {
    char *url;
    int status;
    int run;
} naming_url_t;

typedef struct {
    int size;
    naming_url_t *list;
} naming_status_t;

typedef struct {
    char *url;
    int idx;
    void (*log)(const char *, ...);
    int (*validate)(const char *, const char **, int *httpcode);
} naming_validator_int_t;

volatile int keep_going = 1;
THREAD wthr;
MUTEX mutex;
naming_status_t* nlist = NULL;

void write_naming_value(const char *key, const char *value);
char *read_naming_value(const char *key);

static void *url_watchdog(void *arg) {
    int i, j = 0, s = 0, t;
    naming_validator_t *v = (naming_validator_t *) arg;
    t = v->poll_scan;
    while (keep_going) {
        for (i = 0; i < nlist->size; i++) {
            MUTEX_LOCK(mutex);
            s = nlist->list[i].status;
            MUTEX_UNLOCK(mutex);
            if (s == 1) {
                write_naming_value(AM_NAMING_LOCK, nlist->list[i].url);
                j = 0;
                break;
            }
            j++;
        }
        if (j > 0) {
            write_naming_value(AM_NAMING_LOCK, NULL);
            v->log("naming_validator(): none of url values are valid");
        }
        sleep(t);
    }
    return 0;
}

static void *url_validator(void *arg) {
    const char *status_message;
    int vst, httpcode = 0;
    naming_validator_int_t *v = (naming_validator_int_t *) arg;
    vst = v->validate(v->url, &status_message, &httpcode);
    MUTEX_LOCK(mutex);
    if (vst == 0) {
        v->log("naming_validator(): %s validation succeeded", v->url);
        nlist->list[v->idx].status = 1;
    } else {
        v->log("naming_validator(): %s validation failed with %s (%d), http status code: %d", v->url, status_message, vst, httpcode);
        nlist->list[v->idx].status = 0;
    }
    nlist->list[v->idx].run = 0;
    MUTEX_UNLOCK(mutex);
    free(v);
    return 0;
}

#ifdef _MSC_VER

static VOID CALLBACK callback(PVOID lpParam, BOOLEAN TimerOrWaitFired) {
    int i, j;
    naming_validator_t *v = (naming_validator_t *) lpParam;
#else 

#if defined(__sun) && defined(__SunOS_5_10)

static void callback(void *ta) {
    int i, j;
    naming_validator_t *v = (naming_validator_t *) ta;
#else

static void callback(union sigval si) {
    int i, j;
    naming_validator_t *v = (naming_validator_t *) si.sival_ptr;
#endif

#endif
    for (i = 0; i < v->url_size; i++) {
        THREAD vthr;
        naming_validator_int_t *arg = NULL;
        MUTEX_LOCK(mutex);
        j = nlist->list[i].run;
        MUTEX_UNLOCK(mutex);
        if (j == 1) {
            v->log("naming_validator(): validate is already running for %s", v->url_list[i]);
            continue;
        }
        arg = (naming_validator_int_t *) malloc(sizeof (naming_validator_int_t));
        if (arg == NULL) {
            v->log("naming_validator(): timer callback memory allocation error");
            return;
        }
        arg->log = v->log;
        arg->validate = v->validate;
        arg->url = v->url_list[i];
        arg->idx = i;
        THREAD_CREATE(vthr, url_validator, arg);
        MUTEX_LOCK(mutex);
        nlist->list[i].run = 1;
        MUTEX_UNLOCK(mutex);
    }
}

#ifndef _MSC_VER

#if defined(__sun) && defined(__SunOS_5_10)

static void *evtimer_listener(void *arg) {
    port_event_t ev;
    while (keep_going) {
        if (port_get(port, &ev, NULL) < 0) {
            break;
        }
        if (ev.portev_source == PORT_SOURCE_TIMER) {
            naming_validator_t *ptr = (naming_validator_t *) ev.portev_user;
            callback(ptr);
        } else
            break;
    }
    pthread_exit(NULL);
}

#endif

static int set_timer(timer_t * timer_id, float delay, float interval, timer_callback *func, void *data) {
    int status = 0;
    struct itimerspec ts;
    struct sigevent se;
#if defined(__sun) && defined(__SunOS_5_10)
    THREAD pthr;
    port_notify_t pnotif;
    pnotif.portnfy_port = port;
    pnotif.portnfy_user = data;
    se.sigev_notify = SIGEV_PORT;
    se.sigev_value.sival_ptr = &pnotif;
#else
    se.sigev_notify = SIGEV_THREAD;
    se.sigev_value.sival_ptr = data;
    se.sigev_notify_function = func;
    se.sigev_notify_attributes = NULL;
#endif
    status = timer_create(CLOCK_REALTIME, &se, timer_id);
    ts.it_value.tv_sec = abs(delay);
    ts.it_value.tv_nsec = (delay - abs(delay)) * 1e09;
    ts.it_interval.tv_sec = abs(interval);
    ts.it_interval.tv_nsec = (interval - abs(interval)) * 1e09;
    status = timer_settime(*timer_id, 0, &ts, 0);
#if defined(__sun) && defined(__SunOS_5_10)
    THREAD_CREATE(pthr, evtimer_listener, NULL);
#endif
    return status;
}

#endif

void stop_naming_validator() {
    keep_going = 0;
}

void *naming_validator(void *arg) {
    int i, status;
#ifdef _MSC_VER
    HANDLE tick_q = NULL;
    HANDLE tick = NULL;
#else
    timer_t tick;
#endif
    naming_validator_t *v = (naming_validator_t *) arg;
    if (v->log == NULL || v->validate == NULL) return 0;
    nlist = (naming_status_t *) malloc(sizeof (naming_status_t));
    if (nlist != NULL) {
        nlist->list = (naming_url_t *) calloc(v->url_size, sizeof (nlist->list[0]));
        if (nlist->list != NULL) {
            MUTEX_CREATE(mutex);
            nlist->size = v->url_size;
            for (i = 0; i < v->url_size; i++) {
                nlist->list[i].status = 0;
                nlist->list[i].run = 0;
                nlist->list[i].url = v->url_list[i];
            }
#ifdef _MSC_VER
            tick_q = CreateTimerQueue();
            CreateTimerQueueTimer(&tick, tick_q,
                    (WAITORTIMERCALLBACK) callback, arg, 1000, (v->poll_valid * 1000), WT_EXECUTELONGFUNCTION);
#else
#if defined(__sun) && defined(__SunOS_5_10) 
            if ((port = port_create()) == -1) {
                v->log("naming_validator(): port_create failed");
            }
#endif
            status = set_timer(&tick, 1, v->poll_valid, callback, arg);
#endif
            THREAD_CREATE(wthr, url_watchdog, arg);
            while (keep_going) {
                sleep(1);
            }
#ifdef _MSC_VER
            DeleteTimerQueue(tick_q);
#else
            timer_delete(tick);
#if defined(__sun) && defined(__SunOS_5_10)
            close(port);
#endif
#endif
            free(nlist->list);
            MUTEX_DELETE(mutex);
        }
        free(nlist);
    } else {
        v->log("naming_validator(): memory allocation error");
    }
    return 0;
}

char *read_naming_value(const char *key) {
    char *ret = NULL, fn[1024];
#ifdef _MSC_VER

#define MAXRETRIES  5
#define RETRYDELAY  250

    HANDLE fd = INVALID_HANDLE_VALUE;
    DWORD dwRetries = 0;
    BOOL bSuccess = FALSE;
    DWORD dwErr = 0, br, fs, fsh;
    OVERLAPPED rs;
    /* Windows 2003 and IIS6 need read/write access permission for IUSR_ account to c:/windows/temp/ folder */
    GetTempPath(sizeof (fn), fn);
    strcat(fn, key);
    do {
        fd = CreateFileA(fn, GENERIC_READ, 0, NULL,
                OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if (INVALID_HANDLE_VALUE == fd) {
            dwErr = GetLastError();
            if (ERROR_SHARING_VIOLATION == dwErr) {
                dwRetries += 1;
                Sleep(RETRYDELAY);
                continue;
            } else {
                break;
            }
        }
        bSuccess = TRUE;
        break;
    } while (dwRetries < MAXRETRIES);
    if (bSuccess == TRUE) {
        rs.Offset = 0;
        rs.OffsetHigh = 0;
        rs.hEvent = (HANDLE) 0;
        fs = GetFileSize(fd, &fsh);
        SetFilePointer(fd, 0, NULL, FILE_BEGIN);
        if (LockFileEx(fd, LOCKFILE_FAIL_IMMEDIATELY, 0, fs, fsh, &rs) == TRUE) {
            if (fs > 0 && (ret = (char *) malloc(fs + 1)) != NULL) {
                if (!ReadFile(fd, ret, fs, &br, NULL)) {
                    free(ret);
                    ret = NULL;
                } else {
                    ret[fs] = 0;
                }
            }
            UnlockFileEx(fd, 0, fs, fsh, &rs);
        }
        CloseHandle(fd);
    }
#else
    int fd, fs;
    struct flock fl;
    memset(&fl, 0, sizeof (fl));
    fl.l_type = F_RDLCK;
    fl.l_whence = SEEK_SET;
    fl.l_start = 0;
    fl.l_len = 0;
    sprintf(fn, "/tmp/%s", key);
    if ((fd = open(fn, O_RDONLY)) != -1) {
        if (fcntl(fd, F_SETLKW, &fl) != -1) {
            fs = lseek(fd, (off_t) 0, SEEK_END);
            if (fs > 0 && (ret = (char *) malloc(fs + 1)) != NULL) {
                if (pread(fd, ret, fs, (off_t) 0) == -1) {
                    free(ret);
                    ret = NULL;
                } else {
                    ret[fs] = 0;
                }
            }
            fl.l_type = F_UNLCK;
            fcntl(fd, F_SETLKW, &fl);
        }
        close(fd);
    }
#endif
    return ret;
}

void write_naming_value(const char *key, const char *value) {
    char fn[1024];
#ifdef _MSC_VER
    HANDLE fd;
    DWORD br, fs, fsh;
    OVERLAPPED rs;
    GetTempPath(sizeof (fn), fn);
    strcat(fn, key);
    if ((fd = CreateFileA(fn, GENERIC_WRITE, FILE_SHARE_READ, NULL,
            CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL)) != INVALID_HANDLE_VALUE) {
        if (GetLastError() == ERROR_ALREADY_EXISTS) SetLastError(0);
        rs.Offset = 0;
        rs.OffsetHigh = 0;
        rs.hEvent = (HANDLE) 0;
        fs = GetFileSize(fd, &fsh);
        SetFilePointer(fd, 0, NULL, FILE_BEGIN);
        if (LockFileEx(fd, LOCKFILE_EXCLUSIVE_LOCK | LOCKFILE_FAIL_IMMEDIATELY, 0, fs, fsh, &rs) == TRUE) {
            if (value == NULL) {
                WriteFile(fd, "", (DWORD) 0, &br, NULL);
            } else {
                WriteFile(fd, value, (DWORD) strlen(value), &br, NULL);
            }
            UnlockFileEx(fd, 0, fs, fsh, &rs);
        }
        CloseHandle(fd);
    }
#else
    int fd;
    struct flock fl;
    memset(&fl, 0, sizeof (fl));
    fl.l_type = F_WRLCK;
    fl.l_whence = SEEK_SET;
    fl.l_start = 0;
    fl.l_len = 0;
    sprintf(fn, "/tmp/%s", key);
    if ((fd = open(fn, O_WRONLY | O_CREAT | O_TRUNC, 0644)) != -1) {
        if (fcntl(fd, F_SETLKW, &fl) != -1) {
            lseek(fd, (off_t) 0, SEEK_SET);
            if (value == NULL) {
                write(fd, "", 0);
            } else {
                write(fd, value, strlen(value));
            }
            fl.l_type = F_UNLCK;
            fcntl(fd, F_SETLKW, &fl);
        }
        close(fd);
    }
#endif
}

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
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <netinet/tcp.h>
#include <time.h>
#ifdef __sun
#include <sys/filio.h>
#endif
#ifdef linux
#include <sys/ioctl.h>
#endif

#include "net.h"
#include "utils.h"

#define	MAX_RETRY_COUNT                 5
#define WAIT                            300000 //microseconds

#define SSL_VERIFY_NONE                 0
#define SSL_VERIFY_FAIL_IF_NO_PEER_CERT 0x02
#define SSL_VERIFY_PEER                 0x01
#define SSL_MODE_AUTO_RETRY             0x4
#define SSL_ERROR_SYSCALL               5
#define SSL_ERROR_WANT_READ             2
#define SSL_ERROR_WANT_WRITE            3
#define SSL_OP_NO_SSLv2                 0x01000000L

static pthread_mutex_t *ssl_mutexes = NULL;
static void *crypto_lib_h = NULL;
static void *ssl_lib_h = NULL;

struct ssl_func {
    const char *name;
    void (*ptr)(void);
};

static struct ssl_func ssl_sw[] = {
    {"SSL_library_init", NULL},
    {"SSL_CTX_new", NULL},
    {"SSL_new", NULL},
    {"SSL_set_fd", NULL},
    {"SSL_get_fd", NULL},
    {"SSL_connect", NULL},
    {"SSLv23_client_method", NULL},
    {"SSL_read", NULL},
    {"SSL_write", NULL},
    {"SSL_CTX_free", NULL},
    {"SSL_free", NULL},
    {"SSL_shutdown", NULL},
    {"SSL_CTX_use_certificate", NULL},
    {"SSL_CTX_use_PrivateKey", NULL},
    {"SSL_CTX_check_private_key", NULL},
    {"SSL_CTX_get_cert_store", NULL},
    {"SSL_CTX_add_client_CA", NULL},
    {"SSL_get_verify_result", NULL},
    {"SSL_CTX_ctrl", NULL},
    {"SSL_get_error", NULL},
    {"SSL_get_peer_certificate", NULL},
    {"SSL_get_peer_cert_chain", NULL},
    {"SSL_CTX_load_verify_locations", NULL},
    {"SSL_CTX_set_verify", NULL},
    {NULL, NULL}
};

static struct ssl_func crypto_sw[] = {
    {"CRYPTO_num_locks", NULL},
    {"CRYPTO_set_locking_callback", NULL},
    {"CRYPTO_set_id_callback", NULL},
    {"PKCS12_parse", NULL},
    {"X509_STORE_add_cert", NULL},
    {"d2i_PKCS12_fp", NULL},
    {"PKCS12_free", NULL},
    {"sk_num", NULL},
    {"sk_value", NULL},
    {"EVP_PKEY_free", NULL},
    {"CRYPTO_set_mem_functions", NULL},
    {"X509_free", NULL},
    {"OPENSSL_add_all_algorithms_noconf", NULL},
    {"sk_free", NULL},
    {"X509_get_subject_name", NULL},
    {"X509_get_issuer_name", NULL},
    {"X509_NAME_oneline", NULL},
    {"EVP_CIPHER_CTX_init", NULL},
    {"EVP_EncryptInit_ex", NULL},
    {"EVP_EncryptUpdate", NULL},
    {"EVP_EncryptFinal_ex", NULL},
    {"EVP_DecryptInit_ex", NULL},
    {"EVP_DecryptUpdate", NULL},
    {"EVP_DecryptFinal_ex", NULL},
    {"PKCS5_PBKDF2_HMAC_SHA1", NULL},
    {"EVP_CIPHER_CTX_cleanup", NULL},
    {"EVP_aes_256_cbc", NULL},
    {NULL, NULL}
};

typedef struct ssl_st SSL;
typedef struct ssl_ctx_st SSL_CTX;
typedef struct ssl_method_st SSL_METHOD;
typedef struct pkcs12_st PKCS12;
typedef struct x509_store_st X509_STORE;
typedef struct x509_store_ctx_st X509_STORE_CTX;
typedef struct x509_st X509;
typedef struct stack_st STACK;
#define STACK_OF(type) STACK
typedef struct evp_pkey_st EVP_PKEY;
typedef struct X509_name_st X509_NAME;

typedef struct engine_st ENGINE;
typedef struct evp_cipher_st EVP_CIPHER;

typedef struct evp_cipher_ctx_st {
    const EVP_CIPHER *cipher;
    ENGINE *engine;
    int encrypt;
    int buf_len;
    unsigned char oiv[16];
    unsigned char iv[16];
    unsigned char buf[32];
    int num;
    void *app_data;
    int key_len;
    unsigned long flags;
    void *cipher_data;
    int final_used;
    int block_mask;
    unsigned char final[32];
} EVP_CIPHER_CTX;


#define SSL_library_init (* (int (*)(void)) ssl_sw[0].ptr)
#define SSL_CTX_new (* (SSL_CTX * (*)(SSL_METHOD *)) ssl_sw[1].ptr)
#define SSL_new (* (SSL * (*)(SSL_CTX *)) ssl_sw[2].ptr)
#define SSL_set_fd (* (int (*)(SSL *, int)) ssl_sw[3].ptr)
#define SSL_get_fd (* (int (*)(SSL *)) ssl_sw[4].ptr)
#define SSL_connect (* (int (*)(SSL *)) ssl_sw[5].ptr)
#define SSLv23_client_method (* (SSL_METHOD * (*)(void)) ssl_sw[6].ptr)
#define SSL_read (* (int (*)(SSL *, void *, int)) ssl_sw[7].ptr)
#define SSL_write (* (int (*)(SSL *, const void *,int)) ssl_sw[8].ptr)
#define SSL_CTX_free (* (void (*)(SSL_CTX *)) ssl_sw[9].ptr)
#define SSL_free (* (void (*)(SSL *)) ssl_sw[10].ptr)
#define SSL_shutdown (* (void (*)(SSL *)) ssl_sw[11].ptr)
#define SSL_CTX_use_certificate (* (int (*)(SSL_CTX *, X509 *)) ssl_sw[12].ptr)
#define SSL_CTX_use_PrivateKey (* (int (*)(SSL_CTX *, EVP_PKEY *)) ssl_sw[13].ptr)
#define SSL_CTX_check_private_key (* (int (*)(const SSL_CTX *)) ssl_sw[14].ptr)
#define SSL_CTX_get_cert_store (* (X509_STORE * (*)(const SSL_CTX *)) ssl_sw[15].ptr)
#define SSL_CTX_add_client_CA (* (int (*)(SSL_CTX *,X509 *)) ssl_sw[16].ptr)
#define SSL_get_verify_result (* (int (*)(const SSL *)) ssl_sw[17].ptr)
#define SSL_CTX_ctrl (* (long (*)(SSL_CTX *, int, long, void *)) ssl_sw[18].ptr)
#define SSL_get_error (* (int (*)(SSL *, int)) ssl_sw[19].ptr)
#define SSL_get_peer_certificate (* (X509 * (*)(const SSL *)) ssl_sw[20].ptr)
#define SSL_get_peer_cert_chain (* (STACK_OF(X509) * (*)(const SSL *)) ssl_sw[21].ptr)
#define SSL_CTX_load_verify_locations (* (int (*)(SSL_CTX *, const char *, const char *)) ssl_sw[22].ptr)
#define SSL_CTX_set_verify (* (void (*)(SSL_CTX *, int, int (*verify_callback)(int, X509_STORE_CTX *))) ssl_sw[23].ptr)

#define CRYPTO_num_locks (* (int (*)(void)) crypto_sw[0].ptr)
#define CRYPTO_set_locking_callback \
	(* (void (*)(void (*)(int, int, const char *, int))) crypto_sw[1].ptr)
#define CRYPTO_set_id_callback \
	(* (void (*)(unsigned long (*)(void))) crypto_sw[2].ptr)
#define PKCS12_parse (* (int (*)(PKCS12 *, const char *, EVP_PKEY **, X509 **, STACK_OF(X509) **)) crypto_sw[3].ptr)
#define X509_STORE_add_cert (* (int (*)(X509_STORE *, X509 *)) crypto_sw[4].ptr)
#define d2i_PKCS12_fp (* (PKCS12 * (*)(FILE *, PKCS12 **)) crypto_sw[5].ptr)
#define PKCS12_free (* (void (*)(PKCS12 *)) crypto_sw[6].ptr)
#define sk_num (* (int (*)(const STACK *)) crypto_sw[7].ptr)
#define sk_value (* (void * (*)(const STACK *, int)) crypto_sw[8].ptr)
#define EVP_PKEY_free (* (void (*)(EVP_PKEY *)) crypto_sw[9].ptr)
#define CRYPTO_set_mem_functions (* (int (*)(void *(*m)(size_t),void *(*r)(void *,size_t), void (*f)(void *))) crypto_sw[10].ptr)
#define X509_free (* (void (*)(X509 *)) crypto_sw[11].ptr)
#define OPENSSL_add_all_algorithms_noconf (* (void (*)(void)) crypto_sw[12].ptr)
#define sk_free (* (void (*)(STACK *)) crypto_sw[13].ptr)
#define X509_get_subject_name (* (X509_NAME * (*)(X509 *)) crypto_sw[14].ptr)
#define X509_get_issuer_name (* (X509_NAME * (*)(X509 *)) crypto_sw[15].ptr)
#define X509_NAME_oneline (* (char * (*)(X509_NAME *,char *,int)) crypto_sw[16].ptr)
#define EVP_CIPHER_CTX_init (* (void (*)(EVP_CIPHER_CTX *)) crypto_sw[17].ptr)
#define EVP_EncryptInit_ex (* (int (*)(EVP_CIPHER_CTX *, const EVP_CIPHER *, ENGINE *, unsigned char *, unsigned char *)) crypto_sw[18].ptr)
#define EVP_EncryptUpdate (* (int (*)(EVP_CIPHER_CTX *, unsigned char *,int *, unsigned char *, int)) crypto_sw[19].ptr)
#define EVP_EncryptFinal_ex (* (int (*)(EVP_CIPHER_CTX *, unsigned char *, int *)) crypto_sw[20].ptr)
#define EVP_DecryptInit_ex (* (int (*)(EVP_CIPHER_CTX *, const EVP_CIPHER *,ENGINE *, unsigned char *, unsigned char *)) crypto_sw[21].ptr)
#define EVP_DecryptUpdate (* (int (*)(EVP_CIPHER_CTX *,unsigned char *,int *, unsigned char *, int)) crypto_sw[22].ptr)
#define EVP_DecryptFinal_ex (* (int (*)(EVP_CIPHER_CTX *, unsigned char *, int *)) crypto_sw[23].ptr)
#define PKCS5_PBKDF2_HMAC_SHA1 (* (int (*)(const char *, int, const unsigned char *, int, int, int, unsigned char *)) crypto_sw[24].ptr)
#define EVP_CIPHER_CTX_cleanup (* (int (*)(EVP_CIPHER_CTX *)) crypto_sw[25].ptr)
#define EVP_aes_256_cbc (* (const EVP_CIPHER * (*)(void)) crypto_sw[26].ptr)

struct _tls {
    sock_t sock;
    SSL *sslh;
    SSL_CTX *sslc;
};

static void ssl_locking_callback(int mode, int mutex_num, const char *file, int line) {
    if (mode & 1) {
        (void) pthread_mutex_lock(&ssl_mutexes[mutex_num]);
    } else {
        (void) pthread_mutex_unlock(&ssl_mutexes[mutex_num]);
    }
}

static unsigned long ssl_id_callback(void) {
    return (unsigned long) pthread_self();
}

static void *load_library(const char *so_name, struct ssl_func *sw, int flags) {
    void *lib_handle = NULL;
    struct ssl_func *fp, *fpd;

    union {
        void *p;
        void (*fp)(void);
    } u;

    if ((lib_handle = dlopen(so_name, flags)) == NULL) {
        LOG("load_library() cannot load %s", so_name);
        return NULL;
    }

    fpd = sw;
    for (fp = sw; fp->name != NULL; fp++) {
        u.p = dlsym(lib_handle, fp->name);
        if (u.fp == NULL) {
            LOG("load_library() failed to load SSL support. Could not find \"%s\"", fp->name);
            for (; fpd->name != NULL; fpd++) {
                fpd->ptr = NULL;
            }
            dlclose(lib_handle);
            return NULL;
        } else {
            fp->ptr = u.fp;
        }
    }
    return lib_handle;
}

static void ssl_load() {
    int i, size;
#if defined(__sun) || defined(linux)
    if (!(crypto_lib_h = load_library("libcrypto.so", crypto_sw, RTLD_LAZY | RTLD_GLOBAL))
            || !(ssl_lib_h = load_library("libssl.so", ssl_sw, RTLD_LAZY))) {
        LOG("ssl_load() SSL support is not available");
        if (ssl_lib_h) dlclose(ssl_lib_h);
        if (crypto_lib_h) dlclose(crypto_lib_h);
#endif
    } else if (CRYPTO_set_mem_functions && SSL_library_init && CRYPTO_num_locks &&
            CRYPTO_set_id_callback && CRYPTO_set_locking_callback && OPENSSL_add_all_algorithms_noconf) {
        CRYPTO_set_mem_functions(malloc, realloc, free);
        SSL_library_init();
        size = sizeof (pthread_mutex_t) * CRYPTO_num_locks();
        ssl_mutexes = (pthread_mutex_t *) malloc(size);
        for (i = 0; i < CRYPTO_num_locks(); i++) {
            pthread_mutex_init(&ssl_mutexes[i], NULL);
        }
        CRYPTO_set_id_callback(ssl_id_callback);
        CRYPTO_set_locking_callback(ssl_locking_callback);
        OPENSSL_add_all_algorithms_noconf();
        LOG("ssl_load() SSL support loaded");
    } else {
        LOG("ssl_load() SSL support is not available (not all required symbols are visible)");
    }
}

void ssl_crypto_init() {
    static pthread_once_t once = PTHREAD_ONCE_INIT;
    pthread_once(&once, ssl_load);
}

void ssl_crypto_shutdown() {
    int i;
    if (CRYPTO_set_locking_callback && CRYPTO_set_id_callback && CRYPTO_num_locks) {
        CRYPTO_set_locking_callback(NULL);
        CRYPTO_set_id_callback(NULL);
        for (i = 0; i < CRYPTO_num_locks(); i++) {
            pthread_mutex_destroy(&ssl_mutexes[i]);
        }
    }
    if (ssl_mutexes != NULL) {
        free(ssl_mutexes);
    }
    if (ssl_lib_h) dlclose(ssl_lib_h);
    if (crypto_lib_h) dlclose(crypto_lib_h);
}

void net_initialize() {
    ssl_crypto_init();
}

void net_shutdown() {
    ssl_crypto_shutdown();
}

int net_close(const sock_t sock) {
    LOG("net_close() disconnecting");
    return sock > 0 ? close(sock) : 0;
}

void net_error(int n) {
#if (_POSIX_C_SOURCE >= 200112L || _XOPEN_SOURCE >= 600) && ! _GNU_SOURCE
    size_t size = 1024;
    char *s = malloc(size);
    if (s == NULL) return;
    while (strerror_r(n, s, size) == -1 && errno == ERANGE) {
        size *= 2;
        s = realloc(s, size);
        if (s == NULL) return;
    }
    if (s != NULL) {
        LOG("net_error() %s", s);
        free(s);
    }
#endif
}

sock_t net_connect(const char * const host, const unsigned int port, const long timeout) {
    sock_t sock = -1;
    struct in6_addr serveraddr;
    char service[6];
    struct addrinfo *res, hints;
    int err, saveflags, on = 1;
    struct timeval tva;

    snprintf(service, sizeof (service), "%u", port);

#ifndef AI_NUMERICSERV
#define AI_NUMERICSERV 0
#endif

    memset(&hints, 0, sizeof (struct addrinfo));
    hints.ai_flags = AI_NUMERICSERV;
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    err = inet_pton(AF_INET, host, &serveraddr);
    if (err == 1) {
        hints.ai_family = AF_INET;
        hints.ai_flags |= AI_NUMERICHOST;
    } else {
        err = inet_pton(AF_INET6, host, &serveraddr);
        if (err == 1) {
            hints.ai_family = AF_INET6;
            hints.ai_flags |= AI_NUMERICHOST;
        }
    }

    if ((err = getaddrinfo(host, service, &hints, &res)) != 0) {
        LOG("cannot resolve address %s:%d, error %d", host, port, err);
        return -1;
    }

    if ((sock = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
        LOG("cannot create socket, error %d", errno);
        net_error(errno);
        freeaddrinfo(res);
        return -1;
    }

    if (timeout > 0) {
        memset(&tva, 0, sizeof (tva));
        tva.tv_sec = timeout;
        tva.tv_usec = 0;
        if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (char *) &tva, sizeof (tva)) < 0) {
            LOG("net_connect() setsockopt  SO_RCVTIMEO error %d", errno);
            net_error(errno);
        }
        if (setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, (char *) &tva, sizeof (tva)) < 0) {
            LOG("net_connect() setsockopt  SO_SNDTIMEO error %d", errno);
            net_error(errno);
        }
    }
    if (setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, (void *) &on, sizeof (on)) < 0) {
        LOG("net_connect() setsockopt  TCP_NODELAY error %d", errno);
        net_error(errno);
    }
    /* turn off bind address checking, and allow port numbers to be reused */
    if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (void *) &on, sizeof (on)) < 0) {
        LOG("net_connect() setsockopt  SO_REUSEADDR error %d", errno);
        net_error(errno);
    }

    saveflags = fcntl(sock, F_GETFL, 0);
    fcntl(sock, F_SETFL, saveflags | O_NONBLOCK);
    err = connect(sock, res->ai_addr, res->ai_addrlen);

    if (err == 0) {
        LOG("net_connect() connected to %s:%d", host, port);
    } else if (err < 0) {
        int ern = errno;
        if (ern != EWOULDBLOCK && ern != EINPROGRESS) {
            LOG("net_connect() connect error %d", ern);
            net_error(ern);
        }
        if (ern == EWOULDBLOCK || ern == EINPROGRESS) {
            fd_set fds, eds;
            struct timeval tv;
            memset(&tv, 0, sizeof (tv));
            tv.tv_sec = timeout == 0 ? 4 : timeout;
            tv.tv_usec = 0;
            FD_ZERO(&fds);
            FD_ZERO(&eds);
            FD_SET(sock, &fds);
            FD_SET(sock, &eds);
            if (select(FD_SETSIZE, 0, &fds, &eds, &tv) == 0) {
                LOG("net_connect() timeout connecting to %s:%d", host, port);
                net_close(sock);
                sock = -1;
            } else {
                if (FD_ISSET(sock, &fds)) {
                    int ret;
                    size_t rlen = sizeof (ret);
                    if (getsockopt(sock, SOL_SOCKET, SO_ERROR, &ret, (socklen_t *) & rlen) == 0) {
                        LOG("net_connect() connected to %s:%d", host, port);
                    } else {
                        LOG("net_connect() getsockopt error %d", errno);
                        net_error(errno);
                        net_close(sock);
                        sock = -1;
                    }
                } else if (FD_ISSET(sock, &eds)) {
                    LOG("net_connect() error %d", errno);
                    net_error(errno);
                    net_close(sock);
                    sock = -1;
                }
            }
        } else {
            net_close(sock);
            sock = -1;
        }
    }
    /* restore blocking mode for valid socket */
    if (sock != -1) fcntl(sock, F_SETFL, saveflags);
    freeaddrinfo(res);
    return sock;
}

int wait_for_io(sock_t sock, int timeout, int for_read) {
    struct timeval tv, *tvptr;
    fd_set fdset;
    int srv, err;
    do {
        FD_ZERO(&fdset);
        FD_SET(sock, &fdset);
        if (timeout < 0) {
            tvptr = NULL;
        } else {
            tv.tv_sec = 0;
            tv.tv_usec = timeout;
            tvptr = &tv;
        }
        srv = select(sock + 1, for_read ? &fdset : NULL, for_read ? NULL : &fdset, NULL, tvptr);
        err = errno;
    } while (srv == -1 && errno == EINTR);
    if (srv == 0) {
        return -1; /*timeout*/
    } else if (srv == -1) {
        return err; /*fatal error ( >0 )*/
    }
    return FD_ISSET(sock, &fdset) ? 0 /*OK*/ : err;
}

ssize_t net_read(const sock_t sock, char ** buff) {
    ssize_t out_len = 0, len = 0;
    u_long n = 0;
    char tmp[8192];
    int err = 0;
    long sopt = fcntl(sock, F_GETFL, 0);
    char block = (sopt & O_NONBLOCK) != 0 ? 0 : 1;
    do {
        n = 0;
        LOG("net_read() reading");
        memset(&tmp[0], 0, sizeof (tmp));
        len = recv(sock, tmp, sizeof (tmp), 0);
        if (len > 0) {
            LOG("net_read() read %d bytes", len);
            *buff = (char *) realloc(*buff, out_len + len + 1);
            if (*buff == NULL) {
                LOG("net_read() memory allocation error");
                break;
            }
            memcpy((*buff) + out_len, tmp, len);
            out_len += len;
            if (block) {
                if (ioctl(sock, FIONREAD, &n) < 0) {
                    LOG("net_read() ioctl failed");
                    net_error(errno);
                    break;
                }
                if (n > 0) {
                    LOG("net_read() more data available (%ld), continue reading", n);
                }
            }
        } else if (len == -1 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
            if ((err = wait_for_io(sock, WAIT, 1)) == 0) {
                len = 1;
            } else {
                if (err == -1) {
                    LOG("net_read() read retry timeout");
                } else {
                    LOG("net_read() read retry failed, error: %d", err);
                }
            }
        } else if (len == 0) {
            break;
        } else {
            net_error(errno);
            break;
        }
    } while (len > 0);

    if (*buff != NULL) {
        (*buff)[out_len] = 0;
    } else out_len = 0;

    return out_len;
}

ssize_t net_write(const sock_t sock, const char *buff, const size_t len) {
    ssize_t wrtlen, ttllen, reqlen;
    for (ttllen = 0, reqlen = len; reqlen > 0;) {
        if ((wrtlen = send(sock, buff + ttllen, reqlen, 0)) == -1) {
            int e = errno;
            LOG("net_write() socket not available, error %d", e);
            if ((e == EAGAIN || e == EWOULDBLOCK)) {
                if ((e = wait_for_io(sock, WAIT, 0)) == 0) {
                    continue;
                } else {
                    if (e == -1) {
                        LOG("net_write() write retry timeout");
                    } else {
                        LOG("net_write() write retry failed, error: %d", e);
                    }
                }
            }
            return -1;
        }
        LOG("net_write() writing %d bytes", wrtlen);
        ttllen += wrtlen;
        reqlen -= wrtlen;
    }
    return ttllen;
}

ssize_t tls_read(tls_t *ssd, char **buff) {
    ssize_t out_len = 0, len = 0, retry_count = 0;
    char tmp[4096];
    if (ssd != NULL && SSL_read) {
        long sopt = fcntl(SSL_get_fd(ssd->sslh), F_GETFL, 0);
        char block = (sopt & O_NONBLOCK) != 0 ? 0 : 1;
        do {
            memset(&tmp[0], 0, sizeof (tmp));
            len = SSL_read(ssd->sslh, tmp, sizeof (tmp));
            if (len > 0) {
                *buff = (char *) realloc(*buff, out_len + len + 1);
                if (*buff == NULL) {
                    LOG("tls_read() memory allocation error");
                    break;
                }
                memcpy((*buff) + out_len, tmp, len);
                out_len += len;
                retry_count = 0;
                if (block) {
                    int n = -1;
                    if (ioctl(SSL_get_fd(ssd->sslh), FIONREAD, &n) < 0) {
                        LOG("tls_read() ioctl failed, %d", errno);
                    }
                    if (n > 0) {
                        LOG("tls_read() more data available, continue reading", n);
                        len = n;
                        continue;
                    }
                    break;
                }
            } else if (len < 0) {
                switch (SSL_get_error(ssd->sslh, len)) {
                    case SSL_ERROR_WANT_READ:
                        if (wait_for_io(SSL_get_fd(ssd->sslh), WAIT, 1) == 0) {
                            retry_count++;
                            LOG("tls_read() data available, retrying (%d)", retry_count);
                            continue;
                        }
                    case SSL_ERROR_WANT_WRITE:
                        if (wait_for_io(SSL_get_fd(ssd->sslh), WAIT, 0) == 0) {
                            retry_count++;
                            LOG("tls_read() data available, retrying (%d)", retry_count);
                            continue;
                        }
                }
                break;
            } else if (len == 0) break;
        } while (len > 0 || retry_count < MAX_RETRY_COUNT);
        if (retry_count >= MAX_RETRY_COUNT)
            LOG("tls_read() max %d retries exhausted", MAX_RETRY_COUNT);
        if (*buff != NULL) {
            (*buff)[out_len] = 0;
        } else out_len = 0;
    }
    return out_len;
}

ssize_t tls_write(tls_t *ssd, const char *buffer, const size_t len) {
    ssize_t wrtlen, ttllen, reqlen, retry_count = 0;
    if (ssd != NULL && SSL_write) {
        for (ttllen = 0, reqlen = len; reqlen > 0;) {
            if (retry_count < MAX_RETRY_COUNT) {
                if (0 > (wrtlen = SSL_write(ssd->sslh, buffer + ttllen, reqlen))) {
                    int e = SSL_get_error(ssd->sslh, wrtlen);
                    LOG("tls_write() socket not available, error %d", e);
                    switch (e) {
                        case SSL_ERROR_WANT_READ:
                        {
                            if (wait_for_io(SSL_get_fd(ssd->sslh), WAIT, 1) == 0) {
                                retry_count++;
                                LOG("tls_write() socket available, retrying (%d)", retry_count);
                                continue;
                            }
                        }
                            break;
                        case SSL_ERROR_WANT_WRITE:
                        {
                            if (wait_for_io(SSL_get_fd(ssd->sslh), WAIT, 0) == 0) {
                                retry_count++;
                                LOG("tls_write() socket available, retrying (%d)", retry_count);
                                continue;
                            }
                        }
                            break;
                    }
                    return -1;
                }
                LOG("tls_write() writing %d bytes", wrtlen);
                ttllen += wrtlen;
                reqlen -= wrtlen;
                retry_count = 0;
            } else {
                LOG("tls_write() max %d retries exhausted", MAX_RETRY_COUNT);
                return -1;
            }
        }
    }
    return ttllen;
}

void show_server_cert(const SSL* s) {
    X509 *cert;
    char *line;
    cert = SSL_get_peer_certificate(s);
    if (cert != NULL) {
        LOG("Server certificates:");
        line = X509_NAME_oneline(X509_get_subject_name(cert), 0, 0);
        LOG("Subject: %s", line);
        free(line);
        line = X509_NAME_oneline(X509_get_issuer_name(cert), 0, 0);
        LOG("Issuer: %s", line);
        free(line);
        X509_free(cert);
    }
}

tls_t * tls_initialize(sock_t sock, int verifycert, const char *pkcs12file, const char *pkcs12pass) {
    tls_t *ssl = NULL;
    FILE *fp = NULL;
    PKCS12 *p12 = NULL;
    EVP_PKEY *pkey = NULL;
    X509 *cert = NULL;
    STACK_OF(X509) *ca = NULL;

    int status, retry_count = 0;

    if (sock) {
        ssl = malloc(sizeof (tls_t));
        ssl->sslh = NULL;
        ssl->sslc = NULL;
        ssl->sock = sock;

        ssl->sslc = SSL_CTX_new && SSLv23_client_method && SSL_new && SSL_set_fd && SSL_get_fd && SSL_connect ?
                SSL_CTX_new(SSLv23_client_method()) : NULL;
        if (ssl->sslc == NULL) {
            LOG("%s: SSL context is not available", __func__);
            tls_free(ssl);
            return NULL;
        }

        SSL_CTX_ctrl(ssl->sslc, 32 /*SSL_CTRL_OPTIONS*/, SSL_MODE_AUTO_RETRY, NULL);
        SSL_CTX_ctrl(ssl->sslc, 32 /*SSL_CTRL_OPTIONS*/, SSL_OP_NO_SSLv2, NULL);

        if (!(fp = fopen(pkcs12file, "rb"))) {
            LOG("%s: pkcs12 file %s is not available", __func__, pkcs12file == NULL ? "" : pkcs12file);
        } else {
            if (d2i_PKCS12_fp && PKCS12_parse && SSL_CTX_use_certificate && X509_free && SSL_CTX_use_PrivateKey && EVP_PKEY_free &&
                    PKCS12_free && SSL_CTX_check_private_key && SSL_CTX_get_cert_store && X509_STORE_add_cert) {
                d2i_PKCS12_fp(fp, &p12);
                fclose(fp);
                if (!p12) {
                    LOG("%s: error reading pkcs12 file %s", __func__, pkcs12file);
                } else {
                    if (!PKCS12_parse(p12, pkcs12pass, &pkey, &cert, &ca)) {
                        LOG("%s: error parsing pkcs12 file %s", __func__, pkcs12file);
                    } else {
                        if (cert != NULL) {
                            SSL_CTX_use_certificate(ssl->sslc, cert);
                            X509_free(cert);
                        }
                        if (pkey != NULL) {
                            SSL_CTX_use_PrivateKey(ssl->sslc, pkey);
                            EVP_PKEY_free(pkey);
                        }
                        PKCS12_free(p12);
                        SSL_CTX_check_private_key(ssl->sslc);
                        //SSL_CTX_set_verify_depth
                        if (ca && sk_num(ca)) {
                            int i;
                            LOG("%s: processing CA list (size: %d)", __func__, sk_num(ca));
                            X509_STORE *csto = SSL_CTX_get_cert_store(ssl->sslc);
                            for (i = 0; i < sk_num(ca); i++) {
                                X509 *ce = (X509 *) sk_value(ca, i);
                                if (!X509_STORE_add_cert(csto, ce))
                                    LOG("%s: X509_STORE_add_cert error", __func__);
                                if (!SSL_CTX_add_client_CA(ssl->sslc, ce))
                                    LOG("%s: SSL_CTX_add_client_CA error", __func__);
                                X509_free(ce);
                            }
                        }
                        if (ca) sk_free(ca);
                    }
                }
            }
        }

        ssl->sslh = SSL_new(ssl->sslc);
        if (ssl->sslh == NULL) {
            LOG("%s: SSL handle is not available", __func__);
            tls_free(ssl);
            return NULL;
        }
        if (!SSL_set_fd(ssl->sslh, ssl->sock)) {
            LOG("%s: SSL socket set failed", __func__);
            tls_free(ssl);
            return NULL;
        }

        /* do handshake */
        do {
            status = SSL_connect(ssl->sslh);
            if (status < 0) {
                switch (SSL_get_error(ssl->sslh, status)) {
                    case SSL_ERROR_WANT_READ:
                        if (wait_for_io(SSL_get_fd(ssl->sslh), WAIT, 1) == 0) {
                            retry_count++;
                            LOG("tls_initialize() socket available, retrying (%d)", retry_count);
                            continue;
                        }
                    case SSL_ERROR_WANT_WRITE:
                        if (wait_for_io(SSL_get_fd(ssl->sslh), WAIT, 0) == 0) {
                            retry_count++;
                            LOG("tls_initialize() socket available, retrying (%d)", retry_count);
                            continue;
                        }
                    case SSL_ERROR_SYSCALL:
                        LOG("tls_initialize() connect error (%d)", errno);
                        tls_free(ssl);
                        return NULL;
                }
            } else if (status == 1) break;
            LOG("tls_initialize: SSL connect failed");
            tls_free(ssl);
            return NULL;
        } while (retry_count < MAX_RETRY_COUNT);

        if (retry_count >= MAX_RETRY_COUNT) {
            LOG("tls_initialize() ssl/tls handshake max %d retries exhausted", MAX_RETRY_COUNT);
            tls_free(ssl);
            return NULL;
        }

        if (verifycert) {
            if (SSL_get_verify_result(ssl->sslh) != 0/*X509_V_OK*/) {
                LOG("server certificate validation failed");
                tls_free(ssl);
                return NULL;
            } else {
                show_server_cert(ssl->sslh);
                LOG("server certificate validation succeeded");
            }
        }
    }
    return ssl;
}

void tls_free(tls_t * s) {
    if (!s) return;
    if (s->sslh) {
        SSL_shutdown(s->sslh);
        SSL_free(s->sslh);
    }
    if (s->sslc)
        SSL_CTX_free(s->sslc);
    free(s);
    s = NULL;
}

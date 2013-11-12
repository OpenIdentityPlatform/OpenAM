/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: connection.cpp,v 1.10 2010/03/10 05:09:38 dknab Exp $
 *
 */
/*
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */
#include <stdexcept>
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
#include <iostream>
#include <sstream>
#include "am.h"
#include "connection.h"
#include "log.h"
#include "string_util.h"
#include "version.h"

USING_PRIVATE_NAMESPACE

#define SSL_VERIFY_NONE                 0
#define SSL_VERIFY_FAIL_IF_NO_PEER_CERT 0x02
#define SSL_VERIFY_PEER                 0x01
#define SSL_MODE_AUTO_RETRY             0x4
#define SSL_ERROR_SYSCALL               5
#define SSL_ERROR_WANT_READ             2
#define SSL_ERROR_WANT_WRITE            3
#define SSL_OP_NO_SSLv2                 0x01000000L
#define	MAX_RETRY_COUNT                 5
#define SOCKET_IO_WAIT_TIME             300000 //microseconds

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
    {"SSL_CTX_set_cipher_list", NULL},
    {"SSL_load_client_CA_file", NULL},
    {"SSL_CTX_set_client_CA_list", NULL},
    {"SSL_CTX_use_certificate_file", NULL},
    {"SSL_CTX_use_PrivateKey_file", NULL},
    {"SSL_set_client_CA_list", NULL},
    {"SSL_CTX_use_certificate_chain_file", NULL},
    {"SSL_CTX_set_default_passwd_cb", NULL},
    {"SSL_CTX_set_default_passwd_cb_userdata", NULL},
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
typedef int pem_password_cb(char *buf, int size, int rwflag, void *userdata);

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
#define SSL_CTX_set_cipher_list (* (int (*)(SSL_CTX *,const char *)) ssl_sw[24].ptr)
#define SSL_load_client_CA_file (* (STACK_OF(X509_NAME) * (*)(const char *)) ssl_sw[25].ptr)
#define SSL_CTX_set_client_CA_list (* (void (*)(SSL_CTX *, STACK_OF(X509_NAME) *)) ssl_sw[26].ptr)
#define SSL_CTX_use_certificate_file (* (int (*)(SSL_CTX *, const char *, int)) ssl_sw[27].ptr)
#define SSL_CTX_use_PrivateKey_file (* (int (*)(SSL_CTX *, const char *, int)) ssl_sw[28].ptr)
#define SSL_set_client_CA_list (* (void (*)(SSL *, STACK_OF(X509_NAME) *)) ssl_sw[29].ptr)
#define SSL_CTX_use_certificate_chain_file (* (int (*)(SSL_CTX *, const char *)) ssl_sw[30].ptr)
#define SSL_CTX_set_default_passwd_cb (* (void (*)(SSL_CTX *, pem_password_cb *)) ssl_sw[31].ptr)
#define SSL_CTX_set_default_passwd_cb_userdata (* (void (*)(SSL_CTX *, void *)) ssl_sw[32].ptr)

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

typedef struct _tls tls_t;

struct _tls {
    int sock;
    SSL *sslh;
    SSL_CTX *sslc;
};

static int password_cb(char *buf, int size, int rwflag, void *passwd) {
    strncpy(buf, (char *) passwd, size);
    buf[size - 1] = '\0';
    return (int) (strlen(buf));
}

extern "C" void ssl_locking_callback(int mode, int mutex_num, const char *file, int line) {
    if (mode & 1) {
        pthread_mutex_lock(&ssl_mutexes[mutex_num]);
    } else {
        pthread_mutex_unlock(&ssl_mutexes[mutex_num]);
    }
}

extern "C" unsigned long ssl_id_callback(void) {
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
        return NULL;
    }

    fpd = sw;
    for (fp = sw; fp->name != NULL; fp++) {
        u.p = dlsym(lib_handle, fp->name);
        if (u.fp == NULL) {
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

extern "C" void ssl_load() {
    int i, size;
    if (!(crypto_lib_h = load_library("libcrypto.so", crypto_sw, RTLD_LAZY | RTLD_GLOBAL))
            || !(ssl_lib_h = load_library("libssl.so", ssl_sw, RTLD_LAZY))) {
        Log::log(Log::ALL_MODULES, Log::LOG_WARNING,
                "Connection::initialize() SSL support is not available");
        if (ssl_lib_h) dlclose(ssl_lib_h);
        if (crypto_lib_h) dlclose(crypto_lib_h);
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
        Log::log(Log::ALL_MODULES, Log::LOG_INFO,
                "Connection::initialize() SSL support is available");
    } else {
        Log::log(Log::ALL_MODULES, Log::LOG_WARNING,
                "Connection::initialize() SSL support is not available (not all required symbols are visible)");
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
    if (ssl_lib_h || crypto_lib_h) {
        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                "Connection::shutdown() unloading SSL libraries");
    }
    if (ssl_lib_h) {
        dlclose(ssl_lib_h);
        ssl_lib_h = NULL;
    }
    if (crypto_lib_h) {
        dlclose(crypto_lib_h);
        crypto_lib_h = NULL;
    }
}

void show_server_cert(const SSL* s) {
    X509 *cert;
    char *line;
    if (SSL_get_peer_certificate) {
        cert = SSL_get_peer_certificate(s);
        if (cert != NULL) {
            line = X509_NAME_oneline(X509_get_subject_name(cert), 0, 0);
            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                    "Connection::Connection() server certificate subject: %s", line);
            free(line);
            line = X509_NAME_oneline(X509_get_issuer_name(cert), 0, 0);
            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                    "Connection::Connection() server certificate issuer: %s", line);
            free(line);
            X509_free(cert);
        }
    }
}

int wait_for_io(int sock, int timeout, int for_read) {
    struct timeval tv, *tvptr;
    fd_set fdset;
    int srv;
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
    } while (srv == -1 && errno == EINTR);
    if (srv == 0) {
        return -1;
    } else if (srv < 0) {
        return errno;
    }
    return FD_ISSET(sock, &fdset) ? 0 : -2;
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

tls_t * tls_initialize(int sock, int verifycert, const char *keyfile, const char *keypass,
        const char *certfile, const char *cafile, const char *ciphers) {
    tls_t *ssl = NULL;
    int status, retry_count = 0;
    const char *cl = ciphers != NULL ? ciphers : "HIGH:MEDIUM";

    if (sock) {
        ssl = (tls_t *) malloc(sizeof (tls_t));
        ssl->sslh = NULL;
        ssl->sslc = NULL;
        ssl->sock = sock;

        ssl->sslc = SSL_CTX_new && SSLv23_client_method && SSL_new && SSL_set_fd
                && SSL_get_fd && SSL_connect && SSL_CTX_set_cipher_list && SSL_CTX_set_verify
                && SSL_CTX_load_verify_locations && SSL_CTX_use_certificate_file && SSL_CTX_use_PrivateKey_file
                && SSL_CTX_set_default_passwd_cb && SSL_CTX_set_default_passwd_cb_userdata ?
                SSL_CTX_new(SSLv23_client_method()) : NULL;
        if (ssl->sslc == NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL context is not available");
            tls_free(ssl);
            return NULL;
        }

        SSL_CTX_ctrl(ssl->sslc, 32, SSL_MODE_AUTO_RETRY, NULL);
        /*SSL_CTX_ctrl(ssl->sslc, 32, SSL_OP_NO_SSLv2, NULL);*/

        if (SSL_CTX_set_cipher_list(ssl->sslc, cl) != 1) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL cipher list \"%s\" is not available", cl);
            tls_free(ssl);
            return NULL;
        }

        SSL_CTX_set_verify(ssl->sslc, SSL_VERIFY_NONE, NULL);

        if (cafile) {
            if (!SSL_CTX_load_verify_locations(ssl->sslc, cafile, NULL)) {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                        "Connection::Connection() SSL failed to load trusted CA certificates file \"%s\"", cafile);
            }
        }

        if (certfile) {
            if (!SSL_CTX_use_certificate_file(ssl->sslc, certfile, 1)) {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                        "Connection::Connection() SSL failed to load client certificate file \"%s\"", certfile);
            }
        }

        if (keyfile) {
            if (keypass != NULL) {
                SSL_CTX_set_default_passwd_cb_userdata(ssl->sslc, (void *) keypass);
                SSL_CTX_set_default_passwd_cb(ssl->sslc, password_cb);
            }
            if (!SSL_CTX_use_PrivateKey_file(ssl->sslc, keyfile, 1)) {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                        "Connection::Connection() SSL failed to load private key file \"%s\"", keyfile);
            }
        }

        ssl->sslh = SSL_new(ssl->sslc);
        if (ssl->sslh == NULL) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL handle is not available");
            tls_free(ssl);
            return NULL;
        }
        if (!SSL_set_fd(ssl->sslh, ssl->sock)) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL socket set failed");
            tls_free(ssl);
            return NULL;
        }

        /* do SSL handshake */
        do {
            status = SSL_connect(ssl->sslh);
            if (status < 0) {
                switch (SSL_get_error(ssl->sslh, status)) {
                    case SSL_ERROR_WANT_READ:
                        if (wait_for_io(SSL_get_fd(ssl->sslh), SOCKET_IO_WAIT_TIME, 1) == 0) {
                            retry_count++;
                            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                                    "Connection::Connection() SSL socket available, retrying (%d)", retry_count);
                            continue;
                        }
                    case SSL_ERROR_WANT_WRITE:
                        if (wait_for_io(SSL_get_fd(ssl->sslh), SOCKET_IO_WAIT_TIME, 0) == 0) {
                            retry_count++;
                            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                                    "Connection::Connection() SSL socket available, retrying (%d)", retry_count);
                            continue;
                        }
                    case SSL_ERROR_SYSCALL:
                        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                                "Connection::Connection() SSL connect error (%d)", errno);
                        tls_free(ssl);
                        return NULL;
                }
            } else if (status == 1) break;
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL connect failed");
            tls_free(ssl);
            return NULL;
        } while (retry_count < MAX_RETRY_COUNT);

        if (retry_count >= MAX_RETRY_COUNT) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Connection::Connection() SSL handshake max %d retries exhausted", MAX_RETRY_COUNT);
            tls_free(ssl);
            return NULL;
        }

        if (verifycert) {
            if (SSL_get_verify_result(ssl->sslh) != 0) {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                        "Connection::Connection() SSL server certificate validation failed");
                show_server_cert(ssl->sslh);
                tls_free(ssl);
                return NULL;
            } else {
                Log::log(Log::ALL_MODULES, Log::LOG_DEBUG,
                        "Connection::Connection() SSL server certificate validation succeeded");
                show_server_cert(ssl->sslh);
            }
        }
    }
    return ssl;
}

unsigned long Connection::timeout = NETWORK_TIMEOUT;
std::string Connection::proxyHost = "";
std::string Connection::proxyUser = "";
std::string Connection::proxyPassword = "";
std::string Connection::proxyPort = "";
std::string Connection::cipherList = "";
bool Connection::trustServerCerts = true;
std::string Connection::keyFile = "";
std::string Connection::caFile = "";
std::string Connection::certFile = "";
std::string Connection::keyPassword = "";

Connection::Connection(const ServerInfo& srv) : sock(-1), statusCode(-1), dataLength(-1), server(srv), ssl(NULL) {
    struct in6_addr serveraddr;
    char service[6];
    struct addrinfo *res, hints;
    int err, saveflags, on = 1;
    struct timeval tva;
    const char *host = server.getHost().c_str();
    unsigned int port = server.getPort();

    snprintf(service, 6, "%u", port);

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
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() cannot resolve address %s:%d, error %d",
                host, port, err);
        return;
    }


    if ((sock = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0) {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() cannot create socket, error %d", errno);
        net_error(errno);
        freeaddrinfo(res);
        return;
    }

    if (timeout != 0) {
        memset(&tva, 0, sizeof (tva));
        tva.tv_sec = timeout;
        tva.tv_usec = 0;
        if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (char *) &tva, sizeof (tva)) < 0) {
            Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection::Connection() unable to set read timeout");
            net_error(errno);
        }
        if (setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, (char *) &tva, sizeof (tva)) < 0) {
            Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection::Connection() unable to set write timeout");
            net_error(errno);
        }
    }
    if (setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, (void *) &on, sizeof (on)) < 0) {
        Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection::Connection() setsockopt  TCP_NODELAY error");
        net_error(errno);
    }
    if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (void *) &on, sizeof (on)) < 0) {
        Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection::Connection() setsockopt  SO_REUSEADDR error");
        net_error(errno);
    }

    saveflags = fcntl(sock, F_GETFL, 0);
    fcntl(sock, F_SETFL, saveflags | O_NONBLOCK);
    err = connect(sock, res->ai_addr, res->ai_addrlen);

    if (err == 0) {
        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::Connection() connected to %s:%d", host, port);
    } else if (err < 0) {
        int ern = errno;
        if (ern != EWOULDBLOCK && ern != EINPROGRESS) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() connect error %d", ern);
            net_error(ern);
        }
        if (ern == EWOULDBLOCK || ern == EINPROGRESS) {
            fd_set fds, eds;
            struct timeval tv;
            memset(&tv, 0, sizeof (tv));
            tv.tv_sec = timeout == 0 ? NETWORK_TIMEOUT / 1000 : timeout / 1000;
            tv.tv_usec = 0;
            FD_ZERO(&fds);
            FD_ZERO(&eds);
            FD_SET(sock, &fds);
            FD_SET(sock, &eds);
            if (select(FD_SETSIZE, 0, &fds, &eds, &tv) == 0) {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() timeout connecting to %s:%d", host, port);
                http_close();
            } else {
                if (FD_ISSET(sock, &fds)) {
                    int ret;
                    socklen_t rlen = sizeof (ret);
                    if (getsockopt(sock, SOL_SOCKET, SO_ERROR, &ret, &rlen) == 0) {
                        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::Connection() connected to %s:%d", host, port);
                    } else {
                        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() getsockopt error %d", errno);
                        net_error(errno);
                        http_close();
                    }
                } else if (FD_ISSET(sock, &eds)) {
                    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::Connection() error %d", errno);
                    net_error(errno);
                    http_close();
                }
            }
        } else {
            http_close();
        }
    }

    if (sock != -1) {
        fcntl(sock, F_SETFL, saveflags);

        if (server.useSSL()) {
            ssl = tls_initialize(sock, trustServerCerts ? 0 : 1,
                    keyFile.empty() ? NULL : keyFile.c_str(),
                    keyPassword.empty() ? NULL : keyPassword.c_str(),
                    certFile.empty() ? NULL : certFile.c_str(),
                    caFile.empty() ? NULL : caFile.c_str(),
                    cipherList.empty() ? NULL : cipherList.c_str());
        }
    }
    freeaddrinfo(res);
}

void Connection::http_close() {
    if (static_cast<tls_t *> (NULL) != ssl) {
        tls_free(static_cast<tls_t *> (ssl));
    }
    if (sock > 0) {
        close(sock);
    }
    sock = -1;
}

ssize_t Connection::response(char ** buff) {
    ssize_t out_len = 0, len = 0;
    u_long n = 0;
    int err = 0;
    char *tmp = NULL;
#define TEMP_SIZE 8192
    if ((tmp = (char *) malloc(TEMP_SIZE))) {
        if (server.useSSL()) {
            if (static_cast<tls_t *> (NULL) != ssl) {
                tls_t *ssd = static_cast<tls_t *> (ssl);
                do {
                    n = 0;
                    memset((void *) tmp, 0, TEMP_SIZE);
                    len = SSL_read(ssd->sslh, tmp, TEMP_SIZE);
                    if (len > 0) {
                        *buff = (char *) realloc(*buff, out_len + len + 1);
                        if (*buff == NULL) {
                            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() SSL memory allocation error");
                            break;
                        }
                        memcpy((*buff) + out_len, tmp, len);
                        out_len += len;
                        if (ioctl(SSL_get_fd(ssd->sslh), FIONREAD, &n) < 0) {
                            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() SSL ioctl failed, %d", errno);
                            net_error(errno);
                            break;
                        }
                        if (n > 0) {
                            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::response() SSL more data available (%ld), continue reading", n);
                        }
                    } else if (len == 0) {
                        break;
                    } else {
                        switch (SSL_get_error(ssd->sslh, len)) {
                            case SSL_ERROR_WANT_READ:
                                if ((err = wait_for_io(SSL_get_fd(ssd->sslh), SOCKET_IO_WAIT_TIME, 1)) == 0) {
                                    len = 1;
                                }
                            case SSL_ERROR_WANT_WRITE:
                                if ((err = wait_for_io(SSL_get_fd(ssd->sslh), SOCKET_IO_WAIT_TIME, 0)) == 0) {
                                    len = 1;
                                }
                        }
                        break;
                    }
                } while (len > 0);
            } else {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() SSL connection is not available");
            }
        } else {
            do {
                n = 0;
                memset((void *) tmp, 0, TEMP_SIZE);
                len = recv(sock, tmp, TEMP_SIZE, 0);
                if (len > 0) {
                    *buff = (char *) realloc(*buff, out_len + len + 1);
                    if (*buff == NULL) {
                        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() memory allocation error");
                        break;
                    }
                    memcpy((*buff) + out_len, tmp, len);
                    out_len += len;
                    if (ioctl(sock, FIONREAD, &n) < 0) {
                        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() ioctl failed, %d", errno);
                        net_error(errno);
                        break;
                    }
                    if (n > 0) {
                        Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::response() more data available (%ld), continue reading", n);
                    }
                } else if (len < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
                    Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Connection::response() got %s error, retrying",
                            errno == EAGAIN ? "EAGAIN" : "EWOULDBLOCK");
                    if ((err = wait_for_io(sock, SOCKET_IO_WAIT_TIME, 1)) == 0) {
                        len = 1;
                    } else {
                        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::response() read retry failed, error: %d", err);
                    }
                } else if (len == 0) {
                    break;
                } else {
                    net_error(errno);
                    break;
                }
            } while (len > 0);
        }
        free(tmp);
    }

    if (*buff != NULL) {
        (*buff)[out_len] = 0;
    } else out_len = 0;

    return out_len;
}

ssize_t Connection::request(const char *buff, const size_t len) {
    ssize_t wrtlen, ttllen = 0, reqlen;
    if (server.useSSL()) {
        if (static_cast<tls_t *> (NULL) != ssl) {
            tls_t *ssd = static_cast<tls_t *> (ssl);
            for (ttllen = 0, reqlen = len; reqlen > 0;) {
                if (0 > (wrtlen = SSL_write(ssd->sslh, buff + ttllen, reqlen))) {
                    int e = SSL_get_error(ssd->sslh, wrtlen);
                    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::request() SSL socket not available, error %X", e);
                    switch (e) {
                        case SSL_ERROR_WANT_READ:
                        {
                            if (wait_for_io(SSL_get_fd(ssd->sslh), SOCKET_IO_WAIT_TIME, 1) == 0) {
                                continue;
                            }
                        }
                            break;
                        case SSL_ERROR_WANT_WRITE:
                        {
                            if (wait_for_io(SSL_get_fd(ssd->sslh), SOCKET_IO_WAIT_TIME, 0) == 0) {
                                continue;
                            }
                        }
                            break;
                    }
                    return -1;
                }
                Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::request() SSL writing %ld bytes", wrtlen);
                ttllen += wrtlen;
                reqlen -= wrtlen;
            }
        } else {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::request() SSL connection is not available");
        }
    } else {
        for (ttllen = 0, reqlen = len; reqlen > 0;) {
            if (0 > (wrtlen = send(sock, buff + ttllen, reqlen, 0))) {
                int e = errno;
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Connection::request() socket not available, error %X", e);
                net_error(e);
                switch (e) {
                    case EINTR:
                        continue;
                    case ECONNRESET:
                        return 0;
                }
                return -1;
            }
            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::request() writing %ld bytes", wrtlen);
            ttllen += wrtlen;
            reqlen -= wrtlen;
        }
    }
    return ttllen;
}

Connection::~Connection() {
    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
            "Connection::http_close(): cleaning up");
    http_close();
}

am_status_t Connection::sendRequest(const char *type, std::string& uri, ConnHeaderMap& hdrs, std::string& data) {
    bool status = false;
    char *buffer = NULL;
    if (sock > 0 && type) {
        std::stringstream hs;
        hs << type << (uri.empty() ? " /" : uri)
                << " HTTP/1.0\r\n"
                << "User-Agent: OpenAM Web Agent/" << Version::getAgentVersion() << "\r\n"
                << "Connection: close\r\n";

        /* add request headers */
        ConnHeaderMap::iterator it = hdrs.begin();
        ConnHeaderMap::iterator itEnd = hdrs.end();
        for (; it != itEnd; ++it) {
            hs << (*it).first << "\r\n";
        }

        if (strstr(type, "POST") != NULL) {
            hs << "Content-Length: " << data.length() << "\r\n\r\n" << data;
        } else {
            hs << "\r\n";
        }

        Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                "Connection::sendRequest():\n%s", hs.str().c_str());

        ssize_t sz = request(hs.str().c_str(), hs.str().length());
        if (sz > 0) {

            sz = response(&buffer);
            if (sz > 0 && buffer) {

                dataBuffer.reserve(sz);
                dataBuffer.append(buffer, sz);
                dataBuffer.erase(0, dataBuffer.find("\r\n\r\n") + 4);
                dataLength = dataBuffer.length();

                std::string header(buffer);
                header.erase(header.find("\r\n\r\n") + 4);
                statusCode = strtol(&header[header.find(' ') + 1], NULL, 10);
                header.erase(0, header.find("\r\n") + 2);

                Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                        "Connection::response(): HTTP %d", statusCode);

                std::stringstream stringStream(header);
                std::string line;
                while (std::getline(stringStream, line)) {
                    std::size_t prev = 0, pos;
                    while ((pos = line.find_first_of("\r\n", prev)) != std::string::npos) {
                        if (pos > prev) {
                            std::string entry = line.substr(prev, pos - prev);
                            std::string::size_type n = entry.find_first_of(':');
                            if (n != std::string::npos) {
                                headers.insert(ConnHeaderMapValue(entry.substr(0, n), entry.substr(n + 2)));
                            }
                            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                                    "Connection::response(): Header: %s", entry.c_str());
                        }
                        prev = pos + 1;
                    }
                    if (prev < line.length()) {
                        std::string entry = line.substr(prev, std::string::npos);
                        std::string::size_type n = entry.find_first_of(':');
                        if (n != std::string::npos) {
                            headers.insert(ConnHeaderMapValue(entry.substr(0, n), entry.substr(n + 2)));
                        }
                        Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
                                "Connection::response(): Header: %s", entry.c_str());
                    }
                }

                status = true;
                free(buffer);
            }
        }
    }
    return status ? AM_SUCCESS : AM_HTTP_ERROR;
}

extern "C" int decrypt_base64(const char *, char *, const char*);

am_status_t Connection::initialize(const Properties& properties) {
    if (!ssl_lib_h || !crypto_lib_h) {
        std::string decrypt_key = properties.get(AM_POLICY_KEY_PROPERTY, "");
        keyPassword = properties.get(AM_COMMON_CERT_KEY_PASSWORD_PROPERTY, "");
        if (!decrypt_key.empty() && !keyPassword.empty()) {
            char decrypt_passwd[100] = "";
            if (decrypt_base64(keyPassword.c_str(), decrypt_passwd, decrypt_key.c_str()) == 0) {
                keyPassword.assign(decrypt_passwd);
            } else {
                Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                        "Connection::initialize(): failed to decrypt private key password");
            }
        }
        timeout = properties.getUnsigned(AM_COMMON_CONNECT_TIMEOUT_PROPERTY, NETWORK_TIMEOUT);
        proxyPort = properties.get(AM_COMMON_FORWARD_PROXY_PORT, "");
        proxyHost = properties.get(AM_COMMON_FORWARD_PROXY_HOST, "");
        proxyUser = properties.get(AM_COMMON_FORWARD_PROXY_USER, "");
        proxyPassword = properties.get(AM_COMMON_FORWARD_PROXY_PASSWORD, "");
        trustServerCerts = properties.getBool(AM_COMMON_TRUST_SERVER_CERTS_PROPERTY, true);
        certFile = properties.get(AM_COMMON_CERT_FILE_PROPERTY, "");
        keyFile = properties.get(AM_COMMON_CERT_KEY_PROPERTY, "");
        caFile = properties.get(AM_COMMON_CERT_CA_FILE_PROPERTY, "");
        cipherList = properties.get(AM_COMMON_CIPHERS_PROPERTY, "");
    }
    ssl_crypto_init();
    return AM_SUCCESS;
}

am_status_t Connection::initialize_in_child_process(const Properties& properties) {
    return AM_SUCCESS;
}

am_status_t Connection::shutdown() {
    ssl_crypto_shutdown();
    return AM_SUCCESS;
}

am_status_t Connection::shutdown_in_child_process() {
    return AM_SUCCESS;
}

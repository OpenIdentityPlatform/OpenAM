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
#ifndef NET_H
#define NET_H

typedef int sock_t;

void net_initialize();
void net_shutdown();

sock_t net_connect(const char * const host, const unsigned int port, const long timeout);
int net_close(const sock_t sock);

ssize_t net_read(const sock_t sock, char **buff);
ssize_t net_write(const sock_t sock, const char *buff, const size_t len);

typedef struct _tls tls_t;

tls_t *tls_initialize(sock_t sock, int verifycert, const char *pkcs12file, const char *pkcs12pass);
void tls_free(tls_t *tls);

ssize_t tls_read(tls_t *ssd, char **buffer);
ssize_t tls_write(tls_t *ssd, const char *buffer, const size_t len);

#endif

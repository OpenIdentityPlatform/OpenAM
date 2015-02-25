/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
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

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#define NTDDI_VERSION 0x0501
#include <winsock2.h>
#include <in6addr.h>
#include <ws2tcpip.h>
#define strtok_r strtok_s
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <limits.h>
#endif

static const unsigned char inverted_bits[8] = {0x00, 0x80, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE};
static uint8_t zero[16] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
static uint8_t one6[16] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
static uint8_t one4[16] = {0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

#ifdef _MSC_VER

int inet_pton(int af, const char *src, void *dst) {
    struct sockaddr_storage ss;
    int size = sizeof (ss);
    char src_copy[INET6_ADDRSTRLEN + 1];
    memset(&(ss), 0, sizeof (ss));
    strncpy(src_copy, src, INET6_ADDRSTRLEN + 1);
    src_copy[INET6_ADDRSTRLEN] = 0;
    if (WSAStringToAddress(src_copy, af, NULL, (struct sockaddr *) &ss, &size) == 0) {
        switch (af) {
            case AF_INET:
                *(struct in_addr *) dst = ((struct sockaddr_in *) &ss)->sin_addr;
                return 1;
            case AF_INET6:
                *(struct in6_addr *) dst = ((struct sockaddr_in6 *) &ss)->sin6_addr;
                return 1;
        }
    }
    return 0;
}

const char *inet_ntop(int af, const void *src, char *dst, socklen_t size) {
    struct sockaddr_storage ss;
    unsigned long s = size;
    memset(&(ss), 0, sizeof (ss));
    ss.ss_family = af;
    switch (af) {
        case AF_INET:
            ((struct sockaddr_in *) &ss)->sin_addr = *(struct in_addr *) src;
            break;
        case AF_INET6:
            ((struct sockaddr_in6 *) &ss)->sin6_addr = *(struct in6_addr *) src;
            break;
    }
    return (WSAAddressToString((struct sockaddr *) &ss, sizeof (ss), NULL, dst, &s) == 0) ? dst : NULL;
}
#endif

static int address(const char *ip, uint8_t *addr) {
    struct in_addr a4;
    struct in6_addr a6;
    if (inet_pton(AF_INET6, ip, &a6) > 0) {
        memcpy(addr, &a6.s6_addr, 16);
        return AF_INET6;
    } else if (inet_pton(AF_INET, ip, &a4) > 0) {
        memcpy(addr, &a4.s_addr, 4);
        return AF_INET;
    }
    return 0;
}

static void shiftl(uint8_t *o, size_t size) {
    unsigned int index;
    for (index = 0; index < size; index++) {
        uint8_t carryFlag = (o[index] & 0x80) > 0 ? 1 : 0;
        if (index > 0) if (carryFlag == 1) o[index - 1] = (o[index - 1] | 0x01);
        o[index] = (o[index] << 1);
    }
}

static void shiftlr(uint8_t *r, uint8_t *o, size_t size) {
    memcpy(r, o, size);
    shiftl(r, size);
}

static void shiftr(uint8_t *o, size_t size) {
    int index;
    int re = (int) size - 1;
    for (index = re; index >= 0; index--) {
        uint8_t carryFlag = (o[index] & 0x01) > 0 ? 1 : 0;
        if (index < re) if (carryFlag == 1) o[index + 1] = (o[index + 1] | 0x80);
        o[index] = (o[index] >> 1);
    }
}

static void shiftrr(uint8_t *r, uint8_t *o, size_t size) {
    memcpy(r, o, size);
    shiftr(r, size);
}

static void and(uint8_t *r, uint8_t *a, uint8_t *b, size_t s) {
    size_t i;
    for (i = 0; i < s; i++) r[i] = a[i] & b[i];
}

static void or(uint8_t *r, uint8_t *a, uint8_t *b, size_t s) {
    size_t i;
    for (i = 0; i < s; i++) r[i] = a[i] | b[i];
}

static void xor(uint8_t *r, uint8_t *a, uint8_t *b, size_t s) {
    size_t i;
    for (i = 0; i < s; i++) r[i] = a[i] ^ b[i];
}

static void not(uint8_t *a, size_t s) {
    size_t i;
    for (i = 0; i < s; i++) a[i] = ~(a[i]);
}

static void add(uint8_t *r, uint8_t *a, uint8_t *b, size_t s) {
    uint8_t aa[16], bb[16], ai[16], bi[16];
    memcpy(ai, a, s);
    memcpy(bi, b, s);
    do {
        and(aa, ai, bi, s);
        xor(bb, ai, bi, s);
        shiftlr(ai, aa, s);
        memcpy(bi, bb, s);
    } while (memcmp(aa, zero, s) != 0);
    memcpy(r, bb, s);
}

static void sub(uint8_t *r, uint8_t *a, uint8_t *b, size_t s) {
    uint8_t t[16], ta[16];
    memcpy(t, b, s);
    memcpy(ta, a, s);
    not(t, s);
    add(t, t, (s == 4 ? one4 : one6), s);
    add(r, ta, t, s);
}

static unsigned int prefix(uint8_t *a, int family) {
    int s = family == AF_INET6 ? 16 : 4;
    unsigned int i = family == AF_INET6 ? 128 : 32;
    uint8_t j[16];
    and(j, a, (s == 4 ? one4 : one6), s);
    while (memcmp(j, (s == 4 ? one4 : one6), s) != 0) {
        shiftr(a, s);
        i -= 1;
        and(j, a, (s == 4 ? one4 : one6), s);
    }
    return i;
}

static void l1(uint8_t *a, size_t s) {
    uint8_t i[16];
    and(i, a, (s == 4 ? one4 : one6), s);
    while (memcmp(i, zero, s) != 0) {
        shiftr(a, s);
        and(i, a, (s == 4 ? one4 : one6), s);
    }
}

static int l2(uint8_t *a, size_t s) {
    uint8_t i[16];
    int j = 0;
    shiftrr(i, a, s);
    while (memcmp(i, zero, s) != 0) {
        shiftr(a, s);
        j += 1;
        shiftrr(i, a, s);
    }
    return j;
}

static int match_cidr(const unsigned char* address, const unsigned char* mask, unsigned int mask_bits) {
    unsigned int divisor = mask_bits / 8;
    unsigned int modulus = mask_bits % 8;
    if (modulus) if ((address[divisor] & inverted_bits[modulus]) != (mask[divisor] & inverted_bits[modulus])) return 0;
    if (memcmp(address, mask, divisor)) return 0;
    return 1;
}

static void r2c(uint8_t *ip, uint8_t *a, uint8_t *b, int f, int *r, void (*log)(const char *, ...)) {
    int k;
    char straddr[INET6_ADDRSTRLEN];
    char ipaddr[INET6_ADDRSTRLEN];
    size_t s = f == AF_INET6 ? 16 : 4;
    uint8_t i[16], j, lxh[16], t[16], mid[16];
    xor(lxh, a, b, s);
    memcpy(i, lxh, s);
    l1(i, s);
    or(t, a, lxh, s);
    if (memcmp(i, zero, s) == 0 && memcmp(t, b, s) == 0) {
        sub(t, b, a, s);
        not(t, s);
        if (inet_ntop(f, a, straddr, sizeof (straddr)) != NULL && inet_ntop(f, ip, ipaddr, sizeof (ipaddr)) != NULL) {
            unsigned int bits = prefix(t, f);
            if (match_cidr(ip, a, bits) == 1) {
                if (log != NULL) log("ip_range_match(): found ip [%s] in range [%s/%d]", ipaddr, straddr, bits);
                (*r) += 1;
            } else {
                if (log != NULL) log("ip_range_match(): ip [%s] is not in range [%s/%d]", ipaddr, straddr, bits);
            }
        }
    } else {
        memcpy(i, lxh, s);
        j = l2(i, s);
        for (k = 0; k < j; k++) shiftl(i, s);
        sub(t, i, (s == 4 ? one4 : one6), s);
        not(t, s);
        and(mid, t, b, s);
        sub(t, mid, (s == 4 ? one4 : one6), s);
        r2c(ip, a, t, f, r, log);
        r2c(ip, mid, b, f, r, log);
    }
}

static int ip_range_match(const char *ip, const char *from, const char *to, void (*log)(const char *, ...)) {
    uint8_t ia[16], fa[16], ta[16];
    int f = 0;
    int r = 0;
    if ((f = address(ip, ia)) == address(from, fa) && f == address(to, ta) && f != 0) {
        r2c(ia, fa, ta, f, &r, log);
    }
    return r > 0 ? 1 : 0;
}

int ip_match(const char *ip, const char **list, unsigned int listsize, void (*log)(const char *, ...)) {
    unsigned char addr_raw[16];
    unsigned char mask_raw[16];
    int f = 0;
    unsigned int i;
    unsigned int bits = 0;
    char *p = NULL, *p_buf = NULL;
    char buf[INET6_ADDRSTRLEN];
    char cbuf[INET6_ADDRSTRLEN];
    for (i = 0; i < listsize; i++) {
        memset(buf, 0, sizeof (buf));
        memcpy(buf, list[i], sizeof (buf) - 1);
        memset(cbuf, 0, sizeof (cbuf));
        if (strchr(buf, '-') != NULL && strchr(buf, '/') == NULL) {
            /* ip range, 192.168.1.1-192.168.2.3,  match*/
            if ((p = strtok_r(buf, "-", &p_buf)) == NULL) continue;
            memcpy(cbuf, p, sizeof (cbuf) - 1);
            if ((p = strtok_r(NULL, "/", &p_buf)) == NULL) continue;
            if (ip_range_match(ip, cbuf, p, log) > 0) {
                return 1;
            }
        } else {
            /* cidr, 192.168.1.1/24, match*/
            if ((p = strtok_r(buf, "/", &p_buf)) == NULL) continue;
            memcpy(cbuf, p, sizeof (cbuf) - 1);
            if ((p = strtok_r(NULL, "/", &p_buf)) != NULL) {
                bits = atoi(p);
            } else {
                bits = 129;
            }
            if ((f = address(ip, addr_raw)) == address(cbuf, mask_raw) && f != 0) {
                unsigned int s = f == AF_INET6 ? 128 : 32;
                if (bits > s) bits = s;
                if (match_cidr(addr_raw, mask_raw, bits) == 1) {
                    if (log != NULL) log("ip_cidr_match(): found ip [%s] in range [%s]", ip, list[i]);
                    return 1;
                } else {
                    if (log != NULL) log("ip_cidr_match(): ip [%s] is not in range [%s]", ip, list[i]);
                }
            }
        }
    }
    return 0;
}



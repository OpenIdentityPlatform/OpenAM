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
 * $Id: utils.h,v 1.7 2008/08/26 00:18:37 subbae Exp $
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */

#ifndef __UTILS_H__
#define __UTILS_H__

#include <ctype.h>
#include <string>
#include <algorithm>
#include <stdint.h>
#include <am_policy.h>
#include "internal_macros.h"
#include "xml_element.h"
#include <set>

#ifdef _MSC_VER
typedef uint64_t uintmax_t;
#endif

namespace Utils {

    /**
     * Trims leading and lagging white spaces.
     */
    void trim(std::string &str);

    am_resource_match_t
    compare(const char *pat, const char *resName,
            const am_resource_traits_t *traits,
            bool fwdcmp, bool usePatterns);

    am_resource_match_t match_patterns(const char * /*pattern*/,
            const char * /*target*/,
            bool /*ignorecase*/,
            bool /*onelevelwildcard*/,
            char /*separator*/);

    time_t getTTL(const PRIVATE_NAMESPACE_NAME::XMLElement &, unsigned long);

    /* Throws 
     *	std::range_error if value is too small or large.
     *	std::domain_error if valid is invalid otherwise.
     */
    std::size_t getNumber(const std::string &);

    std::size_t getNumLength(long);

    bool is_prime(unsigned int);

    unsigned int get_prime(unsigned int);

    std::string toString(std::size_t);

    void expandEntityRefs(std::string &);

    class CmpFunc {
    public:

        CmpFunc(bool ignore_case = false) : icase(ignore_case) {
        }

        CmpFunc(const CmpFunc &cmpFunc) : icase(cmpFunc.icase) {
        }

        static bool cmp_ignore_case(char c1, char c2) {
            return toupper(c1) < toupper(c2);
        }

        bool operator()(const std::string &s1, const std::string &s2) const {
            if (icase == false) {
                return s1 < s2;
            } else {
                return std::lexicographical_compare(s1.begin(), s1.end(),
                        s2.begin(), s2.end(),
                        cmp_ignore_case);
            }
        }
    private:
        bool icase;
    };

    typedef struct url_info {
        char *url;
        size_t url_len;
        char *protocol;
        char *host;
        unsigned short port;
        am_bool_t has_parameters;
        am_bool_t has_patterns;
    } url_info_t;

    typedef struct url_info_list {
        unsigned int size;
        url_info_t *list;
    } url_info_list_t;

    typedef struct {
        char *name; // name of cookie.
        char *value; // value of cookie.
        char *domain; // cookie domain, or NULL if no domain.
        char *path; // cookie path, or NULL if no path.
        char *max_age; // max age, or NULL if no max age.
        am_bool_t isSecure; //if cookie is secure or not
        am_bool_t isHttpOnly; //if cookie is httponly or not
    } cookie_info_t;

    typedef struct cookie_info_list {
        unsigned int size;
        cookie_info_t *list;
    } cookie_info_list_t;

    /**
     * POST data structure to hold name value pair
     */
    typedef struct name_value_pair {
        char *name;
        char *value;
    } name_value_pair_t;

    /**
     * POST data structure to hold an array of name value pairs
     */
    typedef struct post_struct {
        char *buffer;
        name_value_pair_t *namevalue;
        int count;
    } post_struct_t;

    typedef struct boot_info_t {
        const char *agent_props_location;
        const char *agent_passwd;
        const char *agent_name;
        const char *agent_config_file;
        am_policy_t policy_handle;
        am_properties_t properties;
        url_info_list_t naming_url_list;
        am_log_module_id_t log_module;
        const char *shared_agent_profile_name;
        const char *realm_name;
        unsigned long url_validation_level;
        unsigned long ping_interval;
        unsigned long ping_fail_count;
        unsigned long ping_ok_count;
        const char *default_url_set;
        unsigned long connect_timeout;
        unsigned long receive_timeout;
    } boot_info_t;

    void parseIPAddresses(const std::string &property,
            std::set<std::string> &ipAddrSet);
    void parseCookieDomains(const std::string &property,
            std::set<std::string> &CDListSet);
    void cleanup_cookie_info(cookie_info_t *cookie_data);
    void cleanup_url_info_list(url_info_list_t *url_list);
    am_status_t parseCookie(std::string cookie,
            cookie_info_t *cookie_data);
    am_status_t parse_url(const char *url_str,
            size_t len,
            url_info_t *entry_ptr,
            am_bool_t validateURLs);
    am_status_t parse_url_list(const char *url_list_str,
            char sep,
            url_info_list_t *list_ptr,
            am_bool_t validateURLs);
    void cleanup_cookie_info_list(
            cookie_info_list_t *cookie_list);
    am_status_t parseCookieList(const char *property,
            char sep,
            cookie_info_list_t *cookie_list);
    am_status_t initCookieResetList(
            cookie_info_list_t *cookie_list,
            std::size_t domain_len,
            const char* cookieResetDefaultDomain);
    am_status_t initCookieResetList(
            cookie_info_list_t *cookie_list, const char* cookieResetDefaultDomain);

    enum flags {
        FL_ZERO = 0x01,
        FL_MINUS = 0x02,
        FL_PLUS = 0x04,
        FL_TICK = 0x08,
        FL_SPACE = 0x10,
        FL_HASH = 0x20,
        FL_SIGNED = 0x40,
        FL_UPPER = 0x80
    };

    enum ranks {
        rank_char = -2,
        rank_short = -1,
        rank_int = 0,
        rank_long = 1,
        rank_longlong = 2
    };

    inline flags operator|(flags a, flags b) {
        return static_cast<flags> (static_cast<int> (a) | static_cast<int> (b));
    }

    size_t format_int(char *q, size_t n, uintmax_t val, enum flags flags,
            int base, int width, int prec);

    int am_vsnprintf(char *buffer, size_t n, const char *format, va_list ap);

    int am_vasprintf(char **strp, const char *fmt, va_list ap);

    int am_printf(char **buffer, const char *fmt, ...);
    
}

#endif

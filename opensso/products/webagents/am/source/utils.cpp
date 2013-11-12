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
 * $Id: utils.cpp,v 1.16 2010/02/09 20:21:33 dknab Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdexcept>
#include <limits.h>
#include <cmath>
#include <cerrno>
#include <string>
#include <time.h>
#include "properties.h"
#include "utils.h"
#include "url.h"

using std::string;
using namespace Utils;
USING_PRIVATE_NAMESPACE;

const char *POLICY_SERVICE = "PolicyService";
const char *POLICY_RESPONSE = "PolicyResponse";
const char *SESSION_NOTIFICATION = "SessionNotification";
const char *AGENT_CONFIG_CHANGE_NOTIFICATION = "AgentConfigChangeNotification";
const char *ADD_LISTENER_RESPONSE = "AddPolicyListenerResponse";
const char *REMOVE_LISTENER_RESPONSE = "RemovePolicyListenerResponse";
const char *SESSION = "Session";
const char *POLICY_CHANGE_NOTIFICATION = "PolicyChangeNotification";
const char *SUBJECT_CHANGE_NOTIFICATION = "SubjectChangeNotification";
const char *RESOURCE_RESULT = "ResourceResult";
const char *POLICY_DECISION = "PolicyDecision";
const char *RESPONSE_ATTRIBUTES = "ResponseAttributes";
const char *ACTION_DECISION = "ActionDecision";
const char *ATTRIBUTE_VALUE_PAIR = "AttributeValuePair";
const char *RESPONSE_DECISIONS = "ResponseDecisions";
const char *RESOURCE_NAME = "ResourceName";
const char *SERVICE_NAME = "serviceName";
const char *SESSION_ID_ATTRIBUTE = "sid";
const char *SESSION_STATE_ATTRIBUTE = "state";
const char *SESSION_STATE_VALUE_VALID = "valid";
const char *SESSION_NOTIF_TIME = "Time";
const char *SESSION_NOTIF_TYPE = "Type";
const char *ATTRIBUTE_NAME = "name";
const char *NAME = "name";
const char *ATTRIBUTE = "Attribute";
const char *VALUE = "Value";
const char *TIME_TO_LIVE = "timeToLive";
const char *ADVICES = "Advices";
const char *NOTIFICATION_SET = "NotificationSet";
const char *NOTIFICATION = "Notification";
const char *NOTIFICATION_TYPE = "type";
const char *NOTIF_TYPE_MODIFIED = "modified";
const char *NOTIF_TYPE_ADDED = "added";
const char *NOTIF_TYPE_DELETED = "deleted";
const char *RESPONSE_SET = "ResponseSet";
const char *RESPONSE = "Response";
const char *NOTIFICATION_ID = "notid";
const char *REQUEST_ID = "reqid";
const char *VERSION = "vers";
const char *VERSION_STR = "version";
const char *REQUEST_ID_STR = "requestId";
const char *NOTIFICATION_SET_VERSION = "1.0";
const char *REQUEST_SET_VERSION = "1.0";
const char *REVISION_STR = "revisionNumber";
const char *ADVICE_LIST_RESPONSE = "AdvicesHandleableByAMResponse";
const char *SERVER_HANDLED_ADVICES = "AdvicesHandleableByAM";

const static string STAR("*");

#if	defined(LLONG_MAX)
#define MAX_64_BIT_INT	LLONG_MAX
#elif	defined(WIN32)
#define	MAX_64_BIT_INT	_I64_MAX
#elif   defined(HPUX)
#define MAX_64_BIT_INT  LONG_LONG_MAX
#elif defined(LINUX)
#define MAX_64_BIT_INT __LONG_LONG_MAX__
#else
#error "no constant available for the maximum value of a 64-bit integer"
#endif

#define	HTTP_PREFIX	"http://"
#define	HTTP_PREFIX_LEN (sizeof(HTTP_PREFIX) - 1)
#define	HTTP_DEF_PORT	80
#define	HTTPS_PREFIX	"https://"
#define	HTTPS_PREFIX_LEN (sizeof(HTTPS_PREFIX) - 1)
#define HTTPS_DEF_PORT	443

/* This is the entity reference table.  It is mandatory that & be the
 * first tag to be replaced and so when modifying this structure in future
 * make sure & always remains first.
 */
#define FIRST_ENTITY_REF '&'

typedef struct {
    char key;
    const char *value;
} entityRefTable;
const entityRefTable eRefTable[] = {
    {FIRST_ENTITY_REF, "&amp;"},
    {'\'', "&apos;"},
    {'"', "&quot;"},
    {'<', "&lt;"},
    {'>', "&gt;"}
};

bool areCharEqual(const char *c1, const char *c2, bool caseignorecmp) {
    bool result = false;
    char d1 = (char) (*c1);
    char d2 = (char) (*c2);

    if (caseignorecmp && isupper(d1)) {
        d1 = tolower(d1);
    }
    if (caseignorecmp && isupper(d2)) {
        d2 = tolower(d2);
    }
    if (d1 == d2) {
        result = true;
    }
    return result;
}

am_resource_match_t
Utils::match_patterns(const char *patbegin, const char *matchbegin,
        bool caseignorecmp, bool onelevelwildcard,
        char separator) {

    const char *p1 = patbegin;
    
    if (patbegin == NULL || matchbegin == NULL) {
        return AM_NO_MATCH;
    }
    if (*patbegin == '\0' && *matchbegin == '\0') {
        return AM_EXACT_MATCH;
    }
    while (*p1 != '\0' && *p1 != '*') {
        // check for one-level wild card pattern
        if ((*p1 == '-') && (*(p1 + 1) == '*') && (*(p1 + 2) == '-')) {
            onelevelwildcard = true;
            break;
        }
        p1++;
    }
    if (*p1 == '\0') {
        if (caseignorecmp) {
            if (strcasecmp(patbegin, matchbegin) == 0) {
                return AM_EXACT_MATCH;
            } else {
                return AM_NO_MATCH;
            }
        } else {
            if (strcmp(patbegin, matchbegin) == 0) {
                return AM_EXACT_MATCH;
            } else {
                return AM_NO_MATCH;
            }
        }
    } else {
        bool result;
        if (caseignorecmp) {
            result = (strncasecmp(patbegin,
                    matchbegin, p1 - patbegin) == 0);
        } else {
            result = (strncmp(patbegin, matchbegin, p1 - patbegin) == 0);
        }
        if (result == false) {
            return AM_NO_MATCH;
        }
    }
    if (onelevelwildcard) {
        // Incoming pattern has one level wild card in it (-*-) which
        // means the pattern -*- should match everything except /
        // eg : http://host:port/-*-/test.html should match with
        //      http://host:port/abc/test.html and should not match with
        //      http://host:port/a/bc/test.html
        // The algorithm uses a combination of patbegin and matchbegin 
        // and starts comparing from the beginning in the matchbegin string
        const char *s1 = matchbegin;
        int start_position = 0;
    
        // upto the start_position, pattern matching is complete
        while (start_position < (p1 - patbegin)) {
            s1++;
            start_position++;
        }
        while (*p1 != '\0') {
            char *p2 = NULL;
            char *s2 = NULL;
            int count = 0;
            int match_position = 0;
            int char_match = 0;
            int match_count = 0;
            int query_count = 0;

            while (*p1 == '-' || *p1 == '*') {
                p1++;
            }

            if ((*p1 == '\0') && (*s1 == '\0')) {
                // No more characters left to compare
                // -*- matches with the remaining characters
                return AM_EXACT_PATTERN_MATCH;
            }
            // Continue with the rest of the pattern
            std::string matchbegin_s(s1);
            if (*p1 == '\0') {
                // if the last character is a "/" , need to ignore the same
                std::size_t pos = matchbegin_s.find_last_of("/");
                if (pos == matchbegin_s.length() - 1) {
                    matchbegin_s.erase(matchbegin_s.length() - 1);
                }
                // Check whether the pattern has a "?", need to 
                // exclude "?" from the matchbegin
                for (std::size_t j = 0, query_index;
                        (query_index = matchbegin_s.find('?', j)) != string::npos;
                        j = query_index + 1) {
                    query_count++;
                }
                // the pattern is ending with -*-
                for (std::size_t i = 0, match_index;
                        (match_index = matchbegin_s.find('/', i)) != string::npos;
                        i = match_index + 1) {
                    match_count++;
                }
                if ((query_count == 1) || (match_count > 0)) {
                    return AM_NO_MATCH;
                } else {
                    return AM_EXACT_PATTERN_MATCH;
                }
            }
            size_t s1sz = strlen(s1);
            p2 = (char *) malloc(s1sz + 1);
            if (p2 != NULL) {
                memset(p2, '\0', s1sz + 1);
                // Find either the first pattern or end of the string
                // p2 will have a substring which is in between the patterns
                // if it exists or till the p1 is null or we are running out of space
                // (matchbegin is shorter than actual pattern)
                while ((*p1 != '-') && (*p1 != '*') && (*p1 != '\0') && (count < s1sz)) {
                    p2[count++] = *p1++;
                    char_match++;
                }

                std::string patternbegin(p2);
                s2 = (char *) malloc(s1sz + 1);
                if (s2 != NULL) {
                    memset(s2, '\0', s1sz + 1);
                    // Try to find the substring in the matchbegin, get the
                    // match position (position where the substring begins)
                    match_position = matchbegin_s.find(patternbegin, 0);
                    if (match_position > 0) {
                        // Substring exists, now starting from the index 0 
                        // upto the match_position, collect the characters
                        // to determine whether a "/" exists here (collected
                        // in s2). If it exists, it means no match else there
                        // is a match
                        for (std::size_t i = 0; i < match_position; i++) {
                            s2[i] = s1[i];
                        }
                        std::string s2_string(s2);
                        std::string::size_type loc = s2_string.find(separator, 0);
                        if (loc != std::string::npos) {
                            // Separator exists, return no match
                            free(p2);
                            free(s2);
                            return AM_NO_MATCH;
                        }
                    } else {
                        // substring does not exist, return no match
                        free(p2);
                        free(s2);
                        return AM_NO_MATCH;
                    }
                    // Pattern matching success so far
                    // Move pointer s1 upto the point where match 
                    // is complete
                    for (size_t i = 0; i < (match_position + char_match); i++) {
                        s1++;
                    }
                    free(p2);
                    free(s2);
                } else {
                    // Unable to allocate memory, return no match
                    free(p2);
                    return AM_NO_MATCH;
                }
            } else {
                // Unable to allocate memory, return no match
                return AM_NO_MATCH;
            }
        }
        return AM_EXACT_PATTERN_MATCH;
    } else {
        const char *s1 = matchbegin;
        const char *s2 = matchbegin;
        // ignoring mulitple '*'s
        while (*p1 == '*') {
            p1++;
        }
        /* pattern ends with *, so return true. */
        if (*p1 == '\0') {
            return AM_EXACT_PATTERN_MATCH;
        }
        // find the end of the string.
        // we need to go backwards.
        while (*(s1 + 1) != '\0') {
            s1++;
        }
        while (*s2 != '\0' && !areCharEqual(s2, p1, caseignorecmp)) {
            *s2++;
        }
        if (*s2 == '\0') {
            return AM_NO_MATCH;
        }
        for (; s1 >= s2; s1--) {
            if (areCharEqual(s1, p1, caseignorecmp)) {
                if (Utils::match_patterns(p1, s1,
                        caseignorecmp, onelevelwildcard,
                        separator) != AM_NO_MATCH) {
                    return AM_EXACT_PATTERN_MATCH;
                }
            }
        }
        return AM_NO_MATCH;
    }
}

time_t Utils::getTTL(const PRIVATE_NAMESPACE_NAME::XMLElement &element,
        unsigned long policy_clock_skew) {
    std::string ttl;
    time_t retVal = 0;
    unsigned long long val = 0;
    struct tm tm;
    time_t rawtime;
    time(&rawtime);

#ifdef _MSC_VER
    localtime_s(&tm, &rawtime);
#else
    localtime_r(&rawtime, &tm);
#endif

    if (element.getAttributeValue(TIME_TO_LIVE, ttl)) {
        if (sscanf(ttl.c_str(), "%llu", &val) != 1) {
            val = 0;
        }
    }

    if (val == 0 || val >= LONG_MAX) {
        /* <ActionDecision timeToLive="9223372036854775807"> inside <PolicyResponse> 
              is set to LONG_MAX meaning no expiration time. We use current time stamp + 1 year instead */
        tm.tm_year += 1;
    } else {
        time_t valtime = (time_t) val;
#ifdef _MSC_VER
        localtime_s(&tm, &valtime);
#else
        localtime_r(&valtime, &tm);
#endif
    }

    // Adjust any policy clock skew if set
    if (policy_clock_skew > 0) {
        tm.tm_sec += policy_clock_skew;
    }

    retVal = mktime(&tm);

    return retVal;
}

am_resource_match_t
compare_sub_pat(const char *r1, const char *r2,
        const am_resource_traits_t *traits, bool fwdcmp) {
    am_resource_match_t result = AM_EXACT_PATTERN_MATCH;

    if (r1 != NULL && r2 != NULL) {
        string r1_str(r1);
        string r2_str(r2);
        string::size_type r1_loc = r1_str.find("?", 0);
        string::size_type r2_loc = r2_str.find("?", 0);

        // First thing to check is whether r1 or r2 have a "?" in it.
        // This requires extra pattern matching. If neither one of them 
        // have it, we can ignore the below check and continue as before.
        if ((r1_loc != string::npos) || (r2_loc != string::npos)) {
            // We have a "?" in either the pattern or the match_string
            // Need to parse them and see whether they are matching or 
            // not
            if ((r1_loc != string::npos) && (r2_loc == string::npos) ||
                    (r1_loc == string::npos) && (r2_loc != string::npos)) {
                // Either one of them does not have a "?", it is a no match
                return AM_NO_MATCH;
            }
            // We have a "?" in both the pattern and the match string,
            // continue further. The algorithm is to do strchr the pattern and
            // the match string at "?" and send this each of the pattern 
            // tokens and match tokens to the match_patterns algorithm. Also
            // perform the similar call for the remainder of the pattern and
            // the match string
            const char *tmp_r1 = r1;
            const char *tmp_r2 = r2;
            char *pattern = NULL;
            char *match_str = NULL;
            size_t pattern_len = 0;
            size_t match_len = 0;

            pattern_len = strlen(r1) + 1;
            pattern = (char *) malloc(pattern_len);
            memset(pattern, '\0', pattern_len);

            tmp_r1 = strchr(r1, '?');
            if (tmp_r1 != NULL) {
                match_len = strlen(r2) + 1;
                match_str = (char *) malloc(match_len);
                memset(match_str, '\0', match_len);

                tmp_r2 = strchr(r2, '?');
                if (tmp_r2 != NULL) {
                    strncpy(pattern, r1, tmp_r1 - r1);
                    strncpy(match_str, r2, tmp_r2 - r2);

                    if (pattern != NULL && match_str != NULL) {
                        result = match_patterns(pattern, match_str,
                                B_TRUE == traits->ignore_case, false,
                                traits->separator);
                        if (result != AM_NO_MATCH) {
                            // success so far, continue for the remainder of 
                            // the pattern and the match string 
                            memset(pattern, '\0', pattern_len);
                            memset(match_str, '\0', match_len);

                            // Skip the '?' character
                            tmp_r1++;
                            tmp_r2++;

                            strcpy(pattern, tmp_r1);
                            strcpy(match_str, tmp_r2);

                            if (pattern != NULL && match_str != NULL) {
                                result = match_patterns(pattern, match_str,
                                        B_TRUE == traits->ignore_case, false,
                                        traits->separator);
                            }
                        }
                    }
                } else {
                    result = AM_NO_MATCH;
                }
            } else {
                result = AM_NO_MATCH;
            }
            if (pattern != NULL) free(pattern);
            if (match_str != NULL) free(match_str);
            return result;
        }
    }

    if ((result = match_patterns(r1, r2,
            B_TRUE == traits->ignore_case, false,
            traits->separator)) != AM_NO_MATCH)
        return result;

    std::string r_1(r1);
    size_t size = r_1.size() - 1;

    if (*(r1 + size) == '*')
        return AM_NO_MATCH;

    if (*(r1 + size) != traits->separator) {
        PUSH_BACK_CHAR(r_1, traits->separator);
    }

    r_1.append(STAR);

    if (match_patterns(r_1.c_str(), r2, B_TRUE == traits->ignore_case, false,
            traits->separator) != AM_NO_MATCH)
        return AM_SUB_RESOURCE_MATCH;

    return AM_NO_MATCH;
}

inline bool
rescmp(const char *r1, const char *r2, const am_resource_traits_t *traits) {
    if (B_TRUE == traits->ignore_case) {
        return (strcasecmp(r1, r2) == 0);
    } else {
        return (strcmp(r1, r2) == 0);
    }
    return false;
}

bool
ressubcmp(const char *r1, const char *r2, const am_resource_traits_t *traits,
        bool fwdcmp) {
    bool retVal = false;
    if (fwdcmp) {
        char *tmp;
#define BUF_LEN 1024
        char buf[BUF_LEN];
        std::size_t r1_size = strlen(r1);

        if (r1_size > BUF_LEN - 2) {
            string r_1(r1);

            // If separator is already there, then don't append it.
            if (r_1[r1_size - 1] != traits->separator) {
                PUSH_BACK_CHAR(r_1, traits->separator);
            }
            tmp = strdup(r_1.c_str());
        } else {
            tmp = (char *) &buf;
            memcpy(tmp, r1, r1_size + 1);
            if (tmp[r1_size - 1] != traits->separator) {
                tmp[r1_size] = traits->separator;
                tmp[r1_size + 1] = '\0';
            }
        }

        if (B_TRUE == traits->ignore_case) {
            if (strncasecmp(tmp, r2, r1_size) == 0)
                retVal = true;
        } else {
            if (strncmp(tmp, r2, r1_size) == 0)
                retVal = true;
        }
        if (tmp != (char *) &buf) {
            free(tmp);
            tmp = NULL;
        }
    } else {
        string r_1(r1);

        // if separator is already there, then don't prepend it.
        if (*r1 == traits->separator) {
            r_1.insert((std::size_t)0, (std::size_t)1, traits->separator);
        }

        size_t r1size = r_1.size();
        size_t r2size = strlen(r2);

        if (r1size > r2size) return false;

        const char *rr2 = r2 + (r2size - r1size - 1);
        if (B_TRUE == traits->ignore_case) {
            if (strncasecmp(r_1.c_str(), rr2, r1size) == 0) return true;
        } else {
            if (strncmp(r_1.c_str(), rr2, r1size) == 0) return true;
        }
    }
    return retVal;
}

am_resource_match_t
compare_pat(const char *r1, const char *r2,
        const am_resource_traits_t *traits, bool fwdcmp) {
    // Check for sub || exact match.
    am_resource_match_t resMatch = compare_sub_pat(r1, r2, traits, fwdcmp);
    if (resMatch != AM_NO_MATCH)
        return resMatch;

    // if the last char in r1 is a *, r2 in no way can be
    // a super resource.
    std::string r_1(r1);
    std::string r_2(r2);
    size_t size = r_1.size() - 1;
    size_t r2size = r_2.size() - 1;

    // If it is backward comparison, reverse the strings
    if (!fwdcmp) {
        string tmp;
        string::reverse_iterator iter;
        for (iter = r_1.rbegin();
                iter != r_1.rend(); iter++) {
            tmp += *iter;
        }

        r_1 = tmp;

        tmp.resize(0);
        for (iter = r_2.rbegin();
                iter != r_2.rend(); iter++) {
            tmp += *iter;
        }
        r_2 = tmp;
    }

    const char *x = r_1.c_str();
    if (x[size] == '*' || x[0] == '*')
        return AM_NO_MATCH;

    int r1idx = 0;
    while (r1idx < (int) size && x[r1idx] != '*') r1idx++;

    // match patterns needs to be executed for those strings
    // ending with ('*' and ct.separator) or (ch and ct.separator)
    char ch = r_2[r2size];

    if (ch != traits->separator) {
        PUSH_BACK_CHAR(r_2, traits->separator);
        r2size++;
    }

    for (int i = 0; i <= r1idx; i++) {
        if (x[i] == traits->separator) {
            string tmp = r_1.substr(0, i + 1);
            if (match_patterns(tmp.c_str(), r_2.c_str(),
                    B_TRUE == traits->ignore_case, false,
                    traits->separator) != AM_NO_MATCH)
                return AM_SUPER_RESOURCE_MATCH;
        }
    }

    return AM_NO_MATCH;
}

am_resource_match_t
compare_nopat(const char *r1, const char *r2,
        const am_resource_traits_t *traits, bool fwdcmp) {
    if (rescmp(r1, r2, traits)) {
        return AM_EXACT_MATCH;
    }

    if (ressubcmp(r1, r2, traits, fwdcmp)) {
        return AM_SUB_RESOURCE_MATCH;
    }

    if (ressubcmp(r2, r1, traits, fwdcmp)) {
        return AM_SUPER_RESOURCE_MATCH;
    }

    return AM_NO_MATCH;
}

am_resource_match_t
Utils::compare(const char *pat, const char *resName,
        const am_resource_traits_t *traits,
        bool fwdcmp, bool usePatterns) {

    const char *r1 = pat;

    if (pat == NULL || resName == NULL)
        return AM_NO_MATCH;

    if (*pat == '\0' && *resName == '\0')
        return AM_EXACT_MATCH;

    if (usePatterns)
        while (*r1 != '\0' && *r1 != '*') r1++;

    /* Has patterns? */
    if (usePatterns && *r1 != '\0') {
        /* Yes, has patterns */
        return compare_pat(pat, resName, traits, fwdcmp);
    } else {
        /* No patterns */
        return compare_nopat(pat, resName, traits, fwdcmp);
    }
}

void Utils::trim(std::string &str) {
    size_t t = 0;
    size_t size = str.size();
    if (size == 0)
        return;
    char c = 0;

    while (t < size && ((c = str.at(t)) != '\0') && (c == '\t' || c == ' ')) t++;

    if (t > 0) {
        str.erase(0, t);
    }

    size = str.size();
    if (size > 0) {
        t = size - 1;
        while (t >= 0 && ((c = str.at(t)) != 0) && (c == ' ' || c == '\t')) t--;

        str.erase(t + 1, size - t);
    }
    return;
}

/* Throws 
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
std::size_t
Utils::getNumber(const std::string &value) {
    long result;
    char *endPtr;

    errno = 0;
    result = std::strtol(value.c_str(), &endPtr, 0);
    if (0 != errno || *endPtr) {
        if (ERANGE == errno) {
            throw std::range_error(value + " has too large");
        } else {
            throw std::domain_error(value + " is not a valid long integer.");
        }
    }
    return result;
}

#define NUM_BUF_LEN 50

std::size_t
Utils::getNumLength(long num) {
    char dataStr[NUM_BUF_LEN];
    memset(dataStr, 0, NUM_BUF_LEN);
    snprintf(dataStr, NUM_BUF_LEN, "%ld", num);
    return strlen(dataStr);
}

std::string
Utils::toString(std::size_t num) {
    char dataStr[NUM_BUF_LEN];
    memset(dataStr, 0, NUM_BUF_LEN);
    snprintf(dataStr, NUM_BUF_LEN, "%ld", num);
    return std::string(dataStr);
}

void
Utils::expandEntityRefs(std::string &str) {
    std::size_t numElements = sizeof (eRefTable) / sizeof (entityRefTable);
    for (std::size_t i = 0; i < numElements; i++) {
        std::size_t pos = 0;
        pos = str.find(eRefTable[i].key, pos);
        while (pos != std::string::npos) {
            str.replace(pos, 1, eRefTable[i].value);
            pos = str.find(eRefTable[i].key, pos + 1);
        }
    }
}

/**
 * Given a number this function returns a boolean
 * indicating whether the number is a prime number.
 * It uses the SQRT(n) method of primality test.
 */
bool
Utils::is_prime(unsigned number) {
    unsigned loop = 0;
    unsigned max_count = 0;
    bool prime_found = false;

    if ((number % 2) != 0) {

        prime_found = true;

        max_count = (unsigned int) std::sqrt((double) number) + 1;

        for (loop = 3; loop <= max_count; loop += 2) {
            if ((number % loop) == 0) {
                prime_found = false;
                break;
            }
        }
    }

    return prime_found;
}

/**
 * Given a number, this function returns the next
 * prime number occuring in the positive number scale.
 */
unsigned int
Utils::get_prime(unsigned int number) {
    if (is_prime(number)) {
        return (number);
    }

    if ((number % 2) == 0) {
        ++number;
    }

    while (is_prime(number) == false) {
        number += 2;
    }

    return number;
}

void Utils::parseIPAddresses(const std::string &property,
        std::set<std::string> &ipAddrSet) {
    size_t space = 0, curPos = 0;
    std::string iplist(property);
    size_t size = iplist.size();

    while (space < size) {
        space = iplist.find(' ', curPos);
        std::string ipAddr;
        if (space == std::string::npos) {
            ipAddr = iplist.substr(curPos, size - curPos);
            space = size;
        } else {
            ipAddr = iplist.substr(curPos, space - curPos);
        }
        curPos = space + 1;
        if (ipAddr.size() == 0)
            continue;
        ipAddrSet.insert(ipAddr);
    }
}

/* Throws std::exception's from string methods */

/* Throws std::exception's from string methods */
void Utils::parseCookieDomains(const std::string &property,
        std::set<std::string> &CDListSet) {
    size_t space = 0, curPos = 0;
    std::string cdlist(property);
    size_t size = cdlist.size();

    while (space < size) {
        space = cdlist.find(' ', curPos);
        std::string cookiedomain;
        if (space == std::string::npos) {
            cookiedomain = cdlist.substr(curPos, size - curPos);
            space = size;
        } else {
            cookiedomain = cdlist.substr(curPos, space - curPos);
        }
        curPos = space + 1;
        if (cookiedomain.size() == 0)
            continue;
        CDListSet.insert(cookiedomain);
    }
}

void Utils::cleanup_cookie_info(cookie_info_t *cookie_data) {
    if (cookie_data != NULL) {
        if (cookie_data->name != NULL) {
            free(cookie_data->name);
            cookie_data->name = NULL;
        }
        if (cookie_data->value != NULL) {
            free(cookie_data->value);
            cookie_data->value = NULL;
        }
        if (cookie_data->domain != NULL) {
            free(cookie_data->domain);
            cookie_data->domain = NULL;
        }
        if (cookie_data->max_age != NULL) {
            if (cookie_data->max_age[0] != '0') {

                free(cookie_data->max_age);
                cookie_data->max_age = NULL;
            }
        }
        if (cookie_data->path != NULL) {
            free(cookie_data->path);
            cookie_data->path = NULL;
        }
    }
}

void Utils::cleanup_url_info_list(url_info_list_t *url_list) {
    unsigned int i;

    if (url_list->list != NULL) {
        if (url_list->size > 0) {

            for (i = 0; i < url_list->size; i++) {

                if (url_list->list[i].url != NULL) {
                    free(url_list->list[i].url);
                    url_list->list[i].url = NULL;
                }
                if (url_list->list[i].host != NULL) {
                    free(url_list->list[i].host);
                    url_list->list[i].host = NULL;
                }
            }

            free(url_list->list);

            url_list->list = NULL;
        }
    }

    url_list->size = 0;
}

/*
 * Parse a cookie string represenation of the form
 * name[=value][;Domain=value][;Max-Age=value][;Path=value]
 *
 * Throws std::exception's from string methods.
 */
am_status_t Utils::parseCookie(std::string cookie,
        cookie_info_t *cookie_data) {
    char *holder = NULL;
    char* temp_str = const_cast<char*> (cookie.c_str());

    if (cookie_data == NULL || temp_str == NULL) {
        return AM_INVALID_ARGUMENT;
    }

    cleanup_cookie_info(cookie_data);

    //Process name=value

    char *token = NULL;
    std::string tempstr;
    token = strtok_r(temp_str, ";", &holder);
    if (token == NULL) {
        return AM_INVALID_ARGUMENT;
    }
    tempstr = token;
    Utils::trim(tempstr);
    token = const_cast<char*> (tempstr.c_str());
    std::size_t len = strlen(token);
    char *loc = strchr(token, '=');
    if (loc == NULL) {
        cookie_data->name = (char *) malloc(len + 1);
        if (cookie_data->name == NULL) {
            return AM_NO_MEMORY;
        }
        strcpy(cookie_data->name, token);
    } else {
        len = len - strlen(loc);
        cookie_data->name = (char *) malloc(len + 1);
        if (cookie_data->name == NULL) {
            return AM_NO_MEMORY;
        }
        strncpy(cookie_data->name, token, len);
        cookie_data->name[len] = '\0';
        cookie_data->value = (char *) malloc(strlen(loc));
        if (cookie_data->name == NULL) {
            cleanup_cookie_info(cookie_data);
            return AM_NO_MEMORY;
        }
        strcpy(cookie_data->value, loc + 1);
    }

    token = NULL;
    token = strtok_r(NULL, ";", &holder);

    while (token != NULL) {
        tempstr = token;
        Utils::trim(tempstr);
        token = const_cast<char *> (tempstr.c_str());
        len = strlen(token);
        loc = NULL;
        loc = strstr(token, "Domain=");
        if (loc != NULL) {
            loc = loc + strlen("Domain=");
            len = strlen(loc);
            cookie_data->domain = (char *) malloc(len + 1);
            if (cookie_data->domain == NULL) {
                cleanup_cookie_info(cookie_data);
                return AM_NO_MEMORY;
            }
            /*use domain value exactly as set in a configuration (do not remove leading '.')*/
            strcpy(cookie_data->domain, loc);
            cookie_data->domain[len] = '\0';
        } else {
            loc = strstr(token, "Max-Age=");
            if (loc != NULL) {
                loc = loc + strlen("Max-Age=");
                len = strlen(loc);
                cookie_data->max_age = (char *) malloc(len + 1);
                if (cookie_data->max_age == NULL) {
                    cleanup_cookie_info(cookie_data);
                    return AM_NO_MEMORY;
                }
                strcpy(cookie_data->max_age, loc);
            } else {
                loc = strstr(token, "Path=");
                if (loc != NULL) {
                    loc = loc + strlen("Path=");
                    len = strlen(loc);
                    cookie_data->path = (char *) malloc(len + 1);
                    if (cookie_data->path == NULL) {
                        cleanup_cookie_info(cookie_data);
                        return AM_NO_MEMORY;
                    }
                    strcpy(cookie_data->path, loc);
                } else {
                }
            }
        }
        token = strtok_r(NULL, ";", &holder);
    }

    return AM_SUCCESS;
}

am_status_t Utils::parse_url(const char *url_str,
        size_t len,
        url_info_t *entry_ptr,
        am_bool_t validateURLs) {
    const char *url = url_str;
    size_t url_len = len;
    std::string normalizedURL;
    am_status_t status = AM_SUCCESS;
    size_t host_offset = 0;
    const char *protocol;

    if (NULL != url) {
        /**
         * FIX_NEXT_RELEASE
         * This is a hack that I've put here.  The next release,
         * we should be doing away with anything to do with URLs that's
         * not in URL class.
         *
         * For now, compare wether it is a URL we are talking about or
         * some regular expression like *.gif.
         * If it is a URL, then, normalize it.
         */
        if (strncasecmp(url, HTTP_PREFIX, HTTP_PREFIX_LEN) == 0 ||
                strncasecmp(url, HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0) {
            try {
                URL urlObject(url, len);
                urlObject.getURLString(normalizedURL);
                url = normalizedURL.c_str();
                url_len = normalizedURL.size();
                protocol = urlObject.getProtocolString();
            } catch (InternalException &iex) {
                status = AM_INVALID_ARGUMENT;
            } catch (std::exception &ex) {
                status = AM_INVALID_ARGUMENT;
            } catch (...) {
                status = AM_INVALID_ARGUMENT;
            }
        }

        if (validateURLs == AM_TRUE && status == AM_SUCCESS) {
            if (url_len >= MIN_URL_LEN) {
                if (strncasecmp(url, HTTPS_PREFIX, HTTPS_PREFIX_LEN) == 0) {
                    entry_ptr->port = HTTPS_DEF_PORT;
                    host_offset = HTTPS_PREFIX_LEN;
                } else if (strncasecmp(url, HTTP_PREFIX,
                        HTTP_PREFIX_LEN) == 0) {
                    entry_ptr->port = HTTP_DEF_PORT;
                    host_offset = HTTP_PREFIX_LEN;
                } else {
                    status = AM_INVALID_ARGUMENT;
                }
            } else {
                status = AM_INVALID_ARGUMENT;
            }
        }
    } else {
        status = AM_INVALID_ARGUMENT;
    }

    if (AM_SUCCESS == status) {
        entry_ptr->url = (char *) malloc(url_len + 1);
        entry_ptr->host = (char *) malloc(url_len - host_offset + 1);
        entry_ptr->protocol = const_cast<char *> (protocol);
        if (NULL != entry_ptr->url && NULL != entry_ptr->host) {
            char *temp_ptr;

            memcpy(entry_ptr->url, url, url_len);
            entry_ptr->url[url_len] = '\0';
            entry_ptr->url_len = url_len;
            if (strchr(entry_ptr->url, '?') != NULL) {
                entry_ptr->has_parameters = AM_TRUE;
            } else {
                entry_ptr->has_parameters = AM_FALSE;
            }

            if (am_policy_resource_has_patterns(entry_ptr->url) == B_TRUE) {
                entry_ptr->has_patterns = AM_TRUE;
            } else {
                entry_ptr->has_patterns = AM_FALSE;
            }

            url_len -= host_offset;
            url += host_offset;
            if (url_len > 0) {
                memcpy(entry_ptr->host, url, url_len);
            }
            entry_ptr->host[url_len] = '\0';

            temp_ptr = strchr(entry_ptr->host, '/');
            if (temp_ptr != NULL) {
                *temp_ptr = '\0';
            }

            temp_ptr = strchr(entry_ptr->host, ':');
            if (NULL != temp_ptr) {
                *(temp_ptr++) = '\0';

                entry_ptr->port = 0;
                while (isdigit(*temp_ptr)) {
                    entry_ptr->port = (entry_ptr->port * 10) + *temp_ptr - '0';
                    temp_ptr += 1;
                }
            }
        } else {
            if (NULL == entry_ptr->url) {
            } else {
                free(entry_ptr->url);
                entry_ptr->url = NULL;
            }
            if (NULL == entry_ptr->host) {
            } else {
                free(entry_ptr->host);
                entry_ptr->host = NULL;
            }
            status = AM_NO_MEMORY;
        }
    }

    return status;
}

am_status_t Utils::parse_url_list(const char *url_list_str,
        char sep,
        url_info_list_t *list_ptr,
        am_bool_t validateURLs) {
    am_status_t status = AM_SUCCESS;
    int num_elements = 0;
    cleanup_url_info_list(list_ptr);
    if (url_list_str != NULL) {
        const char *temp_ptr = url_list_str;

        // removing leading spaces and separators.
        while (*temp_ptr == ' ' || *temp_ptr == sep) {
            temp_ptr += 1;
        }

        if (*temp_ptr != '\0') {
            url_list_str = temp_ptr;

            /* Calculate num elems */
            do {
                num_elements += 1;

                while (*temp_ptr != '\0' && *temp_ptr != ' ' &&
                        *temp_ptr != sep) {
                    temp_ptr += 1;
                }
                while (*temp_ptr == ' ' || *temp_ptr == sep) {
                    temp_ptr += 1;
                }
            } while (*temp_ptr != '\0');

            list_ptr->list = (url_info_t *) calloc(num_elements,
                    sizeof (list_ptr->list[0]));
            if (NULL != list_ptr->list) {
                temp_ptr = url_list_str;
                do {
                    size_t len = 0;

                    while (temp_ptr[len] != '\0' && temp_ptr[len] != ' ' &&
                            temp_ptr[len] != sep) {
                        len += 1;
                    }

                    status = parse_url(temp_ptr, len,
                            &list_ptr->list[list_ptr->size],
                            validateURLs);

                    if (AM_SUCCESS == status) {
                        temp_ptr += len;
                        list_ptr->size += 1;
                    } else {
                        break;
                    }

                    while (*temp_ptr == ' ' || *temp_ptr == sep) {
                        temp_ptr += 1;
                    }
                } while (*temp_ptr != '\0');
            } else {
                status = AM_NO_MEMORY;
            }
        }
    }

    return status;
}

void Utils::cleanup_cookie_info_list(
        cookie_info_list_t *cookie_list) {
    unsigned int i;

    if (cookie_list != NULL) {
        for (i = 0; i < cookie_list->size; i++) {
            cleanup_cookie_info(&(cookie_list->list[i]));
        }
        free(cookie_list->list);
    }
    cookie_list->list = NULL;
    cookie_list->size = 0;
}

am_status_t Utils::parseCookieList(const char *property,
        char sep,
        cookie_info_list_t *cookie_list) {
    size_t num_cookies = 0;

    if (property == NULL || cookie_list == NULL) {
        return AM_INVALID_ARGUMENT;
    }

    cleanup_cookie_info_list(cookie_list);

    const char *temp_ptr = property;

    // removing leading spaces and separators.
    while (*temp_ptr == ' ' || *temp_ptr == sep) {
        temp_ptr += 1;
    }

    if (*temp_ptr == '\0') {
        cookie_list->size = 0;
        cookie_list->list = NULL;
        return AM_SUCCESS;
    }

    /* Calculate num elems */
    do {
        num_cookies += 1;

        while (*temp_ptr != '\0' && *temp_ptr != ' ' &&
                *temp_ptr != sep) {
            temp_ptr += 1;
        }
        while (*temp_ptr == ' ' || *temp_ptr == sep) {
            temp_ptr += 1;
        }
    } while (*temp_ptr != '\0');

    cookie_list->list = (cookie_info_t *) calloc(num_cookies,
            sizeof (cookie_info_t));
    if (cookie_list->list == NULL) {
        return AM_NO_MEMORY;
    }

    memset(cookie_list->list, 0, num_cookies * sizeof (cookie_info_t));

    size_t space = 0, curPos = 0;
    unsigned int idx = 0;
    std::string cookies(property);
    Utils::trim(cookies);
    size_t size = cookies.size();

    while (space < size) {
        space = cookies.find(',', curPos);
        std::string cookie;
        if (space == std::string::npos) {
            cookie = cookies.substr(curPos, size - curPos);
            space = size;
        } else {
            cookie = cookies.substr(curPos, space - curPos);
        }
        curPos = space + 1;
        Utils::trim(cookie);
        if (cookie.size() == 0)
            continue;

        if (AM_SUCCESS == parseCookie(cookie, &cookie_list->list[idx])) {
            idx++;
        } else {
        }

    }
    cookie_list->size = idx;
    return AM_SUCCESS;
}

am_status_t Utils::initCookieResetList(cookie_info_list_t *cookie_list, const char* cookie_reset_default_domain) {
    const char *DEFAULT_PATH = "/";

    if (cookie_list == NULL || cookie_list->list == NULL) {
        return AM_INVALID_ARGUMENT;
    }

    for (size_t i = 0; i < cookie_list->size; ++i) {

        cookie_info_t *cookie_data = &cookie_list->list[i];
        if (cookie_data != NULL) {

            if (cookie_data->domain == NULL) {
                std::size_t domain_len = strlen(cookie_reset_default_domain);
                cookie_data->domain = (char *) malloc(domain_len + 1);
                if (cookie_data->domain == NULL) {
                    cleanup_cookie_info(cookie_data);
                    return AM_NO_MEMORY;
                }
                strcpy(cookie_data->domain,
                        cookie_reset_default_domain);
            }

            if (cookie_data->max_age != NULL) {
                if (cookie_data->max_age[0] == '\0') {
                    free(cookie_data->max_age);
                    // max_age cannot be an empty string for with older browsers
                    // netscape 4.79, IE 5.5, mozilla < 1.4.
                    // If specified as an empty string in the config,
                    // don't set it at all in the cookie header.
                    cookie_data->max_age = NULL;
                }
            } else {
                // by default, delete cookie on reset.
                cookie_data->max_age = const_cast<char*> ("0");
            }

            if (cookie_data->path != NULL) {
                if (cookie_data->path[0] == '\0') {
                    free(cookie_data->path);
                    // path must be '/' for older browsers IE,
                    // netscape 4.79 to work
                    cookie_data->path = strdup(DEFAULT_PATH);
                }
            } else {
                cookie_data->path = strdup(DEFAULT_PATH);
            }
        }
    }
    return AM_SUCCESS;
}

am_status_t Utils::initCookieResetList(
        cookie_info_list_t *cookie_list,
        std::size_t domain_len,
        const char* cookieResetDefaultDomain) {
    const char *DEFAULT_PATH = "/";

    if (cookie_list == NULL || cookie_list->list == NULL) {
        return AM_INVALID_ARGUMENT;
    }

    for (size_t i = 0; i < cookie_list->size; ++i) {

        cookie_info_t *cookie_data = &cookie_list->list[i];
        if (cookie_data != NULL) {

            if (cookie_data->domain == NULL) {

                cookie_data->domain = (char *) malloc(domain_len + 1);
                if (cookie_data->domain == NULL) {
                    cleanup_cookie_info(cookie_data);
                    return AM_NO_MEMORY;
                }
                strcpy(cookie_data->domain,
                        cookieResetDefaultDomain);
            }

            if (cookie_data->max_age != NULL) {
                if (cookie_data->max_age[0] == '\0') {
                    free(cookie_data->max_age);
                    // max_age cannot be an empty string for with older browsers
                    // netscape 4.79, IE 5.5, mozilla < 1.4.
                    // If specified as an empty string in the config,
                    // don't set it at all in the cookie header.
                    cookie_data->max_age = NULL;
                }
            } else {
                // by default, delete cookie on reset.
                cookie_data->max_age = const_cast<char*> ("0");
            }

            if (cookie_data->path != NULL) {
                if (cookie_data->path[0] == '\0') {
                    free(cookie_data->path);
                    // path must be '/' for older browsers IE,
                    // netscape 4.79 to work
                    cookie_data->path = strdup(DEFAULT_PATH);
                }
            } else {
                cookie_data->path = strdup(DEFAULT_PATH);
            }
        }
    }
    return AM_SUCCESS;
}

#define EMIT(x) do{if (o<n){*q++ = (x);} o++; }while(0)

size_t Utils::format_int(char *q, size_t n, uintmax_t val, enum flags flags,
        int base, int width, int prec) {
    char *qq;
    size_t o = 0, oo;
    static const char lcdigits[] = "0123456789abcdef";
    static const char ucdigits[] = "0123456789ABCDEF";
    const char *digits;
    uintmax_t tmpval;
    int minus = 0;
    int nd = 0, nchars;
    int ta, tb;

    digits = (flags & FL_UPPER) ? ucdigits : lcdigits;

    if (flags & FL_SIGNED && (intmax_t) val < 0) {
        minus = 1;
        val = (uintmax_t) (-(intmax_t) val);
    }

    tmpval = val;
    while (tmpval) {
        tmpval /= base;
        nd++;
    }

    if (flags & FL_HASH && base == 8) {
        if (prec < nd + 1)
            prec = nd + 1;
    }

    if (nd < prec) {
        nd = prec;
    } else if (val == 0) {
        nd = 1;
    }

    if (flags & FL_TICK) {
        ta = (base == 16) ? 4 : 3;
    } else {
        ta = nd;
    }

    nd += (nd - 1) / ta;

    nchars = nd;

    if (minus || (flags & (FL_PLUS | FL_SPACE)))
        nchars++;
    if ((flags & FL_HASH) && base == 16) {
        nchars += 2;
    }

    if (!(flags & (FL_MINUS | FL_ZERO)) && width > nchars) {
        while (width > nchars) {
            EMIT(' ');
            width--;
        }
    }

    if (minus)
        EMIT('-');
    else if (flags & FL_PLUS)
        EMIT('+');
    else if (flags & FL_SPACE)
        EMIT(' ');

    if ((flags & FL_HASH) && base == 16) {
        EMIT('0');
        EMIT((flags & FL_UPPER) ? 'X' : 'x');
    }

    if ((flags & (FL_MINUS | FL_ZERO)) == FL_ZERO && width > nd) {
        while (width > nchars) {
            EMIT('0');
            width--;
        }
    }

    q += nd;
    o += nd;
    qq = q;
    oo = o;

    tb = ta;
    while (nd > 0) {
        if (!tb--) {
            qq--;
            oo--;
            nd--;
            if (oo < n)
                *qq = '_';
            tb = ta - 1;
        }
        qq--;
        oo--;
        nd--;
        if (oo < n)
            *qq = digits[val % base];
        val /= base;
    }

    while ((flags & FL_MINUS) && width > nchars) {
        EMIT(' ');
        width--;
    }

    return o;
}

#define MIN_RANK	Utils::rank_char
#define MAX_RANK	Utils::rank_longlong

#define INTMAX_RANK	Utils::rank_longlong
#define SIZE_T_RANK	Utils::rank_long
#define PTRDIFF_T_RANK	Utils::rank_long

int Utils::am_vsnprintf(char *buffer, size_t n, const char *format, va_list ap) {
    const char *p = format;
    char ch;
    char *q = buffer;
    size_t o = 0;
    uintmax_t val = 0;
    int rank = Utils::rank_int;
    int width = 0;
    int prec = -1;
    int base;
    size_t sz;
    enum flags flags = static_cast<enum flags> (0);

    enum {
        st_normal,
        st_flags,
        st_width,
        st_prec,
        st_modifiers
    } state = st_normal;
    const char *sarg;
    char carg;
    int slen;

    while ((ch = *p++)) {
        switch (state) {
            case st_normal:
                if (ch == '%') {
                    state = st_flags;
                    flags = static_cast<enum flags> (0);
                    rank = rank_int;
                    width = 0;
                    prec = -1;
                } else {
                    EMIT(ch);
                }
                break;

            case st_flags:
                switch (ch) {
                    case '-':
                        flags = flags | FL_MINUS;
                        break;
                    case '+':
                        flags = flags | FL_PLUS;
                        break;
                    case '\'':
                        flags = flags | FL_TICK;
                        break;
                    case ' ':
                        flags = flags | FL_SPACE;
                        break;
                    case '#':
                        flags = flags | FL_HASH;
                        break;
                    case '0':
                        flags = flags | FL_ZERO;
                        break;
                    default:
                        state = st_width;
                        p--;
                        break;
                }
                break;

            case st_width:
                if (ch >= '0' && ch <= '9') {
                    width = width * 10 + (ch - '0');
                } else if (ch == '*') {
                    width = va_arg(ap, int);
                    if (width < 0) {
                        width = -width;
                        flags = flags | FL_MINUS;
                    }
                } else if (ch == '.') {
                    prec = 0;
                    state = st_prec;
                } else {
                    state = st_modifiers;
                    p--;
                }
                break;

            case st_prec:
                if (ch >= '0' && ch <= '9') {
                    prec = prec * 10 + (ch - '0');
                } else if (ch == '*') {
                    prec = va_arg(ap, int);
                    if (prec < 0)
                        prec = -1;
                } else {
                    state = st_modifiers;
                    p--;
                }
                break;

            case st_modifiers:
                switch (ch) {
                    case 'h':
                        rank--;
                        break;
                    case 'l':
                        rank++;
                        break;
                    case 'j':
                        rank = INTMAX_RANK;
                        break;
                    case 'z':
                        rank = SIZE_T_RANK;
                        break;
                    case 't':
                        rank = PTRDIFF_T_RANK;
                        break;
                    case 'L':
                    case 'q':
                        rank += 2;
                        break;
                    default:
                        state = st_normal;
                        if (rank < MIN_RANK)
                            rank = MIN_RANK;
                        else if (rank > MAX_RANK)
                            rank = MAX_RANK;

                        switch (ch) {
                            case 'P':
                                flags = flags | FL_UPPER;
                            case 'p':
                                base = 16;
                                prec = (CHAR_BIT * sizeof (void *) + 3) / 4;
                                flags = flags | FL_HASH;
                                val = (uintmax_t) (uintptr_t) va_arg(ap, void *);
                                goto is_integer;

                            case 'd':
                            case 'i':
                                base = 10;
                                flags = flags | FL_SIGNED;
                                switch (rank) {
                                    case rank_char:
                                        val =
                                                (uintmax_t) (intmax_t) (signed char) va_arg(ap,
                                                signed
                                                int);
                                        break;
                                    case rank_short:
                                        val =
                                                (uintmax_t) (intmax_t) (signed short) va_arg(ap,
                                                signed
                                                int);
                                        break;
                                    case rank_int:
                                        val = (uintmax_t) (intmax_t) va_arg(ap, signed int);
                                        break;
                                    case rank_long:
                                        val = (uintmax_t) (intmax_t) va_arg(ap, signed long);
                                        break;
                                    case rank_longlong:
                                        val =
                                                (uintmax_t) (intmax_t) va_arg(ap, signed long long);
                                        break;
                                }
                                goto is_integer;
                            case 'o':
                                base = 8;
                                goto is_unsigned;
                            case 'u':
                                base = 10;
                                goto is_unsigned;
                            case 'X':
                                flags = flags | FL_UPPER;
                            case 'x':
                                base = 16;
                                goto is_unsigned;

is_unsigned:
                                switch (rank) {
                                    case rank_char:
                                        val =
                                                (uintmax_t) (unsigned char) va_arg(ap, unsigned int);
                                        break;
                                    case rank_short:
                                        val =
                                                (uintmax_t) (unsigned short) va_arg(ap,
                                                unsigned int);
                                        break;
                                    case rank_int:
                                        val = (uintmax_t) va_arg(ap, unsigned int);
                                        break;
                                    case rank_long:
                                        val = (uintmax_t) va_arg(ap, unsigned long);
                                        break;
                                    case rank_longlong:
                                        val = (uintmax_t) va_arg(ap, unsigned long long);
                                        break;
                                }


is_integer:
                                sz = format_int(q, (o < n) ? n - o : 0, val, flags, base,
                                        width, prec);
                                q += sz;
                                o += sz;
                                break;

                            case 'c':
                                carg = (char) va_arg(ap, int);
                                sarg = &carg;
                                slen = 1;
                                goto is_string;
                            case 's':
                                sarg = va_arg(ap, const char *);
                                sarg = sarg ? sarg : "(null)";
                                slen = (int) strlen(sarg);
                                goto is_string;

is_string:{
                                    char sch;
                                    int i;

                                    if (prec != -1 && slen > prec)
                                        slen = prec;

                                    if (width > slen && !(flags & FL_MINUS)) {
                                        char pad = (flags & FL_ZERO) ? '0' : ' ';
                                        while (width > slen) {
                                            EMIT(pad);
                                            width--;
                                        }
                                    }
                                    for (i = slen; i; i--) {
                                        sch = *sarg++;
                                        EMIT(sch);
                                    }
                                    if (width > slen && (flags & FL_MINUS)) {
                                        while (width > slen) {
                                            EMIT(' ');
                                            width--;
                                        }
                                    }
                                }
                                break;

                            case 'n':
                            {
                                switch (rank) {
                                    case rank_char:
                                        *va_arg(ap, signed char *) = (signed char) o;
                                        break;
                                    case rank_short:
                                        *va_arg(ap, signed short *) = (signed short) o;
                                        break;
                                    case rank_int:
                                        *va_arg(ap, signed int *) = (signed int) o;
                                        break;
                                    case rank_long:
                                        *va_arg(ap, signed long *) = (signed long) o;
                                        break;
                                    case rank_longlong:
                                        *va_arg(ap, signed long long *) = (signed long long) o;
                                        break;
                                }
                            }
                                break;

                            default:
                                EMIT(ch);
                                break;
                        }
                }
        }
    }

    if (o < n)
        *q = '\0';
    else if (n > 0)
        buffer[n - 1] = '\0';

    return (int) o;
}

int Utils::am_vasprintf(char **strp, const char *fmt, va_list ap) {
    va_list aq;
    int ret = 0;
#ifndef va_copy
#define va_copy(dst, src) ((void)((dst) = (src)))
#endif
    va_copy(aq, ap);
    ret = am_vsnprintf(NULL, 0, fmt, aq);
    va_end(aq);
    if ((*strp = (char *) malloc(ret + 1)) == NULL)
        return (-1);
    ret = am_vsnprintf(*strp, ret + 1, fmt, ap);
    return (ret);
}

int Utils::am_printf(char **buffer, const char *fmt, ...) {
    int size;
    char *tmp = NULL;
    va_list ap;
    va_start(ap, fmt);
    tmp = *buffer;
    size = am_vasprintf(buffer, fmt, ap);
    free(tmp);
    va_end(ap);
    return size;
}


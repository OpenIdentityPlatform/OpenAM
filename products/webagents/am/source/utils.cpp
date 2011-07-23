/* The contents of this file are subject to the terms
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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 */ 
#include <stdexcept>
#include <limits.h>
#include <cmath>
#include <cerrno>
#include <string>
#include <prprf.h>
#include "properties.h"
#include "utils.h"

using std::string;
using namespace Utils;
USING_PRIVATE_NAMESPACE;

const char *POLICY_SERVICE="PolicyService";
const char *POLICY_RESPONSE="PolicyResponse";
const char *SESSION_NOTIFICATION="SessionNotification";
const char *ADD_LISTENER_RESPONSE="AddPolicyListenerResponse";
const char *REMOVE_LISTENER_RESPONSE="RemovePolicyListenerResponse";
const char *SESSION="Session";
const char *POLICY_CHANGE_NOTIFICATION="PolicyChangeNotification";
const char *SUBJECT_CHANGE_NOTIFICATION="SubjectChangeNotification";
const char *RESOURCE_RESULT="ResourceResult";
const char *POLICY_DECISION="PolicyDecision";
const char *RESPONSE_ATTRIBUTES="ResponseAttributes";
const char *ACTION_DECISION="ActionDecision";
const char *ATTRIBUTE_VALUE_PAIR = "AttributeValuePair";
const char *RESPONSE_DECISIONS="ResponseDecisions";
const char *RESOURCE_NAME="ResourceName";
const char *SERVICE_NAME="serviceName";
const char *SESSION_ID_ATTRIBUTE="sid";
const char *SESSION_STATE_ATTRIBUTE="state";
const char *SESSION_STATE_VALUE_VALID="valid";
const char *SESSION_NOTIF_TIME="Time";
const char *SESSION_NOTIF_TYPE="Type";
const char *ATTRIBUTE_NAME="name";
const char *NAME = "name";
const char *ATTRIBUTE="Attribute";
const char *VALUE="Value";
const char *TIME_TO_LIVE="timeToLive";
const char *ADVICES="Advices";
const char *NOTIFICATION_SET="NotificationSet";
const char *NOTIFICATION="Notification";
const char *NOTIFICATION_TYPE="type";
const char *NOTIF_TYPE_MODIFIED="modified";
const char *NOTIF_TYPE_ADDED="added";
const char *NOTIF_TYPE_DELETED="deleted";
const char *RESPONSE_SET="ResponseSet";
const char *RESPONSE="Response";
const char *NOTIFICATION_ID="notid";
const char *REQUEST_ID="reqid";
const char *VERSION="vers";
const char *VERSION_STR="version";
const char *REQUEST_ID_STR="requestId";
const char *NOTIFICATION_SET_VERSION="1.0";
const char *REQUEST_SET_VERSION="1.0";
const char *REVISION_STR="revisionNumber";
const char *ADVICE_LIST_RESPONSE="AdvicesHandleableByAMResponse";
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
#define PRE_SCALE_MAX	(MAX_64_BIT_INT / PR_USEC_PER_MSEC)

/* This is the entity reference table.  It is mandatory that & be the
 * first tag to be replaced and so when modifying this structure in future
 * make sure & always remains first.
 */
#define FIRST_ENTITY_REF '&'
typedef struct {
    char key;
    const char *value;
} entityRefTable;
const entityRefTable eRefTable[] = { {FIRST_ENTITY_REF, "&amp;"},
				     {'\'', "&apos;"},
				     {'"', "&quot;"},
				     {'<', "&lt;"},
				     {'>', "&gt;"}
				   };

bool areCharEqual(const char *c1, const char *c2, bool caseignorecmp)
{
    bool result = false;
    char d1 = (char)(*c1);
    char d2 = (char)(*c2);
    
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
		      bool caseignorecmp) {

    const char *p1 = patbegin;
    
    if(patbegin == NULL || matchbegin == NULL) {
        return AM_NO_MATCH;
    }
    if(*patbegin == '\0' && *matchbegin == '\0') {
        return AM_EXACT_MATCH;
    }
    while(*p1 != '\0' && *p1 !='*') {
        p1++;
    }    
    if(*p1 == '\0') {
        if(caseignorecmp) {
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
        if(caseignorecmp) {
            result = (strncasecmp(patbegin, matchbegin, p1 - patbegin) == 0);
        } else {
            result = (strncmp(patbegin, matchbegin, p1 - patbegin) == 0);
        }
        if(result == false) {
            return AM_NO_MATCH;
        }
    }
    
    // ignoring mulitple '*'s
    while(*p1 == '*') {
        p1++;
    }
    // pattern ends with *, so return true.
    if(*p1 == '\0') {
        return AM_EXACT_PATTERN_MATCH; 
    }
    const char *s1 = matchbegin;
    const char *s2 = matchbegin;
    // Find the end of the string. We need to go backwards.
    while(*(s1+1) != '\0') {
        s1++;
    }    
    while(*s2 != '\0' && !areCharEqual(s2, p1, caseignorecmp)) {
        *s2++;
    }
    if(*s2 == '\0') {
        return AM_NO_MATCH;
    }
    for(; s1 >= s2; s1--) {
        if (areCharEqual(s1, p1, caseignorecmp)) {
            if(Utils::match_patterns(p1, s1, caseignorecmp) != AM_NO_MATCH) {
                return AM_EXACT_PATTERN_MATCH;
            }
        }
    }
    return AM_NO_MATCH;
}

PRTime
Utils::getTTL(const PRIVATE_NAMESPACE_NAME::XMLElement &element, 
              unsigned long policy_clock_skew) {
    std::string ttl;
    PRTime retVal = 0;
    PRExplodedTime explodedExpTime;
    PRExplodedTime tmpExplodedExpTime;

    if (element.getAttributeValue(TIME_TO_LIVE, ttl)) {
	       
	if (PR_sscanf(ttl.c_str(), "%lld", &retVal) == 1) {
	    // Scale the millisecond time to microseconds, but make
	    // sure that we don't exceed the maximum.
	    if (retVal < PRE_SCALE_MAX) {
		retVal *= PR_USEC_PER_MSEC;
	    } else {
		retVal = MAX_64_BIT_INT;
	    }
	}
    }
    // Adjust any policy clock skew if set
    if (policy_clock_skew > 0) {
        PR_ExplodeTime(retVal, PR_LocalTimeParameters, &explodedExpTime);   
        explodedExpTime.tm_sec += policy_clock_skew;
        PR_NormalizeTime(&explodedExpTime, PR_LocalTimeParameters);
        retVal = PR_ImplodeTime(&explodedExpTime);
    }    
    return retVal;
}


am_resource_match_t
compare_sub_pat(const char *r1, const char *r2,
		const am_resource_traits_t *traits) {
    am_resource_match_t result;
    if((result = match_patterns(r1, r2,
				B_TRUE==traits->ignore_case)) != AM_NO_MATCH)
	return result;

    string r_1(r1);
#if defined(_AMD64_)
    size_t size = r_1.size() - 1;
#else
    int size = r_1.size() - 1;
#endif
    if(*(r1 + size) == '*')
	return AM_NO_MATCH;

    if(*(r1 + size) != traits->separator) {
	PUSH_BACK_CHAR(r_1,traits->separator);
    }

    r_1.append(STAR);

    if(match_patterns(r_1.c_str(), r2, B_TRUE==traits->ignore_case) != AM_NO_MATCH)
	return AM_SUB_RESOURCE_MATCH;

    return AM_NO_MATCH;
}

inline bool
rescmp(const char *r1, const char *r2, const am_resource_traits_t *traits) {
    if(B_TRUE==traits->ignore_case) {
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
    if(fwdcmp) {
	char *tmp;
#define BUF_LEN 1024
	char buf[BUF_LEN];
	std::size_t r1_size = strlen(r1);
	
	if(r1_size > BUF_LEN - 2) {
	    string r_1(r1);
	    
	    // If separator is already there, then don't append it.
	    if(r_1[r1_size - 1] != traits->separator) {
		PUSH_BACK_CHAR(r_1, traits->separator);
	    }
	    tmp = strdup(r_1.c_str());
	} else {
	    tmp = (char *)&buf;
	    memcpy(tmp, r1, r1_size + 1);
	    if(tmp[r1_size - 1] != traits->separator) {
		tmp[r1_size] = traits->separator;
		tmp[r1_size + 1] = '\0';
	    }
	}
	
	if(B_TRUE==traits->ignore_case) {
	    if(strncasecmp(tmp, r2, r1_size) == 0)
		retVal = true;
	} else {
	    if (strncmp(tmp, r2, r1_size) == 0)
		retVal = true;
	}
	if(tmp != (char *)&buf) {
	    free(tmp);
	    tmp = NULL;
	}
    } else {
	string r_1(r1);

	// if separator is already there, then don't prepend it.
	if(*r1 == traits->separator) {
	    r_1.insert((std::size_t)0, (std::size_t)1, traits->separator);
	}

#if defined(_AMD64_)
	size_t r1size = r_1.size();
	size_t r2size = strlen(r2);
#else
	int r1size = r_1.size();
	int r2size = strlen(r2);
#endif
	if(r1size > r2size) return false;

	const char *rr2 = r2 + (r2size - r1size - 1);
	if(B_TRUE==traits->ignore_case) {
	    if(strncasecmp(r_1.c_str(), rr2, r1size) == 0) return true;
	} else {
	    if(strncmp(r_1.c_str(), rr2, r1size) == 0) return true;
	}
    }
    return retVal;
}



am_resource_match_t
compare_pat(const char *r1, const char *r2,
	    const am_resource_traits_t *traits, bool fwdcmp) {
    // Check for sub || exact match.
    am_resource_match_t resMatch = compare_sub_pat(r1, r2, traits);
    if(resMatch != AM_NO_MATCH)
	return resMatch;

    // if the last char in r1 is a *, r2 in no way can be
    // a super resource.
    string r_1(r1);
    string r_2(r2);
#if defined(_AMD64_)
    size_t size = r_1.size() - 1;
    size_t r2size = r_2.size() - 1;
#else
    int size = r_1.size() - 1;
    int r2size = r_2.size() - 1;
#endif

    // If it is backward comparison, reverse the strings
    if(!fwdcmp) {
	string tmp;
	string::reverse_iterator iter;
	for(iter = r_1.rbegin();
	    iter != r_1.rend(); iter++) {
	    tmp += *iter;
	}

	r_1 = tmp;

	tmp.resize(0);
	for(iter = r_2.rbegin();
	    iter != r_2.rend(); iter++) {
	    tmp += *iter;
	}
	r_2 = tmp;
    }

    const char *x = r_1.c_str();
    if(x[size] == '*' || x[0] == '*')
	return AM_NO_MATCH;

    int r1idx = 0;
    while(r1idx < size && x[r1idx] != '*') r1idx++;

    // match patterns needs to be executed for those strings
    // ending with ('*' and ct.separator) or (ch and ct.separator)
    char ch = r_2[r2size];

    if(ch != traits->separator) {
	PUSH_BACK_CHAR(r_2, traits->separator);
	r2size++;
    }

    for(int i = 0; i <= r1idx; i++) {
	if(x[i] == traits->separator) {
	    string tmp = r_1.substr(0, i+1);
	    if(match_patterns(tmp.c_str(), r_2.c_str(),
			      B_TRUE==traits->ignore_case) != AM_NO_MATCH)
		return AM_SUPER_RESOURCE_MATCH;
	}
    }

    return AM_NO_MATCH;
}

am_resource_match_t
compare_nopat(const char *r1, const char *r2,
	      const am_resource_traits_t *traits, bool fwdcmp) {
    if(rescmp(r1, r2, traits)) {
	return AM_EXACT_MATCH;
    }

    if(ressubcmp(r1, r2, traits, fwdcmp)) {
	return AM_SUB_RESOURCE_MATCH;
    }

    if(ressubcmp(r2, r1, traits, fwdcmp)) {
	return AM_SUPER_RESOURCE_MATCH;
    }

    return AM_NO_MATCH;
}

am_resource_match_t
Utils::compare(const char *pat, const char *resName,
	       const am_resource_traits_t *traits,
	       bool fwdcmp, bool usePatterns) {

    const char *r1=pat;

    if(pat == NULL || resName == NULL)
	return AM_NO_MATCH;

    if(*pat == '\0' && *resName == '\0')
	return AM_EXACT_MATCH;

    if(usePatterns)
	while(*r1 != '\0' && *r1 != '*') r1++;

    /* Has patterns? */
    if(usePatterns && *r1 != '\0') {
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
    if(size == 0)
	return;
    char c = 0;

    while(t < size && ((c = str.at(t)) != '\0') && (c == '\t' || c == ' ')) t++;

    if(t > 0) {
	str.erase(0, t);
    }

    size = str.size();
    if(size > 0) {
	t = size - 1;
	while(t >= 0 && ((c = str.at(t)) != 0) && (c == ' ' || c == '\t')) t--;

	str.erase(t + 1, size - t);
    }
    return;
}


/* Throws 
 *	std::range_error if value is too small or large.
 *	std::domain_error if valid is invalid otherwise.
 */
std::size_t
Utils::getNumber(const std::string &value) 
{
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

    PR_snprintf(dataStr, NUM_BUF_LEN, "%ld", num);
    return strlen(dataStr);
}

std::string
Utils::toString(std::size_t num) {
    char dataStr[NUM_BUF_LEN];
    memset(dataStr, 0, NUM_BUF_LEN);
    PR_snprintf(dataStr, NUM_BUF_LEN, "%ld", num);
    return std::string(dataStr);
}


void
Utils::expandEntityRefs(std::string &str) {
    std::size_t numElements = sizeof(eRefTable)/sizeof(entityRefTable);
    for(std::size_t i = 0; i < numElements; i++) {
	std::size_t pos = 0;
	pos = str.find(eRefTable[i].key, pos);
	while(pos != std::string::npos) {
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

	max_count = (unsigned int)std::sqrt((double)number) + 1;

	for (loop = 3; loop <= max_count; loop += 2) {
	    if ((number % loop ) == 0) {
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
Utils::get_prime(unsigned int number)
{
    if (is_prime(number)) {
        return(number);
    }

    if ( (number % 2) == 0 ) {
        ++number;
    }

    while (is_prime(number) == false) {
        number += 2;
    }

    return number;
}

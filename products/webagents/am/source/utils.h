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
#ifndef __UTILS_H__
#define __UTILS_H__

#include <ctype.h>
#include <string>
#include <algorithm>
#include <prtime.h>
#include <am_policy.h>
#include "internal_macros.h"
#include "xml_element.h"

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
				   bool /*ignorecase*/);

PRTime getTTL(const PRIVATE_NAMESPACE_NAME::XMLElement &, unsigned long);

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
    CmpFunc(bool ignore_case=false):icase(ignore_case) {}
    CmpFunc(const CmpFunc &cmpFunc):icase(cmpFunc.icase) {}

    static bool cmp_ignore_case(char c1, char c2) {
	return toupper(c1) < toupper(c2);
    }
 
    bool  operator()(const std::string &s1, const std::string &s2) const {
	if(icase == false) {
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

}

#endif

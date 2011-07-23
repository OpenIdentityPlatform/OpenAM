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
#ifndef __ACTION_DECISION_H__
#define __ACTION_DECISION_H__
#include <list>
#include <string>
#include <prtime.h>

#include "ref_cnt_ptr.h"
#include "key_value_map.h"
#include "utils.h"

BEGIN_PRIVATE_NAMESPACE

class ActionDecision:public RefCntObj {
 private:
    std::string actionName;
    PRTime timeToLive;
    std::list<std::string> values;
    KeyValueMap advices;
    ActionDecision();

 public:
    typedef std::list<std::string>::const_iterator const_action_values_iterator;

    inline const std::list<std::string> &getActionValues() const {
	return values;
    }

    inline const KeyValueMap &getAdvices() const {
	return advices;
    }

    ActionDecision(std::string &, PRTime);
    virtual ~ActionDecision();

    bool addActionValues(XMLElement &);

    inline PRTime getTimeStamp() const { return timeToLive; }

    inline void addAdvice(XMLElement &avp) {
	advices.insert(avp);
    }

};

typedef RefCntPtr<ActionDecision> ActDecRefCntPtr;

END_PRIVATE_NAMESPACE
#endif

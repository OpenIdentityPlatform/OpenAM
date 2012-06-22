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
 * $Id: policy_decision.h,v 1.4 2008/06/25 08:14:34 qcheng Exp $
 *
 */ 
#ifndef __POLICY_DECISION_H__
#define __POLICY_DECISION_H__

#include "ref_cnt_ptr.h"
#include "internal_exception.h"
#include "policy_resource.h"
#include "key_value_map.h"

BEGIN_PRIVATE_NAMESPACE
class PolicyDecision;
typedef RefCntPtr<PolicyDecision> PDRefCntPtr;
class Properties;
class KeyValueMap;
class ActionDecision;

class PolicyDecision:public RefCntObj {
 private:
    KVMRefCntPtr env;
    typedef std::map<std::string, ActionDecision *> ActionDecisionMap;
    ActionDecisionMap actionDecisions;
    KeyValueMap advices;
    KeyValueMap responses;
    KeyValueMap responseAttributes;
    bool refetch;
    ResourceName resourceName;

    PolicyDecision(const ResourceName &resName, KVMRefCntPtr ev)
	: RefCntObj(), env(ev), actionDecisions(), advices(), refetch(true),
	  resourceName(resName)
    {
    }

 public:
    static const PDRefCntPtr INVALID_POLICY_DECISION;
    virtual ~PolicyDecision();

    static PDRefCntPtr
	/* Throws InternalException upon error */
	construct_policy_decision(const ResourceName &,
				  XMLElement &,
				  const KVMRefCntPtr);

    PolicyDecision(const ResourceName &resName) :RefCntObj(), refetch(true),
						 resourceName(resName) {
    }

    inline bool isStale() {
	return refetch;
    }

    bool isStale(const std::string &actionName);

    inline void markStale() {
	refetch = true;
	return;
    }

    void addActionDecision(std::string& actionName,
			   ActionDecision* actionDecision) {
	actionDecisions[actionName] = actionDecision;
    }

    bool matchEnvParams(const KeyValueMap &);

    ActionDecision *getActionDecision(const std::string& actionName) {
	return actionDecisions[actionName];
    }


    inline const KeyValueMap &getAttributeResponses() {
	return responses;
    }

    inline const KeyValueMap &getResponseAttributes() {
	return responseAttributes;
    }

    inline const ResourceName &getName() const {
	return resourceName;
    }
};



END_PRIVATE_NAMESPACE
#endif

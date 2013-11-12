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
 * $Id: policy_entry.cpp,v 1.5 2008/06/25 08:14:34 qcheng Exp $
 *
 */ 

#include "policy_resource.h"
#include "policy_entry.h"
#include "policy_engine.h"
#include "log.h"

using namespace PRIVATE_NAMESPACE_NAME;
using std::string;

PolicyEntry::PolicyEntry(const SSOToken &ssoTok,
		   const KeyValueMap &env,
		   const Properties &attr_map,
		   am_resource_traits_t rTraits): RefCntObj(),
						  ssoToken(ssoTok),
						  rsrcTraits(rTraits),
						  lock(), cookies(),
						  dirty(false),
						  map(env) {
}

PolicyEntry::~PolicyEntry() {
    ScopeLock myLock(lock);
    std::list<Tree *>::iterator iter = forest.begin();
    while (iter != forest.end()) {
	Tree *tree = *iter;
	iter = forest.erase(iter);
	delete(tree);
    }
}

Tree *
PolicyEntry::getTree(const ResourceName &resName, bool usePatterns) const {
    Tree *retVal = NULL;
    std::list<Tree *>::const_iterator iter = forest.begin();
    for(;iter != forest.end(); iter++) {
	retVal = *iter;
	if(retVal != NULL && (retVal->isInTree(resName, usePatterns))) {
	    break;
	}
    }

    if(iter == forest.end())
	retVal = NULL;

    return retVal;
}

const PDRefCntPtr
PolicyEntry::getPolicyDecision(const string &resName) const {
    Tree *tree = static_cast<Tree *>(NULL);

    ScopeLock myLock(lock);
    // find the resource result.
    tree = getTree(resName, true);
    if(tree == NULL) {
	return PolicyDecision::INVALID_POLICY_DECISION;
    }

    return tree->dfsearch(resName);
}

void
PolicyEntry::getAllPolicyDecisions(const string &resName,
				std::vector<PDRefCntPtr> &results) const {
    Tree *tree = NULL;
    ScopeLock myLock(lock);

    std::list<Tree *>::const_iterator iter = forest.begin();
    for(; iter != forest.end(); iter++) {
	tree = *iter;
	if(tree != NULL) {
	    tree->dfsearch_all(resName, results);
	}
    }
    return;
}

bool
PolicyEntry::create_policy_tree(XMLElement& resultNode, KVMRefCntPtr env) {
    if(!resultNode.isNamed(RESOURCE_RESULT))
	return false;

    Tree *newTree = new Tree(resultNode, rsrcTraits, env);
    forest.push_back(newTree);
    return true;
}


bool
PolicyEntry::append_policy_to_tree(Tree &tree, XMLElement &elem,
				KVMRefCntPtr env) {
    string resName;
    XMLElement subElem;

    if(!elem.isValid() || !elem.isNamed(RESOURCE_RESULT) ||
       !elem.getAttributeValue(NAME, resName))
	return false;

    XMLElement policyDec;
    if(elem.getSubElement(POLICY_DECISION, policyDec)) {
	// TO BE DONE
	PDRefCntPtr pDec =
	    PolicyDecision::construct_policy_decision(resName,
						      policyDec,
						      env);
	tree.insert(pDec);
    }

    if(elem.getSubElement(RESOURCE_RESULT, subElem)) {
	for(; subElem.isValid(); subElem.nextSibling(RESOURCE_RESULT)) {
	    append_policy_to_tree(tree, subElem, env);
	}
    }
    return true;
}

bool
PolicyEntry::add_policy(XMLElement &policyNode, KVMRefCntPtr env) {
    string name;

    if(!policyNode.isNamed(RESOURCE_RESULT))
	return false;

    ScopeLock myLock(lock);
    if(policyNode.getAttributeValue(NAME,name)) {
	Tree *tree = getTree(name, false);

	if(tree) {
	    append_policy_to_tree(*tree, policyNode, env);
	} else {
	    // Check if the resource name is a parent of any
	    // of the existing trees.  If yes, then we need
	    // to add this resource as the root node and attach
	    // the existing tree as subordinates.
	    Tree *super = NULL;
	    std::list<Tree *>::iterator iter = forest.begin();
	    for(; iter != forest.end(); iter++) {
		super = *iter;

		// Is a parent of tree.
		if(super->isParentOfTree(name)) {
		    append_policy_to_tree(*super, policyNode, env);
		}
	    }

	    // Log that this policy result does not belong to
	    // any existing tree.
	    create_policy_tree(policyNode, env);
	}
    }
    return true;
}

bool
PolicyEntry::removePolicy(const ResourceName &resName) {
    ScopeLock myLock(lock);
    bool retVal = false;
    Tree *tree = getTree(resName, false);
    if(tree != NULL) {      
      if (tree->remove(resName)) {
         return true;
      } else {
         // tree->remove() has failed because resName is the root of the tree.
         // In this case need to remove the tree from the forest to delete
         // the policy
         std::list<Tree *>::iterator iter = forest.begin();
         while (iter != forest.end()) {
             tree = *iter;
             if(tree != NULL && (tree->isInTree(resName, false))) {
                 iter = forest.erase(iter);
                 delete(tree);
                 retVal=true;
                 continue;
             }
             iter++;
         }
      }
    }
    return retVal;
}

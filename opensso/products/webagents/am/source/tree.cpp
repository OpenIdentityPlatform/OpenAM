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
 * $Id: tree.cpp,v 1.7 2010/03/10 06:47:41 kiran_gonipati Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

#include "tree.h"
#include "am_policy.h"

using std::string;
using std::list;
using namespace PRIVATE_NAMESPACE_NAME;

const NodeRefPtr Node::NULL_NODE(NULL);

///////////////
// Node
///////////////
Node::Node(NodeRefPtr parent, PDRefCntPtr &root):pElement(root),
					   pParent(parent) {
    assert(root != NULL);
}

Node::~Node() {
	destroyNode();
}

void Node::destroyNode() {
    ScopeLock myLock(nodeLock);
    if(hasSubNodes()) {
	std::list<NodeRefPtr>::iterator iter;
	for(iter = nodes.begin(); iter != nodes.end();) {
	    NodeRefPtr subNode = *iter;
	    iter = nodes.erase(iter);
	}
    }
    pParent = NULL;
}


///////////////
// Tree
///////////////
Tree::Tree(PDRefCntPtr &root,
	   am_resource_traits_t rTraits): rsrcTraits(rTraits),
            rootNode(new Node(Node::NULL_NODE, root)) {
}

Tree::~Tree() {
    // fix for memory leak issue 2883
    rootNode->removeAllChildren();
}

/* Throws std::invalid_argument if any argument is invalid */
Tree::Tree(XMLElement &elem,
	   am_resource_traits_t rTraits,
	   KVMRefCntPtr env)
    : rsrcTraits(rTraits),
      rootNode(Node::NULL_NODE)
{
    if(elem.isNamed(RESOURCE_RESULT)) {
	try {
	    addSubNodes(Node::NULL_NODE, elem, env);
	} catch(const std::invalid_argument &ia) {
	    rootNode = NULL;
	    throw;
	}
    } else {
      throw std::invalid_argument("Tree::Tree(XMLElement &, "
				  "am_resource_traits_t, KVMRefCntPtr)"
				  " has an invalid XML as input.");
    }
}

/* Throws std::invalid_argument if any argument is invalid */
void
Tree::addSubNodes(NodeRefPtr parent, XMLElement &thisNode,
		  KVMRefCntPtr env)
{
    string resName;
    if(thisNode.isNamed(RESOURCE_RESULT) &&
       thisNode.getAttributeValue(NAME, resName)) {
	XMLElement decn;
	NodeRefPtr newNode;
	if(thisNode.getSubElement(POLICY_DECISION, decn)) {
	    PDRefCntPtr decision =
		PolicyDecision::construct_policy_decision(resName, decn,
							  env);

	    newNode = new Node(parent, decision);

	    // If the parent is NULL, then this is the root Node.
	    if(parent != NULL) {
		parent->addNode(newNode);
	    } else {
		rootNode = newNode;
	    }

	} else {
	    throw std::invalid_argument("Tree:addSubNodes(...) Cannot find "
					"Policy Decision node under resource "
					"result.");
	}

	XMLElement subElems;
	if(thisNode.getSubElement(RESOURCE_RESULT, subElems)) {
	    for(;subElems.isValid(); subElems.nextSibling(RESOURCE_RESULT)) {
		addSubNodes(newNode, subElems, env);
	    }
	}
    } else {
      throw std::invalid_argument("Tree::addSubNodes(...) has an invalid XML as input.");
    }
}

bool
Tree::insert(PDRefCntPtr &elem) {
    if(rootNode == NULL) {
	// This is a spanking new tree.
	// let's add the node as the root node.
	rootNode = new Node(Node::NULL_NODE, elem);
    }

    SearchResult result = find(rootNode, elem->getName(), false);
    switch(result.first) {
    case AM_EXACT_MATCH:
	    result.second->replace(elem);
	    break;
    case AM_NO_MATCH:
	return false;
    case AM_SUB_RESOURCE_MATCH:
	return insertBelow(result.second, elem);
	break;
    case AM_SUPER_RESOURCE_MATCH:
	NodeRefPtr newRoot(new Node(Node::NULL_NODE, elem));
	rootNode->setParent(newRoot);
	rootNode = newRoot;
	return true;
	break;
    }
    return true;
}

bool
Tree::insertBelow(NodeRefPtr parent, PDRefCntPtr &elem) {
    std::list<NodeRefPtr>::iterator iter;
    NodeRefPtr newNode(new Node(parent, elem));

    // add the subordinate nodes of newNode under
    // new node.  In the next loop, we iterate nodes
    // under new node and remove them from parent.
    // Reason: if we remove nodes under parent in this
    // loop, iterator becomes invalid.
    ScopeLock myLock(treeLock);
    for(iter = parent->begin(); iter != parent->end(); iter++) {
	NodeRefPtr node = *iter;
	PDRefCntPtr data = node->getPolicyDecision();
	switch(compare(data->getName(), elem->getName(), false)) {
	case AM_EXACT_MATCH:
	case AM_SUB_RESOURCE_MATCH:
	    return false;

	case AM_NO_MATCH:
	    break;
	case AM_SUPER_RESOURCE_MATCH:
	    newNode->addNode(node);
	    break;
	}
    }

    for(iter = newNode->begin(); iter != newNode->end(); iter++) {
	NodeRefPtr node = *iter;
	parent->remove(node);
    }

    parent->addNode(newNode);

    return true;
}

void
Tree::showNodes() const {
    PDRefCntPtr rootNodePolicyDecision = rootNode->getPolicyDecision();
    ResourceName rootNodeResouceName = rootNodePolicyDecision->getName();
    std::string rootNodeName = rootNodeResouceName.getString();
    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Tree::showNodes rootNode: %s", rootNodeName.c_str());
    if (rootNode->hasSubNodes()) {
        std::list<NodeRefPtr>::iterator iter;
        for (iter = rootNode->begin(); iter != rootNode->end(); iter++) {
            NodeRefPtr node = *iter;
            PDRefCntPtr data = node->getPolicyDecision();
            ResourceName res = data->getName();
            std::string name = res.getString();
            Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Tree::showNodes node: %s", name.c_str());
        }
    }
}

/**
 * This function removes a node from a tree.
 * The function will fail if root node is being removed.
 */
bool
Tree::remove(const ResourceName &elem, bool recursive) {
    SearchResult result = find(rootNode, elem, false);


    if(result.first == AM_EXACT_MATCH) {
	NodeRefPtr delNode = result.second;
	// First get the parent.
	if(recursive) {
	    if(delNode->hasSubNodes()) {
		delNode->removeAllChildren();
	    }
	} else {
	    NodeRefPtr parent = delNode->getParent();

	    // Transfer all children of the node to be deleted
	    // to the parent.
	    if(parent != NULL) {
		parent->removeChildOnly(delNode);
	    } else {
		// if this is the root node we are trying to remove
		// the operation must fail.
		return false;
	    }
	}
    }
    rootNode->removeAllChildren();
    return true;
}

void
Tree::dfsearch_all(const ResourceName &resName,
        std::vector<PDRefCntPtr> &results,
        bool usePatterns) const {
    if (Log::isLevelEnabled(Log::ALL_MODULES, Log::LOG_MAX_DEBUG)) {
        showNodes();
    }
    search_recursive(resName, rootNode, results, usePatterns);
    return;
}

void
Tree::search_recursive(const ResourceName &resName,
		       const NodeRefPtr node,
		       std::vector<PDRefCntPtr> &results,
		       bool usePatterns) const {

    am_resource_match_t compRes;
    compRes = compare(node->getPolicyDecision()->getName(),
		      resName,
		      usePatterns);

    std::list<NodeRefPtr>::const_iterator iter;
    switch(compRes) {
    case AM_EXACT_MATCH:
	results.push_back(node->getPolicyDecision());
	break;
    case AM_EXACT_PATTERN_MATCH:
	results.push_back(node->getPolicyDecision());
	// fall through to the following
    case AM_SUB_RESOURCE_MATCH:
    case AM_NO_MATCH: 
	for(iter = node->begin(); iter != node->end(); iter++) {
	    search_recursive(resName, *iter, results, usePatterns);
	}
	break;
    default:
	break;
    }
    return;
}

PDRefCntPtr
Tree::dfsearch(const ResourceName& res, bool usePatterns) {
    SearchResult result = find(rootNode, res, usePatterns);
    if(result.first == AM_EXACT_MATCH || result.first == AM_EXACT_PATTERN_MATCH) {
	return result.second->getPolicyDecision();
    }
    return PolicyDecision::INVALID_POLICY_DECISION;
}

SearchResult
Tree::find(NodeRefPtr startNode, const ResourceName &policyRes,
	   bool usePatterns = true) {
    am_resource_match_t compRes;
    std::list<NodeRefPtr>::const_iterator iter;

    const ResourceName& resName = startNode->getPolicyDecision()->getName();
    compRes = compare(resName, policyRes, usePatterns);

    if(compRes != AM_SUB_RESOURCE_MATCH && compRes != AM_EXACT_PATTERN_MATCH) {
	if(compRes == AM_EXACT_MATCH)
	    return SearchResult(AM_EXACT_MATCH, startNode);
	return SearchResult(compRes, Node::NULL_NODE);
    }

    for(iter = startNode->begin(); iter != startNode->end(); ++iter) {
	NodeRefPtr node = *iter;
	const PDRefCntPtr &element = node->getPolicyDecision();
	switch((compare(element->getName(), policyRes, usePatterns))) {
	case AM_EXACT_MATCH:
	    return SearchResult(AM_EXACT_MATCH, node);
	    break;
	case AM_NO_MATCH:
	    continue;
	    break;
	case AM_SUPER_RESOURCE_MATCH:
	    return SearchResult(AM_SUB_RESOURCE_MATCH, startNode);
	case AM_SUB_RESOURCE_MATCH:
	case AM_EXACT_PATTERN_MATCH:
	    {
		SearchResult result = find(node, policyRes, usePatterns);
		switch(result.first) {
		    // In case of the node below returns SUB_RESOURCE_MATCH,
		    // EXACT_PATTERN_MATCH or EXACT_MATCH,
		    // we just need to pass on the result upwards.
		case AM_SUB_RESOURCE_MATCH:
		case AM_EXACT_PATTERN_MATCH:
		case AM_EXACT_MATCH:
		    // Bingo! got a match.
		    return result;
		case AM_SUPER_RESOURCE_MATCH:
		case AM_NO_MATCH:
		    return SearchResult(AM_SUB_RESOURCE_MATCH, startNode);
		}
	    }
	    break;
	}
    }

    // This is the level we have to add the node.
    if(iter == startNode->end()) {
	if(compRes == AM_SUB_RESOURCE_MATCH || compRes == AM_EXACT_PATTERN_MATCH)
	    return SearchResult(compRes, startNode);
	else
	    return SearchResult(compRes, Node::NULL_NODE);
    }
    return SearchResult(AM_NO_MATCH, Node::NULL_NODE);
}

bool
Tree::isInTree(const ResourceName &resName, bool usePatterns) {

    const PDRefCntPtr &policy = rootNode->getPolicyDecision();
    am_resource_match_t rslt = compare(policy->getName(), resName,
				       usePatterns);
    if(rslt == AM_SUB_RESOURCE_MATCH ||rslt == AM_EXACT_MATCH ||
       rslt == AM_EXACT_PATTERN_MATCH) {
	return true;
    }

    return false;
}

bool
Tree::isParentOfTree(const ResourceName &resName) {
    PDRefCntPtr policy = rootNode->getPolicyDecision();
    am_resource_match_t rslt = compare(policy->getName(), resName, true);
    return (rslt == AM_SUPER_RESOURCE_MATCH);
}

/**
 * r1 is the policy resource name
 * r2 requested resource name
 * usePatterns - When constructing the tree, the usePatterns is set to false
 *               otherwise its set to true.
 */
am_resource_match_t
Tree::compare(const string &r1, const string &r2, bool usePatterns) const {
    return (*rsrcTraits.cmp_func_ptr)(&rsrcTraits,
				    r1.c_str(), r2.c_str(),
				    usePatterns?B_TRUE:B_FALSE);
}

am_resource_match_t
Tree::compare(const ResourceName &r1,
	      const ResourceName &r2, bool usePatterns) const {
    return compare(r1.getString(), r2.getString(), usePatterns);
}

bool
Tree::markStale(const ResourceName & resName, bool recursive) {
    SearchResult result = find(rootNode, resName, false);

    if(result.first == AM_EXACT_MATCH) {
	NodeRefPtr delNode = result.second;
	delNode->markNodeStale(recursive);
	return true;
    }
    return false;
}

void
Tree::outdatePolicyDecisions(const std::string &resName) {
    SearchResult result = find(rootNode, resName, false);
    NodeRefPtr delNode = result.second;
    if(result.first == AM_EXACT_MATCH) {
	NodeRefPtr parent = delNode->getParent();
	delNode->markNodeStale(true);
	if(parent != NULL) {
	    parent->removeChildOnly(delNode);
	}
    }
    return;
}

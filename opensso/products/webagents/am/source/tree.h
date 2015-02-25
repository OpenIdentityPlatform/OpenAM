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
 * $Id: tree.h,v 1.4 2008/06/25 08:14:40 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

#ifndef __TREE_H__
#define __TREE_H__
#include <cassert>
#include <stdexcept>
#include <list>
#include <iterator>
#include <utility>
#include "key_value_map.h"
#include <am_policy.h>
#include "policy_resource.h"
#include "policy_decision.h"
#include "internal_macros.h"
#include "scope_lock.h"

BEGIN_PRIVATE_NAMESPACE

class Node;
typedef RefCntPtr<Node> NodeRefPtr;

class Node:public RefCntObj {
 private:
    PDRefCntPtr pElement;
    Mutex nodeLock;
    std::list<NodeRefPtr> nodes;
    NodeRefPtr pParent;
    friend class Tree;
    inline void setParent(NodeRefPtr parentNode) {
	pParent = parentNode;
    }

 public:
    static const NodeRefPtr NULL_NODE;
    Node(NodeRefPtr parent, PDRefCntPtr&);
    ~Node();
	void destroyNode();

    inline bool hasSubNodes() const {
	return (!nodes.empty());
    }

    inline std::list<NodeRefPtr>::const_iterator begin() const {
	return nodes.begin();
    }

    inline std::list<NodeRefPtr>::iterator begin() {
	return nodes.begin();
    }

    inline std::list<NodeRefPtr>::iterator erase(std::list<NodeRefPtr>::iterator position) {
	return nodes.erase(position);
    }

    inline std::list<NodeRefPtr>::const_iterator end() const {
	return nodes.end();
    }

    inline std::list<NodeRefPtr>::iterator end() {
	return nodes.end();
    }

    inline void replace(PDRefCntPtr &newValue) {
	pElement = newValue;
	return;
    }

    inline NodeRefPtr getParent() const {
	return pParent;
    }

    inline const PDRefCntPtr &getPolicyDecision() const {
	return pElement;
    }

    inline void addNode(NodeRefPtr childNode) {
	assert(childNode != NULL);

	childNode->pParent = this;
	nodes.insert(nodes.begin(), childNode);
	return;
    }

    /**
     * Does not delete the childNode itself.
     */
    inline void replaceParent(NodeRefPtr parent, NodeRefPtr childNode) {
	assert(childNode != NULL && parent != NULL);
	parent->nodes.remove(childNode);
	addNode(childNode);
	return;
    }

    inline void remove(NodeRefPtr childNode) {
	assert(childNode != NULL);
	nodes.remove(childNode);
	return;
    }

    /*
     * preserves the children of the node being removed.
     */
    void removeChildOnly(NodeRefPtr childNode) {
	assert(childNode != NULL);
	remove(childNode);

	std::list<NodeRefPtr>::iterator iter;
	for(iter = childNode->begin(); iter != childNode->end(); ++iter) {
	    addNode(*iter);
	}
	return;
    }

    void removeAllChildren() {
	if(hasSubNodes()) {
	    std::list<NodeRefPtr>::iterator iter;
	    for(iter = begin(); iter != end();++iter) {
		NodeRefPtr delNode = *iter;
		delNode->removeAllChildren();
	    }
	    nodes.clear();
	}
	return;
    }

    void markNodeStale(bool recursive) {
	if(!pElement->isStale()) {
	    PDRefCntPtr pNewElement(new PolicyDecision(pElement->getName()));
	    pElement = pNewElement;
	}

	if(recursive && hasSubNodes()) {
	    std::list<NodeRefPtr>::iterator iter;
	    for(iter = begin(); iter != end(); ++iter) {
		NodeRefPtr markNode = *iter;
		markNode->markNodeStale(recursive);
	    }
	}
	return;
    }

};

typedef std::pair<am_resource_match_t, NodeRefPtr> SearchResult;

class Tree {
private:
    am_resource_traits_t rsrcTraits;
    Mutex treeLock;
    NodeRefPtr rootNode;
    SearchResult find(NodeRefPtr,  const ResourceName &, bool);

    /**
     * This function is being used by insert.  This is a special case where
     * the elements below are either children to the node being added or peers.
     * It is guaranteed that the parent node is a superior of the node being
     * added.
     */
    bool insertBelow(NodeRefPtr /*parent*/, PDRefCntPtr &);
    /* Throws std::invalid_argument if any argument is invalid */
    void addSubNodes(NodeRefPtr /*parent*/, XMLElement & /*policy results*/,
		     KVMRefCntPtr /*env*/);
    void search_recursive(const ResourceName &,
			  const NodeRefPtr,
			  std::vector<PDRefCntPtr> &, bool) const;
public:

    ~Tree();
    am_resource_match_t compare(const std::string &,
			  const std::string &, bool) const;
    am_resource_match_t compare(const ResourceName &,
			  const ResourceName &, bool) const;

    /* The constructors throw std::invalid_argument if any argument is invalid*/
    Tree(PDRefCntPtr &, am_resource_traits_t rTraits);
    Tree(XMLElement &,
	 am_resource_traits_t,
	 KVMRefCntPtr);
    bool insert(PDRefCntPtr &);
    PDRefCntPtr dfsearch(const ResourceName &, bool usePatterns=true);

    void dfsearch_all(const ResourceName &,
		      std::vector<PDRefCntPtr> &,
		      bool usePatterns=true) const;

    bool isInTree(const ResourceName &, bool usePatterns = true);
    bool isParentOfTree(const ResourceName &);

    /**
     * This function removes a node from a tree.
     * The function will fail if root node is being removed.
     */
    bool remove(const ResourceName &, bool recursive=false);

    bool markStale(const ResourceName &, bool recursive=false);

    void outdatePolicyDecisions(const std::string &);
    
    void showNodes() const;
};

END_PRIVATE_NAMESPACE

#endif

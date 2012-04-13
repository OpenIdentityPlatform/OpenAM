/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Tree.java,v 1.7 2009/06/04 11:49:18 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tree {
    private TreeNode rootNode;

    public Tree(TreeNode rootNode) {
        this.rootNode = rootNode;
    }

    public boolean isEmpty() {
        return rootNode == null;
    }

    public TreeNode remove(TreeNode tn) {
        if (remover(rootNode, tn)) {
            rootNode = null;
        }

        return rootNode;
    }

    private boolean remover(TreeNode currentTn, TreeNode removeTn) {
        if (currentTn == removeTn) {
            return true;
        }
        if (currentTn instanceof ContainerTreeNode) {
            ContainerTreeNode ctn = (ContainerTreeNode)currentTn;
            for (TreeNode childTn: ctn.getTreeNodes()) {
                if (remover(childTn, removeTn)) {
                    ctn.getTreeNodes().remove(childTn);
                    return false;
                }
            }
        }
        return false;
    }

    public int size() {
        int size = sizer(rootNode, true);
        return size;
    }

    public int sizeLeafs() {
        int size = sizer(rootNode, false);
        return size;
    }

    private int sizer(TreeNode currentTn, boolean countContainers) {
        if (currentTn == null) {
            return 0;
        } else if (currentTn instanceof ContainerTreeNode) {
            int mySize = countContainers ? 1 : 0;
            ContainerTreeNode ctn = (ContainerTreeNode)currentTn;
            for (TreeNode childTn: ctn.getTreeNodes()) {
                mySize += sizer(childTn, countContainers);
            }
            return mySize;
        } else {
            return 1;
        }
    }

    public List<TreeNode> asList() {
        return asListr(rootNode);
    }

    private List<TreeNode> asListr(TreeNode currentTn) {
        if (currentTn == null) {
            return null;
        } else if (currentTn instanceof ContainerTreeNode) {
            ContainerTreeNode ctn = (ContainerTreeNode)currentTn;
            List<TreeNode> nodes = new ArrayList<TreeNode>();
            nodes.add(currentTn);
            for (TreeNode childTn: ctn.getTreeNodes()) {
                nodes.addAll(asListr(childTn));
            }
            return nodes;
        } else {
            return Collections.singletonList(currentTn);
        }
    }

    public List<TreeNode> getAsList() {
        return asList();
    }
}

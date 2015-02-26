/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.utils.indextree.nodefactory;

import org.forgerock.openam.entitlement.utils.indextree.nodecreator.NodeCreator;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.DefaultTreeNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.MultiWildcardNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.RootNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.SingleWildcardNode;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.TreeNode;

/**
 * Provides a basic tree node factory implementation.
 * 
 * @author apforrest
 */
public class BasicTreeNodeFactory extends AbstractTreeNodeFactory {

	public BasicTreeNodeFactory() {
	    // Add the two wildcard types.
		addNodeCreator(MultiWildcardNode.WILDCARD, new NodeCreator() {
		    
			@Override
			public TreeNode createNode(char nodeValue) {
				return new MultiWildcardNode();
			}

		});
		
		addNodeCreator(SingleWildcardNode.WILDCARD, new NodeCreator() {
		    
			@Override
			public TreeNode createNode(char nodeValue) {
				return new SingleWildcardNode();
			}
			
		});
	}

	@Override
	public TreeNode getRootNode() {
		return new RootNode();
	}

	@Override
	protected TreeNode createDefaultNode(char nodeValue) {
		return new DefaultTreeNode(nodeValue);
	}

}

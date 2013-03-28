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
package org.forgerock.openam.entitlement.utils.indextree.treenodes;

/**
 * Provides the default implementation of a tree node.
 * 
 * Matches a value that is equal to the tree node value.
 * 
 * @author apforrest
 */
public class DefaultTreeNode extends BasicTreeNode {

	private final char nodeValue;

	public DefaultTreeNode(char nodeValue) {
		this.nodeValue = nodeValue;
	}

	@Override
	public char getNodeValue() {
		return nodeValue;
	}

	@Override
	public boolean hasInterestIn(char value) {
		return nodeValue == value;
	}

}

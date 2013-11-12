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

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.ContextKey;
import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;

/**
 * Wildcard tree node that will match any character except for '#' and '?',
 * and any character where the previous character was a '/'.
 *  
 * @author apforrest
 */
public class SingleWildcardNode extends BasicTreeNode {

	public static final char WILDCARD = '^';

	@Override
	public char getNodeValue() {
		return WILDCARD;
	}

	@Override
	public boolean isWildcard() {
		return true;
	}

	@Override
    public boolean hasInterestIn(char value, SearchContext context) {
        if (value == '?' || value == '#') {
            // Ignore illegal character unless it is the last character.
            return context.has(ContextKey.LAST_CHARACTER);
        }

        if (context.has(ContextKey.LEVEL_REACHED)) {
            // Next URL level reached, so no longer interested.
            context.remove(ContextKey.LEVEL_REACHED);
            return false;
        }

        if (value == '/') {
            if (context.has(ContextKey.LAST_CHARACTER)) {
                // '^' after a '/' matches one or more characters.
                return false;
            }

            // Make a note that the end of a URL level has been reached.
            context.add(ContextKey.LEVEL_REACHED, Boolean.TRUE);
            return true;
        }

        return true;
    }

}

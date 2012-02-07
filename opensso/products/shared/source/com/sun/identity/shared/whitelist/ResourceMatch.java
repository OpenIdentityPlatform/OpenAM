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
 * $Id: ResourceMatch.java,v 1.1 2009/11/24 21:42:35 madan_ranganath Exp $
 *
 */


package com.sun.identity.shared.whitelist;

/**
 * The class <code>ResourceMatch</code> defines the results
 * of a resource match with respect to Policy.
 *
 * @supported.all.api
 */
public class ResourceMatch extends Object {

    private String resourceMatch;

    /**
     * The <code>EXACT_MATCH</code> specifies
     * the resources are exactly the same.
     */
    public static final ResourceMatch
	EXACT_MATCH = new ResourceMatch("exact_match");

    /**
     * The <code>WILDCARD_MATCH</code> specifies
     * the resources are wildcard match
     */
    public static final ResourceMatch
	WILDCARD_MATCH = new ResourceMatch("wildcard_match");

    /**
     * The <code>SUB_RESOURCE_MATCH</code> specifies
     * the provided resource is a sub resource.
     */
    public static final ResourceMatch
	SUB_RESOURCE_MATCH = new ResourceMatch("sub_resource_match");

    /**
     * The <code>SUPER_RESOURCE_MATCH</code> specifies
     * the provided resource is more specific than
     * this resource
     */
    public static final ResourceMatch
	SUPER_RESOURCE_MATCH = new ResourceMatch("super_resource_match");

    /**
     * The <code>NO_MATCH</code> specifies
     * the resources do not match
     */
    public static final ResourceMatch
	NO_MATCH = new ResourceMatch("no_match");

        public static final String RESOURCE_COMPARATOR_DELIMITER = "delimiter";

    public static final String RESOURCE_COMPARATOR_WILDCARD = "wildcard";

    public static final String RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD
        = "oneLevelWildcard";

    public static final String RESOURCE_COMPARATOR_CASE_SENSITIVE =
        "caseSensitive";

    private ResourceMatch() {
	// do nothing
    }

    private ResourceMatch(String matchType) {
	resourceMatch = matchType;
    }

    /**
     * Method to get string representation of the resource match.
     *
     * @return string representation of the resource match.
     */
    public String toString() {
	return (resourceMatch);
    }

    /**
     * Method to check if two resource match objects are equal.
     *
     * @param resourceMatch object to which this object will be
     * compared with
     *
     * @return <code>true</code> if the resources match;
     * <code>false</code> otherwise;
     */
    public boolean equals(Object resourceMatch) {
	if (resourceMatch instanceof ResourceMatch) {
	    ResourceMatch rm = (ResourceMatch) resourceMatch;
	    return (rm.resourceMatch.equals(this.resourceMatch));
	}
	return (false);
    }
}

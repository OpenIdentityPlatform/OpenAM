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
 * $Id: ExactMatchResourceName.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 *
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This plugin extends the functionality provided in
 * <code>PrefixResourceName</code> to provide special handling to
 * URL type prefix resource names in <code>canonicalize</code> method
 * like validating port, assigning default port of 80, if port absent etc.
 */
public class ExactMatchResourceName
        implements Comparator, ResourceName {

    public void initialize(Map configParams) {
    }

    /**
     * Canonicalizes a string.
     *
     * @param str the url string to be canonicalized
     * @return the url string in its canonicalized form.
     * @throws EntitlementException if the url string is invalid
     */
    public String canonicalize(String str)
            throws EntitlementException {
        return str.toLowerCase();
    }

    /**
     * This method is used to compare two url query parameter
     * strings. A query parameter string is in the form of
     * variablename=value.
     * 
     * @param o1 a url query parameter to be compared  
     * @param o2 a url query parameter to be compared  
     * @return -1 if o1 < o2; 0 if o1 = o2; 1 if o1 > o2
     */
    public int compare(Object o1, Object o2) {
        String s1 = (String) o1;
        String s2 = (String) o2;

        if (s1 == null) {
            if (s2 != null) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (s2 == null) {
                return 1;
            }
        }

        return s1.compareToIgnoreCase(s2);
    }

    public boolean matches(String s, String p) {
        String ps = p.replaceAll("\\*", ".*");
        Pattern pattern = Pattern.compile(ps);
        Matcher m = pattern.matcher(s);

        return m.matches();
    }

    public String getSubResource(String resource, String superResource) {
        return resource;
    }

    public String append(String superResource, String subResource) {
        return superResource + subResource;
    }

    public ResourceMatch compare(
            String requestResource,
            String targetResource,
            boolean wildcardCompare) {
        if (wildcardCompare) {
            if (matches(requestResource, targetResource)) {
                return ResourceMatch.WILDCARD_MATCH;
            }
            return ResourceMatch.NO_MATCH;
        } else {
            if (compare(requestResource, targetResource) == 0) {
                return ResourceMatch.EXACT_MATCH;
            }
            return ResourceMatch.NO_MATCH;
        }
    }

    public Set getServiceTypeNames() {
        return null;
    }
}

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
 * $Id: RegExResourceName.java,v 1.1 2009/12/07 19:53:02 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.am.util.Cache;
import com.sun.identity.entitlement.interfaces.ResourceName;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author dennis
 */
public class RegExResourceName implements ResourceName {
    private String delimiter = "/";
    private static final ReadWriteLock patternCacheLock =
        new ReentrantReadWriteLock();

    private static final int MAX_CACHE_SIZE = 1000;
    private static Cache patternCache = new Cache(MAX_CACHE_SIZE);

    public Set getServiceTypeNames() {
        return null;
    }

    public void initialize(Map configParams) {
        // do nothing
    }

    public ResourceMatch compare(String origRes, String compRes,
        boolean wildcardCompare) {
        ResourceMatch result = ResourceMatch.NO_MATCH;

        if ((origRes == null) && (compRes == null)) {
            result = ResourceMatch.EXACT_MATCH;
        } else if ((origRes == null) || (compRes == null)) {
            result = ResourceMatch.NO_MATCH;
        } else {
            String orig = origRes.toLowerCase();
            String target = compRes.toLowerCase();

            if (orig.equals(target)) {
                return ResourceMatch.EXACT_MATCH;
            }

            ResourceMatch match = resourceMatch(orig, target);
            if (!match.equals(ResourceMatch.NO_MATCH)) {
                result = match;
            }

            result = (wildcardCompare) ? patternMatch(orig, target) : match;
        }
        return result;
    }

    private ResourceMatch resourceMatch(String s1, String s2) {
        if (!s1.endsWith(delimiter)) {
            s1 += delimiter;
        }
        if (!s2.endsWith(delimiter)) {
            s2 += delimiter;
        }

        if (s2.startsWith(s1)) {
            return ResourceMatch.SUB_RESOURCE_MATCH;
        }
        if (s1.startsWith(s2)) {
            return ResourceMatch.SUPER_RESOURCE_MATCH;
        }

        return ResourceMatch.NO_MATCH;
    }

    public String append(String superResource, String subResource) {
        // Remove duplicate /
        if (superResource.endsWith(delimiter) &&
            subResource.startsWith(delimiter))
        {
            superResource = superResource.substring(0,
                superResource.length() -1);
        }

        if (!superResource.endsWith(delimiter) &&
            !subResource.startsWith(delimiter))
        {
            subResource = delimiter + subResource;
        }

        return superResource+subResource;
    }

    public String getSubResource(String resource, String superResource) {
        if (!superResource.endsWith(delimiter)) {
            superResource = superResource + delimiter;
        }
        return (resource.startsWith(superResource)) ?
            resource.substring(superResource.length()) : null;
    }

    public String canonicalize(String res) throws EntitlementException {
        return res;
    }

    private ResourceMatch patternMatch(String base, String target) {
        String strBase = (!base.endsWith(delimiter)) ?
            base += delimiter : base;
        String strTarget = (!target.endsWith(delimiter)) ?
            target += delimiter : target;
        
        if (strTarget.startsWith(strBase)) {
            return ResourceMatch.SUB_RESOURCE_MATCH;
        }
        if (strBase.startsWith(strTarget)) {
            return ResourceMatch.SUPER_RESOURCE_MATCH;
        }

        Pattern pattern = getPatternFromCache(base);
        Matcher matcher = pattern.matcher(target.replace("*", ""));
        if (matcher.matches()) {
            if (matcher.group(1).length() == 0) {
                return ResourceMatch.WILDCARD_MATCH;
            } else {
                return ResourceMatch.SUB_RESOURCE_MATCH;
            }
        } else {
            pattern = getPatternFromCache(target);
            matcher = pattern.matcher(base.replace("*", ""));

            if (matcher.matches()) {
                if (matcher.group(1).length() == 0) {
                    return ResourceMatch.WILDCARD_MATCH;
                } else {
                    return ResourceMatch.SUPER_RESOURCE_MATCH;
                }
            }
            return ResourceMatch.NO_MATCH;
        }
    }

    private static Pattern getPatternFromCache(String strPattern) {
        patternCacheLock.writeLock().lock();
        try {
            Pattern pattern = (Pattern)patternCache.get(strPattern);
            if (pattern != null) {
                return pattern;
            }

            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < strPattern.length()-1; i++) {
                char c = strPattern.charAt(i);
                if (c == '.') {
                    buff.append("\\.");
                } else if (c == '*') {
                    buff.append(".*?");
                } else {
                    buff.append(c);
                }
            }

            char lastChar = strPattern.charAt(strPattern.length()-1);
            if (lastChar == '*') {
                buff.append(".*");
            } else {
                buff.append(lastChar);
            }

            pattern = Pattern.compile(buff.toString() + "(.*)");
            patternCache.put(strPattern, pattern);
            return pattern;
        } finally {
            patternCacheLock.writeLock().unlock();
        }
    }
}

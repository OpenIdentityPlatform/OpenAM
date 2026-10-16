/**
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
 * $Id: ResourceComparatorValidator.java,v 1.5 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/**
 * Portions Copyright 2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.policy;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Set;
import java.util.StringTokenizer;
import org.forgerock.openam.utils.CollectionUtils;

/**
 * This validator checks the wildcard and oneLevelWildcard defined
 * for a resource comparator. If they are equal or overlapping
 * then they are invalid values.
 */
public class ResourceComparatorValidator implements ServiceAttributeValidator {

    // dereference debug object from Policy Manager so that Policy Manager
    // class will not be initialized.
    private static final Debug debug = Debug.getInstance("amPolicy");

    /**
     * Validates a set of values for rules built into this method. 
     * Returns <code>true</code> if "wildcard" and "oneLevelWildcard"
     * are either equal or overlapping in values.
     *
     * @param values the set of values to be validated
     * @return <code>true</code> if "wildcard" and "oneLevelWildcard"
     * are either equal or overlapping in values, <code>false</code> 
     * otherwise
     */    
    public boolean validate(Set<String> values) {
        if (!CollectionUtils.isEmpty(values)) {
            // values is a set. each element in the set is of the form
            // serviceType=1|class=com.sun.identity.policy.Class|wildcard=*|
            // caseSensitive=true|one_level_wildcard=-*-
            for (String val : values) {
                if (val != null) {
                    StringTokenizer st = new StringTokenizer(val, "|");
                    String[] tokens = new String[6];
                    int count = 0;
                    while (st.hasMoreTokens()) {
                        tokens[count++] = st.nextToken();
                        if (count > 5) { // accept only first six tokens
                            break;
                        }
                    }
                    String wildcardPattern = null;
                    String oneLevelPattern = null;
                    for (int i = 0; i < count; i++) {
                        int equal = tokens[i].indexOf("=");
                        String name = tokens[i].substring(0, equal);
                        String value = tokens[i].substring(equal + 1);
                        if (debug.messageEnabled()) {
                            debug.message("ResourceComparatorValidator.validate():Attr Name = " + name +
                                    " Attr Value = " + value);
                        }
                        if (name.equalsIgnoreCase(PolicyConfig.RESOURCE_COMPARATOR_WILDCARD)) {
                            wildcardPattern = value;
                        } else if (name.equalsIgnoreCase(PolicyConfig.RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD)) {
                            oneLevelPattern = value;
                        }
                    }
                    if ((wildcardPattern != null) && (oneLevelPattern != null)) {
                        if (wildcardPattern.equals(oneLevelPattern)) {
                            debug.error("ResourceComparatorValidator.validate()"
                                    + "Wildcard and one level wildcard pattern "
                                    + "cannot be same");
                            return false;
                        }
                        if (!wildcardPattern.contains(oneLevelPattern) && !oneLevelPattern.contains(wildcardPattern)) {
                            if (debug.messageEnabled()) {
                                debug.message("ResourceComparatorValidator.validate():about to do overlap check");
                            }
                            // find if the wildcard and one level wildcard patterns overlap
                            boolean overlap = startsWithASuffixOf(oneLevelPattern, wildcardPattern)
                                    || startsWithASuffixOf(wildcardPattern, oneLevelPattern);
                            if (overlap) {
                                debug.error("ResourceComparatorValidator.validate():Wildcard and one level "
                                        + "wildcard pattern cannot be overlapping");
                                return false;
                            }
                        } // if either of the  patterns not nested
                    } // end if both patterns are not null
                } // if elemVal != null
            } // while
        } // if values not empty
        return true;
    }

    /**
     * Returns <code>true</code> if <code>value</code> starts with any non empty suffix of
     * <code>pattern</code>, which is what makes the two patterns overlap.
     * <p>
     * The overlap check used to look for the same thing by walking every pair of positions in
     * the two patterns and allocating a fresh suffix of both of them for every pair whose
     * characters matched, which costs O(n<sup>3</sup>) in their length. Neither half of the
     * test it made there depends on the other pattern's position, and a suffix that is a
     * prefix of the other pattern necessarily begins with that pattern's first character, so
     * the pair the old loop needed to reach it was always among the ones it walked: this
     * gives the same answer in O(n<sup>2</sup>) character comparisons and no allocation.
     * <p>
     * The length of the patterns is chosen by whoever supplies the attribute values, and the
     * JAXRPC <code>validateServiceAttributes</code> endpoint lets any authenticated caller
     * name this validator and hand it values of its own, so the cost has to be bounded here
     * rather than left to the caller.
     *
     * @param value the string to test.
     * @param pattern the pattern whose suffixes are tried.
     * @return <code>true</code> if a suffix of <code>pattern</code> is a prefix of
     *         <code>value</code>.
     */
    private static boolean startsWithASuffixOf(String value, String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (value.regionMatches(0, pattern, i, pattern.length() - i)) {
                return true;
            }
        }
        return false;
    }
}

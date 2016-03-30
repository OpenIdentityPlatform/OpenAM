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



package com.sun.identity.policy;

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * This validator checks the wildcard and oneLevelWildcard defined
 * for a resource comparator. If they are equal or overlapping
 * then they are invalid values.
 */
public class ResourceComparatorValidator implements ServiceAttributeValidator {

    String separator = "|";

    // dereference debug object from Policy Manager so that Policy Manager
    // class will not be initialized.
    private static final Debug debug = Debug.getInstance("amPolicy");

    public ResourceComparatorValidator() {
    }

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
    public boolean validate(Set values) {
        if (values != null && !values.isEmpty()) {
            // values is a set. each element in the set is of the form
            // serviceType=1|class=com.sun.identity.policy.Class|wildcard=*|
            // caseSensitive=true|one_level_wildcard=-*-
            Iterator valIterator = values.iterator();
            while (valIterator.hasNext()) {
                String elemVal = (String) valIterator.next();
                if (elemVal != null) {
                    StringTokenizer st = new StringTokenizer(elemVal, "|");
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
                        if (name == null) {
                            debug.error("ResourceComparatorValidator."
                                   +"validate(): name is null");
                            continue;
                        }
                        if (value == null) {
                            debug.error("ResourceComparatorValidator."
                                +"validate(): value is null");
                            continue;
                        }
                        if (debug.messageEnabled()) {
                            debug.message("ResourceComparatorValidator."+
                                "validate():Attr Name = " + name +
                                " Attr Value = " + value);
                        }
                        if (name.equalsIgnoreCase(
                            PolicyConfig.RESOURCE_COMPARATOR_WILDCARD)) {
                            wildcardPattern = value;
                        } else if (name.equalsIgnoreCase(PolicyConfig.
                            RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD)) {
                            oneLevelPattern = value;
                        }
                    }
                    if ((wildcardPattern != null) && 
                        (oneLevelPattern != null)) 
                    {
                        if (wildcardPattern.equals(oneLevelPattern)) {
                            debug.error("ResourceComparatorValidator.validate()"
                                +"Wildcard and one level wildcard pattern "
                                + "cannot be same");
                            return false;
                        }
                        if (wildcardPattern.indexOf(oneLevelPattern) == -1 &&
                            oneLevelPattern.indexOf(wildcardPattern) == -1) {
                            boolean overlap = false;
                            if (debug.messageEnabled()) {
                                debug.message("ResourceComparatorValidator."
                                    +"validate():about to do overlap check");
                            }
                            // find if the wildcard and one level wildcard 
                            // patterns overlap
                            int oneLevelWildLength = oneLevelPattern.length();
                            int wildcardLength = wildcardPattern.length();
                            char[] wildcard = wildcardPattern.toCharArray();
                            char[] oneWildcard = oneLevelPattern.toCharArray();
                            for (int i = 0; i < wildcardPattern.length(); i++) {
                                for (int j = 0; j < oneLevelPattern.length(); 
                                    j++) 
                                {
                                    if (wildcard[i] == oneWildcard[j]) {
                                        String remString1 = String.valueOf(
                                            wildcard, i,wildcardLength -i);
                                        String remString2 = String.valueOf(
                                            oneWildcard, j,
                                                oneLevelWildLength -j);
                                        if (oneLevelPattern.startsWith(
                                            remString1) || 
                                            wildcardPattern.startsWith(
                                            remString2))
                                        {
                                            overlap = true;
                                            break;
                                        }
                                    }
                                }
                                if (overlap) {
                                    break;
                                }
                            }
                            if (overlap) {
                                debug.error("ResourceComparatorValidator."
                                    +"validate():Wildcard and one level "
                                    +"wildcard pattern cannot be overlapping");
                                return false;
                            }
                        } // if either of the  patterns not nested
                    } // end if both patterns are not null
                } // if elemVal != null
            } // while
        } // if values not empty
        return true;
    }
}

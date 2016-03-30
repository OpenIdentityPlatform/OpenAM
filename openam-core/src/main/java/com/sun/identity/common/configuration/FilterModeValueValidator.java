/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FilterModeValueValidator.java,v 1.3 2008/07/03 09:39:14 veiming Exp $
 *
 */

package com.sun.identity.common.configuration;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates Filter Mode property value in Agent Properties. e.g.
 * <code>com.sun.identity.agents.config.filter.mode</code>
 * The values in set of properties can be application specific:
 *    -for example [somecontextroot]=J2EE_POLICY  or one of other valid values
 *     from  NONE, SSO_ONLY, URL_POLICY, J2EE_POLICY, ALL 
 * Can also have *one* global value:
 *     -where value has no brackets or context root key and is just a value
 *     NONE, SSO_ONLY, URL_POLICY, J2EE_POLICY, ALL 
 *     -also allow a global value format: "=ALL" or none etc, which includes an 
 *     equal sign since in property file style an entry like 
 *     com.sun.someprop=ALL would have a value "=ALL"
 *
 * Does not allow  []=ALL or none etc.
 * Does not allow the characters "[" or "]" inside the brackets since this is
 * not a likely valid character in a context root name and it helps us avoid 
 * the user error of a typo and we use bracket as a special character in parsing.
 * Ultimately this regular expression could be stricter since the key inside 
 * of brackets is a context root of a web app [somecontextroot] and context 
 * root values only allow certain value, but since agent code and 
 * other places dont validate for this strictness, this validator should be
 * close to as loose as rest of code for consistency.
 * 
 * Overall set of values:
 * -Empty set is not allowed since user must specify something.
 * -Set can only have *one* global value value of NONE, SSO_ONLY, URL_POLICY, 
 * J2EE_POLICY, ALL so java code checks for repeats.
 * -Duplicates: The validation logic does not check for duplicates in terms of
 * the whole value ie [somecontextroot]=ALL being included twice. Since a set
 * is used to hold the values, and Set add method only adds the specified 
 * element to this set if it is not already present so it will not contain any 
 * duplicates. So no check necessary. The UI may allow a user to enter 
 * duplicates but ultimately they are ignored and not stored as part of 
 * agents config.
 * Duplicates are exact duplicates, since set does not distinguish between 
 * letter case of a key, so "Key" is a different key from "key". We dont check
 * for duplicates such as this type of possible case.
 * However, we do check for exact duplicate context root name within whole 
 * values, so if user inputs [mycontextroot]=ALL and [mycontextroot]=NONE then
 * both values will be in the set, and this code will detect that 
 * "mycontextroot" is a duplicate so set will be considered invalid.
 */
public class FilterModeValueValidator implements ServiceAttributeValidator {

    private static final String globalRegularExpression = 
            "(\\s*=?\\s*(NONE|SSO_ONLY|URL_POLICY|J2EE_POLICY|ALL)\\s*)";
    private static final String appSpecificRegularExpression =  
            "(\\s*\\[\\s*[\\S&&[^\\[]&&[^\\]]]+\\s*\\]\\s*=\\s*(NONE|SSO_ONLY|URL_POLICY|J2EE_POLICY|ALL)\\s*)";
            
    private static final String regularExpression =  
            "(" + appSpecificRegularExpression + "|" + globalRegularExpression + ")";

    private static final Pattern pattern = Pattern.compile(regularExpression);
    private static final Pattern globalPattern = 
                                 Pattern.compile(globalRegularExpression);   
    
    public FilterModeValueValidator() {
    }

    /**
     * Returns <code>true</code> if values are of filter mode type.
     * 
     * @param values the set of values to be validated
     * @return <code>true</code> if values are of filter mode type.
     */
     public boolean validate(Set values) {
        boolean valid = true;
        boolean globalFound = false; //can only have zero or one global value

        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.iterator(); (i.hasNext() && valid);) {
                String str = (String)i.next();
                if (str!=null) {
                    str = str.trim();
                    Matcher m = pattern.matcher(str);
                    valid = m.matches();

                    //now test for duplicate global value
                    Matcher globalMatcher = globalPattern.matcher(str);
                    boolean globalMatch = globalMatcher.matches();

                     //if value matches global and previously found one too
                    if (globalFound && globalMatch && valid) {
                        valid = false; //more than one global value so invalid
                    } else if (globalMatch && valid) {
                        globalFound = true; //found first global
                    }                   
                }
            }
        } else {
            valid = false; //empty set not valid
        }
        
        if (valid) 
            valid = MapDuplicateKeyChecker.checkForNoDuplicateKeyInValue(values);
        return valid;
    }
}

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
 * $Id: MapValueValidator.java,v 1.3 2008/07/03 09:39:13 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common.configuration;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates map value properties in Agent Properties. e.g.
 * <code>com.sun.identity.agents.config.response.attribute.mapping[]=</code>
 * Map values should be Strings of the form:
 *   [somekey] = somevalue
 *   etc 
 *   l.h.s is key value surrounded by brackets
 *   separator is mandatory "=" equals sign
 *   r.h.s is some string value, anything since this is not so fined- 
 *         grained to test values of any one specific property's value set.
 *   r.h.s can be empty or just whitespace 
 *   l.h.s key value can NOT be empty
 *   l.h.s key values can not be duplicates
 *   white space is allowed everywhere
 *   blank or empty values are allowed as some props dont have any value 
 *         to be specified
 *
 *   Some strange examples that would be acceptable values:
 *       blank or empty set
 *       [a_key_but_no_value]=     (note no value on r.h.s)
 *       []=                       (a common default value)
 *       [  ] =                    (variation on default value)
 *       [key_and_or_value_contains_=_sign] ==   (equal sign is valid data)
 *
 *   Note:
 *       brackets are not allowed as part of key or value data
 *       exact duplicate keys not allowed 
 *       
 */
public class MapValueValidator implements ServiceAttributeValidator {

    /**
     *  1)  [[\\S]&&[^\\[]&&[^\\]]]+   means at least one non-whitespace 
     *                                 character except not a [ or ]
     *  
     *  2)  [[^\\[]&&[^\\]]]*          means zero or more of anything 
     *                                 except not a [ or ] 
     *
     *  1 & 2 are combined to match values that have a key
     *
     *  3)  \\s*\\[\\s*\\]\\s*=\\s*    means the case of []=  which is used for 
     *                                 many default map values
     */
    
    //also used by GlobalMapValueValidator so package scoped
    static final String KEY_WITH_NO_BRACKETS =  
            "(\\s*\\[\\s*[[\\S]&&[^\\[]&&[^\\]]]+[[^\\[]&&[^\\]]]*\\s*\\]\\s*=.*)";
    //also used by GlobalMapValueValidator  so package scoped
    static final String DEFAULT_NO_KEY_JUST_BRACKETS =  
            "(\\s*\\[\\s*\\]\\s*=\\s*)";
     
    private static final String regularExpression = KEY_WITH_NO_BRACKETS 
                                    + "|" + DEFAULT_NO_KEY_JUST_BRACKETS;
    
    private static final Pattern pattern = Pattern.compile(regularExpression);

    public MapValueValidator() {
    }

    /**
     * Returns <code>true</code> if values are of map type format.
     * 
     * @param values contains the set of values to be validated
     * @return <code>true</code> if values are of map type format.
     */
    public boolean validate(Set values) {
        boolean valid = true; //blank or emtpy values set are valid
     
        //since a Set is used and set can not have duplicate entries 
        //we dont need to test for duplicates of *whole* value
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.iterator(); (i.hasNext() && valid);) {
                String str = ((String)i.next()).trim();
                if (str.length() > 0) {
                    Matcher m = pattern.matcher(str);
                    valid = m.matches();
                }
            }
        }
        if (valid) 
            valid = MapDuplicateKeyChecker.checkForNoDuplicateKeyInValue(values);
        return valid;
    }
    
}

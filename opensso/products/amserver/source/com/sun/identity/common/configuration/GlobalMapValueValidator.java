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
 * $Id: GlobalMapValueValidator.java,v 1.4 2008/07/10 22:03:17 veiming Exp $
 *
 */

package com.sun.identity.common.configuration;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates Global Map property value in Agent Properties.
 * These "Global" Map properties are just like Map properties, 
 * except they ALSO allow you to specify one global value as the default for 
 * the property, so when a key is not specified so mapping exists, then
 * this global value is used. 
 * e.g.
 * <code>com.sun.identity.agents.config.someglobalmapprop=somevalue</code>
 * Note, can have only *one* global value:
 *     -where value has no brackets or context root key and is just a value
 *     -also allow a global value format: "=SomeValue" etc, which
 *     includes an equal sign since in property file style an entry like 
 *     com.sun.someprop=ALL would have a value "=SomeValue"
 *     -can not have a blank r.h.s like "= "
 *     -can not have space inside characters of r.h.s, like "= some space"
 *
 * This validator accepts all the values accepted by MapValueValidator and 
 * addtionally accepts global format style values.
 *
 * See the MapValueValidator.java class for non global map 
 * regular expressions and examples of acceptable values.
 */
public class GlobalMapValueValidator implements ServiceAttributeValidator {

    //global format is no whitespace in between characters
    // and no brackets allowed
    private static final String globalRegularExpression = 
            "(\\s*[\\S&&[^\\[]&&[^\\]]]+\\s*)";
     
    private static final String appSpecificRegularExpression = 
            MapValueValidator.KEY_WITH_NO_BRACKETS    
            + "|" 
            + MapValueValidator.DEFAULT_NO_KEY_JUST_BRACKETS;
            
    private static final String regularExpression =  
            "(" + appSpecificRegularExpression + "|" + globalRegularExpression + ")";
    

    private static final Pattern pattern = Pattern.compile(regularExpression);
    private static final Pattern globalPattern = 
                                 Pattern.compile(globalRegularExpression);   
    
    public GlobalMapValueValidator() {
    }

    /**
     * Returns <code>true</code> if values are of global map format.
     * 
     * @param values the set of values to be validated
     * @return <code>true</code> if values are of global map format.
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

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ListValueValidator.java,v 1.5 2008/06/25 05:42:28 qcheng Exp $
 *
 */

package com.sun.identity.common.configuration;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates list value in Agent Properties. e.g.
 * <code>com.sun.identity.agents.config.login.form[0]=</code>
 * List values should be Strings of the form:
 *   [0] = some value
 *   [1] = some value
 *   [some sequential index] = some value
 *   etc 
 *   l.h.s is index value surrounded by brackets
 *   separator is mandatory "=" equals sign
 *   r.h.s is some string value, anything since this is not so fined 
 *         grained to test values of any one specific property's value set.
 *   index value should be an integer >= 0
 *   index values can not be duplicates
 *   index values collectively can contain missing mubers
 *   white space is allowed everywhere (except between digits of index value)
 *   blank or empty values are allowed as some props dont have any value 
 *         to be specified
 *
 *   Some examples that would be acceptable values:
 *       blank or empty set
 *       [0] =     (note no value on r.h.s)
 *       
 *
 */
public class ListValueValidator implements ServiceAttributeValidator {

    private static final Pattern pattern = 
          Pattern.compile("(\\s*\\[\\s*\\d++\\s*\\]\\s*=.*)");
            
    public ListValueValidator() {
    }

    /**
     * Returns <code>true</code> if values are of list typed.
     * 
     * @param values the set of values to be validated
     * @return <code>true</code> if values are of list format type.
     */
    public boolean validate(Set values) {
        boolean valid = true; //blank or emtpy values set are valid
     
        //since a Set is used and set can not have duplicates I dont 
        //need to test for duplicates of *whole* value
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
            valid = checkForValidIntegerKeyInValue(values);
        return valid;
    }
    
    /**
     * Values in set are of the form [integerindex]=somevalue and this integer
     * index is later used in code which will create a list array with the 
     * integer index as the lookup key. So we need to check for the case of 
     * duplicate integers, integers must be >= 0
     * Note, the integers do not have to be sequential and can skip numbers etc.
     *
     * So need to parse values and get the integer index keys and see if valid.
     *
     * @param values must be a valid set of inputs. Must be valid in the sense
     *        that each of its values are like [0]=somevalue
     *
     * @return true if set is good and contains no duplicate keys
     */
    private boolean checkForValidIntegerKeyInValue(Set values) {
        boolean valid  = true;
        HashSet keySet = new HashSet();
        int indexNumber = -1;
        
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.iterator(); (i.hasNext() && valid);) {
                String str = ((String)i.next()).trim();             
                if (str.length() > 0) {
                    //extract key from whole value
                    int startIndex = str.indexOf("[");
                    int endIndex =   str.indexOf("]");
                    str=str.substring(startIndex+1, endIndex).trim();
                    try {
                        indexNumber = Integer.parseInt(str);
                    } catch (NumberFormatException nfe) {
                        valid =false;
                    }
                    if (indexNumber <0 ) valid=false;
                    if (keySet.add(str) == false) valid=false;
                }
            }
        }
        return valid;
    }
}

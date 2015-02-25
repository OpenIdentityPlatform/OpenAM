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
 * $Id: MapDuplicateKeyChecker.java,v 1.3 2008/07/03 09:39:14 veiming Exp $
 *
 */

package com.sun.identity.common.configuration;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * This helper class is used by the map validators to detect if a duplicate key
 * is in the set. Many of the map properties in Agent Properties. e.g.
 * <code>com.sun.identity.agents.config.filter.mode</code> are validated with
 * pattern matching, and additionally need to check if there is more than one
 * setting for the exact same key.
 * The values in set of properties should be, for example
 * [somecontextroot]=somevalue and "somecontextroot" should only be set to one 
 * value.
 *
 * Precondition :Assumes that values in the Set dont allow the characters "[" 
 * or "]" inside the brackets, so it use bracket as a special character in 
 * parsing.
 *
 * Duplicates are exact duplicates, since set does not distinguish between 
 * letter case of a key, so "Key" is a different key from "key". We dont check
 * for duplicates such as this type of possible case.
 * However, we do check for exact duplicate context root name within whole 
 * values, so if user inputs [mycontextroot]=ALL and [mycontextroot]=NONE then
 * both values will be in the set, and this code will detect that 
 * "mycontextroot" is a duplicate so set will be considered invalid.
 */
public class MapDuplicateKeyChecker {  
    
    /**
     * Values in set are of the form [somekey]=somevalue and this value 
     * is later used in code which will use the "somekey" as a key. So we
     * need to check for the case of duplicate key eg:
     *    [samekey]=value
     *    [samekey]=another_value_for_same_key
     * So need to parse values and get the keys and see if any duplicates.
     *
     * This method should only be called after a validation method has 
     * determined that the set of values is valid in terms of patter matching.
     *
     * For case sensitivity, note if key is "KEY" or "key" or "Key"
     * they are each considered distinct and not duplicates.
     *
     * @param values must be a valid set of inputs. Must be valid in the sense
     *        that each of its values are like [somekey]=somevalue
     *
     * @return true if set is good and contains no duplicate keys
     */
    static boolean checkForNoDuplicateKeyInValue(Set values) {
        boolean valid  = true;
        HashSet keySet = new HashSet();
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.iterator(); (i.hasNext() && valid);) {
                String str = ((String)i.next()).trim();             
                if (str.length() > 0) {
                    //extract key from whole value
                    int startIndex = str.indexOf("[");
                    //global values do not have brackets, so dont check
                    //for key if no starting bracket, else extract key and  
                    //check if it is a duplicate of key already in keySet
                    if(startIndex != -1) {
                        int endIndex =   str.indexOf("]");
                        str=str.substring(startIndex+1, endIndex).trim();
                        if (keySet.add(str) == false) valid=false;
                    }
                }
            }
        }    
        return valid;
    }
}

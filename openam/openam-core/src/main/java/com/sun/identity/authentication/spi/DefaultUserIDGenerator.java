/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultUserIDGenerator.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>DefaultUserIDGenerator</code> generates a set of
 * user IDs based on the first name and last name of the user.
 */
public class DefaultUserIDGenerator implements UserIDGenerator {
    private static final String ATTRIBUTE_FIRST_NAME = "givenname";
    private static final String ATTRIBUTE_LAST_NAME = "sn";
    private static final String EMPTY_STRING = "";
    private static final String NAME_SEPARATOR = "_";
    
    /**
     * Generates a set of user IDs.
     * The parameter <code>num</code> refers to the maximum number of user IDs
     * returned. It is possible that the size of the returned
     * <code>Set</code> is smaller than the parameter num.
     * 
     * @param orgName the DN of the organization
     * @param attributes the keys in the <code>Map</code> contains the
     *                   attribute names and their corresponding values in
     *                   the <code>Map</code> is a <code>Set</code> that
     *                   contains the values for the attribute
     * @param num the maximum number of returned user IDs; 0 means there
     *        is no limit
     * @return a set of auto-generated user IDs.
     */
    public Set generateUserIDs(String orgName, Map attributes, int num) {
        Set userIDs = new HashSet();
        
        String lastName = getAttributeValue(
            attributes, ATTRIBUTE_LAST_NAME).toLowerCase();
        String firstName = getAttributeValue(
            attributes, ATTRIBUTE_FIRST_NAME).toLowerCase();
        
        if (firstName.equals(EMPTY_STRING) || lastName.equals(EMPTY_STRING)) {
            return userIDs;
        }
        
               //Check for non-ascii characters
        
        firstName = filterNonAsciiChars(firstName);
        if(firstName.length() == 0)
                return userIDs;

        lastName = filterNonAsciiChars(lastName);
        if(lastName.length() == 0)
                return userIDs;

         
        userIDs.add(firstName + lastName);
        userIDs.add(firstName + NAME_SEPARATOR + lastName);
        userIDs.add(lastName + firstName);
        userIDs.add(lastName + NAME_SEPARATOR + firstName);
        
        String firstCharFirstName = firstName.substring(0, 1);
        String firstCharLastName = lastName.substring(0, 1);
        
        userIDs.add(firstCharFirstName + lastName);
        userIDs.add(firstCharFirstName + NAME_SEPARATOR + lastName);
        
        userIDs.add(firstCharLastName + firstName);
        userIDs.add(firstCharLastName + NAME_SEPARATOR + firstName);
        
        userIDs.add(firstName + firstCharLastName);
        userIDs.add(firstName + NAME_SEPARATOR + firstCharLastName);
        
        if (num == 0) {
            return userIDs;
            
        } else {
            return returnSet(userIDs, num);
        }
    }   

    //Check for Non-ascii characters in User Name

    private String filterNonAsciiChars(String userName){
        char[] chUserName = userName.toCharArray();
        StringBuilder newUserName = new StringBuilder("");

        for(int i=0; i < chUserName.length; i++){
                if (chUserName[i] <= (int)0x80){
                        newUserName.append(chUserName[i]);
                }
        }
                  return (newUserName.toString());
    }
                                                  
    
    private String getAttributeValue(Map attrs, String attrName) {
        Set values = (Set) attrs.get(attrName);
        if (values != null) {
            Iterator it = values.iterator();
            if (it.hasNext()) {
                return ((String) it.next());
            }
        }
        return EMPTY_STRING;
    }
    
    private Set returnSet(Set origSet, int num) {
        if (num < 0) {
            return Collections.EMPTY_SET;
        }
        
        if (num < origSet.size()) {
            Iterator it = origSet.iterator();
            Set newSet = new HashSet();
            for (int i = 0; i < num; i++) {
                newSet.add((String) it.next());
            }
            return newSet;
        }
        
        return origSet;
    }
}

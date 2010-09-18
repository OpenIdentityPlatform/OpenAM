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
 * $Id: UserMappingMode.java,v 1.2 2008/06/25 05:51:37 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import com.sun.identity.agents.util.ConstrainedSelection;

/**
 * A convenience class for defining the user mapping mode that is applicable
 * to the Agent runtime.
 */
public class UserMappingMode extends ConstrainedSelection {

   /**
    * The integer representation of User Mapping Mode: USER_ID
    */
    public static final int INT_MODE_USER_ID = 0;
    
   /**
    * The integer representation of User Mapping Mode: LDAP_ATTRIBUTE
    */
    public static final int INT_MODE_PROFILE_ATTRIBUTE   = 1;
    
   /**
    * The integer representation of User Mapping Mode: HTTP_HEADER
    */
    public static final int INT_MODE_HTTP_HEADER = 2;
    
   /**
    * The integer represntation of User Mapping Mode: SESSION_PROPERTY
    */
    public static final int INT_MODE_SESSION_PROPERTY = 3;
    
   /**
    * The String representation of User Mapping Mode: USER_ID
    */
    public static final String STR_MODE_USER_ID = "USER_ID";
   
   /**
    * The String representation of User Mapping Mode: LDAP_ATTRIBUTE
    */
    public static final String STR_MODE_PROFILE_ATTRIBUTE = "PROFILE_ATTRIBUTE";
    
   /**
    * The String representation of User Mapping Mode: HTTP_HEADER
    */
    public static final String STR_MODE_HTTP_HEADER = "HTTP_HEADER";
    
   /**
    * The String representation of User Mapping Mode: SESSION_PROPERTY
    */
    public static final String STR_MODE_SESSION_PROPERTY = "SESSION_PROPERTY";

   /**
    * User Mapping Mode instance representing mode: USER_ID
    */
    public static final UserMappingMode MODE_USER_ID
        = new UserMappingMode(STR_MODE_USER_ID, INT_MODE_USER_ID);

   /**
    * User Mapping Mode instance representing mode; LDAP_ATTRIBUTE
    */
    public static final UserMappingMode MODE_PROFILE_ATTRIBUTE
        = new UserMappingMode(STR_MODE_PROFILE_ATTRIBUTE, 
                INT_MODE_PROFILE_ATTRIBUTE);

   /**
    * User Mapping Mode instance representing mode: HTTP_HEADER
    */
    public static final UserMappingMode MODE_HTTP_HEADER
        = new UserMappingMode(STR_MODE_HTTP_HEADER, INT_MODE_HTTP_HEADER);
    
   /**
    * User Mapping Mode instance representing mode: SESSION_PROPERTY
    */
    public static final UserMappingMode MODE_SESSION_PROPERTY
            = new UserMappingMode(STR_MODE_SESSION_PROPERTY, 
                    INT_MODE_SESSION_PROPERTY);

   /**
    * A list of all valid User Mapping Modes.
    */
    private static final UserMappingMode[] values
        = new UserMappingMode[] { MODE_USER_ID, MODE_PROFILE_ATTRIBUTE, 
            MODE_HTTP_HEADER, MODE_SESSION_PROPERTY };

   /**
    * Convenience method to retrieve a <code>UserMappingMode</code> instance
    * based on the given integer representation.
    * 
    * @param modeInt the integer representation of the requested 
    * <code>UserMappingMode</code> instance.
    * 
    * @return the corresponding <code>UserMappingMode</code> to the given 
    * <code>modeInt</code> value or <code>null</code> if no such mode exists.
    */
    public static UserMappingMode get(int modeInt) {
        return (UserMappingMode) ConstrainedSelection.get(modeInt, values);
    }
    
   /**
    * Convenience method to retrieve a <code>UserMappingMode</code> instance
    * based on the given String representation.
    * 
    * @param modeString the String representation of the requested 
    * <code>UserMappingMode</code> instance.
    * 
    * @return the corresponding <code>UserMappingMode</code> to the given 
    * <code>modeString</code> value or <code>null</code> if no such mode exists.
    */
    public static UserMappingMode get(String modeString) {
        return (UserMappingMode) ConstrainedSelection.get(modeString, values);
    }

    private UserMappingMode(String name, int intValue) {
        super(name, intValue);
    }
}

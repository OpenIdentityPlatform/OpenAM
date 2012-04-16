/* The contents of this file are subject to the terms
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
 * $Id: IDMConstants.java,v 1.2 2007/09/25 17:36:30 bt199000 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idm;

/**
 * <code>IDMConstants</code> is an interface which contains identity management 
 * attributes, keys, and values.
 */
public interface IDMConstants {
        
    /**
     * Identities test case prefix string
     */
    static final String IDM_TESTCASES_PREFIX = "test";
    
    /**
     * Identities type <code>user<code>
     */
    static final String IDM_IDENTITY_TYPE_USER = "user";
    
    /**
     * Identities type <code>role<code>
     */
    static final String IDM_IDENTITY_TYPE_ROLE = "role";
    
    /**
     * Identities type <code>group<code>
     */
    static final String IDM_IDENTITY_TYPE_GROUP = "group";
    
    /**
     * Identities type <code>filteredrole<code>
     */
    static final String IDM_IDENTITY_TYPE_FILTEREDROLE = "filteredrole";
    
    /**
     * Identities type <code>agent<code>
     */
    static final String IDM_IDENTITY_TYPE_AGENT = "agent";
    
    /**
     * Identities properties key <code>count<code>
     */
    static final String IDM_KEY_COUNT = "count";
    
    /**
     * Identities properties key <code>action<code>
     */
    static final String IDM_KEY_ACTION = "action";
    
    /**
     * Identities properties key <code>description<code>
     */
    static final String IDM_KEY_DESCRIPTION = "description";
    
    /**
     * Identities properties key <code>realm<code>
     */
    static final String IDM_KEY_REALM_NAME = "realm";
    
    /**
     * Identities properties key <code>name<code>
     */
    static final String IDM_KEY_IDENTITY_NAME = "name";
    
    /**
     * Identities properties key <code>attributes<code>
     */
    static final String IDM_KEY_IDENTITY_ATTR = "attributes";
    
    /**
     * Identities properties key <code>idtype<code>
     */
    static final String IDM_KEY_IDENTITY_TYPE = "type";
    
    /**
     * Identities properties key <code>member_name<code>
     */
    static final String IDM_KEY_IDENTITY_MEMBER_NAME = "member_name";
    
    /**
     * Identities properties key <code>member_idtype<code>
     */
    static final String IDM_KEY_IDENTITY_MEMBER_TYPE = "member_type";
    
    /**
     * Identities properties key <code>expected_error_code<code>
     */
    static final String IDM_KEY_EXPECTED_ERROR_CODE = "expected_error_code";
    
    /**
     * Identities properties key <code>expected_result<code>
     */
    static final String IDM_KEY_EXPECTED_RESULT = "expected_result";
    
    /**
     * Identities properties key <code>expected_error_message<code>
     */
    static final String IDM_KEY_EXPECTED_ERROR_MESSAGE = 
            "expected_error_message";

    /**
     * Identities properties key <code>separate character<code>
     */
    static final String IDM_KEY_SEPARATE_CHARACTER = ";";
}

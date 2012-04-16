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
 * $Id: DelegationConstants.java,v 1.3 2008/04/25 14:57:46 kanduls Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.delegation;

import com.sun.identity.qatest.idm.IDMConstants;

/**
 * <code>DelegationConstants</code> is an interface which contains 
 * keys used in DelegationTest.properites file and some constants.
 */
public interface DelegationConstants extends IDMConstants {
    
    /**
     * Key to hold policy config data file name <code>policy_file_name<code>
     */
    static final String POLICY_FILE_NAME = "policy_file_name";
     
    /**
     * Key to hold referral policy config data file name
     * <code>referral_policy_file_name<code>
     */
    static final String REFERRAL_POLICY_FILE_NAME ="referral_policy_file_name";
    
    /**
     * Key to hold global config policy data file name
     * <code>global_policy_file_name<code>
     */
    static final String GLOBAL_POLICY_FILE_NAME = "global_policy_file_name";
    
    /**
     * Key to hold policy config no <code>policy_config_no<code>
     */
    static final String POLICY_CONFIG_NO = "policy_config_no";
    
    /**
     * Key to hold reference policy config no <code>ref_policy_config_no<code>
     */
    static final String REF_POLICY_CONFIG_NO = "ref_policy_config_no";
    
    /**
     * Attribute delimiter
     */
    static final String ATTR_DELIM = "|";
    
    /**
     * Attribute Value delimiter
     */
    static final String VAL_DELIM = ",";
    
    /**
     * Key to hold service name <code>service_name<code>
     */
    static final String SERVICE_NAME = "service_name";
    
    /**
     * Key to hold Attribute and values for the service
     * <code>attr_value_pair<code>
     */
    static final String ATTR_VALUE_PAIR = "attr_value_pair";
    
    /**
     * Key to hold datastore config index <code>datastore_config_idx<code>
     */
    static final String DS_CONF_IDX = "datastore_config_idx";
    
    /**
     * Key to hold password <code>password<code>
     */
    static final String IDM_KEY_IDENTITY_PASSWORD = "password";
    
    /**
     * Key to hold privileges <code>privileges<code>
     */
    static final String IDM_KEY_IDENTITY_PRIVILEGES = "privileges";
    
    /**
     * Property that specifies if the test case is negative
     */
    static final String SHOULD_FAIL = "should_fail";
    
    /**
     * Schema type
     */
    static final String SCHEMA_TYPE = "schema_type";
    
    /**
     * Delegation Global properties file name.
     */
    static final String DELEGATION_GLOBAL = "DelegationGlobal";
    
    /**
     * Key to hold assign privileges success msg
     */
     static final String PRIV_ADD_SUCCESS_MSG = "priv-add-success";
     
     /**
      * Key to hold already have privileges
      */
     static final String ALREADY_HAVE_PRIV_MSG = "already-have-priv";
     
     /**
      * Key to hold privileges remove message
      */
     static final String PRIV_REMOVE_SUCCESS_MSG = "remove-priv";
     
     /**
      * Key to hold realm delete messsage
      */
     static final String REALM_DEL_MSG = "realm-del";
     
}
